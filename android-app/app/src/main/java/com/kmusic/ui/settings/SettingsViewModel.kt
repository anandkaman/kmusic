package com.kmusic.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmusic.data.local.ServerConfigManager
import com.kmusic.data.local.ThemePreferenceManager
import com.kmusic.data.local.UserSessionManager
import com.kmusic.data.model.User
import kotlinx.coroutines.launch
import java.io.File

/**
 * SettingsViewModel - ViewModel for settings screen
 *
 * Manages:
 * - User information display
 * - Theme preferences
 * - Server configuration
 * - Logout functionality
 * - File upload (admin only)
 */
class SettingsViewModel(
    private val userSessionManager: UserSessionManager,
    private val serverConfigManager: ServerConfigManager,
    private val themePreferenceManager: ThemePreferenceManager
) : ViewModel() {

    // User info
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    // Theme
    private val _currentTheme = MutableLiveData<String>()
    val currentTheme: LiveData<String> = _currentTheme

    // Server
    private val _serverAddress = MutableLiveData<String>()
    val serverAddress: LiveData<String> = _serverAddress

    // Upload state
    private val _uploadProgress = MutableLiveData<Int>(0)
    val uploadProgress: LiveData<Int> = _uploadProgress

    private val _uploadStatus = MutableLiveData<UploadStatus>(UploadStatus.Idle)
    val uploadStatus: LiveData<UploadStatus> = _uploadStatus

    // Events
    private val _logoutEvent = MutableLiveData<Boolean>()
    val logoutEvent: LiveData<Boolean> = _logoutEvent

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadUserInfo()
        loadTheme()
        loadServerInfo()
    }

    /**
     * Load current user information
     */
    private fun loadUserInfo() {
        val user = userSessionManager.getUser()
        _currentUser.value = user
    }

    /**
     * Load current theme
     */
    private fun loadTheme() {
        _currentTheme.value = themePreferenceManager.getTheme()
    }

    /**
     * Load server information
     */
    private fun loadServerInfo() {
        val address = serverConfigManager.getServerAddress()
        val port = serverConfigManager.getPort()
        val protocol = if (serverConfigManager.useHttps()) "https" else "http"
        _serverAddress.value = "$protocol://$address:$port"
    }

    /**
     * Change theme
     */
    fun changeTheme(theme: String) {
        themePreferenceManager.saveTheme(theme)
        themePreferenceManager.applyTheme(theme)
        _currentTheme.value = theme
    }

    /**
     * Get theme display name
     */
    fun getThemeDisplayName(): String {
        return themePreferenceManager.getThemeDisplayName()
    }

    /**
     * Check if user is admin
     */
    fun isAdmin(): Boolean {
        return userSessionManager.isAdmin()
    }

    /**
     * Upload music file (admin only)
     */
    fun uploadMusicFile(file: File) {
        if (!isAdmin()) {
            _errorMessage.value = "Only admins can upload music"
            return
        }

        viewModelScope.launch {
            try {
                _uploadStatus.value = UploadStatus.Uploading
                _uploadProgress.value = 0

                // TODO: Implement actual file upload with Retrofit
                // This requires multipart file upload
                // For now, show a placeholder

                // Simulate upload progress
                for (i in 0..100 step 10) {
                    _uploadProgress.value = i
                    kotlinx.coroutines.delay(100)
                }

                _uploadStatus.value = UploadStatus.Success
                _uploadProgress.value = 100

            } catch (e: Exception) {
                _uploadStatus.value = UploadStatus.Error(e.message ?: "Upload failed")
                _errorMessage.value = e.message ?: "Upload failed"
            }
        }
    }

    /**
     * Reset upload state
     */
    fun resetUploadState() {
        _uploadStatus.value = UploadStatus.Idle
        _uploadProgress.value = 0
    }

    /**
     * Logout current user
     */
    fun logout() {
        userSessionManager.clearSession()
        _logoutEvent.value = true
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Upload status sealed class
     */
    sealed class UploadStatus {
        object Idle : UploadStatus()
        object Uploading : UploadStatus()
        object Success : UploadStatus()
        data class Error(val message: String) : UploadStatus()
    }
}
