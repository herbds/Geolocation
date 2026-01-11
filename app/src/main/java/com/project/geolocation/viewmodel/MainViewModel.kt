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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // 🟢 ESTADO PARA LA UI (Compose)
    var isTransmitting by mutableStateOf(false)
        private set

    // 🟢 ESTADO PARA LA ACTIVITY (Foreground Service)
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning = _isServiceRunning.asStateFlow()

    var pendingDestination by mutableStateOf<PendingDestination?>(null)
        private set

    private var destinationPollingJob: Job? = null

    companion object {
        private const val DESTINATION_POLL_INTERVAL = 10000L // 10 seconds
        private const val ARRIVAL_THRESHOLD_METERS = 50.0 // 🔴 Radio de llegada (50 metros)
    }


    init {
        hasLocationPermission = permissionManager.hasLocationPermission

        locationManager.locationCallback = { location ->
            currentLocation = location
            checkArrival(location)
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

        startDestinationPolling()
    }

    fun startTransmission() {
        if (!hasLocationPermission) {
            android.util.Log.w("MainViewModel", "No location permission")
            return
        }

        // 1. Activamos la transmisión local
        isTransmitting = true

        // 2. Avisamos a la MainActivity que debe iniciar el Foreground Service
        _isServiceRunning.value = true

        locationManager.startLocationUpdates()
        android.util.Log.d("MainViewModel", "📡 Transmission started")
    }
    // 🔴 Función para calcular distancia y completar viaje
    private fun checkArrival(location: Location) {
        val destination = pendingDestination ?: return

        // Creamos un objeto Location para el destino para usar distanceTo
        val destLoc = Location("service").apply {
            latitude = destination.latitude
            longitude = destination.longitude
        }

        val distanceInMeters = location.distanceTo(destLoc)

        if (distanceInMeters <= ARRIVAL_THRESHOLD_METERS) {
            android.util.Log.d("MainViewModel", "🏁 ¡Destino alcanzado! Distancia: $distanceInMeters m")
            completeTrip()
        }
    }
    private fun completeTrip() {
        // 1. Limpiamos el destino actual
        clearDestination()
    }
    
    fun stopTransmission() {
        // 1. Apagamos la transmisión local
        isTransmitting = false

        // 2. Avisamos a la MainActivity que debe detener el servicio y la notificación
        _isServiceRunning.value = false

        locationManager.stopLocationUpdates()
        android.util.Log.d("MainViewModel", "🛑 Transmission stopped")
    }

    fun startDestinationPolling() {
        if (destinationPollingJob?.isActive == true) return

        destinationPollingJob = viewModelScope.launch {
            while (true) {
                try {
                    val destination = networkManager.fetchPendingDestination()
                    if (destination != null && destination != pendingDestination) {
                        pendingDestination = destination
                    } else if (destination == null && pendingDestination != null) {
                        pendingDestination = null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "🎯 Error polling: ${e.message}")
                }
                delay(DESTINATION_POLL_INTERVAL)
            }
        }
    }

    fun stopDestinationPolling() {
        destinationPollingJob?.cancel()
        destinationPollingJob = null
    }

    fun clearDestination() {
        pendingDestination = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTransmission()
        stopDestinationPolling()
    }
}