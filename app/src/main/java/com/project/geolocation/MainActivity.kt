package com.project.geolocation

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.project.geolocation.location.LocationManager
import com.project.geolocation.network.NetworkManager
import com.project.geolocation.permissions.PermissionManager
import com.project.geolocation.ui.screens.MainScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var permissionManager: PermissionManager
    private lateinit var locationManager: LocationManager
    private lateinit var networkManager: NetworkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeManagers()
        permissionManager.checkPermissions()

        setContent {
            MainScreen(
                currentLocation = locationManager.currentLocation,
                hasLocationPermission = permissionManager.hasLocationPermission,
                onGetLocation = {
                    if (permissionManager.hasLocationPermission) {
                        locationManager.getLocation()
                    } else {
                        permissionManager.requestLocationPermission()
                    }
                },
                onSendLocation = {
                    lifecycleScope.launch {
                        val ip = "152.201.160.241"
                        val port = 5050
                        val identifier = "Oliver Workspace"

                        networkManager.sendLocation(ip, port, locationManager.currentLocation, identifier)
                    }
                }
            )
        }
    }

    private fun initializeManagers() {
        permissionManager = PermissionManager(
            activity = this,
            onLocationPermissionResult = { granted ->
                if (granted) {
                    Toast.makeText(this, "Permiso de ubicación concedido", Toast.LENGTH_SHORT).show()
                    locationManager.getLocation()
                } else {
                    Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_LONG).show()
                }
            }
        )

        locationManager = LocationManager(
            context = this,
            hasLocationPermission = { permissionManager.hasLocationPermission }
        )

        networkManager = NetworkManager(context = this)
    }
}