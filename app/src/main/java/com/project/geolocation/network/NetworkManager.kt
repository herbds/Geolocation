package com.project.geolocation.network

import android.content.Context
import android.location.Location
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NetworkManager(private val context: Context) {
    suspend fun sendLocation(ipAddress: String, port: Int, location: Location?, identifier: String) {
        if (location == null) {
            showToast("No se ha obtenido una ubicación para enviar.")
            return
        }

        val time = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(location.time))
        val message = "Identifier: $identifier, Lat: ${location.latitude}, Lon: ${location.longitude}, Time: $time"

        withContext(Dispatchers.IO) {
            var socket: Socket? = null
            var outputStream: OutputStream? = null
            try {
                socket = Socket(ipAddress, port)

                outputStream = socket.getOutputStream()
                outputStream.write(message.toByteArray(Charsets.UTF_8))
                outputStream.flush()
                showToast("Ubicación enviada a $identifier")

            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Error al enviar: ${e.message}")
            } finally {
                outputStream?.close()
                socket?.close()
            }
        }
    }

    private suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}