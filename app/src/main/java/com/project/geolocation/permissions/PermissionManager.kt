package com.project.geolocation.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat

class PermissionManager(
    private val activity: ComponentActivity,
    private val onLocationPermissionResult: (Boolean) -> Unit
) {
    var hasLocationPermission by mutableStateOf(false)
        private set

    var hasBackgroundPermission by mutableStateOf(false)
        private set

    // 1. Launcher para ubicación normal (Fine/Coarse)
    private val locationPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        hasLocationPermission = fineLocation || coarseLocation

        // Si aceptó lo normal, procedemos a avisar a la Activity
        onLocationPermissionResult(hasLocationPermission)
    }

    // 2. Launcher para ubicación en segundo plano (Background)
    private val backgroundPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasBackgroundPermission = isGranted
    }

    fun checkPermissions() {
        val fine = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        hasLocationPermission = fine || coarse

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hasBackgroundPermission = ContextCompat.checkSelfPermission(
                activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            hasBackgroundPermission = hasLocationPermission
        }
    }

    fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    fun requestBackgroundLocationPermission() {
        // En Android 11+ (API 30), el usuario DEBE ser enviado a los ajustes.
        // El sistema mostrará un diálogo que lo lleva allá automáticamente.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }
}