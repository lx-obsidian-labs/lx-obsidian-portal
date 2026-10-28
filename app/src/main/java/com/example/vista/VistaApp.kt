package com.example.vista

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltAndroidApp
class VistaApp : Application(), ImageLoaderFactory, Configuration.Provider {

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var preferencesManager: com.example.vista.data.local.PreferencesManager

    /**
     * Required for constructor injection in Workers, and it works only because the default
     * WorkManagerInitializer is removed from the manifest - see there.
     */
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    /**
     * Process-lifetime scope for start-up work. Held as a field rather than created per call so the
     * job is not left dangling on an object that outlives everything in the process anyway.
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        initAds()
        setupMessaging()
        scheduleMovieOfTheDay()
        scheduleCacheMaintenance()
    }

    private fun scheduleCacheMaintenance() {
        appScope.launch {
            runCatching {
                com.example.vista.worker.CacheMaintenanceScheduler.schedule(this@VistaApp)
            }
        }
    }

    /**
     * Registers the daily suggestion on app start.
     *
     * Re-registered every launch rather than only on the settings toggle, because the schedule is
     * lost on app update, on uninstall, and when a user clears app data - all of which are exactly
     * when this needs to be re-asserted. [MovieOfTheDayScheduler] uses a KEEP policy, so this is
     * cheap and does not push the next run out.
     *
     * Safe to call this early: Hilt has already injected [workerFactory] by the time onCreate runs,
     * and WorkManager initialises on demand from [workManagerConfiguration].
     */
    private fun scheduleMovieOfTheDay() {
        appScope.launch {
            runCatching {
                val enabled = preferencesManager.movieOfTheDayEnabled.first()
                com.example.vista.motd.MovieOfTheDayScheduler.schedule(this@VistaApp, enabled)
            }
        }
    }

    /**
     * Creates the announcement channel up-front (so messages displayed by the system while the
     * app is backgrounded have a valid default channel) and puts this install on the broadcast
     * topics the Firebase CLI sends announcements to.
     *
     * Two topics, and both stay subscribed. [VistaMessagingService.TOPIC_GLOBAL] is the one every
     * 1.0.5+ install already has and is what to send to when the whole audience should hear
     * something. The market topic narrows a send to South Africa or everyone else - it is additive,
     * so an install that has not yet been updated still receives global broadcasts, and no message
     * is ever sent to both topics at once.
     */
    private fun setupMessaging() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                getSystemService(android.app.NotificationManager::class.java)
                    ?.createNotificationChannel(
                        android.app.NotificationChannel(
                            com.example.vista.data.remote.firebase.VistaMessagingService.CHANNEL_ID,
                            "Updates",
                            android.app.NotificationManager.IMPORTANCE_HIGH
                        ).apply { description = "New Vista Cinema releases and announcements" }
                    )
            }
            com.google.firebase.messaging.FirebaseMessaging.getInstance()
                .subscribeToTopic(com.example.vista.data.remote.firebase.VistaMessagingService.TOPIC_GLOBAL)
        } catch (_: Exception) {
            // Best effort: a device without Play services still runs; the in-app updater covers it.
        }

        // Reading the persisted country is a DataStore hit, so it cannot happen on the main thread
        // during cold start.
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val market = com.example.vista.util.CountryResolver.marketTopic(
                    preferencesManager.ensureCountryCode()
                )
                com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic(market)
            } catch (_: Exception) {
                // Best effort, same as the global subscribe above.
            }
        }
    }

    /** AdMob init can block the main thread for seconds; defer it so cold start stays fast.
     *  MobileAds.initialize runs off the main thread, but AppOpenAd.load/InterstitialAd.load
     *  MUST be called on the main thread. */
    private fun initAds() {
        try {
            val isTv = packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
            if (!isTv) {
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    try {
                        MobileAds.initialize(this@VistaApp)
                    } finally {
                        withContext(Dispatchers.Main) {
                            try {
                                com.example.vista.util.AdManager.loadAppOpenAd(this@VistaApp)
                            } catch (_: Exception) {
                                // Never crash on ad load failure
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Never crash on ads init failure
        }
    }

    override fun newImageLoader(): ImageLoader = imageLoader
}