package com.example.vista.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

object NetworkUtils {
    /** Inspect the route Android will actually use, including captive-portal validation. */
    fun isConnected(context: Context): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        return isValidated(manager?.getNetworkCapabilities(manager.activeNetwork))
    }

    private fun isValidated(capabilities: NetworkCapabilities?): Boolean =
        capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    fun observeConnectivity(context: Context): Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                // A default route can be available before validation (hotel/login Wi-Fi).
                if (network == connectivityManager.activeNetwork) trySend(isValidated(capabilities))
            }

            override fun onLost(network: Network) {
                // Losing the old Wi-Fi route must not hide a working cellular replacement.
                trySend(isConnected(context))
            }

            override fun onUnavailable() {
                trySend(isConnected(context))
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)
        trySend(isConnected(context))

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}
