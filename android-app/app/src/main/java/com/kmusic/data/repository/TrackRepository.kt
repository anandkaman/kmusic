package com.kmusic.data.repository

import com.kmusic.data.api.RetrofitClient
import com.kmusic.data.local.ServerConfigManager
import com.kmusic.data.model.Track
import com.kmusic.data.model.TracksResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * TrackRepository - Repository for track data operations
 *
 * Handles:
 * - Fetching tracks from the API
 * - Searching tracks
 * - Caching (future implementation)
 */
class TrackRepository(
    private val serverConfigManager: ServerConfigManager
) {

    /**
     * Get all tracks with pagination
     */
    suspend fun getTracks(limit: Int = 50, offset: Int = 0): Result<TracksResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val api = RetrofitClient.api
                val response = api.getTracks(limit, offset)

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to fetch tracks: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get a specific track by ID
     */
    suspend fun getTrack(trackId: Long): Result<Track> {
        return withContext(Dispatchers.IO) {
            try {
                val api = RetrofitClient.api
                val response = api.getTrack(trackId)

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to fetch track: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Search tracks by query
     */
    suspend fun searchTracks(query: String): Result<TracksResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val api = RetrofitClient.api
                val response = api.searchTracks(query)

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to search tracks: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get stream URL for a track
     */
    fun getStreamUrl(track: Track): String {
        return track.getStreamUrl(serverConfigManager.getBaseUrl())
    }

    /**
     * Get artwork URL for a track
     */
    fun getArtworkUrl(track: Track): String? {
        return track.getArtworkUrl(serverConfigManager.getBaseUrl())
    }
}
