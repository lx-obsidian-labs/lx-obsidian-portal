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

class PlaybackAccess @Inject constructor(
    @ApplicationContext private val context: Context, private val session: SessionManager,
    private val preferences: PreferencesManager
) {
    suspend fun requireAccess() {
        check(session.isSessionValid()) { "Your viewing session has ended. Please sign in." }
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val caps = manager?.getNetworkCapabilities(manager.activeNetwork)
        check(caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) { "You're offline. Reconnect to start playback." }
        val settings = preferences.settings.first()
        if (settings.cellularStreaming != CellularStreamingSetting.ALWAYS_ALLOW) {
            check(caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                "Connect to Wi-Fi or allow cellular playback in Settings before continuing."
            }
        }
    }
}
