package com.project.geolocation.ui.screens

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.geolocation.ui.components.PermissionButtons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    currentLocation: Location?,
    hasLocationPermission: Boolean,
    isTransmitting: Boolean,
    onStartTransmission: () -> Unit,
    onStopTransmission: () -> Unit,
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
                    PermissionButtons(hasLocationPermission = hasLocationPermission)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = onStartTransmission,
                            enabled = !isTransmitting,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasLocationPermission) Color(0xFF22C55E) else Color(0xFF3B82F6),
                                disabledContainerColor = Color(0xFFD1D5DB)
                            )
                        ) {
                            Text(
                                if(hasLocationPermission) "Iniciar" else "Pedir Permiso", 
                                color = Color.White, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Button(
                            onClick = onStopTransmission,
                            enabled = isTransmitting,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444),
                                disabledContainerColor = Color(0xFFD1D5DB)
                            )
                        ) {
                            Text("Detener", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    LocationDataDisplay(location = currentLocation)
                }
            }
        }
    }
}

@Composable
fun LocationDataDisplay(location: Location?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Datos de Ubicación a Enviar",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF4C1D95),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF)),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DataRow("Latitud:", location?.latitude?.toString() ?: "Esperando datos...")
                DataRow("Longitud:", location?.longitude?.toString() ?: "Esperando datos...")
                val timeString = location?.time?.let {
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it))
                } ?: "N/A"
                DataRow("Hora:", timeString)
            }
        }
    }
}

@Composable
fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.Medium, color = Color(0xFF581C87))
        Text(text = value, color = Color(0xFF374151))
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen(
        currentLocation = null,
        hasLocationPermission = false,
        isTransmitting = false,
        onStartTransmission = {},
        onStopTransmission = {}
    )
}