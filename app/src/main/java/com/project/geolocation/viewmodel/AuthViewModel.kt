package com.project.geolocation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.project.geolocation.network.NetworkManager
import com.project.geolocation.network.RegisterRequest
import com.project.geolocation.security.SecureTokenManager
import com.project.geolocation.ui.screens.RegistrationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    data object Loading : AuthState()
    data class LoggedIn(val uid: String) : AuthState()
    data class LoggedOut(val error: String? = null) : AuthState()
}

class AuthViewModel(
    private val networkManager: NetworkManager,
    private val secureTokenManager: SecureTokenManager
) : ViewModel() {

    private val auth = Firebase.auth

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState = _authState.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val token = secureTokenManager.getToken()
        if (auth.currentUser != null && token != null) {
            _authState.value = AuthState.LoggedIn(auth.currentUser!!.uid)
        } else {
            _authState.value = AuthState.LoggedOut()
        }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                val user = result.user ?: throw Exception("Usuario de Firebase nulo")

                val firebaseToken = user.getIdToken(true).await()?.token
                    ?: throw Exception("Token de Firebase nulo")

                val jwtToken = networkManager.loginToInstance(firebaseToken)

                if (jwtToken != null) {
                    secureTokenManager.saveToken(jwtToken)
                    _authState.value = AuthState.LoggedIn(user.uid)
                } else {
                    _authState.value = AuthState.LoggedOut("Error al iniciar sesión en el backend")
                    auth.signOut()
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error en login: ${e.message}")
                _authState.value = AuthState.LoggedOut(e.message ?: "Error desconocido")
            }
        }
    }

    fun register(data: RegistrationData) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val request = RegisterRequest(
                    password = data.pass, // <-- CAMBIO
                    nombre_completo = data.nombre,
                    cedula = data.cedula,
                    email = data.email,
                    telefono = data.telefono,
                    empresa = data.empresa
                )

                Log.d("AuthViewModel", "Enviando solicitud de registro al backend...")
                val jwtToken = networkManager.registerOnAllInstances(request)

                if (jwtToken != null) {
                    secureTokenManager.saveToken(jwtToken)
                    val result = auth.signInWithEmailAndPassword(data.email, data.pass).await()
                    val user = result.user ?: throw Exception("Usuario nulo después de login post-registro")
                    
                    Log.d("AuthViewModel", "Registro y login exitosos.")
                    _authState.value = AuthState.LoggedIn(user.uid)
                
                } else {
                    Log.w("AuthViewModel", "El backend falló el registro.")
                    _authState.value = AuthState.LoggedOut("Error al registrar en el backend. El usuario podría ya existir.")
                }

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error en registro: ${e.message}")
                _authState.value = AuthState.LoggedOut(e.message ?: "Error desconocido")
            }
        }
    }

    fun logout() {
        auth.signOut()
        secureTokenManager.clearToken()
        _authState.value = AuthState.LoggedOut()
    }
}