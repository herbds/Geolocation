package com.project.geolocation.security

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Serializable
data class UserData(
    val nombreCompleto: String,
    val cedula: String,
    val email: String,
    val password: String,
    val telefono: String,
    val empresa: String
)

class LocalAuthManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // SharedPreferences for user data backups (full name, password, email, phone, company)
    private val userDataPrefs = EncryptedSharedPreferences.create(
        context,
        "user_data_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // SharedPreferences for active user cedula (ID number storage)
    private val activeUserPrefs = EncryptedSharedPreferences.create(
        context,
        "active_user_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // SharedPreferences for logged user cedula (current session - only ID number)
    private val loggedUserPrefs = EncryptedSharedPreferences.create(
        context,
        "logged_user_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val TAG = "LocalAuthManager"
        private const val KEY_USER_DATA = "user_data_"
        private const val KEY_ACTIVE_CEDULA = "active_cedula"
        private const val KEY_LOGGED_CEDULA = "logged_cedula"
    }

    /**
     * Register a new user
     * Stores full backup (name, password, email, phone, company) and cedula separately
     */
    fun registerUser(userData: UserData): Boolean {
        return try {
            // Check if user already exists
            if (getUserData(userData.cedula) != null) {
                Log.w(TAG, "User with cedula ${userData.cedula} already exists")
                return false
            }

            // Store complete user data as JSON backup
            val userJson = Json.encodeToString(userData)
            userDataPrefs.edit()
                .putString("$KEY_USER_DATA${userData.cedula}", userJson)
                .apply()

            // Store only cedula (ID number) in active user prefs
            activeUserPrefs.edit()
                .putString(KEY_ACTIVE_CEDULA, userData.cedula)
                .apply()

            Log.d(TAG, "✅ User registered successfully: ${userData.cedula}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Registration error: ${e.message}", e)
            false
        }
    }

    /**
     * Login user with email and password (local verification)
     * Verifies backup data matches and creates logged session with only cedula
     */
    fun loginUser(email: String, password: String): String? {
        // Find user by email and password in backup data
        val allKeys = userDataPrefs.all.keys
        for (key in allKeys) {
            if (key.startsWith(KEY_USER_DATA)) {
                val userJson = userDataPrefs.getString(key, null) ?: continue
                try {
                    val userData = Json.decodeFromString<UserData>(userJson)
                    if (userData.email == email && userData.password == password) {
                        // Create logged session with only cedula (ID number)
                        loggedUserPrefs.edit()
                            .putString(KEY_LOGGED_CEDULA, userData.cedula)
                            .apply()
                        
                        Log.d(TAG, "✅ Login successful for cedula: ${userData.cedula}")
                        return userData.cedula
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing user data: ${e.message}")
                    continue
                }
            }
        }
        Log.w(TAG, "❌ Login failed: Invalid credentials")
        return null
    }

    /**
     * Get complete user data by cedula (from backup)
     */
    fun getUserData(cedula: String): UserData? {
        return try {
            val userJson = userDataPrefs.getString("$KEY_USER_DATA$cedula", null)
            userJson?.let { Json.decodeFromString<UserData>(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user data: ${e.message}")
            null
        }
    }

    /**
     * Get current logged user cedula (only ID number from current session)
     */
    fun getLoggedUserCedula(): String? {
        return loggedUserPrefs.getString(KEY_LOGGED_CEDULA, null)
    }

    /**
     * Get active user cedula (only ID number from storage)
     */
    fun getActiveUserCedula(): String? {
        return activeUserPrefs.getString(KEY_ACTIVE_CEDULA, null)
    }

    /**
     * Check if user is logged in (verifies logged session exists)
     */
    fun isUserLoggedIn(): Boolean {
        return getLoggedUserCedula() != null
    }

    /**
     * Logout current user
     * Transfers cedula (ID number) from logged session to active storage
     */
    fun logout() {
        // Transfer logged cedula to active cedula (for next login)
        val loggedCedula = getLoggedUserCedula()
        if (loggedCedula != null) {
            activeUserPrefs.edit()
                .putString(KEY_ACTIVE_CEDULA, loggedCedula)
                .apply()
            Log.d(TAG, "📋 Cedula transferred to active storage: $loggedCedula")
        }

        // Clear logged user session
        loggedUserPrefs.edit().remove(KEY_LOGGED_CEDULA).apply()
        Log.d(TAG, "👋 User logged out")
    }

    /**
     * Clear all data (for testing purposes)
     */
    fun clearAllData() {
        userDataPrefs.edit().clear().apply()
        activeUserPrefs.edit().clear().apply()
        loggedUserPrefs.edit().clear().apply()
        Log.d(TAG, "🗑️ All data cleared")
    }
}