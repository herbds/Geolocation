package com.project.geolocation.network

import android.content.Context
import android.location.Location
import android.util.Log
import kotlinx.coroutines.Dispatchers
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

    private data class Workspace(val name: String, val ip: String, val port: Int)

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
            sendViaUDP(workspace.ip, workspace.port, message, workspace.name)
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