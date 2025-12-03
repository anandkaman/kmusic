package com.kmusic.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kmusic.data.local.ServerConfigManager
import com.kmusic.data.repository.TrackRepository

/**
 * Factory for creating PlayerViewModel with dependencies
 */
class PlayerViewModelFactory(
    private val trackRepository: TrackRepository,
    private val serverConfigManager: ServerConfigManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            return PlayerViewModel(trackRepository, serverConfigManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
