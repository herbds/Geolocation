package com.project.geolocation.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.util.UUID

object DeviceIdentifier {

    private const val PREFS_NAME = "device_id_prefs"
    private const val KEY_DEVICE_UUID = "device_uuid"


    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Intentar obtener UUID guardado
        var uuid = prefs.getString(KEY_DEVICE_UUID, null)

        if (uuid == null) {
            // Generar nuevo UUID basado en Android ID + timestamp
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown"

            uuid = UUID.nameUUIDFromBytes(
                "$androidId-${System.currentTimeMillis()}".toByteArray()
            ).toString()

            // Guardar para futuras ejecuciones
            prefs.edit().putString(KEY_DEVICE_UUID, uuid).apply()

            Log.d("DeviceIdentifier", "🆕 Nuevo UUID generado: $uuid")
        } else {
            Log.d("DeviceIdentifier", "📱 UUID existente: $uuid")
        }

        val manufacturer = Build.MANUFACTURER.replace(" ", "-")
        val model = Build.MODEL.replace(" ", "-")

        // Tomar solo los primeros 8 caracteres del UUID
        val shortUuid = uuid.take(8)

        val deviceId = "${manufacturer}_${model}_${shortUuid}"

        Log.d("DeviceIdentifier", "📱 Device ID final: $deviceId")
        return deviceId
    }


    fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL

        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
    }


    fun getDeviceInfo(context: Context): DeviceInfo {
        return DeviceInfo(
            deviceId = getDeviceId(context),
            deviceName = getDeviceName(),
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            sdkVersion = Build.VERSION.SDK_INT
        )
    }
}

/**
 * Data class con información del dispositivo
 */
data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkVersion: Int
)
