package com.project.geolocation.network

import android.content.Context
import android.location.Location
import android.util.Log
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NetworkManager(
    private val context: Context,
    private val getUserId: () -> String?
) {

    companion object {
        private const val TAG = "NetworkManager"
        private const val SERVER_DOMAIN = "sebastian.tumaquinaya.com"
        private const val UDP_PORT = 5049
    }

    /**
     * Sends location via UDP to Sebastian's server
     */
    suspend fun broadcastLocationUdp(location: Location) {
        val userId = getUserId()
        if (userId == null) {
            Log.e(TAG, "📡 Error: No user ID available. Cannot send location.")
            return
        }

        val time = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(location.time))
        val message = "Lat: ${location.latitude}, Lon: ${location.longitude}, Time: $time, UserID: $userId"

        Log.d(TAG, "📡 Sending location with user_id: $userId")
        sendViaUDP(message)
    }

    /**
     * Envía alerta de desviación de ruta
     */
    suspend fun sendOffRouteAlert(
        location: Location,
        isOffRoute: Boolean,
        distance: Double
    ) {
        val userId = getUserId()
        if (userId == null) {
            Log.e(TAG, "⚠️ Error: No user ID available. Cannot send route alert.")
            return
        }

        val time = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(location.time))

        val message = buildOffRouteMessage(
            deviceId = userId,
            deviceName = android.os.Build.MODEL ?: "Unknown",
            latitude = location.latitude,
            longitude = location.longitude,
            time = time,
            isOffRoute = isOffRoute,
            distance = distance
        )

        Log.d(TAG, "🚨 Sending off-route alert: ${if (isOffRoute) "OFF_ROUTE" else "ON_ROUTE"} (${distance.toInt()}m)")
        sendViaUDP(message)
    }

    private fun buildOffRouteMessage(
        deviceId: String,
        deviceName: String,
        latitude: Double,
        longitude: Double,
        time: String,
        isOffRoute: Boolean,
        distance: Double
    ): String {
        return """
            |TYPE: ROUTE_ALERT
            |DeviceID: $deviceId
            |DeviceName: $deviceName
            |Status: ${if (isOffRoute) "OFF_ROUTE" else "ON_ROUTE"}
            |Distance: ${distance.toInt()}m
            |Lat: $latitude
            |Lon: $longitude
            |Time: $time
        """.trimMargin()
    }

    /**
     * Fetches pending destination from Sebastian's server
     */
    suspend fun fetchPendingDestination(): PendingDestination? = withContext(Dispatchers.IO) {
        val userId = getUserId()
        if (userId == null) {
            Log.e(TAG, "🎯 Error: No user ID available. Cannot fetch destinations.")
            return@withContext null
        }

        Log.d(TAG, "🎯 Fetching destinations for user: $userId")

        try {
            val url = "https://$SERVER_DOMAIN/database/destination/$userId"
            Log.d(TAG, "🌐 Fetching from: $url")

            val response: HttpResponse = ApiClient.client.get(url)
            
            if (response.status.value != 200) {
                Log.w(TAG, "⚠️ HTTP ${response.status.value}")
                return@withContext null
            }

            val destinationResponse: DestinationResponse = response.body()
            Log.d(TAG, "✅ Found ${destinationResponse.count} destinations")

            val pendingDestination = destinationResponse.destinations
                .filter { it.status == "pending" }
                .maxByOrNull { destination ->
                    try {
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                        dateFormat.parse(destination.created_at)?.time ?: 0L
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing date: ${destination.created_at}", e)
                        0L
                    }
                }

            pendingDestination?.let {
                Log.d(TAG, "🎯 Pending destination: ${it.latitude}, ${it.longitude}")
                PendingDestination(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    timestamp = it.created_at,
                    source = SERVER_DOMAIN
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "🎯 Error fetching destinations: ${e.message}", e)
            null
        }
    }

    /**
     * Notifica al servidor que el usuario llegó al destino
     */
    suspend fun completeDestination(): Boolean = withContext(Dispatchers.IO) {
        val userId = getUserId()
        if (userId == null) {
            Log.e(TAG, "🏁 Error: No user ID available. Cannot complete destination.")
            return@withContext false
        }

        try {
            val url = "https://$SERVER_DOMAIN/api/destination/complete"
            Log.d(TAG, "🏁 Completing destination for user: $userId")

            val response: HttpResponse = ApiClient.client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(CompleteDestinationRequest(user_id = userId))
            }

            if (response.status.value == 200) {
                Log.d(TAG, "✅ Destination marked as completed")
                return@withContext true
            } else {
                Log.w(TAG, "⚠️ Failed to complete destination: HTTP ${response.status.value}")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error completing destination: ${e.message}", e)
            return@withContext false
        }
    }

    private suspend fun sendViaUDP(message: String) {
        withContext(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.soTimeout = 3000

                val buffer = message.toByteArray(Charsets.UTF_8)
                val address = InetAddress.getByName(SERVER_DOMAIN)
                val packet = DatagramPacket(buffer, buffer.size, address, UDP_PORT)

                socket.send(packet)
                Log.d(TAG, "Message: ($message)")
                Log.d(TAG, "✅ UDP sent to $SERVER_DOMAIN:$UDP_PORT")

            } catch (e: Exception) {
                Log.e(TAG, "❌ UDP error: ${e.message}", e)
            } finally {
                socket?.close()
            }
        }
    }
}

// Data class para el request de completar destino
@Serializable
data class CompleteDestinationRequest(
    val user_id: String
)
