package com.example.vista.util

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {

    // ==================== App Open Ad ====================
    private var appOpenAd: AppOpenAd? = null
    private var appOpenAdShownThisSession = false
    private const val APP_OPEN_AD_UNIT_ID = "ca-app-pub-8968817915668565/8390544721"

    fun loadAppOpenAd(context: Context) {
        if (appOpenAd != null) return

        val adRequest = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            APP_OPEN_AD_UNIT_ID,
            adRequest,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenAd = null
                }
            }
        )
    }

    fun showAppOpenAd(activity: Activity, onAdDismissed: () -> Unit = {}) {
        if (appOpenAdShownThisSession || appOpenAd == null) {
            onAdDismissed()
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                appOpenAdShownThisSession = true
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                appOpenAd = null
                onAdDismissed()
            }
        }
        appOpenAd?.show(activity)
    }

    // ==================== Interstitial Ad ====================
    private var interstitialAd: InterstitialAd? = null
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-8968817915668565/8390544721"

    // Strategic Ad Delivery Controls.
    // The cadence is mode-dependent: guests see interstitials more often than signed-in viewers.
    // GuestSessionManager owns the numbers so the guest policy lives in one place.
    private var lastShownTimestamp: Long = System.currentTimeMillis()
    private var guestMode: Boolean = false

    private val minIntervalMs: Long
        get() = if (guestMode) GuestSessionManager.GUEST_AD_INTERVAL_MS
        else GuestSessionManager.MEMBER_AD_INTERVAL_MS

    private val sessionAdLimit: Int
        get() = if (guestMode) GuestSessionManager.GUEST_AD_SESSION_LIMIT
        else GuestSessionManager.MEMBER_AD_SESSION_LIMIT

    private var adsShownInSession = 0

    /** Switches the interstitial cadence between guest and signed-in policies. */
    fun setGuestMode(isGuest: Boolean) {
        if (guestMode == isGuest) return
        guestMode = isGuest
        // Start the new policy from a clean slate rather than inheriting the other mode's timing.
        lastShownTimestamp = System.currentTimeMillis()
        adsShownInSession = 0
    }

    /** Resets per-session counters. Used when a guest becomes a member. */
    fun resetSession() {
        lastShownTimestamp = System.currentTimeMillis()
        adsShownInSession = 0
        appOpenAdShownThisSession = false
    }

    fun loadInterstitialAd(context: Context) {
        if (interstitialAd != null) return

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit = {}) {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastAd = currentTime - lastShownTimestamp

        val shouldShow = interstitialAd != null &&
                        timeSinceLastAd >= minIntervalMs &&
                        adsShownInSession < sessionAdLimit

        if (shouldShow) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    lastShownTimestamp = System.currentTimeMillis()
                    adsShownInSession++
                    loadInterstitialAd(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    onAdDismissed()
                }
            }
            interstitialAd?.show(activity)
        } else {
            onAdDismissed()
        }
    }
}
