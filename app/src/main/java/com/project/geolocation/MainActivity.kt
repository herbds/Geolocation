package com.project.geolocation

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.project.geolocation.location.LocationManager
import com.project.geolocation.permissions.PermissionManager
import com.project.geolocation.sms.SmsManager
import com.project.geolocation.ui.screens.MainScreen

class MainActivity : ComponentActivity() {

    private lateinit var permissionManager: PermissionManager
    private lateinit var locationManager: LocationManager
    private lateinit var smsManager: SmsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize managers
        initializeManagers()

        // Check permissions on startup
        permissionManager.checkPermissions()

        setContent {
            MainScreen(
                currentLocation = locationManager.currentLocation,
                hasLocationPermission = permissionManager.hasLocationPermission,
                hasSmsPermission = permissionManager.hasSmsPermission,
                onRequestLocationPermission = {
                    permissionManager.requestLocationPermission()
                },
                onRequestSmsPermission = {
                    permissionManager.requestSmsPermission()
                },
                onGetLocation = {
                    locationManager.getLocation()
                },
                onSendSMS = { phoneNumber ->
                    smsManager.sendSMS(phoneNumber, locationManager.currentLocation)
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
            },
            onSmsPermissionResult = { granted ->
                if (granted) {
                    Toast.makeText(this, "Permiso de SMS concedido", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permiso de SMS denegado", Toast.LENGTH_LONG).show()
                }
            }
        )

        locationManager = LocationManager(
            context = this,
            hasLocationPermission = { permissionManager.hasLocationPermission }
        )

        smsManager = SmsManager(
            context = this,
            hasSmsPermission = { permissionManager.hasSmsPermission }
        )
    }
}