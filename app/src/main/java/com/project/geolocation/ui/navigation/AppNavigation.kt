package com.project.geolocation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.navigation
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.project.geolocation.ui.screens.LoginScreen
import com.project.geolocation.ui.screens.MainScreen
import com.project.geolocation.ui.screens.RegisterScreen
import com.project.geolocation.viewmodel.AuthViewModel
import com.project.geolocation.viewmodel.AuthState
import com.project.geolocation.viewmodel.MainViewModel

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    mainViewModel: MainViewModel
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.LoggedIn -> {
                navController.navigate("main") {
                    popUpTo("auth") { inclusive = true }
                }
            }
            is AuthState.LoggedOut -> {
                // Navega a 'login' solo si no estamos ya en una ruta de 'auth'
                if (navController.currentBackStackEntry?.destination?.parent?.route != "auth") {
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            }
            // ⬇️ CORRECCIÓN AQUÍ ⬇️
            // Se añade la rama que faltaba para que el 'when' sea exhaustivo.
            is AuthState.Loading -> {
                // No se necesita ninguna acción de navegación.
                // La UI mostrará el 'isLoading' (Spinner)
                // y este LaunchedEffect simplemente esperará al siguiente estado.
            }
        }
    }

    NavHost(navController = navController, startDestination = "auth") {

        navigation(route = "auth", startDestination = "login") {
            composable("login") {
                val error = if (authState is AuthState.LoggedOut) (authState as AuthState.LoggedOut).error else null
                LoginScreen(
                    onLoginClick = { email, pass -> authViewModel.login(email, pass) },
                    onNavigateToRegister = { navController.navigate("register") },
                    isLoading = authState is AuthState.Loading,
                    error = error
                )
            }
            composable("register") {
                val error = if (authState is AuthState.LoggedOut) (authState as AuthState.LoggedOut).error else null
                RegisterScreen(
                    onRegisterClick = { data -> authViewModel.register(data) },
                    onNavigateToLogin = { navController.popBackStack() }, // Regresa a login
                    isLoading = authState is AuthState.Loading,
                    error = error
                )
            }
        }

        composable("main") {
            MainScreen(
                mainViewModel = mainViewModel,
                onLogout = { authViewModel.logout() }
            )
        }
    }
}
