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

class NetworkManager(private val context: Context) {

    private data class Workspace(val name: String, val ip: String, val port: Int)

    private val workspaces = listOf(
        Workspace("Oliver", "3.150.118.46", 5049),
        Workspace("Hernando", "3.227.231.10", 5049),
        Workspace("Sebastian", "34.230.160.177", 5049),
        Workspace("Alan", "35.172.201.236", 5049)
    )

    companion object {
        private const val TAG = "NetworkManager"
    }

    suspend fun broadcastLocationUdp(location: Location) {
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