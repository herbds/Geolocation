package com.project.geolocation.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.geolocation.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onLogout: () -> Unit
) {
    val currentLocation = mainViewModel.currentLocation
    val hasLocationPermission = mainViewModel.hasLocationPermission
    val isTransmitting = mainViewModel.isTransmitting
    val pendingDestination = mainViewModel.pendingDestination

    // ✅ NUEVO: Estado para detección de desviación de ruta
    var isOffRoute by remember { mutableStateOf(false) }
    var offRouteDistance by remember { mutableStateOf(0.0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF5FF))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sistema de Geolocalización",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4C1D95),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            TextButton(onClick = onLogout) {
                Text("Salir", color = Color(0xFFEF4444))
            }
        }

        // ✅ NUEVO: Alerta de desviación de ruta (prioridad alta)
        if (isOffRoute && pendingDestination != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFEE2E2)
                ),
                border = BorderStroke(2.dp, Color(0xFFEF4444))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠️",
                        fontSize = 28.sp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "¡Fuera de ruta!",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B),
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Te desviaste ${offRouteDistance.toInt()} metros",
                            color = Color(0xFF991B1B),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Destino asignado
        if (pendingDestination != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "🎯 Destino Asignado",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )

                            // ✅ NUEVO: Badge de estado de ruta
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Lat: ${String.format("%.6f", pendingDestination.latitude)}",
                            fontSize = 12.sp,
                            color = Color(0xFF166534)
                        )
                        Text(
                            "Lon: ${String.format("%.6f", pendingDestination.longitude)}",
                            fontSize = 12.sp,
                            color = Color(0xFF166534)
                        )
                        Text(
                            pendingDestination.timestamp,
                            fontSize = 11.sp,
                            color = Color(0xFF166534)
                        )
                    }

                }
            }
        }

        // Botones de control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { mainViewModel.startTransmission() },
                enabled = !isTransmitting,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasLocationPermission) Color(0xFF22C55E) else Color(0xFF3B82F6)
                )
            ) {
                Text(if (hasLocationPermission) "Iniciar" else "Permiso")
            }
            Button(
                onClick = { mainViewModel.stopTransmission() },
                enabled = isTransmitting,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Detener")
            }
        }

        // Mapa
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mapa en Tiempo Real",
                        modifier = Modifier.padding(8.dp),
                        fontWeight = FontWeight.Bold
                    )

                    // ✅ NUEVO: Indicador visual en el mapa
                    if (pendingDestination != null) {
                        Surface(
                            color = if (isOffRoute) Color(0xFFEF4444) else Color(0xFF22C55E),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                        }
                    }
                }

                // ✅ ACTUALIZADO: WebView con callback de estado de ruta
                LeafletMapWebView(
                    currentLocation = currentLocation,
                    pendingDestination = pendingDestination,
                    onDestinationCleared = {
                        mainViewModel.clearDestination()
                        isOffRoute = false
                        offRouteDistance = 0.0
                    },
                    onRouteStatusChanged = { offRoute, distance ->
                        isOffRoute = offRoute
                        offRouteDistance = distance
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Datos de ubicación
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "📍 Ubicación Actual",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4C1D95)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Latitud: ${currentLocation?.latitude ?: "---"}", fontSize = 13.sp)
                Text("Longitud: ${currentLocation?.longitude ?: "---"}", fontSize = 13.sp)
                val time = currentLocation?.time?.let {
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it))
                } ?: "---"
                Text("Hora: $time", fontSize = 13.sp)


            }
        }
    }
}