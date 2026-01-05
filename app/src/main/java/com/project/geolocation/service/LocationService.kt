package com.project.geolocation.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.wifi.WifiManager
import android.os.*
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationCallback
import com.project.geolocation.MainActivity
import com.project.geolocation.network.NetworkManager
import com.project.geolocation.security.LocalAuthManager
import kotlinx.coroutines.*

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var networkManager: NetworkManager
    private lateinit var localAuthManager: LocalAuthManager

    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val CHANNEL_ID = "LocationServiceChannel"
    private val NOTIFICATION_ID = 1

    private var locationCount = 0
    private var sendSuccessCount = 0
    private var sendErrorCount = 0

    // 🔴 FIX: bandera para evitar iniciar dos veces
    private var isTracking = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            startForeground(NOTIFICATION_ID, createNotification())
            Log.d("LocationService", "✅ startForeground llamado con éxito")
        } catch (e: Exception) {
            Log.e("LocationService", "❌ Error al iniciar foreground: ${e.message}")
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        localAuthManager = LocalAuthManager(applicationContext)

        networkManager = NetworkManager(this) {
            localAuthManager.getLoggedUserCedula()
        }

        acquireWakeLocks()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    locationCount++
                    sendLocationToServers(location)
                    updateNotification(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("LocationService", "▶️ Service started")

        // 🔴 FIX: evitar múltiples requestLocationUpdates
        if (!isTracking) {
            startLocationUpdates()
            isTracking = true
        }

        return START_STICKY
    }

    // =====================================================
    // 📍 LOCATION
    // =====================================================

    private fun startLocationUpdates() {

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10_000L
        )
            .setMinUpdateIntervalMillis(5_000L)
            .setWaitForAccurateLocation(false)
            .build()

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("LocationService", "❌ Location permission missing")
            stopSelf()
            return
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )

        Log.d("LocationService", "✅ Location updates started correctly")
    }



    private fun hasLocationPermissions(): Boolean {
        val fine = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val background =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            } else true

        return fine && background
    }

    // =====================================================
    // 🔔 NOTIFICATION
    // =====================================================

    private fun createNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🌐 Geolocalización activa")
            .setContentText("Transmitiendo ubicación en segundo plano")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(createPendingIntent())
            .build()

    private fun updateNotification(location: Location) {
        val successRate =
            if (locationCount > 0) (sendSuccessCount * 100 / locationCount) else 0

        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("📡 Enviando ubicación")
                .setContentText(
                    "📍 ${"%.6f".format(location.latitude)}, ${"%.6f".format(location.longitude)}"
                )
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(createPendingIntent())
                .build()

        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Servicio de Ubicación",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setShowBadge(false)
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun createPendingIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    // =====================================================
    // 🌐 NETWORK
    // =====================================================

    private fun sendLocationToServers(location: Location) {
        serviceScope.launch {
            try {
                networkManager.broadcastLocationUdp(location)
                sendSuccessCount++
            } catch (e: Exception) {
                sendErrorCount++
                Log.e("LocationService", "❌ Network error", e)
            }
        }
    }

    // =====================================================
    // 🔋 WAKELOCKS
    // =====================================================

    private fun acquireWakeLocks() {
        val wifiManager =
            applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        wifiLock = wifiManager.createWifiLock(
            WifiManager.WIFI_MODE_FULL_LOW_LATENCY,
            "Geo:WifiLock"
        )
        wifiLock?.acquire()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Geo:WakeLock"
        )
        wakeLock?.acquire()
    }

    private fun releaseWakeLocks() {
        wifiLock?.takeIf { it.isHeld }?.release()
        wakeLock?.takeIf { it.isHeld }?.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        releaseWakeLocks()
        serviceScope.cancel()
        isTracking = false
        Log.d("LocationService", "🛑 Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
