package com.kmusic.data.api

import com.kmusic.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface MusicApiService {

    // Auth endpoints
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @GET("/api/user/me")
    suspend fun getCurrentUser(): Response<User>

    // Health check
    @GET("/health")
    suspend fun healthCheck(): Response<Map<String, String>>

    // Track endpoints
    @GET("/api/tracks")
    suspend fun getTracks(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<TracksResponse>

    @GET("/api/tracks/{id}")
    suspend fun getTrack(@Path("id") trackId: Long): Response<Track>

    @GET("/api/tracks/search")
    suspend fun searchTracks(@Query("q") query: String): Response<TracksResponse>

    // Playlist endpoints
    @GET("/api/playlists")
    suspend fun getPlaylists(): Response<PlaylistsResponse>

    @GET("/api/playlists/{id}")
    suspend fun getPlaylist(@Path("id") playlistId: Long): Response<PlaylistWithTracks>

    @POST("/api/playlists")
    suspend fun createPlaylist(@Body request: CreatePlaylistRequest): Response<Playlist>

    @PUT("/api/playlists/{id}")
    suspend fun updatePlaylist(
        @Path("id") playlistId: Long,
        @Body request: UpdatePlaylistRequest
    ): Response<Playlist>

    @DELETE("/api/playlists/{id}")
    suspend fun deletePlaylist(@Path("id") playlistId: Long): Response<Map<String, String>>

    @POST("/api/playlists/{id}/tracks")
    suspend fun addTrackToPlaylist(
        @Path("id") playlistId: Long,
        @Body request: AddTrackToPlaylistRequest
    ): Response<Map<String, String>>

    @DELETE("/api/playlists/{playlistId}/tracks/{trackId}")
    suspend fun removeTrackFromPlaylist(
        @Path("playlistId") playlistId: Long,
        @Path("trackId") trackId: Long
    ): Response<Map<String, String>>
}
