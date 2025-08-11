package com.project.geolocation.ui.screens

import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.geolocation.ui.components.LocationDisplay
import com.project.geolocation.ui.components.PermissionButtons
import com.project.geolocation.ui.components.PhoneNumberInput

@Composable
fun MainScreen(
    currentLocation: Location?,
    hasLocationPermission: Boolean,
    hasSmsPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onGetLocation: () -> Unit,
    onSendSMS: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var phoneNumber by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text("Enviar mi ubicación por SMS")

        Spacer(modifier = Modifier.height(20.dp))

        PermissionButtons(
            hasLocationPermission = hasLocationPermission,
            hasSmsPermission = hasSmsPermission,
            onRequestLocationPermission = onRequestLocationPermission,
            onRequestSmsPermission = onRequestSmsPermission
        )

        Spacer(modifier = Modifier.height(20.dp))

        PhoneNumberInput(
            phoneNumber = phoneNumber,
            onPhoneNumberChange = { phoneNumber = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

        LocationDisplay(
            currentLocation = currentLocation,
            hasLocationPermission = hasLocationPermission,
            onGetLocation = onGetLocation
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                onSendSMS(phoneNumber)
            },
            enabled = currentLocation != null && phoneNumber.isNotEmpty() && hasSmsPermission
        ) {
            Text("Enviar SMS")
        }
    }
}