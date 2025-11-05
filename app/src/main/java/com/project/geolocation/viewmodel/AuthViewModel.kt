package com.project.geolocation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.geolocation.security.LocalAuthManager
import com.project.geolocation.security.UserData
import com.project.geolocation.ui.screens.RegistrationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    data object Loading : AuthState()
    data class LoggedIn(val cedula: String) : AuthState()
    data class LoggedOut(val error: String? = null) : AuthState()
}

class AuthViewModel(
    private val localAuthManager: LocalAuthManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState = _authState.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        if (localAuthManager.isUserLoggedIn()) {
            val cedula = localAuthManager.getLoggedUserCedula()
            if (cedula != null) {
                _authState.value = AuthState.LoggedIn(cedula)
                Log.d("AuthViewModel", "✅ User already logged in: $cedula")
            } else {
                _authState.value = AuthState.LoggedOut()
            }
        } else {
            _authState.value = AuthState.LoggedOut()
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Local authentication - verify credentials from backup
                val cedula = localAuthManager.loginUser(email, password)
                
                if (cedula != null) {
                    Log.d("AuthViewModel", "✅ Login successful for cedula: $cedula")
                    _authState.value = AuthState.LoggedIn(cedula)
                } else {
                    Log.w("AuthViewModel", "❌ Login failed: Invalid credentials")
                    _authState.value = AuthState.LoggedOut("Credenciales incorrectas")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Login error: ${e.message}")
                _authState.value = AuthState.LoggedOut(e.message ?: "Error desconocido")
            }
        }
    }

    fun register(data: RegistrationData) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val userData = UserData(
                    nombreCompleto = data.nombre,
                    cedula = data.cedula,
                    email = data.email,
                    password = data.pass,
                    telefono = data.telefono,
                    empresa = data.empresa
                )

                // Register locally - store full backup and cedula
                val success = localAuthManager.registerUser(userData)

                if (success) {
                    Log.d("AuthViewModel", "✅ Registration successful for cedula: ${userData.cedula}")
                    // Auto-login after successful registration
                    _authState.value = AuthState.LoggedIn(userData.cedula)
                } else {
                    Log.w("AuthViewModel", "❌ Registration failed: User already exists")
                    _authState.value = AuthState.LoggedOut("El usuario con esta cédula ya existe")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Registration error: ${e.message}")
                _authState.value = AuthState.LoggedOut(e.message ?: "Error desconocido")
            }
        }
    }

    fun logout() {
        // Transfer cedula from logged session to active storage
        localAuthManager.logout()
        _authState.value = AuthState.LoggedOut()
        Log.d("AuthViewModel", "👋 User logged out")
    }
}