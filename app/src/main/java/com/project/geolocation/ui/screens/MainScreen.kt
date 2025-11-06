package com.project.geolocation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF5FF))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Sistema de Geolocalización",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4C1D95),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            TextButton(onClick = onLogout) {
                Text("Salir")
            }
        }

        // Show destination info if available
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
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "🎯 Destino Asignado",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534)
                        )
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
                    TextButton(
                        onClick = { mainViewModel.clearDestination() }
                    ) {
                        Text("✖ Limpiar")
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { mainViewModel.startTransmission() },
                enabled = !isTransmitting,
                modifier = Modifier.weight(1f)
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

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Mapa en Tiempo Real",
                    modifier = Modifier.padding(8.dp),
                    fontWeight = FontWeight.Bold
                )
                LeafletMapWebView(
                    currentLocation = currentLocation,
                    pendingDestination = pendingDestination,
                    onDestinationCleared = { mainViewModel.clearDestination() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Latitud: ${currentLocation?.latitude ?: "---"}", fontSize = 14.sp)
                Text("Longitud: ${currentLocation?.longitude ?: "---"}", fontSize = 14.sp)
                val time = currentLocation?.time?.let {
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it))
                } ?: "---"
                Text("Hora: $time", fontSize = 14.sp)
            }
        }
    }
}
