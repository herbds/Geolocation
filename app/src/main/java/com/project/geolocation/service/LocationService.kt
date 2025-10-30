package com.project.geolocation.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.project.geolocation.MainActivity
import com.project.geolocation.network.NetworkManager
import com.project.geolocation.security.SecureTokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var networkManager: NetworkManager
    private lateinit var secureTokenManager: SecureTokenManager

    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val CHANNEL_ID = "LocationServiceChannel"
    private val NOTIFICATION_ID = 1

    private var locationCount = 0
    private var sendSuccessCount = 0
    private var sendErrorCount = 0

    override fun onCreate() {
        super.onCreate()
        Log.d("LocationService", "🚀 Service creado")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        secureTokenManager = SecureTokenManager(applicationContext)
        networkManager = NetworkManager(this, secureTokenManager)

        acquireWakeLocks()

        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    locationCount++
                    Log.d("LocationService", "📍 Ubicación #$locationCount: ${location.latitude}, ${location.longitude}")

                    sendLocationToAWS(location)
                    updateNotification(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("LocationService", "▶️ Service iniciado")

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        startLocationUpdates()

        return START_STICKY
    }

    private fun acquireWakeLocks() {
        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            val wifiLockMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            } else {
                @Suppress("DEPRECATION")
                WifiManager.WIFI_MODE_FULL_HIGH_PERF
            }

            wifiLock = wifiManager.createWifiLock(
                wifiLockMode, // Usar el modo determinado
                "GeolocationService:WifiLock"
            )

            wifiLock?.acquire()
            Log.d("LocationService", "✅ WifiLock adquirido")

            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "GeolocationService:WakeLock"
            )
            wakeLock?.acquire(10 * 60 * 1000L /*10 minutos*/)
            Log.d("LocationService", "✅ WakeLock adquirido")

        } catch (e: Exception) {
            Log.e("LocationService", "❌ Error adquiriendo WakeLocks: ${e.message}")
        }
    }

    private fun releaseWakeLocks() {
        try {
            wifiLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d("LocationService", "🔓 WifiLock liberado")
                }
            }
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d("LocationService", "🔓 WakeLock liberado")
                }
            }
        } catch (e: Exception) {
            Log.e("LocationService", "❌ Error liberando WakeLocks: ${e.message}")
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L
        ).apply {
            setMinUpdateIntervalMillis(5000L)
            setWaitForAccurateLocation(false)
        }.build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.d("LocationService", "✅ Actualizaciones de ubicación iniciadas (cada 10s)")
        } else {
            Log.e("LocationService", "❌ Sin permisos de ubicación")
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Servicio de Ubicación",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Seguimiento de ubicación en segundo plano"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("🌐 Sistema de Geolocalización")
        .setContentText("Iniciando seguimiento...")
        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setContentIntent(createPendingIntent())
        .build()

    private fun updateNotification(location: Location) {
        val successRate = if (locationCount > 0) {
            (sendSuccessCount.toFloat() / locationCount * 100).toInt()
        } else 0

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("📡 Transmitiendo ($successRate% éxito)")
            .setContentText("📍 ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)} | ${locationCount} enviadas")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation) // Puedes cambiar esto
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(createPendingIntent())
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun sendLocationToAWS(location: Location) {
        Log.d("LocationService", "📤 Enviando a AWS: ${location.latitude}, ${location.longitude}")

        serviceScope.launch {
            try {
                networkManager.broadcastLocationUdp(location)
                sendSuccessCount++
                Log.d("LocationService", "✅ Enviado exitosamente (total exitosos: $sendSuccessCount)")
            } catch (e: Exception) {
                sendErrorCount++
                Log.e("LocationService", "❌ Error enviando a AWS (total errores: $sendErrorCount): ${e.message}", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        releaseWakeLocks()
        serviceScope.cancel()
        Log.d("LocationService", "🛑 Service detenido. Stats: $locationCount ubicaciones, $sendSuccessCount exitosos, $sendErrorCount errores")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
