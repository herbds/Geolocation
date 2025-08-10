package com.project.geolocation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation by mutableStateOf<Location?>(null)
    private var hasLocationPermission by mutableStateOf(false)
    private var hasSmsPermission by mutableStateOf(false)

    // Launcher para permisos de ubicación
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        hasLocationPermission = fineLocation || coarseLocation

        if (hasLocationPermission) {
            Toast.makeText(this, "Permiso de ubicación concedido", Toast.LENGTH_SHORT).show()
            getLocation()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_LONG).show()
        }
    }

    // Launcher para permiso de SMS
    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPermission = isGranted
        if (isGranted) {
            Toast.makeText(this, "Permiso de SMS concedido", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permiso de SMS denegado", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Verificar permisos al inicio
        checkPermissions()

        setContent {
            var phoneNumber by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text("Enviar mi ubicación por SMS")

                Spacer(modifier = Modifier.height(20.dp))

                // Estado de permisos
                Text("Permiso ubicación: ${if (hasLocationPermission) "✓" else "✗"}")
                Text("Permiso SMS: ${if (hasSmsPermission) "✓" else "✗"}")

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Número de teléfono") },
                    placeholder = { Text("3001234567") }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Botón para pedir permisos de ubicación
                Button(onClick = {
                    requestLocationPermission()
                }) {
                    Text("Pedir permiso ubicación")
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Botón para pedir permiso de SMS
                Button(onClick = {
                    requestSmsPermission()
                }) {
                    Text("Pedir permiso SMS")
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Botón para obtener ubicación
                Button(
                    onClick = { getLocation() },
                    enabled = hasLocationPermission
                ) {
                    Text("Obtener ubicación")
                }

                Spacer(modifier = Modifier.height(10.dp))

                currentLocation?.let { location ->
                    Text("Lat: ${location.latitude}")
                    Text("Lng: ${location.longitude}")
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        sendSMS(phoneNumber)
                    },
                    enabled = currentLocation != null && phoneNumber.isNotEmpty() && hasSmsPermission
                ) {
                    Text("Enviar SMS")
                }
            }
        }
    }

    private fun checkPermissions() {
        hasLocationPermission = (
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                )

        hasSmsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun requestSmsPermission() {
        smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
    }

    private fun getLocation() {
        if (!hasLocationPermission) {
            Toast.makeText(this, "No hay permiso de ubicación", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = location
                    Toast.makeText(this, "Ubicación obtenida", Toast.LENGTH_SHORT).show()
                } else {
                    // Solicitar nueva ubicación
                    val locationRequest = LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY, 1000L
                    ).setMaxUpdates(1).build()

                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        object : LocationCallback() {
                            override fun onLocationResult(result: LocationResult) {
                                currentLocation = result.lastLocation
                                Toast.makeText(this@MainActivity, "Nueva ubicación obtenida", Toast.LENGTH_SHORT).show()
                                fusedLocationClient.removeLocationUpdates(this)
                            }
                        },
                        mainLooper
                    )
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error obteniendo ubicación", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Error de permisos: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendSMS(phoneNumber: String) {
        if (!hasSmsPermission) {
            Toast.makeText(this, "No hay permiso de SMS", Toast.LENGTH_SHORT).show()
            return
        }

        val location = currentLocation
        if (location == null) {
            Toast.makeText(this, "No hay ubicación", Toast.LENGTH_SHORT).show()
            return
        }

        val message = "Mi ubicación: ${location.latitude}, ${location.longitude}\n" +
                "Ver en Google Maps: https://maps.google.com/?q=${location.latitude},${location.longitude}"

        try {
            // Método 1: Intent para abrir app de SMS (ESTE SÍ FUNCIONA SIEMPRE)
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", message)
            }
            startActivity(intent)

            Toast.makeText(this, "Abriendo app de SMS...", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            // Método 2: SmsManager como backup
            try {
                @Suppress("DEPRECATION")
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Toast.makeText(this, "SMS enviado directamente", Toast.LENGTH_SHORT).show()
            } catch (e2: Exception) {
                android.util.Log.e("SMS_ERROR", "Ambos métodos fallaron: ${e2.message}")
                Toast.makeText(this, "Error: ${e2.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}