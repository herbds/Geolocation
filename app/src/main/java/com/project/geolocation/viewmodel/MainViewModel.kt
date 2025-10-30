package com.project.geolocation.viewmodel

import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.geolocation.location.LocationManager
import com.project.geolocation.network.NetworkManager
import com.project.geolocation.permissions.PermissionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(
    private val locationManager: LocationManager,
    private val networkManager: NetworkManager,
    private val permissionManager: PermissionManager
) : ViewModel() {

    var isTransmitting by mutableStateOf(false)
        private set
    var currentLocation by mutableStateOf<Location?>(null)
        private set
    val hasLocationPermission: Boolean
        get() = permissionManager.hasLocationPermission

    private var transmissionJob: Job? = null

    init {
        viewModelScope.launch {
            locationManager.startLocationUpdates()
            viewModelScope.launch {
                while(isActive) {
                    currentLocation = locationManager.currentLocation
                    delay(1000)
                }
            }
        }
    }

    fun startTransmission() {
        if (!hasLocationPermission) {
            permissionManager.requestLocationPermission()
            return
        }
        if (isTransmitting) return

        isTransmitting = true

        locationManager.startLocationUpdates()

        transmissionJob = viewModelScope.launch {
            while (isActive) {
                locationManager.requestFreshLocation { freshLocation ->
                    val locationToSend = freshLocation ?: locationManager.currentLocation
                    
                    if (locationToSend != null) {
                        viewModelScope.launch {
                            networkManager.broadcastLocationUdp(locationToSend)
                        }
                    }
                }
                delay(10000L)
            }
        }
    }

    fun stopTransmission() {
        if (!isTransmitting) return
        isTransmitting = false

        transmissionJob?.cancel()
        transmissionJob = null
    }

    fun requestLocationPermission() {
        permissionManager.requestLocationPermission()
    }

    override fun onCleared() {
        super.onCleared()
        locationManager.stopLocationUpdates()
        transmissionJob?.cancel()
    }
}
