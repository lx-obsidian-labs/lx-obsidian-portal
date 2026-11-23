package com.example.vista.data.playback

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.vista.data.local.PreferencesManager
import com.example.vista.data.session.SessionManager
import com.example.vista.domain.model.CellularStreamingSetting
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class MeteredPlaybackConsentRequired : IllegalStateException("Confirm mobile or metered data use to continue.")

class PlaybackAccess @Inject constructor(
    @ApplicationContext private val context: Context, private val session: SessionManager,
    private val preferences: PreferencesManager
) {
    suspend fun requireAccess(meteredConsent: Boolean = false) {
        check(session.isSessionValid()) { "Your viewing session has ended. Please sign in." }
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val caps = manager?.getNetworkCapabilities(manager.activeNetwork)
        check(caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            "Internet access is unavailable. Check your connection or complete the Wi-Fi sign-in."
        }
        val settings = preferences.settings.first()
        val usesData = manager.isActiveNetworkMetered || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        if (usesData) {
            when (settings.cellularStreaming) {
                CellularStreamingSetting.WIFI_ONLY -> error("Connect to an unmetered Wi-Fi or Ethernet network, or change your streaming preference in Settings.")
                CellularStreamingSetting.ALWAYS_ASK -> if (!meteredConsent) throw MeteredPlaybackConsentRequired()
                CellularStreamingSetting.ALWAYS_ALLOW -> Unit
            }
        }
    }
}
