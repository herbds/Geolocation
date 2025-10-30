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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class FirebaseTokenRequest(val token: String)

@Serializable
data class LoginResponse(val status: String, val token: String? = null)

@Serializable
data class RegisterRequest(
    val password: String,
    val nombre_completo: String,
    val cedula: String,
    val email: String,
    val telefono: String,
    val empresa: String
)

@Serializable
data class RegisterResponse(
    val status: String,
    val token: String? = null,
    val user_id: Int? = null
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
    )

    // ✅ NUEVO: Obtener device ID una sola vez
    private val deviceId: String by lazy {
        DeviceIdentifier.getDeviceId(context)
    }

    private val deviceName: String by lazy {
        DeviceIdentifier.getDeviceName()
    }

    private val httpClient = ApiClient.client

    suspend fun loginToInstance(firebaseToken: String): String? {
        val tokenRequest = FirebaseTokenRequest(token = firebaseToken)

        val workspace = workspaces.first()
        // --- CORRECCIÓN AQUÍ ---
        // val url = "https://://${workspace.ip}/auth/firebase-login" // <-- BUG
        val url = "https://${workspace.ip}/auth/firebase-login" // <-- CORREGIDO

        return try {
            val response: LoginResponse = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(tokenRequest)
            }.body()

            if (response.status == "success" && response.token != null) {
                Log.d(TAG, "✅ Login HTTP exitoso. Token recibido.")
                response.token
            } else {
                Log.w(TAG, "❌ Login HTTP fallido (lógico) en ${workspace.name}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error HTTP en login de ${workspace.name}: ${e.message}", e)
            null
        }
    }

    suspend fun registerOnAllInstances(request: RegisterRequest): String? = coroutineScope {
        val deferredResponses = workspaces.map { workspace ->
            async(Dispatchers.IO) {
                val url = "https://${workspace.ip}/auth/register"

                try {
                    val response: RegisterResponse = httpClient.post(url) {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }.body()
                    Log.d(TAG, "Datos ${request}")
                    
                    if (response.status == "success" && response.token != null) {
                        Log.d(TAG, "✅ Registro exitoso en ${workspace.name}")
                    } else {
                        Log.e(TAG, "❌ Error LÓGICO en ${workspace.name}: El servidor respondió con status '${response.status}'")
                    }
                    response
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error HTTP/Red en registro de ${workspace.name}: ${e.message}", e)
                    null
                }
            }
        }

        val results = deferredResponses.awaitAll()

        if (results.all { it != null && it.status == "success" && it.token != null }) {
            Log.d(TAG, "✅ Registro exitoso en TODAS las instancias.")
            results.first()?.token
        } else {
            Log.e(TAG, "❌ Falló el registro en una o más instancias.")
            null
        }
    }


    /**
     * Envía la ubicación por UDP a todas las instancias.
     * El token JWT se lee desde SecureTokenManager.
     */
    suspend fun broadcastLocationUdp(location: Location) {
        val token = secureTokenManager.getToken()
        if (token == null) {
            Log.e(TAG, "📡 Error: No hay JWT guardado. No se puede enviar ubicación.")
            return
        }

        val time = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(location.time))
        val message = "Lat: ${location.latitude}, Lon: ${location.longitude}, Time: $time"

        Log.d(TAG, "📡 Broadcast iniciado: $message")

        workspaces.forEach { workspace ->
            sendViaUDP(workspace.ip, workspace.port, message, workspace.name)
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