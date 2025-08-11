package com.project.geolocation.ui.components

import android.location.Location
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LocationDisplay(
    currentLocation: Location?,
    hasLocationPermission: Boolean,
    onGetLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Button to get location
        Button(
            onClick = onGetLocation,
            enabled = hasLocationPermission
        ) {
            Text("Obtener ubicación")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Display current location if available
        currentLocation?.let { location ->
            Text("Lat: ${location.latitude}")
            Text("Lng: ${location.longitude}")
        }
    }
}