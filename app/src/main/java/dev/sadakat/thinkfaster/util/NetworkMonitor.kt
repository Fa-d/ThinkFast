package dev.sadakat.thinkfaster.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * NetworkMonitor - Monitors network connectivity
 * Phase 4: Sync Worker & Background Sync
 * 
 * Provides reactive network state monitoring via Flow
 */
class NetworkMonitor(private val context: Context) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    /**
     * Check if network is currently available
     */
    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Check if connected to WiFi
     */
    fun isWiFiConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
    
    /**
     * Check if connected to cellular
     */
    fun isCellularConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }
    
    /**
     * Get network type (WiFi, Cellular, None)
     */
    fun getNetworkType(): NetworkType {
        if (!isNetworkAvailable()) return NetworkType.NONE
        
        return when {
            isWiFiConnected() -> NetworkType.WIFI
            isCellularConnected() -> NetworkType.CELLULAR
            else -> NetworkType.OTHER
        }
    }
    
    /**
     * Observe network connectivity changes as a Flow
     */
    fun observeNetworkStatus(): Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkStatus.Available(getNetworkType()))
            }
            
            override fun onLost(network: Network) {
                trySend(NetworkStatus.Unavailable)
            }
            
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                
                if (hasInternet && isValidated) {
                    trySend(NetworkStatus.Available(getNetworkType()))
                } else {
                    trySend(NetworkStatus.Unavailable)
                }
            }
        }
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, callback)
        
        // Emit current state
        if (isNetworkAvailable()) {
            trySend(NetworkStatus.Available(getNetworkType()))
        } else {
            trySend(NetworkStatus.Unavailable)
        }
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
    
    /**
     * Network status sealed class
     */
    sealed class NetworkStatus {
        data class Available(val type: NetworkType) : NetworkStatus()
        data object Unavailable : NetworkStatus()
    }
    
    /**
     * Network type enum
     */
    enum class NetworkType {
        WIFI,
        CELLULAR,
        OTHER,
        NONE
    }
}
