package com.project.geolocation.viewmodel

import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.geolocation.location.LocationManager
import com.project.geolocation.network.NetworkManager
import com.project.geolocation.network.PendingDestination
import com.project.geolocation.permissions.PermissionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel(
    private val locationManager: LocationManager,
    private val networkManager: NetworkManager,
    private val permissionManager: PermissionManager
) : ViewModel() {

    var currentLocation by mutableStateOf<Location?>(null)
        private set

    var hasLocationPermission by mutableStateOf(false)
        private set

    var isTransmitting by mutableStateOf(false)
        private set

    var pendingDestination by mutableStateOf<PendingDestination?>(null)
        private set

    private var destinationPollingJob: Job? = null

    companion object {
        private const val DESTINATION_POLL_INTERVAL = 10000L // 10 seconds
    }

    init {
        hasLocationPermission = permissionManager.hasLocationPermission

        // ▼▼▼ THIS LINE IS NOW CORRECT ▼▼▼
        // It will set the public 'var locationCallback'
        // that you just added to LocationManager.kt
        locationManager.locationCallback = { location ->
            currentLocation = location

            if (isTransmitting) {
                viewModelScope.launch {
                    try {
                        networkManager.broadcastLocationUdp(location)
                    } catch (e: Exception) {
                        android.util.Log.e("MainViewModel", "Error broadcasting location", e)
                    }
                }
            }
        }

        // Start destination polling automatically
        startDestinationPolling()
    }

    fun startTransmission() {
        if (!hasLocationPermission) {
            android.util.Log.w("MainViewModel", "No location permission")
            return
        }

        isTransmitting = true
        locationManager.startLocationUpdates()
        android.util.Log.d("MainViewModel", "📡 Transmission started")
    }

    fun stopTransmission() {
        isTransmitting = false
        locationManager.stopLocationUpdates()
        android.util.Log.d("MainViewModel", "🛑 Transmission stopped")
    }

    /**
     * Start polling for pending destinations
     */
    fun startDestinationPolling() {
        if (destinationPollingJob?.isActive == true) {
            android.util.Log.d("MainViewModel", "🎯 Destination polling already active")
            return
        }

        destinationPollingJob = viewModelScope.launch {
            android.util.Log.d("MainViewModel", "🎯 Starting destination polling (every ${DESTINATION_POLL_INTERVAL/1000}s)")

            while (true) {
                try {
                    val destination = networkManager.fetchPendingDestination()

                    // Only update if it's a new destination (different coordinates or timestamp)
                    if (destination != null && destination != pendingDestination) {
                        pendingDestination = destination
                        android.util.Log.d("MainViewModel", "🎯 New destination received: ${destination.latitude}, ${destination.longitude}")
                    } else if (destination == null && pendingDestination != null) {
                        android.util.Log.d("MainViewModel", "🎯 No more pending destinations")
                        pendingDestination = null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "🎯 Error polling destinations: ${e.message}", e)
                }

                delay(DESTINATION_POLL_INTERVAL)
            }
        }
    }

    /**
     * Stop polling for destinations
     */
    fun stopDestinationPolling() {
        destinationPollingJob?.cancel()
        destinationPollingJob = null
        android.util.Log.d("MainViewModel", "🎯 Destination polling stopped")
    }

    /**
     * Clear current destination
     */
    fun clearDestination() {
        pendingDestination = null
        android.util.Log.d("MainViewModel", "🧹 Destination cleared")
    }

    override fun onCleared() {
        super.onCleared()
        stopTransmission()
        stopDestinationPolling()
    }
}