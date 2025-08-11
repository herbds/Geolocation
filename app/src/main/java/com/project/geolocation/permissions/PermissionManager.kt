package com.project.geolocation.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat

class PermissionManager(
    private val activity: ComponentActivity,
    private val onLocationPermissionResult: (Boolean) -> Unit,
    private val onSmsPermissionResult: (Boolean) -> Unit
) {
    var hasLocationPermission by mutableStateOf(false)
        private set

    var hasSmsPermission by mutableStateOf(false)
        private set

    // Launcher for location permissions
    private val locationPermissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            hasLocationPermission = fineLocation || coarseLocation
            onLocationPermissionResult(hasLocationPermission)
        }

    // Launcher for SMS permission
    private val smsPermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasSmsPermission = isGranted
            onSmsPermissionResult(isGranted)
        }

    fun checkPermissions() {
        hasLocationPermission = (
                ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                )

        hasSmsPermission = ContextCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

    fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    fun requestSmsPermission() {
        smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
    }
}