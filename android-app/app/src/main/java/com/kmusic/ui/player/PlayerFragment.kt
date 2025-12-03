package com.kmusic.ui.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.slider.Slider
import com.kmusic.R
import com.kmusic.data.local.ServerConfigManager
import com.kmusic.data.model.PlaybackState
import com.kmusic.data.model.RepeatMode
import com.kmusic.data.model.ShuffleMode
import com.kmusic.data.model.Track
import com.kmusic.data.repository.TrackRepository
import com.kmusic.databinding.FragmentPlayerBinding
import com.kmusic.service.MusicService

/**
 * PlayerFragment - Music player UI
 *
 * Features:
 * - Displays current track info and album art
 * - Playback controls (play/pause/skip)
 * - Progress slider with seeking
 * - Shuffle and repeat modes
 * - Binds to MusicService for playback
 */
class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PlayerViewModel
    private lateinit var serverConfigManager: ServerConfigManager
    private lateinit var trackRepository: TrackRepository

    // Service binding
    private var musicService: MusicService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val musicBinder = binder as MusicService.MusicBinder
            musicService = musicBinder.getService()
            serviceBound = true

            // Connect service to ViewModel
            viewModel.musicService = musicService

            // Load initial state
            musicService?.getCurrentTrack()?.let { track ->
                updateTrackInfo(track)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
            musicService = null
            viewModel.musicService = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize managers and repository
        serverConfigManager = ServerConfigManager(requireContext())
        trackRepository = TrackRepository(serverConfigManager)

        // Initialize ViewModel
        val factory = PlayerViewModelFactory(trackRepository, serverConfigManager)
        viewModel = ViewModelProvider(this, factory)[PlayerViewModel::class.java]

        // Bind to MusicService
        bindMusicService()

        // Setup UI
        setupControls()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbindMusicService()
        _binding = null
    }

    /**
     * Bind to MusicService
     */
    private fun bindMusicService() {
        val intent = Intent(requireContext(), MusicService::class.java)
        requireContext().startService(intent) // Ensure service is started
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * Unbind from MusicService
     */
    private fun unbindMusicService() {
        if (serviceBound) {
            requireContext().unbindService(serviceConnection)
            serviceBound = false
        }
    }

    /**
     * Setup playback controls
     */
    private fun setupControls() {
        // Play/Pause button
        binding.playPauseButton.setOnClickListener {
            viewModel.togglePlayPause()
        }

        // Previous button
        binding.previousButton.setOnClickListener {
            viewModel.skipToPrevious()
        }

        // Next button
        binding.nextButton.setOnClickListener {
            viewModel.skipToNext()
        }

        // Shuffle button
        binding.shuffleButton.setOnClickListener {
            viewModel.toggleShuffle()
        }

        // Repeat button
        binding.repeatButton.setOnClickListener {
            viewModel.cycleRepeatMode()
        }

        // Progress slider
        binding.progressSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                // User started dragging
            }

            override fun onStopTrackingTouch(slider: Slider) {
                // User stopped dragging - seek to position
                viewModel.seekToProgress(slider.value.toInt())
            }
        })
    }

    /**
     * Observe ViewModel state
     */
    private fun observeViewModel() {
        // Current track
        viewModel.currentTrack.observe(viewLifecycleOwner) { track ->
            track?.let { updateTrackInfo(it) }
        }

        // Playback state
        viewModel.playbackState.observe(viewLifecycleOwner) { state ->
            updatePlaybackState(state)
        }

        // Is playing
        viewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            updatePlayPauseButton(isPlaying)
        }

        // Progress
        viewModel.progress.observe(viewLifecycleOwner) { progress ->
            // Only update if user is not currently dragging
            if (!binding.progressSlider.isPressed) {
                binding.progressSlider.value = progress.toFloat()
            }
        }

        // Current position
        viewModel.currentPosition.observe(viewLifecycleOwner) { position ->
            binding.currentTime.text = formatTime(position)
        }

        // Duration
        viewModel.duration.observe(viewLifecycleOwner) { duration ->
            binding.totalDuration.text = formatTime(duration)
            binding.progressSlider.valueTo = 100f
        }

        // Repeat mode
        viewModel.repeatMode.observe(viewLifecycleOwner) { mode ->
            updateRepeatButton(mode)
        }

        // Shuffle mode
        viewModel.shuffleMode.observe(viewLifecycleOwner) { mode ->
            updateShuffleButton(mode)
        }

        // Has next/previous
        viewModel.hasNext.observe(viewLifecycleOwner) { hasNext ->
            binding.nextButton.isEnabled = hasNext
            binding.nextButton.alpha = if (hasNext) 1f else 0.5f
        }

        viewModel.hasPrevious.observe(viewLifecycleOwner) { hasPrevious ->
            binding.previousButton.isEnabled = hasPrevious
            binding.previousButton.alpha = if (hasPrevious) 1f else 0.5f
        }

        // Error messages
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                // Show error toast or snackbar
                viewModel.clearError()
            }
        }
    }

    /**
     * Update track information display
     */
    private fun updateTrackInfo(track: Track) {
        binding.trackTitle.text = track.title
        binding.artistName.text = track.getDisplayArtist()
        binding.albumName.text = track.getDisplayAlbum()

        // TODO: Load album art with Glide
        // val artworkUrl = trackRepository.getArtworkUrl(track)
        // Glide.with(this)
        //     .load(artworkUrl)
        //     .placeholder(R.drawable.ic_music_note)
        //     .error(R.drawable.ic_music_note)
        //     .into(binding.albumArtImage)
    }

    /**
     * Update playback state (buffering indicator)
     */
    private fun updatePlaybackState(state: PlaybackState) {
        binding.bufferingIndicator.visibility = if (state == PlaybackState.BUFFERING) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    /**
     * Update play/pause button icon
     */
    private fun updatePlayPauseButton(isPlaying: Boolean) {
        binding.playPauseButton.setIconResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    /**
     * Update repeat button appearance
     */
    private fun updateRepeatButton(mode: RepeatMode) {
        when (mode) {
            RepeatMode.OFF -> {
                binding.repeatButton.setIconResource(R.drawable.ic_repeat)
                binding.repeatButton.iconTint = requireContext().getColorStateList(
                    com.google.android.material.R.attr.colorOnSurfaceVariant
                )
            }
            RepeatMode.ALL -> {
                binding.repeatButton.setIconResource(R.drawable.ic_repeat)
                binding.repeatButton.iconTint = requireContext().getColorStateList(
                    com.google.android.material.R.attr.colorPrimary
                )
            }
            RepeatMode.ONE -> {
                binding.repeatButton.setIconResource(R.drawable.ic_repeat_one)
                binding.repeatButton.iconTint = requireContext().getColorStateList(
                    com.google.android.material.R.attr.colorPrimary
                )
            }
        }
    }

    /**
     * Update shuffle button appearance
     */
    private fun updateShuffleButton(mode: ShuffleMode) {
        binding.shuffleButton.iconTint = requireContext().getColorStateList(
            if (mode == ShuffleMode.ON) {
                com.google.android.material.R.attr.colorPrimary
            } else {
                com.google.android.material.R.attr.colorOnSurfaceVariant
            }
        )
    }

    /**
     * Format milliseconds to MM:SS
     */
    private fun formatTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    /**
     * Public method to set queue and start playback
     * Called from other fragments (e.g., HomeFragment)
     */
    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        viewModel.setQueueAndPlay(tracks, startIndex)
    }

    /**
     * Public method to play a single track
     */
    fun playTrack(track: Track) {
        viewModel.playTrack(track)
    }
}
