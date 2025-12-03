package com.kmusic.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmusic.data.model.Track
import com.kmusic.data.repository.TrackRepository
import kotlinx.coroutines.launch

/**
 * HomeViewModel - ViewModel for home screen
 */
class HomeViewModel(
    private val trackRepository: TrackRepository
) : ViewModel() {

    private val _tracks = MutableLiveData<List<Track>>()
    val tracks: LiveData<List<Track>> = _tracks

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Load tracks from repository
     */
    fun loadTracks(limit: Int = 50, offset: Int = 0) {
        _isLoading.value = true

        viewModelScope.launch {
            val result = trackRepository.getTracks(limit, offset)

            result.onSuccess { response ->
                _tracks.value = response.tracks
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to load tracks"
                _isLoading.value = false
            }
        }
    }

    /**
     * Search tracks
     */
    fun searchTracks(query: String) {
        if (query.isBlank()) {
            loadTracks()
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            val result = trackRepository.searchTracks(query)

            result.onSuccess { response ->
                _tracks.value = response.tracks
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Search failed"
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
