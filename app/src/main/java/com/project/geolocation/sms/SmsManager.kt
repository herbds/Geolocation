package com.project.geolocation.sms

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.widget.Toast

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

        val message = "Mi ubicación: ${location.latitude}, ${location.longitude}\n" +
                "Ver en Google Maps: https://maps.google.com/?q=${location.latitude},${location.longitude}"

        try {
            // Method 1: Intent to open SMS app (THIS ONE ALWAYS WORKS)
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", message)
            }
            context.startActivity(intent)

            Toast.makeText(context, "Abriendo app de SMS...", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            // Method 2: SmsManager as backup
            try {
                @Suppress("DEPRECATION")
                val smsManager = android.telephony.SmsManager.getDefault()
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Toast.makeText(context, "SMS enviado directamente", Toast.LENGTH_SHORT).show()
            } catch (e2: Exception) {
                android.util.Log.e("SMS_ERROR", "Ambos métodos fallaron: ${e2.message}")
                Toast.makeText(context, "Error: ${e2.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}