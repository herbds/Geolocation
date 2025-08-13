package com.project.geolocation.sms

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsManager(
    private val context: Context,
    private val hasSmsPermission: () -> Boolean
) {

    fun sendSMS(phoneNumber: String, location: Location?) {
        if (!hasSmsPermission()) {
            Toast.makeText(context, "No hay permiso de SMS", Toast.LENGTH_SHORT).show()
            return
        }

        if (location == null) {
            Toast.makeText(context, "No hay ubicación", Toast.LENGTH_SHORT).show()
            return
        }

        val timestamp = location.time
        val longitude = location.longitude
        val latitude = location.latitude
        val time = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(location.time))

        val message = " latitud: $latitude ,\n longitud: $longitude,\n timestamp : $timestamp, \n time: $time"

        try {
            // Method 1: SmsManager as backup
            @Suppress("DEPRECATION")
            val smsManager = android.telephony.SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(context, "SMS enviado directamente", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            // Method 2: Intent to open SMS app
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:$phoneNumber")
                    putExtra("sms_body", message)
                }
                context.startActivity(intent)
                Toast.makeText(context, "Abriendo app de SMS...", Toast.LENGTH_SHORT).show()
            } catch (e2: Exception) {
                android.util.Log.e("SMS_ERROR", "Ambos métodos fallaron: ${e2.message}")
                Toast.makeText(context, "Error: ${e2.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
