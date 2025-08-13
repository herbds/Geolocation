package com.project.geolocation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {


        Text(
            text = "Administrador de permisos",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF4C1D95),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Permission color state
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFEFCFF) // 0xFFFEFCFF
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Location Permission
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Permiso ubicación:",
                        fontSize = 14.sp,
                        color = Color(0xFF374151)
                    )
                    Text(
                        text = if (hasLocationPermission) "✓ Concedido" else "✗ Denegado",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (hasLocationPermission) Color(0xFF059669) else Color(0xFFDC2626), // Verde o Rojo
                        modifier = Modifier
                            .background(
                                color = if (hasLocationPermission) Color(0xFFD1FAE5) else Color(0xFFFEE2E2), // Fondo verde claro o rojo claro
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // SMS Permission
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Permiso SMS:",
                        fontSize = 14.sp,
                        color = Color(0xFF374151)
                    )
                    Text(
                        text = if (hasSmsPermission) "✓ Concedido" else "✗ Denegado",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (hasSmsPermission) Color(0xFF059669) else Color(0xFFDC2626), // Verde o Rojo
                        modifier = Modifier
                            .background(
                                color = if (hasSmsPermission) Color(0xFFD1FAE5) else Color(0xFFFEE2E2), // Fondo verde claro o rojo claro
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Request permission button
        Button(
            onClick = onRequestLocationPermission,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Solicitar permiso ubicación")
        }

        Button(
            onClick = onRequestSmsPermission,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Solicitar permiso SMS")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionButtonsPreview(){
    PermissionButtons(
        hasLocationPermission = false,   // Uno concedido
        hasSmsPermission = true,       // Uno denegado
        onRequestLocationPermission = { },
        onRequestSmsPermission = { }
    )
}

@Preview(showBackground = true)
@Composable
fun PermissionButtonsAllGrantedPreview(){
    PermissionButtons(
        hasLocationPermission = true,   // Ambos concedidos
        hasSmsPermission = true,
        onRequestLocationPermission = { },
        onRequestSmsPermission = { }
    )
}
