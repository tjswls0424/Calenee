package org.jin.calenee.util

import android.content.Context
import android.net.*
import android.os.Build
import androidx.lifecycle.LiveData

class NetworkStatusHelper(private val context: Context) :
    LiveData<Boolean>() {
    private var connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    override fun onActive() {
        super.onActive()
        updateConnection()
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                connectivityManager.registerDefaultNetworkCallback(getConnectivityManagerCallback())
            }

            else -> {
                lollipopNetworkRequest()
            }
        }
    }

    override fun onInactive() {
        super.onInactive()
        connectivityManager.unregisterNetworkCallback(getConnectivityManagerCallback())
    }

    private fun lollipopNetworkRequest() {
        val requestBuilder = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)

        connectivityManager.registerNetworkCallback(
            requestBuilder.build(),
            getConnectivityManagerCallback()
        )
    }

    private fun getConnectivityManagerCallback() : ConnectivityManager.NetworkCallback {
        networkCallback = object: ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                postValue(true)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                postValue(false)
            }
        }

        return networkCallback
    }

    private fun updateConnection() {
        postValue(connectivityManager.isDefaultNetworkActive)
    }
}