package com.project.geolocation

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.project.geolocation.location.LocationManager
import com.project.geolocation.network.NetworkManager
import com.project.geolocation.permissions.PermissionManager
import com.project.geolocation.ui.screens.MainScreen
import com.project.geolocation.ui.theme.GeolocationTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var permissionManager: PermissionManager
    private lateinit var locationManager: LocationManager
    private lateinit var networkManager: NetworkManager

    private var isTransmitting by mutableStateOf(false)
    private var transmissionJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeManagers()
        permissionManager.checkPermissions()

        setContent {
            GeolocationTheme {
                MainScreen(
                    currentLocation = locationManager.currentLocation,
                    hasLocationPermission = permissionManager.hasLocationPermission,
                    isTransmitting = isTransmitting,
                    onStartTransmission = { startTransmission() },
                    onStopTransmission = { stopTransmission() }
                )
            }
        }
    }

    private fun startTransmission() {
        if (!permissionManager.hasLocationPermission) {
            Toast.makeText(this, "Debe conceder el permiso de ubicación primero", Toast.LENGTH_LONG).show()
            permissionManager.requestLocationPermission()
            return
        }

        if (isTransmitting) return

        isTransmitting = true
        locationManager.startLocationUpdates()
        Toast.makeText(this, "Iniciando transmisión...", Toast.LENGTH_SHORT).show()

        transmissionJob = lifecycleScope.launch {
            while (isActive) {
                // Pedir ubicación FRESCA justo antes de cada envío
                locationManager.requestFreshLocation { freshLocation ->
                    if (freshLocation != null) {
                        lifecycleScope.launch {
                            networkManager.broadcastLocationUdp(freshLocation)
                        }
                    } else {
                        // Si no se pudo obtener ubicación fresca, usar la última conocida
                        locationManager.currentLocation?.let { location ->
                            lifecycleScope.launch {
                                networkManager.broadcastLocationUdp(location)
                            }
                        }
                    }
                }

                delay(10000L) // Esperar 10 segundos antes del siguiente envío
            }
        }
    }

    private fun stopTransmission() {
        if (!isTransmitting) return

        isTransmitting = false
        locationManager.stopLocationUpdates()
        transmissionJob?.cancel()
        transmissionJob = null
        Toast.makeText(this, "Transmisión detenida.", Toast.LENGTH_SHORT).show()
    }

    private fun initializeManagers() {
        permissionManager = PermissionManager(
            activity = this,
            onLocationPermissionResult = { granted ->
                if (granted) {
                    Toast.makeText(this, "Permiso de ubicación concedido", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_LONG).show()
                }
            }
        )

        locationManager = LocationManager(this)
        networkManager = NetworkManager(this)
    }
}