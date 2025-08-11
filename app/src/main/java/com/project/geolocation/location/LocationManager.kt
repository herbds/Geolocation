package com.project.geolocation.location

import android.content.Context
import android.location.Location
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.*
import com.google.android.gms.location.*

class LocationManager(
    private val context: Context,
    private val hasLocationPermission: () -> Boolean
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    var currentLocation by mutableStateOf<Location?>(null)
        private set

    fun getLocation() {
        if (!hasLocationPermission()) {
            Toast.makeText(context, "No hay permiso de ubicación", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = location
                    Toast.makeText(context, "Ubicación obtenida", Toast.LENGTH_SHORT).show()
                } else {
                    requestNewLocation()
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Error obteniendo ubicación", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Error de permisos: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestNewLocation() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000L
        ).setMaxUpdates(1).build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        currentLocation = result.lastLocation
                        Toast.makeText(context, "Nueva ubicación obtenida", Toast.LENGTH_SHORT).show()
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                },
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Toast.makeText(context, "Error solicitando nueva ubicación: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}