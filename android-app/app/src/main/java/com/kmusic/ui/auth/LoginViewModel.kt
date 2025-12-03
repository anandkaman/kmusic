package com.kmusic.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmusic.data.api.RetrofitClient
import com.kmusic.data.local.ServerConfigManager
import com.kmusic.data.local.UserSessionManager
import com.kmusic.data.model.AuthResponse
import com.kmusic.data.model.LoginRequest
import com.kmusic.data.model.RegisterRequest
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val response: AuthResponse) : AuthState()
    data class Error(val message: String) : AuthState()
}

class LoginViewModel(
    private val sessionManager: UserSessionManager,
    private val serverConfigManager: ServerConfigManager
) : ViewModel() {

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    fun login(username: String, password: String) {
        if (!serverConfigManager.isConfigured()) {
            _authState.value = AuthState.Error("Please configure server settings first")
            return
        }

        if (username.isBlank()) {
            _authState.value = AuthState.Error("Username is required")
            return
        }

        if (password.isBlank()) {
            _authState.value = AuthState.Error("Password is required")
            return
        }

        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                val response = RetrofitClient.api.login(
                    LoginRequest(username, password)
                )

                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    sessionManager.saveSession(authResponse.token, authResponse.user)
                    _authState.value = AuthState.Success(authResponse)
                } else {
                    val errorMessage = when (response.code()) {
                        401 -> "Invalid username or password"
                        500 -> "Server error. Please try again later"
                        else -> "Login failed: ${response.message()}"
                    }
                    _authState.value = AuthState.Error(errorMessage)
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("Unable to resolve host") == true ->
                        "Cannot connect to server. Check server address"
                    e.message?.contains("timeout") == true ->
                        "Connection timeout. Check your network"
                    else -> "Network error: ${e.message}"
                }
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }

    fun register(username: String, email: String, password: String, displayName: String) {
        if (!serverConfigManager.isConfigured()) {
            _authState.value = AuthState.Error("Please configure server settings first")
            return
        }

        // Validation
        if (username.isBlank()) {
            _authState.value = AuthState.Error("Username is required")
            return
        }

        if (email.isBlank()) {
            _authState.value = AuthState.Error("Email is required")
            return
        }

        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return
        }

        if (displayName.isBlank()) {
            _authState.value = AuthState.Error("Display name is required")
            return
        }

        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                val response = RetrofitClient.api.register(
                    RegisterRequest(username, email, password, displayName)
                )

                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    sessionManager.saveSession(authResponse.token, authResponse.user)
                    _authState.value = AuthState.Success(authResponse)
                } else {
                    val errorMessage = when (response.code()) {
                        409 -> "Username or email already exists"
                        400 -> "Invalid input. Please check your details"
                        else -> "Registration failed: ${response.message()}"
                    }
                    _authState.value = AuthState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Network error: ${e.message}")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
