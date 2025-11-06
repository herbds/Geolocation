package com.project.geolocation.location

import android.content.Context
import android.location.Location
import android.os.Looper
import android.widget.Toast
// REMOVED Compose imports - this class should not know about Compose state
import com.google.android.gms.location.*

class LocationManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // ▼▼▼▼▼ FIX 1: REMOVED REDUNDANT COMPOSET STATE ▼▼▼▼▼
    // The ViewModel will be responsible for holding state.
    // var currentLocation by mutableStateOf<Location?>(null)
    //    private set
    // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

    // ▼▼▼▼▼ FIX 2: ADDED THE PUBLIC CALLBACK THE VIEWMODEL NEEDS ▼▼▼▼▼
    /**
     * This is the public callback that MainViewModel will set.
     * It must be a 'var' and must not be 'private'.
     */
    var locationCallback: ((Location) -> Unit)? = null
    // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

    // ▼▼▼▼▼ FIX 3: RENAMED YOUR INTERNAL CALLBACK ▼▼▼▼▼
    private val internalLocationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            // ▼▼▼▼▼ FIX 4: CALL THE PUBLIC CALLBACK ▼▼▼▼▼
            // When Google Play Services gives us a location,
            // pass it to whoever is listening (our ViewModel).
            result.lastLocation?.let {
                locationCallback?.invoke(it)
            }
            // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲
        }
    }

    fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 10000L
        ).build()

        try {
            // Use the internal callback for the fused client
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                internalLocationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Toast.makeText(context, "Error de permisos: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopLocationUpdates() {
        // Use the internal callback to stop updates
        fusedLocationClient.removeLocationUpdates(internalLocationCallback)
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
                // ▼▼▼▼▼ THIS LINE IS NOW FIXED (REMOVED THE '_') ▼▼▼▼▼
                val freshLocation = result.lastLocation
                if (freshLocation != null) {
                    // Also update the main listener
                    locationCallback?.invoke(freshLocation)
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