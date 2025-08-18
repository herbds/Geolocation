package com.project.geolocation.ui.screens

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

@Composable
fun MainScreen(
    currentLocation: Location?,
    hasLocationPermission: Boolean,
    onGetLocation: () -> Unit,
    onSendLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAF5FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Sistema de Geolocalización",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4C1D95),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Envío de ubicación por Red",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF4C1D95),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        hasLocationPermission = hasLocationPermission
                    )

                    LocationDisplay(
                        currentLocation = currentLocation,
                        onGetLocation = onGetLocation
                    )

                    Button(
                        onClick = { onSendLocation() },
                        enabled = currentLocation != null && hasLocationPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4C1D95),
                            disabledContainerColor = Color(0xFFD1D5DB)
                        )
                    ) {
                        Text(
                            text = "Enviar Ubicación a Oliver",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
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
        hasLocationPermission = true,
        onGetLocation = { },
        onSendLocation = { }
    )
}