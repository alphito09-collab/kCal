package com.alphito.kcal.core.common.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

interface NetworkMonitor {
    val isOnline: StateFlow<Boolean>
}

/**
 * Connectivity as a cold callback flow shared into a hot StateFlow on the application scope.
 * Uses `registerNetworkCallback` (the modern replacement for the CONNECTIVITY_ACTION broadcast).
 */
class ConnectivityNetworkMonitor(context: Context, appScope: CoroutineScope) : NetworkMonitor {

    private val connectivityManager = context.getSystemService<ConnectivityManager>()

    override val isOnline: StateFlow<Boolean> = callbackFlow {
        val manager = connectivityManager
        if (manager == null) {
            trySend(true); close(); return@callbackFlow
        }
        val networks = mutableSetOf<Network>()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                networks += network; trySend(true)
            }

            override fun onLost(network: Network) {
                networks -= network; trySend(networks.isNotEmpty())
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        manager.registerNetworkCallback(request, callback)
        trySend(manager.isCurrentlyConnected())
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }
        .conflate()
        .distinctUntilChanged()
        .stateIn(appScope, SharingStarted.WhileSubscribed(5_000), initialValue = true)

    private fun ConnectivityManager.isCurrentlyConnected(): Boolean =
        getNetworkCapabilities(activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
}
