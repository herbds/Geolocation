package com.project.geolocation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PermissionButtons(
    hasLocationPermission: Boolean,
    hasSmsPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Estado de permisos
        Text("Permiso ubicación: ${if (hasLocationPermission) "✓" else "✗"}")
        Text("Permiso SMS: ${if (hasSmsPermission) "✓" else "✗"}")

        Spacer(modifier = Modifier.height(10.dp))

        // Button to request location permissions
        Button(onClick = onRequestLocationPermission) {
            Text("Pedir permiso ubicación")
        }

        // Button to request SMS permission
        Button(onClick = onRequestSmsPermission) {
            Text("Pedir permiso SMS")
        }
    }
}