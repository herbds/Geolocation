package com.project.geolocation

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.project.geolocation.location.LocationManager
import com.project.geolocation.network.NetworkManager
import com.project.geolocation.permissions.PermissionManager
import com.project.geolocation.security.SecureTokenManager
import com.project.geolocation.service.LocationService
import com.project.geolocation.ui.navigation.AppNavigation
import com.project.geolocation.ui.theme.GeolocationTheme
import com.project.geolocation.viewmodel.AuthViewModel
import com.project.geolocation.viewmodel.AuthViewModelFactory
import com.project.geolocation.viewmodel.MainViewModel
import com.project.geolocation.viewmodel.MainViewModelFactory 

class MainActivity : ComponentActivity() {
    private lateinit var permissionManager: PermissionManager
    private lateinit var locationManager: LocationManager
    private lateinit var networkManager: NetworkManager
    private lateinit var secureTokenManager: SecureTokenManager
    private lateinit var authViewModel: AuthViewModel
    private lateinit var mainViewModel: MainViewModel

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

        val authFactory = AuthViewModelFactory(networkManager, secureTokenManager)
        authViewModel = ViewModelProvider(this, authFactory)[AuthViewModel::class.java]

        val mainFactory = MainViewModelFactory(locationManager, networkManager, permissionManager)
        mainViewModel = ViewModelProvider(this, mainFactory)[MainViewModel::class.java]

        setContent {
            GeolocationTheme {
                AppNavigation(
                    authViewModel = authViewModel,
                    mainViewModel = mainViewModel
                )
            }
        }
    }

    private fun initializeManagers() {
        secureTokenManager = SecureTokenManager(applicationContext)
        
        networkManager = NetworkManager(applicationContext, secureTokenManager)
        
        locationManager = LocationManager(this)

        permissionManager = PermissionManager(
            activity = this,
            onLocationPermissionResult = { granted ->
                if (granted) {
                    Toast.makeText(this, "Permiso de ubicación concedido", Toast.LENGTH_SHORT).show()
                    requestBackgroundLocationPermission()
                } else {
                    Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
        
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (permissionManager.hasLocationPermission) {
                backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    }

    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        stopService(serviceIntent)
    }
}
