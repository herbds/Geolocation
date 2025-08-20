package com.project.geolocation.ui.screens

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.geolocation.ui.components.LocationDisplay
import com.project.geolocation.ui.components.PermissionButtons
import kotlin.math.abs

@Composable
fun MainScreen(
    currentLocation: Location?,
    hasLocationPermission: Boolean,
    onGetLocation: () -> Unit,
    onSendLocation: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedProtocol by remember { mutableStateOf("TCP") }

    val destinations = listOf("Oliver Workspace", "Hernando Workspace", "Sebastián Workspace")
    var selectedDestinationIndex by remember { mutableStateOf(0) }

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

                    // Destination wheel selector
                    DestinationWheelSelector(
                        destinations = destinations,
                        selectedIndex = selectedDestinationIndex,
                        onIndexChanged = { selectedDestinationIndex = it }
                    )

                    // Protocol selection section
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Protocolo de Transmisión",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF4C1D95)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // TCP Radio Button
                            Row(
                                modifier = Modifier
                                    .selectable(
                                        selected = selectedProtocol == "TCP",
                                        onClick = { selectedProtocol = "TCP" }
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedProtocol == "TCP",
                                    onClick = { selectedProtocol = "TCP" },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF4C1D95)
                                    )
                                )
                                Text(
                                    text = "TCP",
                                    fontSize = 14.sp,
                                    color = Color(0xFF4C1D95),
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }

                            // UDP Radio Button
                            Row(
                                modifier = Modifier
                                    .selectable(
                                        selected = selectedProtocol == "UDP",
                                        onClick = { selectedProtocol = "UDP" }
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedProtocol == "UDP",
                                    onClick = { selectedProtocol = "UDP" },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF4C1D95)
                                    )
                                )
                                Text(
                                    text = "UDP",
                                    fontSize = 14.sp,
                                    color = Color(0xFF4C1D95),
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { onSendLocation(selectedProtocol, destinations[selectedDestinationIndex]) },
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
                            text = "Enviar a ${destinations[selectedDestinationIndex]} via $selectedProtocol",
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

@Composable
fun DestinationWheelSelector(
    destinations: List<String>,
    selectedIndex: Int,
    onIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Destino de Envío",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF4C1D95)
        )

        // Wheel container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF8F9FA))
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        val threshold = 50f
                        if (abs(dragAmount.y) > threshold) {
                            if (dragAmount.y > 0) {
                                // Swipe down - previous item
                                val newIndex = (selectedIndex - 1 + destinations.size) % destinations.size
                                onIndexChanged(newIndex)
                            } else {
                                // Swipe up - next item
                                val newIndex = (selectedIndex + 1) % destinations.size
                                onIndexChanged(newIndex)
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Main selected item
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4C1D95)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = destinations[selectedIndex],
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Previous item hint (top)
            if (destinations.size > 1) {
                val prevIndex = (selectedIndex - 1 + destinations.size) % destinations.size
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(30.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = destinations[prevIndex],
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF9CA3AF),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Next item hint (bottom)
            if (destinations.size > 1) {
                val nextIndex = (selectedIndex + 1) % destinations.size
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(30.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = (-10).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = destinations[nextIndex],
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF9CA3AF),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Indicator dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (index == selectedIndex) Color(0xFF4C1D95)
                            else Color(0xFFD1D5DB)
                        )
                )
            }
        }

        Text(
            text = "Desliza arriba/abajo para cambiar",
            fontSize = 12.sp,
            fontWeight = FontWeight.Light,
            color = Color(0xFF9CA3AF),
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen(
        currentLocation = null,
        hasLocationPermission = true,
        onGetLocation = { },
        onSendLocation = { protocol, destination -> }
    )
}