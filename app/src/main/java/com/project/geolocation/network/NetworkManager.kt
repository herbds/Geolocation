package com.project.geolocation.network

import android.content.Context
import android.location.Location
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NetworkManager(private val context: Context) {

    // Main method to send location with protocol selection
    suspend fun sendLocation(ipAddress: String, location: Location?, identifier: String, protocol: String) {
        if (location == null) {
            showToast("No se ha obtenido una ubicación para enviar.")
            return
        }

        val time = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(location.time))
        val message = "Identifier: $identifier, Lat: ${location.latitude}, Lon: ${location.longitude}, Time: $time"
        val port = when {
            identifier == "Hernando Workspace" && protocol.uppercase() == "TCP" -> 551
            identifier == "Hernando Workspace" && protocol.uppercase() == "UDP" -> 541
            identifier == "Alan Workspace" && protocol.uppercase() == "TCP" -> 5051
            protocol.uppercase() == "TCP" -> 5050
            else -> 5049

        }

        when (protocol.uppercase()) {
            "TCP" -> sendViaTCP(ipAddress, port, message, identifier)
            "UDP" -> sendViaUDP(ipAddress, port, message, identifier)
            else -> showToast("Protocolo no válido. Use TCP o UDP.")
        }
    }

    // TCP transmission implementation
    private suspend fun sendViaTCP(ipAddress: String, port: Int, message: String, identifier: String) {
        withContext(Dispatchers.IO) {
            var socket: Socket? = null
            var outputStream: OutputStream? = null
            try {
                socket = Socket(ipAddress, port)
                outputStream = socket.getOutputStream()
                outputStream.write(message.toByteArray(Charsets.UTF_8))
                outputStream.flush()
                showToast("Ubicación enviada via TCP a $identifier")

            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Error TCP: ${e.message}")
            } finally {
                outputStream?.close()
                socket?.close()
            }
        }
    }

    // UDP transmission implementation
    private suspend fun sendViaUDP(ipAddress: String, port: Int, message: String, identifier: String) {
        withContext(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                val buffer = message.toByteArray(Charsets.UTF_8)
                val address = InetAddress.getByName(ipAddress)
                val packet = DatagramPacket(buffer, buffer.size, address, port)
                socket.send(packet)
                showToast("Ubicación enviada via UDP a $identifier")

            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Error UDP: ${e.message}")
            } finally {
                socket?.close()
            }
        }
    }

    // Toast message helper
    private suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
