package com.project.geolocation.network

import android.content.Context
import android.location.Location
import android.util.Log
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NetworkManager(
    private val context: Context,
    private val getUserId: () -> String? // Lambda to get current user cedula (ID number)
) {

    private data class Workspace(val name: String, val domain: String, val udpPort: Int)

    private val workspaces = listOf(
        Workspace("Alan", "alan.tumaquinaya.com", 5049),
        Workspace("Hernando", "hernando.tumaquinaya.com", 5049),
        Workspace("Sebastian", "sebastian.tumaquinaya.com", 5049),
        Workspace("Oliver", "oliver.tumaquinaya.com", 5049),
    )

    companion object {
        private const val TAG = "NetworkManager"
    }

    /**
     * Sends location via UDP to all instances.
     * Format: latitude, longitude, timestamp, user_id (cedula)
     */
    suspend fun broadcastLocationUdp(location: Location) {
        val userId = getUserId()
        if (userId == null) {
            Log.e(TAG, "📡 Error: No user ID available. Cannot send location.")
            return
        }

        val time = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(location.time))
        
        // Format: latitude, longitude, timestamp, user_id
        val message = "Lat: ${location.latitude}, Lon: ${location.longitude}, Time: $time, UserID: $userId"

        Log.d(TAG, "📡 Broadcasting location with user_id: $userId")

        workspaces.forEach { workspace ->
            sendViaUDP(workspace.domain, workspace.udpPort, message, workspace.name)
        }
    }

    /**
     * Fetches pending destinations from all workspaces and returns the most recent one
     */
    suspend fun fetchPendingDestination(): PendingDestination? = withContext(Dispatchers.IO) {
        val userId = getUserId()
        if (userId == null) {
            Log.e(TAG, "🎯 Error: No user ID available. Cannot fetch destinations.")
            return@withContext null
        }

        Log.d(TAG, "🎯 Fetching destinations for user: $userId from ${workspaces.size} workspaces")

        try {
            // Launch parallel requests to all workspaces
            val allDestinations = workspaces.map { workspace ->
                async {
                    fetchDestinationFromWorkspace(workspace, userId)
                }
            }.awaitAll().flatten() // Flatten list of lists

            if (allDestinations.isEmpty()) {
                Log.d(TAG, "🎯 No destinations found in any workspace")
                return@withContext null
            }

            // Filter only pending destinations
            val pendingDestinations = allDestinations.filter { it.status == "pending" }

            if (pendingDestinations.isEmpty()) {
                Log.d(TAG, "🎯 No pending destinations found")
                return@withContext null
            }

            Log.d(TAG, "🎯 Found ${pendingDestinations.size} pending destinations")

            // Find the most recent one by parsing the created_at timestamp
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val mostRecent = pendingDestinations.maxByOrNull { destination ->
                try {
                    dateFormat.parse(destination.created_at)?.time ?: 0L
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing date: ${destination.created_at}", e)
                    0L
                }
            }

            mostRecent?.let {
                val pendingDest = PendingDestination(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    timestamp = it.created_at,
                    source = "Multiple workspaces"
                )
                Log.d(TAG, "🎯 Most recent destination: ${it.latitude}, ${it.longitude} from ${it.created_at}")
                pendingDest
            }

        } catch (e: Exception) {
            Log.e(TAG, "🎯 Error fetching destinations: ${e.message}", e)
            null
        }
    }

    /**
     * Fetches destinations from a single workspace
     */
    private suspend fun fetchDestinationFromWorkspace(
        workspace: Workspace,
        userId: String
    ): List<Destination> {
        return try {
            val url = "https://${workspace.domain}/database/destination/$userId"
            Log.d(TAG, "🌐 Fetching from ${workspace.name}: $url")

            val response: HttpResponse = ApiClient.client.get(url)
            
            if (response.status.value == 200) {
                val destinationResponse: DestinationResponse = response.body()
                Log.d(TAG, "✅ ${workspace.name}: Found ${destinationResponse.count} destinations")
                destinationResponse.destinations
            } else {
                Log.w(TAG, "⚠️ ${workspace.name}: HTTP ${response.status.value}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ ${workspace.name} error: ${e.message}")
            emptyList()
        }
    }

    private suspend fun sendViaUDP(ipAddress: String, port: Int, message: String, identifier: String) {
        withContext(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.soTimeout = 3000

                val buffer = message.toByteArray(Charsets.UTF_8)
                val address = InetAddress.getByName(ipAddress)
                val packet = DatagramPacket(buffer, buffer.size, address, port)

                socket.send(packet)
                Log.d(TAG, "Message: ($message)")
                Log.d(TAG, "✅ UDP sent to $identifier ($ipAddress:$port)")

            } catch (e: Exception) {
                Log.e(TAG, "❌ UDP error for $identifier: ${e.message}", e)
            } finally {
                socket?.close()
            }
        }
    }
}