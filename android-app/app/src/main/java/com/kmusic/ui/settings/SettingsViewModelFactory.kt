package com.kmusic.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kmusic.data.local.ServerConfigManager
import com.kmusic.data.local.ThemePreferenceManager
import com.kmusic.data.local.UserSessionManager

/**
 * Factory for creating SettingsViewModel with dependencies
 */
class SettingsViewModelFactory(
    private val userSessionManager: UserSessionManager,
    private val serverConfigManager: ServerConfigManager,
    private val themePreferenceManager: ThemePreferenceManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(
                userSessionManager,
                serverConfigManager,
                themePreferenceManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
