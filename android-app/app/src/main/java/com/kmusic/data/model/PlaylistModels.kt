package com.kmusic.data.model

import com.google.gson.annotations.SerializedName

/**
 * Playlist - Music playlist from the backend
 */
data class Playlist(
    val id: Long,
    val name: String,
    val description: String? = null,
    @SerializedName("user_id")
    val userId: Long,
    @SerializedName("is_public")
    val isPublic: Boolean = false,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

/**
 * PlaylistTrack - Track in a playlist with position
 */
data class PlaylistTrack(
    val id: Long,
    @SerializedName("playlist_id")
    val playlistId: Long,
    @SerializedName("track_id")
    val trackId: Long,
    val position: Int,
    @SerializedName("added_at")
    val addedAt: String,
    val track: Track? = null // Track details if included
)

/**
 * PlaylistWithTracks - Playlist with its tracks
 */
data class PlaylistWithTracks(
    val playlist: Playlist,
    val tracks: List<Track>
)

/**
 * CreatePlaylistRequest - Request to create a new playlist
 */
data class CreatePlaylistRequest(
    val name: String,
    val description: String? = null,
    @SerializedName("is_public")
    val isPublic: Boolean = false
)

/**
 * UpdatePlaylistRequest - Request to update playlist
 */
data class UpdatePlaylistRequest(
    val name: String? = null,
    val description: String? = null,
    @SerializedName("is_public")
    val isPublic: Boolean? = null
)

/**
 * AddTrackToPlaylistRequest - Request to add track to playlist
 */
data class AddTrackToPlaylistRequest(
    @SerializedName("track_id")
    val trackId: Long
)

/**
 * PlaylistsResponse - List of playlists
 */
data class PlaylistsResponse(
    val playlists: List<Playlist>
)
