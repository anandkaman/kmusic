package com.kmusic.data.model

import com.google.gson.annotations.SerializedName

/**
 * Track - Music track from the backend
 */
data class Track(
    val id: Long,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    @SerializedName("album_artist")
    val albumArtist: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    @SerializedName("track_number")
    val trackNumber: Int? = null,
    @SerializedName("disc_number")
    val discNumber: Int? = null,
    val duration: Int = 0, // Duration in seconds
    @SerializedName("file_path")
    val filePath: String,
    @SerializedName("file_size")
    val fileSize: Long = 0,
    val format: String? = null,
    @SerializedName("bit_rate")
    val bitRate: Int? = null,
    @SerializedName("sample_rate")
    val sampleRate: Int? = null,
    @SerializedName("uploaded_by")
    val uploadedBy: Long,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
) {
    /**
     * Get display artist (prefer artist, fallback to album artist)
     */
    fun getDisplayArtist(): String {
        return artist ?: albumArtist ?: "Unknown Artist"
    }

    /**
     * Get display album
     */
    fun getDisplayAlbum(): String {
        return album ?: "Unknown Album"
    }

    /**
     * Get formatted duration (MM:SS)
     */
    fun getFormattedDuration(): String {
        val minutes = duration / 60
        val seconds = duration % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    /**
     * Get stream URL for this track
     */
    fun getStreamUrl(baseUrl: String): String {
        return "$baseUrl/api/tracks/$id/stream"
    }

    /**
     * Get artwork URL for this track (if available)
     */
    fun getArtworkUrl(baseUrl: String): String? {
        return "$baseUrl/api/tracks/$id/artwork"
    }
}

/**
 * TracksResponse - List of tracks from the API
 */
data class TracksResponse(
    val tracks: List<Track>,
    val total: Int
)

/**
 * SearchRequest - Search query
 */
data class SearchRequest(
    val query: String,
    val limit: Int = 50
)

/**
 * UploadResponse - Response after track upload
 */
data class UploadResponse(
    val message: String,
    val track: Track
)

/**
 * PlaybackState - Current playback state
 */
enum class PlaybackState {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    STOPPED,
    ERROR
}

/**
 * RepeatMode - Playback repeat mode
 */
enum class RepeatMode {
    OFF,      // No repeat
    ONE,      // Repeat current track
    ALL       // Repeat entire queue
}

/**
 * ShuffleMode - Playback shuffle mode
 */
enum class ShuffleMode {
    OFF,      // Play in order
    ON        // Shuffle queue
}
