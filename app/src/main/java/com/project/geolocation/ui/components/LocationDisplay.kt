package com.project.geolocation.ui.components

import android.location.Location
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun LocationDisplay(
    currentLocation: Location?,
    onGetLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onGetLocation,
        modifier = modifier
    ) {
        Text("Obtener ubicación actual")
    }
}

@Preview(showBackground = true)
@Composable
fun LocationDisplayPreview(){
    LocationDisplay(
        currentLocation = null,
        onGetLocation = { }
    )
}
