package com.project.geolocation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.project.geolocation.network.NetworkManager
import com.project.geolocation.network.RegisterStep1Request
import com.project.geolocation.network.RegisterStep2Request
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
        // Esta lógica requiere que AMBOS, el SDK de Firebase y tu token, existan
        if (auth.currentUser != null && token != null) {
            _authState.value = AuthState.LoggedIn(auth.currentUser!!.uid)
        } else {
            _authState.value = AuthState.LoggedOut()
        }
    }

    // --- FUNCIÓN DE LOGIN MODIFICADA ---
    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // 1. Iniciar sesión en el SDK de Firebase local PRIMERO.
                // Esto es necesario para que checkCurrentUser() funcione.
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                val user = result.user ?: throw Exception("Usuario de Firebase nulo")

                Log.d("AuthViewModel", "Login en SDK Firebase local exitoso. Obteniendo token JWT del backend...")

                // 2. Ahora, iniciar sesión en el backend (Oliver) con email/pass
                // para obtener el token JWT personalizado.
                val jwtToken = networkManager.loginToInstance(email, pass)

                if (jwtToken != null) {
                    // 3. Guardar el token JWT personalizado
                    secureTokenManager.saveToken(jwtToken)
                    // 4. Actualizar estado a LoggedIn (usando el uid del paso 1)
                    _authState.value = AuthState.LoggedIn(user.uid)
                    Log.d("AuthViewModel", "Login en Backend (Oliver) exitoso. Token JWT guardado.")
                } else {
                    // Si el backend falla, cerramos la sesión de Firebase local
                    Log.w("AuthViewModel", "Error al iniciar sesión en el backend (Oliver).")
                    auth.signOut() 
                    _authState.value = AuthState.LoggedOut("Error al iniciar sesión en el backend")
                }
            } catch (e: Exception) {
                // Esto capturará fallos del SDK de Firebase (ej. pass incorrecta)
                // o excepciones de Ktor (ej. red caída)
                Log.e("AuthViewModel", "Error en login: ${e.message}")
                _authState.value = AuthState.LoggedOut(e.message ?: "Error desconocido")
            }
        }
    }

    // --- Función de Registro (Sin cambios) ---
    fun register(data: RegistrationData) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // --- PASO 1: Crear en Firebase (via Oliver) ---
                val step1Request = RegisterStep1Request(
                    email = data.email,
                    password = data.pass
                )
                Log.d("AuthViewModel", "Iniciando Registro - Paso 1 (en Oliver)...")
                val uid = networkManager.registerStep1(step1Request)

                if (uid == null) {
                    Log.w("AuthViewModel", "Paso 1 fallido. UID nulo.")
                    throw Exception("Error en Paso 1: No se pudo crear el usuario. El email podría ya existir.")
                }
                
                Log.d("AuthViewModel", "Paso 1 exitoso. UID: $uid. Iniciando Paso 2 (en todas las instancias)...")

                // --- PASO 2: Registrar en todas las BD locales ---
                val step2Request = RegisterStep2Request(
                    uid = uid,
                    nombre_completo = data.nombre,
                    cedula = data.cedula,
                    email = data.email,
                    telefono = data.telefono,
                    empresa = data.empresa
                )

                val step2Results = networkManager.registerStep2(step2Request)

                // 1. Verificar que TODAS las instancias funcionaron
                val allSucceeded = step2Results.values.all { it != null && it.status == "success" && it.token != null }
                
                // 2. Obtener el token específico de Oliver (el que vamos a guardar)
                val oliverToken = step2Results["Oliver"]?.token

                // 3. Comprobar la lógica solicitada
                if (allSucceeded && oliverToken != null) {
                    // --- ÉXITO TOTAL ---
                    Log.d("AuthViewModel", "Paso 2 exitoso en TODAS las instancias. Guardando token de Oliver.")
                    secureTokenManager.saveToken(oliverToken)

                    // Iniciar sesión en Firebase (localmente) para completar el flujo
                    val result = auth.signInWithEmailAndPassword(data.email, data.pass).await()
                    val user = result.user ?: throw Exception("Usuario nulo después de login post-registro")
                    
                    _authState.value = AuthState.LoggedIn(user.uid)
                    Log.d("AuthViewModel", "Registro y login local de Firebase completados.")

                } else {
                    // --- FALLO PARCIAL O TOTAL EN PASO 2 ---
                    Log.e("AuthViewModel", "Paso 2 fallido. No todas las instancias tuvieron éxito.")
                    step2Results.forEach { (name, response) ->
                        if (response == null || response.status != "success" || response.token == null) {
                            Log.w("AuthViewModel", "Instancia fallida en Paso 2: $name")
                        }
                    }
                    throw Exception("Error en Paso 2: Falló el registro en una o más instancias. El backend debería haber revertido el Paso 1.")
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