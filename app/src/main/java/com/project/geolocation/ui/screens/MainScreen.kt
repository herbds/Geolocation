package com.project.geolocation.ui.screens

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // BACKGROUND 
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAF5FF)) // 0xFFFAF5FF
    ) {
        // CONTENEDOR PRINCIPAL
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Principal Tittle
            Text(
                text = "Sistema de Geolocalización",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4C1D95), // 0xFF4C1D95
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Subtittle
            Text(
                text = "Envío de ubicación por SMS",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF4C1D95), // 0xFF4C1D95
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Content card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(12.dp)), // Sombra más sutil
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {

                    PermissionButtons(
                        hasLocationPermission = hasLocationPermission,
                        hasSmsPermission = hasSmsPermission,
                        onRequestLocationPermission = onRequestLocationPermission,
                        onRequestSmsPermission = onRequestSmsPermission
                    )

                    PhoneNumberInput(
                        phoneNumber = phoneNumber,
                        onPhoneNumberChange = { phoneNumber = it }
                    )

                    LocationDisplay(
                        currentLocation = currentLocation,
                        onGetLocation = onGetLocation
                    )

                    // Send button
                    Button(
                        onClick = { onSendSMS(phoneNumber) },
                        enabled = currentLocation != null && phoneNumber.isNotEmpty() && hasSmsPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp), // Altura estándar
                        shape = RoundedCornerShape(8.dp), // Bordes menos redondeados
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB), // Azul profesional
                            disabledContainerColor = Color(0xFFD1D5DB) // Gris claro cuando deshabilitado
                        )
                    ) {
                        Text(
                            text = "Enviar SMS",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }

            // Page foot
            Text(
                text = "Proyecto Uninorte - Geolocalización",
                fontSize = 12.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF9CA3AF),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen(
        currentLocation = null,
        hasLocationPermission = false,
        hasSmsPermission = true,
        onRequestLocationPermission = { },
        onRequestSmsPermission = { },
        onGetLocation = { },
        onSendSMS = { }
    )
}
