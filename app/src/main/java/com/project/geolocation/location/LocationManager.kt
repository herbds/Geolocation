package com.project.geolocation.location

import android.content.Context
import android.location.Location
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.location.*

class LocationManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    var currentLocation by mutableStateOf<Location?>(null)
        private set

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            currentLocation = result.lastLocation
        }
    }

    fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 10000L
        ).build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Toast.makeText(context, "Error de permisos: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // Function to get location without cache
    fun requestFreshLocation(callback: (Location?) -> Unit) {
        val freshLocationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 0L
        ).apply {
            setMinUpdateDistanceMeters(0f)
            setMaxUpdateDelayMillis(3000L)  // Waiting time 3 seconds max
        }.build()

        val freshLocationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val freshLocation = result.lastLocation
                if (freshLocation != null) {
                    currentLocation = freshLocation  // Update location
                }
                callback(freshLocation)

                // Important: Remove callback after get location
                fusedLocationClient.removeLocationUpdates(this)
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) {
                    callback(null)
                    fusedLocationClient.removeLocationUpdates(this)
                }
            }
        }

        try {
            // Clean cache before request location
            fusedLocationClient.flushLocations()

            fusedLocationClient.requestLocationUpdates(
                freshLocationRequest,
                freshLocationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Toast.makeText(context, "Error de permisos: ${e.message}", Toast.LENGTH_SHORT).show()
            callback(null)
        }
    }
}