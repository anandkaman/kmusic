package com.kmusic.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.kmusic.MainActivity
import com.kmusic.R
import com.kmusic.data.model.PlaybackState
import com.kmusic.data.model.RepeatMode
import com.kmusic.data.model.ShuffleMode
import com.kmusic.data.model.Track

/**
 * MusicService - Background music playback service
 *
 * Features:
 * - ExoPlayer integration for audio playback
 * - Media3 MediaSession for lock screen and notification controls
 * - Queue management with shuffle and repeat
 * - Foreground service with ongoing notification
 * - Playback state callbacks
 */
class MusicService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "kmusic_playback"
        private const val CHANNEL_NAME = "Music Playback"

        // Actions for notification buttons
        const val ACTION_PLAY = "com.kmusic.PLAY"
        const val ACTION_PAUSE = "com.kmusic.PAUSE"
        const val ACTION_PREVIOUS = "com.kmusic.PREVIOUS"
        const val ACTION_NEXT = "com.kmusic.NEXT"
        const val ACTION_STOP = "com.kmusic.STOP"
    }

    // Service binding
    private val binder = MusicBinder()

    // ExoPlayer and MediaSession
    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    // Playback state
    private var currentTrack: Track? = null
    private var playbackQueue: MutableList<Track> = mutableListOf()
    private var currentQueueIndex: Int = -1
    private var repeatMode: RepeatMode = RepeatMode.OFF
    private var shuffleMode: ShuffleMode = ShuffleMode.OFF
    private var originalQueue: List<Track>? = null // Store original queue for shuffle

    // Callbacks
    private val playbackListeners = mutableListOf<PlaybackListener>()

    override fun onCreate() {
        super.onCreate()
        initializePlayer()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_PLAY -> resume()
                ACTION_PAUSE -> pause()
                ACTION_PREVIOUS -> skipToPrevious()
                ACTION_NEXT -> skipToNext()
                ACTION_STOP -> stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }

    /**
     * Initialize ExoPlayer and MediaSession
     */
    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            addListener(playerListener)
        }

        mediaSession = MediaSession.Builder(this, exoPlayer!!)
            .build()
    }

    /**
     * Release player resources
     */
    private fun releasePlayer() {
        mediaSession?.release()
        mediaSession = null
        exoPlayer?.release()
        exoPlayer = null
    }

    /**
     * Player event listener
     */
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE -> notifyPlaybackState(PlaybackState.IDLE)
                Player.STATE_BUFFERING -> notifyPlaybackState(PlaybackState.BUFFERING)
                Player.STATE_READY -> {
                    val isPlaying = exoPlayer?.isPlaying == true
                    notifyPlaybackState(if (isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED)
                }
                Player.STATE_ENDED -> {
                    handleTrackEnded()
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateNotification()
            notifyPlaybackState(if (isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED)
        }
    }

    /**
     * Create notification channel (Android O+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows currently playing track"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Build foreground notification
     */
    private fun buildNotification(): Notification {
        val track = currentTrack
        val title = track?.title ?: getString(R.string.unknown_track)
        val artist = track?.getDisplayArtist() ?: getString(R.string.unknown_artist)

        // Intent to open app
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification actions
        val playPauseAction = if (isPlaying()) {
            NotificationCompat.Action(
                R.drawable.ic_pause,
                "Pause",
                createPendingIntent(ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                R.drawable.ic_play,
                "Play",
                createPendingIntent(ACTION_PLAY)
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .addAction(R.drawable.ic_skip_previous, "Previous", createPendingIntent(ACTION_PREVIOUS))
            .addAction(playPauseAction)
            .addAction(R.drawable.ic_skip_next, "Next", createPendingIntent(ACTION_NEXT))
            .setStyle(androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(mediaSession!!)
                .setShowActionsInCompactView(0, 1, 2))
            .build()
    }

    /**
     * Create PendingIntent for notification actions
     */
    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Update foreground notification
     */
    private fun updateNotification() {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    /**
     * Handle track ended
     */
    private fun handleTrackEnded() {
        when (repeatMode) {
            RepeatMode.ONE -> {
                // Repeat current track
                exoPlayer?.seekTo(0)
                exoPlayer?.play()
            }
            RepeatMode.ALL -> {
                // Go to next track, or first if at end
                if (hasNext()) {
                    skipToNext()
                } else {
                    skipToIndex(0)
                }
            }
            RepeatMode.OFF -> {
                // Go to next track if available
                if (hasNext()) {
                    skipToNext()
                } else {
                    notifyPlaybackState(PlaybackState.STOPPED)
                }
            }
        }
    }

    // === Public API ===

    /**
     * Play a track
     */
    fun playTrack(track: Track, baseUrl: String) {
        currentTrack = track
        val streamUrl = track.getStreamUrl(baseUrl)

        val mediaItem = MediaItem.fromUri(streamUrl)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
        exoPlayer?.play()

        updateNotification()
        notifyTrackChanged(track)
    }

    /**
     * Set playback queue
     */
    fun setQueue(tracks: List<Track>, startIndex: Int = 0) {
        playbackQueue.clear()
        playbackQueue.addAll(tracks)
        currentQueueIndex = startIndex

        // Reset shuffle state when setting new queue
        if (shuffleMode == ShuffleMode.OFF) {
            originalQueue = null
        } else {
            originalQueue = tracks.toList()
        }
    }

    /**
     * Play track from queue at index
     */
    fun playFromQueue(index: Int, baseUrl: String) {
        if (index in playbackQueue.indices) {
            currentQueueIndex = index
            playTrack(playbackQueue[index], baseUrl)
        }
    }

    /**
     * Add track to queue
     */
    fun addToQueue(track: Track) {
        playbackQueue.add(track)
    }

    /**
     * Remove track from queue
     */
    fun removeFromQueue(index: Int) {
        if (index in playbackQueue.indices) {
            playbackQueue.removeAt(index)
            if (currentQueueIndex > index) {
                currentQueueIndex--
            }
        }
    }

    /**
     * Get current queue
     */
    fun getQueue(): List<Track> = playbackQueue.toList()

    /**
     * Play/Resume playback
     */
    fun resume() {
        exoPlayer?.play()
    }

    /**
     * Pause playback
     */
    fun pause() {
        exoPlayer?.pause()
    }

    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        if (isPlaying()) {
            pause()
        } else {
            resume()
        }
    }

    /**
     * Skip to next track
     */
    fun skipToNext() {
        if (hasNext()) {
            currentQueueIndex++
            currentTrack?.let {
                // We need base URL here - will be passed from UI layer
                notifyTrackChanged(playbackQueue[currentQueueIndex])
            }
        }
    }

    /**
     * Skip to previous track
     */
    fun skipToPrevious() {
        if (hasPrevious()) {
            currentQueueIndex--
            currentTrack?.let {
                notifyTrackChanged(playbackQueue[currentQueueIndex])
            }
        }
    }

    /**
     * Skip to specific index in queue
     */
    fun skipToIndex(index: Int) {
        if (index in playbackQueue.indices) {
            currentQueueIndex = index
            notifyTrackChanged(playbackQueue[index])
        }
    }

    /**
     * Seek to position (milliseconds)
     */
    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    /**
     * Set repeat mode
     */
    fun setRepeatMode(mode: RepeatMode) {
        repeatMode = mode
    }

    /**
     * Set shuffle mode
     */
    fun setShuffleMode(mode: ShuffleMode) {
        shuffleMode = mode

        when (mode) {
            ShuffleMode.ON -> {
                // Save original queue and shuffle
                if (originalQueue == null) {
                    originalQueue = playbackQueue.toList()
                }
                val currentTrack = if (currentQueueIndex >= 0) playbackQueue[currentQueueIndex] else null
                playbackQueue.shuffle()
                // Keep current track at current position
                currentTrack?.let { track ->
                    val newIndex = playbackQueue.indexOf(track)
                    if (newIndex != currentQueueIndex && newIndex >= 0) {
                        playbackQueue.removeAt(newIndex)
                        playbackQueue.add(currentQueueIndex, track)
                    }
                }
            }
            ShuffleMode.OFF -> {
                // Restore original queue
                originalQueue?.let { original ->
                    val currentTrack = if (currentQueueIndex >= 0) playbackQueue[currentQueueIndex] else null
                    playbackQueue.clear()
                    playbackQueue.addAll(original)
                    currentTrack?.let { track ->
                        currentQueueIndex = playbackQueue.indexOf(track)
                    }
                    originalQueue = null
                }
            }
        }
    }

    /**
     * Check if currently playing
     */
    fun isPlaying(): Boolean = exoPlayer?.isPlaying == true

    /**
     * Check if has next track
     */
    fun hasNext(): Boolean = currentQueueIndex < playbackQueue.size - 1

    /**
     * Check if has previous track
     */
    fun hasPrevious(): Boolean = currentQueueIndex > 0

    /**
     * Get current position (milliseconds)
     */
    fun getCurrentPosition(): Long = exoPlayer?.currentPosition ?: 0

    /**
     * Get duration (milliseconds)
     */
    fun getDuration(): Long = exoPlayer?.duration ?: 0

    /**
     * Get current track
     */
    fun getCurrentTrack(): Track? = currentTrack

    /**
     * Get repeat mode
     */
    fun getRepeatMode(): RepeatMode = repeatMode

    /**
     * Get shuffle mode
     */
    fun getShuffleMode(): ShuffleMode = shuffleMode

    // === Listener management ===

    fun addPlaybackListener(listener: PlaybackListener) {
        playbackListeners.add(listener)
    }

    fun removePlaybackListener(listener: PlaybackListener) {
        playbackListeners.remove(listener)
    }

    private fun notifyPlaybackState(state: PlaybackState) {
        playbackListeners.forEach { it.onPlaybackStateChanged(state) }
    }

    private fun notifyTrackChanged(track: Track) {
        playbackListeners.forEach { it.onTrackChanged(track) }
    }

    // === Binder ===

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    /**
     * Playback listener interface
     */
    interface PlaybackListener {
        fun onPlaybackStateChanged(state: PlaybackState)
        fun onTrackChanged(track: Track)
    }
}
