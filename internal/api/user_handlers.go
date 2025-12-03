package api

import (
	"database/sql"
	"encoding/json"
	"net/http"

	"musicstream/internal/models"
)

func (a *API) getUserPreferences(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	var prefs models.UserPreference
	err := a.storage.DB.DB.QueryRow(`
		SELECT user_id, language, theme, bitrate_prefer, auto_play, crossfade_duration,
			offline_mode, notify_new_tracks
		FROM user_preferences WHERE user_id = $1
	`, claims.UserID).Scan(&prefs.UserID, &prefs.Language, &prefs.Theme, &prefs.BitratePrefer,
		&prefs.AutoPlay, &prefs.CrossfadeDur, &prefs.OfflineMode, &prefs.NotifyNewTracks)

	if err != nil {
		if err == sql.ErrNoRows {
			respondError(w, http.StatusNotFound, "preferences not found")
			return
		}
		respondError(w, http.StatusInternalServerError, "database error")
		return
	}

	respondJSON(w, http.StatusOK, prefs)
}

func (a *API) updateUserPreferences(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	var prefs models.UserPreference
	if err := json.NewDecoder(r.Body).Decode(&prefs); err != nil {
		respondError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	_, err := a.storage.DB.DB.Exec(`
		UPDATE user_preferences
		SET language = $1, theme = $2, bitrate_prefer = $3, auto_play = $4,
			crossfade_duration = $5, offline_mode = $6, notify_new_tracks = $7
		WHERE user_id = $8
	`, prefs.Language, prefs.Theme, prefs.BitratePrefer, prefs.AutoPlay,
		prefs.CrossfadeDur, prefs.OfflineMode, prefs.NotifyNewTracks, claims.UserID)

	if err != nil {
		respondError(w, http.StatusInternalServerError, "failed to update preferences")
		return
	}

	respondJSON(w, http.StatusOK, map[string]string{"message": "preferences updated"})
}

func (a *API) search(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	query := r.URL.Query().Get("q")
	if query == "" {
		respondError(w, http.StatusBadRequest, "search query is required")
		return
	}

	searchPattern := "%" + query + "%"

	rows, err := a.storage.DB.DB.Query(`
		SELECT id, title, artist, album, album_artist, genre, year, track_number,
			duration, file_size, file_format, uploaded_by, play_count, created_at, updated_at
		FROM tracks
		WHERE title ILIKE $1 OR artist ILIKE $1 OR album ILIKE $1
		ORDER BY play_count DESC
		LIMIT 50
	`, searchPattern)

	if err != nil {
		respondError(w, http.StatusInternalServerError, "database error")
		return
	}
	defer rows.Close()

	tracks := []models.Track{}
	for rows.Next() {
		var track models.Track
		err := rows.Scan(&track.ID, &track.Title, &track.Artist, &track.Album, &track.AlbumArtist,
			&track.Genre, &track.Year, &track.TrackNumber, &track.Duration, &track.FileSize,
			&track.FileFormat, &track.UploadedBy, &track.PlayCount, &track.CreatedAt, &track.UpdatedAt)
		if err != nil {
			continue
		}
		tracks = append(tracks, track)
	}

	respondJSON(w, http.StatusOK, tracks)
}

func (a *API) getStats(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	type Stats struct {
		TotalTracks    int64 `json:"total_tracks"`
		TotalPlaylists int64 `json:"total_playlists"`
		TotalPlays     int64 `json:"total_plays"`
		TotalDuration  int64 `json:"total_duration_seconds"`
	}

	var stats Stats

	// Get total tracks
	a.storage.DB.DB.QueryRow(`SELECT COUNT(*) FROM tracks`).Scan(&stats.TotalTracks)

	// Get user's playlists
	a.storage.DB.DB.QueryRow(`SELECT COUNT(*) FROM playlists WHERE user_id = $1`, claims.UserID).Scan(&stats.TotalPlaylists)

	// Get total plays for user
	a.storage.DB.DB.QueryRow(`SELECT COUNT(*) FROM play_history WHERE user_id = $1`, claims.UserID).Scan(&stats.TotalPlays)

	// Get total duration of all tracks
	a.storage.DB.DB.QueryRow(`SELECT COALESCE(SUM(duration), 0) FROM tracks`).Scan(&stats.TotalDuration)

	respondJSON(w, http.StatusOK, stats)
}
