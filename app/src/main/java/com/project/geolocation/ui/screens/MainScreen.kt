package com.project.geolocation.ui.screens

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAF5FF))
            .padding(16.dp)
    ) {
        // Título
        Text(
            text = "Sistema de Geolocalización",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4C1D95),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Botones
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onStartTransmission,
                enabled = !isTransmitting,
                modifier = Modifier.weight(1f)
            ) {
                Text(if(hasLocationPermission) "Iniciar" else "Permiso")
            }
            Button(
                onClick = onStopTransmission,
                enabled = isTransmitting,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Detener")
            }
        }

        // 🗺️ MAPA - OCUPA TODO EL ESPACIO DISPONIBLE
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // ✅ Esto hace que ocupe el espacio vertical disponible
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Mapa en Tiempo Real",
                    modifier = Modifier.padding(8.dp),
                    fontWeight = FontWeight.Bold
                )

                // ✅ WebView del mapa
                LeafletMapWebView(
                    currentLocation = currentLocation,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Datos de ubicación (abajo)
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
