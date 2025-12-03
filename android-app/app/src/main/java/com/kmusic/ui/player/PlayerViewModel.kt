package com.kmusic.ui.player

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmusic.data.local.ServerConfigManager
import com.kmusic.data.model.PlaybackState
import com.kmusic.data.model.RepeatMode
import com.kmusic.data.model.ShuffleMode
import com.kmusic.data.model.Track
import com.kmusic.data.repository.TrackRepository
import com.kmusic.service.MusicService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * PlayerViewModel - ViewModel for music player
 *
 * Manages:
 * - Current track state
 * - Playback controls
 * - Progress updates
 * - Queue management
 * - Communication with MusicService
 */
class PlayerViewModel(
    private val trackRepository: TrackRepository,
    private val serverConfigManager: ServerConfigManager
) : ViewModel() {

    // Service reference (set from Fragment after binding)
    var musicService: MusicService? = null
        set(value) {
            field = value
            value?.addPlaybackListener(playbackListener)
            // Update current state from service
            value?.getCurrentTrack()?.let { track ->
                _currentTrack.value = track
                _isPlaying.value = value.isPlaying()
            }
        }

    // Current track
    private val _currentTrack = MutableLiveData<Track?>()
    val currentTrack: LiveData<Track?> = _currentTrack

    // Playback state
    private val _playbackState = MutableLiveData<PlaybackState>(PlaybackState.IDLE)
    val playbackState: LiveData<PlaybackState> = _playbackState

    private val _isPlaying = MutableLiveData<Boolean>(false)
    val isPlaying: LiveData<Boolean> = _isPlaying

    // Progress (0-100)
    private val _progress = MutableLiveData<Int>(0)
    val progress: LiveData<Int> = _progress

    // Current position (milliseconds)
    private val _currentPosition = MutableLiveData<Long>(0)
    val currentPosition: LiveData<Long> = _currentPosition

    // Duration (milliseconds)
    private val _duration = MutableLiveData<Long>(0)
    val duration: LiveData<Long> = _duration

    // Repeat and shuffle modes
    private val _repeatMode = MutableLiveData<RepeatMode>(RepeatMode.OFF)
    val repeatMode: LiveData<RepeatMode> = _repeatMode

    private val _shuffleMode = MutableLiveData<ShuffleMode>(ShuffleMode.OFF)
    val shuffleMode: LiveData<ShuffleMode> = _shuffleMode

    // Queue
    private val _queue = MutableLiveData<List<Track>>(emptyList())
    val queue: LiveData<List<Track>> = _queue

    // Has next/previous
    private val _hasNext = MutableLiveData<Boolean>(false)
    val hasNext: LiveData<Boolean> = _hasNext

    private val _hasPrevious = MutableLiveData<Boolean>(false)
    val hasPrevious: LiveData<Boolean> = _hasPrevious

    // Error state
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Progress update job
    private var progressUpdateJob: Job? = null

    /**
     * Playback listener for MusicService callbacks
     */
    private val playbackListener = object : MusicService.PlaybackListener {
        override fun onPlaybackStateChanged(state: PlaybackState) {
            _playbackState.postValue(state)
            _isPlaying.postValue(state == PlaybackState.PLAYING)

            when (state) {
                PlaybackState.PLAYING -> startProgressUpdates()
                PlaybackState.PAUSED, PlaybackState.STOPPED -> stopProgressUpdates()
                else -> {}
            }
        }

        override fun onTrackChanged(track: Track) {
            _currentTrack.postValue(track)
            updateQueueState()

            // Reset progress for new track
            _progress.postValue(0)
            _currentPosition.postValue(0)

            // Update duration when ready
            viewModelScope.launch {
                delay(100) // Wait for player to be ready
                val duration = musicService?.getDuration() ?: 0
                _duration.postValue(duration)
            }
        }
    }

    /**
     * Play a track
     */
    fun playTrack(track: Track) {
        val baseUrl = serverConfigManager.getBaseUrl()
        musicService?.playTrack(track, baseUrl)
        _currentTrack.value = track
    }

    /**
     * Set queue and play from start
     */
    fun setQueueAndPlay(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) {
            _errorMessage.value = "Queue is empty"
            return
        }

        val baseUrl = serverConfigManager.getBaseUrl()
        musicService?.setQueue(tracks, startIndex)
        musicService?.playFromQueue(startIndex, baseUrl)

        _queue.value = tracks
        updateQueueState()
    }

    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        musicService?.togglePlayPause()
    }

    /**
     * Skip to next track
     */
    fun skipToNext() {
        val baseUrl = serverConfigManager.getBaseUrl()
        musicService?.skipToNext()

        // MusicService will notify via callback, but we need to trigger playback
        viewModelScope.launch {
            delay(50)
            musicService?.getCurrentTrack()?.let { track ->
                musicService?.playTrack(track, baseUrl)
            }
        }
    }

    /**
     * Skip to previous track
     */
    fun skipToPrevious() {
        val baseUrl = serverConfigManager.getBaseUrl()

        // If more than 3 seconds into track, restart it
        val currentPos = musicService?.getCurrentPosition() ?: 0
        if (currentPos > 3000) {
            musicService?.seekTo(0)
        } else {
            musicService?.skipToPrevious()

            viewModelScope.launch {
                delay(50)
                musicService?.getCurrentTrack()?.let { track ->
                    musicService?.playTrack(track, baseUrl)
                }
            }
        }
    }

    /**
     * Seek to position (0-100)
     */
    fun seekToProgress(progressPercent: Int) {
        val duration = musicService?.getDuration() ?: 0
        if (duration > 0) {
            val position = (duration * progressPercent / 100).toLong()
            musicService?.seekTo(position)
            _currentPosition.value = position
            _progress.value = progressPercent
        }
    }

    /**
     * Toggle shuffle mode
     */
    fun toggleShuffle() {
        val currentMode = _shuffleMode.value ?: ShuffleMode.OFF
        val newMode = if (currentMode == ShuffleMode.OFF) ShuffleMode.ON else ShuffleMode.OFF

        musicService?.setShuffleMode(newMode)
        _shuffleMode.value = newMode

        // Update queue display
        _queue.value = musicService?.getQueue() ?: emptyList()
    }

    /**
     * Cycle repeat mode (OFF -> ALL -> ONE -> OFF)
     */
    fun cycleRepeatMode() {
        val currentMode = _repeatMode.value ?: RepeatMode.OFF
        val newMode = when (currentMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }

        musicService?.setRepeatMode(newMode)
        _repeatMode.value = newMode
    }

    /**
     * Add track to queue
     */
    fun addToQueue(track: Track) {
        musicService?.addToQueue(track)
        _queue.value = musicService?.getQueue() ?: emptyList()
        updateQueueState()
    }

    /**
     * Remove track from queue
     */
    fun removeFromQueue(index: Int) {
        musicService?.removeFromQueue(index)
        _queue.value = musicService?.getQueue() ?: emptyList()
        updateQueueState()
    }

    /**
     * Update queue state (has next/previous)
     */
    private fun updateQueueState() {
        _hasNext.value = musicService?.hasNext() ?: false
        _hasPrevious.value = musicService?.hasPrevious() ?: false
    }

    /**
     * Start periodic progress updates
     */
    private fun startProgressUpdates() {
        stopProgressUpdates()

        progressUpdateJob = viewModelScope.launch {
            while (isActive) {
                val position = musicService?.getCurrentPosition() ?: 0
                val duration = musicService?.getDuration() ?: 0

                _currentPosition.postValue(position)
                _duration.postValue(duration)

                if (duration > 0) {
                    val progressPercent = ((position * 100) / duration).toInt().coerceIn(0, 100)
                    _progress.postValue(progressPercent)
                }

                delay(500) // Update every 500ms
            }
        }
    }

    /**
     * Stop progress updates
     */
    private fun stopProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressUpdates()
        musicService?.removePlaybackListener(playbackListener)
    }
}
