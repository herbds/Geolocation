package com.project.geolocation

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.project.geolocation.location.LocationManager
import com.project.geolocation.network.NetworkManager
import com.project.geolocation.permissions.PermissionManager
import com.project.geolocation.service.LocationService
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

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Permiso de notificaciones concedido", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Permiso de ubicación en segundo plano concedido", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permiso de ubicación en segundo plano denegado. La app funcionará solo en primer plano.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeManagers()
        permissionManager.checkPermissions()
        requestNotificationPermission()
        requestBackgroundLocationPermission()

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

        // ✅ NUEVO: Iniciar el servicio en segundo plano
        startLocationService()

        // ✅ MANTENER: También iniciar actualizaciones en la UI para el mapa
        locationManager.startLocationUpdates()

        Toast.makeText(this, "Servicio de ubicación iniciado en segundo plano", Toast.LENGTH_SHORT).show()

        transmissionJob = lifecycleScope.launch {
            while (isActive) {
                locationManager.requestFreshLocation { freshLocation ->
                    if (freshLocation != null) {
                        lifecycleScope.launch {
                            networkManager.broadcastLocationUdp(freshLocation)
                        }
                    } else {
                        locationManager.currentLocation?.let { location ->
                            lifecycleScope.launch {
                                networkManager.broadcastLocationUdp(location)
                            }
                        }
                    }
                }
                delay(10000L)
            }
        }
    }

    private fun stopTransmission() {
        if (!isTransmitting) return

        isTransmitting = false

        // ✅ NUEVO: Detener el servicio en segundo plano
        stopLocationService()

        locationManager.stopLocationUpdates()
        transmissionJob?.cancel()
        transmissionJob = null

        Toast.makeText(this, "Servicio de ubicación detenido.", Toast.LENGTH_SHORT).show()
    }

    // ✅ NUEVO: Función para iniciar el servicio
    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    // ✅ NUEVO: Función para detener el servicio
    private fun stopLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        stopService(serviceIntent)
    }

    // ✅ NUEVO: Solicitar permiso de notificaciones
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ✅ NUEVO: Solicitar permiso de ubicación en background
    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (permissionManager.hasLocationPermission) {
                backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    }

    private fun initializeManagers() {
        permissionManager = PermissionManager(
            activity = this,
            onLocationPermissionResult = { granted ->
                if (granted) {
                    Toast.makeText(this, "Permiso de ubicación concedido", Toast.LENGTH_SHORT).show()
                    // ✅ NUEVO: Después de conceder ubicación, pedir background
                    requestBackgroundLocationPermission()
                } else {
                    Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_LONG).show()
                }
            }
        )

        locationManager = LocationManager(this)
        networkManager = NetworkManager(this)
    }
}