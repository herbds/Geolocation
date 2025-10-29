package com.project.geolocation.network

import android.content.Context
import android.location.Location
import android.util.Log
import com.project.geolocation.utils.DeviceIdentifier  // ✅ NUEVO IMPORT
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

    // ✅ NUEVO: Obtener device ID una sola vez
    private val deviceId: String by lazy {
        DeviceIdentifier.getDeviceId(context)
    }

    private val deviceName: String by lazy {
        DeviceIdentifier.getDeviceName()
    }

    suspend fun broadcastLocationUdp(location: Location) {
        val time = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(location.time))

        // ✅ NUEVO: Incluir Device ID en el mensaje
        val message = buildMessage(
            deviceId = deviceId,
            deviceName = deviceName,
            latitude = location.latitude,
            longitude = location.longitude,
            time = time,
        )

        Log.d("NetworkManager", "📡 Enviando: $message")

        var successCount = 0
        var errorCount = 0

        workspaces.forEach { workspace ->
            val success = sendViaUDP(workspace.ip, workspace.port, message, workspace.name)
            if (success) successCount++ else errorCount++
        }

        Log.d("NetworkManager", "✅ Enviado: $successCount exitosos, $errorCount fallidos")
    }

    // ✅ NUEVO: Construir mensaje estructurado
    private fun buildMessage(
        deviceId: String,
        deviceName: String,
        latitude: Double,
        longitude: Double,
        time: String,
    ): String {
        return """
            |DeviceID: $deviceId
            |DeviceName: $deviceName
            |Lat: $latitude
            |Lon: $longitude
            |Time: $time
        """.trimMargin()
    }

    private suspend fun sendViaUDP(
        ipAddress: String,
        port: Int,
        message: String,
        identifier: String
    ): Boolean = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = 5000

            val buffer = message.toByteArray(Charsets.UTF_8)
            val address = InetAddress.getByName(ipAddress)
            val packet = DatagramPacket(buffer, buffer.size, address, port)

            socket.send(packet)

            Log.d("NetworkManager", "✅ UDP enviado a $identifier ($ipAddress:$port)")
            true

        } catch (e: Exception) {
            Log.e("NetworkManager", "❌ Error UDP a $identifier: ${e.message}")
            e.printStackTrace()
            false

        } finally {
            socket?.close()
        }
    }
}
