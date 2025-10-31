package com.project.geolocation.network

import android.content.Context
import android.location.Location
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

// --- Login Request/Response ---
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(val status: String, val token: String? = null)

// --- Register Requests/Responses ---
@Serializable
data class RegisterStep1Request(
    val email: String,
    val password: String
)

@Serializable
data class RegisterStep1Response(
    val status: String,
    val uid: String? = null,
    val error: String? = null
)

@Serializable
data class RegisterStep2Request(
    val uid: String,
    val nombre_completo: String,
    val cedula: String,
    val email: String,
    val telefono: String,
    val empresa: String
)

@Serializable
data class RegisterStep2Response(
    val status: String,
    val token: String? = null,
    val user_id: Int? = null,
    val error: String? = null
)

// --- NUEVO: Estructura para envío UDP ---
@Serializable
data class UdpLocationPayload(
    val token: String,
    val lat: Double,
    val lon: Double,
    val timestamp: String
)

class NetworkManager(
    private val context: Context,
    private val secureTokenManager: SecureTokenManager
) {

    private data class Workspace(val name: String, val ip: String, val port: Int)

    private val workspaces = listOf(
        Workspace("Oliver", "oliver.tumaquinaya.com", 5049),
        Workspace("Hernando", "hernando.tumaquinaya.com", 5049),
        Workspace("Sebastian", "sebastian.tumaquinaya.com", 5049),
        Workspace("Alan", "alan.tumaquinaya.com", 5049),
    )

    // ✅ NUEVO: Obtener device ID una sola vez
    private val deviceId: String by lazy {
        DeviceIdentifier.getDeviceId(context)
    }

    private val deviceName: String by lazy {
        DeviceIdentifier.getDeviceName()
    }

    private val httpClient = ApiClient.client
    private val json = Json { ignoreUnknownKeys = true }

    // --- Login ---
    suspend fun loginToInstance(email: String, pass: String): String? {
        val loginRequest = LoginRequest(email = email, password = pass)
        val workspace = workspaces.first() // Solo Oliver
        val url = "https://${workspace.ip}/auth/firebase-login"

        return try {
            val response: LoginResponse = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(loginRequest)
            }.body()

            if (response.status == "success" && response.token != null) {
                Log.d(TAG, "✅ Login HTTP exitoso. Token recibido.")
                response.token
            } else {
                Log.w(TAG, "❌ Login HTTP fallido en ${workspace.name}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error HTTP en login: ${e.message}", e)
            null
        }
    }

    // --- Register Step 1 ---
    suspend fun registerStep1(request: RegisterStep1Request): String? {
        val workspace = workspaces.first() // Solo Oliver
        val url = "https://${workspace.ip}/auth/register/step1"
        
        return try {
            val response: RegisterStep1Response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            
            if (response.status == "success" && response.uid != null) {
                Log.d(TAG, "✅ Registro Paso 1 exitoso. UID: ${response.uid}")
                response.uid
            } else {
                Log.e(TAG, "❌ Error en Paso 1: ${response.error ?: "UID nulo"}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error HTTP en registro Paso 1: ${e.message}", e)
            null
        }
    }

    // --- Register Step 2 ---
    suspend fun registerStep2(request: RegisterStep2Request): Map<String, RegisterStep2Response?> = coroutineScope {
        val deferredResponses = workspaces.map { workspace ->
            async(Dispatchers.IO) {
                val url = "https://${workspace.ip}/auth/register/step2"
                val response: RegisterStep2Response? = try {
                    httpClient.post(url) {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }.body<RegisterStep2Response>()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error HTTP en registro Paso 2 (${workspace.name}): ${e.message}", e)
                    null
                }
                
                if (response?.status == "success" && response.token != null) {
                    Log.d(TAG, "✅ Registro Paso 2 exitoso en ${workspace.name}")
                } else {
                    Log.e(TAG, "❌ Error en Paso 2 (${workspace.name}): ${response?.error ?: "Respuesta nula"}")
                }
                
                workspace.name to response
            }
        }
        deferredResponses.awaitAll().toMap()
    }

    /**
     * ✅ NUEVA IMPLEMENTACIÓN: Envía SOLO token, lat, lon, timestamp en JSON
     * Cada backend decodificará el token para obtener uid y luego su user_id local
     */
    suspend fun broadcastLocationUdp(location: Location) {
        val token = secureTokenManager.getToken()
        if (token == null) {
            Log.e(TAG, "📡 Error: No hay JWT guardado. No se puede enviar ubicación.")
            return
        }

        // Crear timestamp en formato esperado por el backend
        val timestamp = java.text.SimpleDateFormat(
            "dd/MM/yyyy HH:mm:ss", 
            java.util.Locale.getDefault()
        ).format(java.util.Date(location.time))

        // Crear payload limpio con solo los 4 campos requeridos
        val payload = UdpLocationPayload(
            token = token,
            lat = location.latitude,
            lon = location.longitude,
            timestamp = timestamp
        )

        // Serializar a JSON
        val jsonMessage = json.encodeToString(payload)
        
        Log.d(TAG, "📡 Broadcasting ubicación a todas las instancias...")
        Log.d(TAG, "📍 Lat: ${location.latitude}, Lon: ${location.longitude}")

        // Enviar a todas las instancias
        workspaces.forEach { workspace ->
            sendViaUDP(workspace.ip, workspace.port, jsonMessage, workspace.name)
        }
    }

    private suspend fun sendViaUDP(ipAddress: String, port: Int, message: String, identifier: String) {
        withContext(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.soTimeout = 3000 // ✅ Timeout de 3 segundos

            val buffer = message.toByteArray(Charsets.UTF_8)
            val address = InetAddress.getByName(ipAddress)
            val packet = DatagramPacket(buffer, buffer.size, address, port)

                socket.send(packet)

                Log.d(TAG, "✅ UDP enviado a $identifier ($ipAddress:$port)")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error UDP para $identifier: ${e.message}", e)
                // ✅ Solo registrar error, NO mostrar Toast en segundo plano
            } finally {
                socket?.close()
            }
        }
    }
}