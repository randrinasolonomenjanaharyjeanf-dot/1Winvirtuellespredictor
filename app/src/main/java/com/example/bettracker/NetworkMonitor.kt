
package com.example.onewinvirtuellepredictor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

    val isOnline: Flow<Boolean> = callbackFlow {

        fun checkConnection() {
            val network =
                connectivityManager.activeNetwork

            val capabilities =
                connectivityManager.getNetworkCapabilities(network)

            val connected =
                capabilities?.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                ) == true &&
                capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )

            trySend(connected)
        }

        val callback =
            object : ConnectivityManager.NetworkCallback() {

                override fun onAvailable(network: Network) {
                    checkConnection()
                }

                override fun onLost(network: Network) {
                    checkConnection()
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities
                ) {
                    checkConnection()
                }
            }

        checkConnection()

        val request = NetworkRequest.Builder()
            .addCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            )
            .build()

        connectivityManager.registerNetworkCallback(
            request,
            callback
        )

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}
