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

    var isTransmitting by mutableStateOf(false)
        private set

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning = _isServiceRunning.asStateFlow()

    var pendingDestination by mutableStateOf<PendingDestination?>(null)
        private set

    // Estado para mostrar mensaje de llegada
    var arrivedAtDestination by mutableStateOf(false)
        private set

    private var destinationPollingJob: Job? = null

    companion object {
        private const val TAG = "MainViewModel"
        private const val DESTINATION_POLL_INTERVAL = 10000L
        private const val ARRIVAL_THRESHOLD_METERS = 50.0
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
                        android.util.Log.e(TAG, "Error broadcasting location", e)
                    }
                }
            }
        }

        startDestinationPolling()
    }

    fun startTransmission() {
        if (!hasLocationPermission) {
            android.util.Log.w(TAG, "No location permission")
            return
        }

        isTransmitting = true
        _isServiceRunning.value = true

        locationManager.startLocationUpdates()
        android.util.Log.d(TAG, "📡 Transmission started")
    }

    private fun checkArrival(location: Location) {
        val destination = pendingDestination ?: return

        val destLoc = Location("destination").apply {
            latitude = destination.latitude
            longitude = destination.longitude
        }

        val distanceInMeters = location.distanceTo(destLoc)

        if (distanceInMeters <= ARRIVAL_THRESHOLD_METERS) {
            android.util.Log.d(TAG, "🏁 ¡Destino alcanzado! Distancia: $distanceInMeters m")
            completeTrip()
        }
    }

    private fun completeTrip() {
        viewModelScope.launch {
            try {
                // 1. Notificar al servidor
                val success = networkManager.completeDestination()

                if (success) {
                    android.util.Log.d(TAG, "🏁 Viaje completado exitosamente")
                    arrivedAtDestination = true
                }

                // 2. Inmediatamente buscar el siguiente destino
                val nextDestination = networkManager.fetchPendingDestination()

                if (nextDestination != null) {
                    // Hay otro destino pendiente → asignarlo directo
                    android.util.Log.d(TAG, "🎯 Siguiente destino: ${nextDestination.buildingName}")
                    pendingDestination = nextDestination
                } else {
                    // No hay más destinos → limpiar
                    clearDestination()
                }

                // 3. Ocultar mensaje de llegada después de 2 segundos (no bloquea)
                if (arrivedAtDestination) {
                    delay(2000)
                    arrivedAtDestination = false
                }

            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error completando viaje: ${e.message}")
                clearDestination()
            }
        }
    }

    fun stopTransmission() {
        isTransmitting = false
        _isServiceRunning.value = false

        locationManager.stopLocationUpdates()
        android.util.Log.d(TAG, "🛑 Transmission stopped")
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
                    android.util.Log.e(TAG, "🎯 Error polling: ${e.message}")
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

    fun dismissArrivalMessage() {
        arrivedAtDestination = false
    }

    override fun onCleared() {
        super.onCleared()
        stopTransmission()
        stopDestinationPolling()
    }
}