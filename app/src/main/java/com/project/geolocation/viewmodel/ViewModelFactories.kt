package com.project.geolocation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project.geolocation.location.LocationManager
import com.project.geolocation.network.NetworkManager
import com.project.geolocation.permissions.PermissionManager
import com.project.geolocation.security.SecureTokenManager

class AuthViewModelFactory(
    private val networkManager: NetworkManager,
    private val secureTokenManager: SecureTokenManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(networkManager, secureTokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MainViewModelFactory(
    private val locationManager: LocationManager,
    private val networkManager: NetworkManager,
    private val permissionManager: PermissionManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(locationManager, networkManager, permissionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}