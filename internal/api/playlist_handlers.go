package api

import (
	"database/sql"
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/go-chi/chi/v5"
	"musicstream/internal/models"
)

func (a *API) createPlaylist(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	var req models.CreatePlaylistRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	if req.Name == "" {
		respondError(w, http.StatusBadRequest, "playlist name is required")
		return
	}

	var playlistID int64
	err := a.storage.DB.DB.QueryRow(`
		INSERT INTO playlists (name, description, user_id, is_public)
		VALUES ($1, $2, $3, $4)
		RETURNING id
	`, req.Name, req.Description, claims.UserID, req.IsPublic).Scan(&playlistID)

	if err != nil {
		respondError(w, http.StatusInternalServerError, "failed to create playlist")
		return
	}

	var playlist models.Playlist
	err = a.storage.DB.DB.QueryRow(`
		SELECT id, name, description, user_id, is_public, track_count, created_at, updated_at
		FROM playlists WHERE id = $1
	`, playlistID).Scan(&playlist.ID, &playlist.Name, &playlist.Description, &playlist.UserID,
		&playlist.IsPublic, &playlist.TrackCount, &playlist.CreatedAt, &playlist.UpdatedAt)

	if err != nil {
		respondError(w, http.StatusInternalServerError, "failed to retrieve playlist")
		return
	}

	respondJSON(w, http.StatusCreated, playlist)
}

func (a *API) getPlaylists(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	rows, err := a.storage.DB.DB.Query(`
		SELECT id, name, description, user_id, is_public, track_count, created_at, updated_at
		FROM playlists
		WHERE user_id = $1 OR is_public = true
		ORDER BY created_at DESC
	`, claims.UserID)

	if err != nil {
		respondError(w, http.StatusInternalServerError, "database error")
		return
	}
	defer rows.Close()

	playlists := []models.Playlist{}
	for rows.Next() {
		var playlist models.Playlist
		err := rows.Scan(&playlist.ID, &playlist.Name, &playlist.Description, &playlist.UserID,
			&playlist.IsPublic, &playlist.TrackCount, &playlist.CreatedAt, &playlist.UpdatedAt)
		if err != nil {
			continue
		}
		playlists = append(playlists, playlist)
	}

	respondJSON(w, http.StatusOK, playlists)
}

func (a *API) getPlaylist(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	playlistID := chi.URLParam(r, "id")

	var playlist models.Playlist
	err := a.storage.DB.DB.QueryRow(`
		SELECT id, name, description, user_id, is_public, track_count, created_at, updated_at
		FROM playlists WHERE id = $1
	`, playlistID).Scan(&playlist.ID, &playlist.Name, &playlist.Description, &playlist.UserID,
		&playlist.IsPublic, &playlist.TrackCount, &playlist.CreatedAt, &playlist.UpdatedAt)

	if err != nil {
		if err == sql.ErrNoRows {
			respondError(w, http.StatusNotFound, "playlist not found")
			return
		}
		respondError(w, http.StatusInternalServerError, "database error")
		return
	}

	// Check access permissions
	if !playlist.IsPublic && playlist.UserID != claims.UserID {
		respondError(w, http.StatusForbidden, "access denied")
		return
	}

	// Get playlist tracks
	rows, err := a.storage.DB.DB.Query(`
		SELECT t.id, t.title, t.artist, t.album, t.album_artist, t.genre, t.year,
			t.track_number, t.duration, t.file_size, t.file_format, t.uploaded_by,
			t.play_count, t.created_at, t.updated_at, pt.position
		FROM playlist_tracks pt
		JOIN tracks t ON pt.track_id = t.id
		WHERE pt.playlist_id = $1
		ORDER BY pt.position ASC
	`, playlistID)

	if err != nil {
		respondError(w, http.StatusInternalServerError, "database error")
		return
	}
	defer rows.Close()

	type PlaylistWithTracks struct {
		models.Playlist
		Tracks []models.Track `json:"tracks"`
	}

	result := PlaylistWithTracks{
		Playlist: playlist,
		Tracks:   []models.Track{},
	}

	for rows.Next() {
		var track models.Track
		var position int
		err := rows.Scan(&track.ID, &track.Title, &track.Artist, &track.Album, &track.AlbumArtist,
			&track.Genre, &track.Year, &track.TrackNumber, &track.Duration, &track.FileSize,
			&track.FileFormat, &track.UploadedBy, &track.PlayCount, &track.CreatedAt,
			&track.UpdatedAt, &position)
		if err != nil {
			continue
		}
		result.Tracks = append(result.Tracks, track)
	}

	respondJSON(w, http.StatusOK, result)
}

func (a *API) updatePlaylist(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	playlistID := chi.URLParam(r, "id")

	var req models.CreatePlaylistRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	// Check ownership
	var userID int64
	err := a.storage.DB.DB.QueryRow(`SELECT user_id FROM playlists WHERE id = $1`, playlistID).Scan(&userID)
	if err != nil {
		if err == sql.ErrNoRows {
			respondError(w, http.StatusNotFound, "playlist not found")
			return
		}
		respondError(w, http.StatusInternalServerError, "database error")
		return
	}

	if userID != claims.UserID {
		respondError(w, http.StatusForbidden, "not authorized to update this playlist")
		return
	}

	_, err = a.storage.DB.DB.Exec(`
		UPDATE playlists
		SET name = $1, description = $2, is_public = $3, updated_at = CURRENT_TIMESTAMP
		WHERE id = $4
	`, req.Name, req.Description, req.IsPublic, playlistID)

	if err != nil {
		respondError(w, http.StatusInternalServerError, "failed to update playlist")
		return
	}

	respondJSON(w, http.StatusOK, map[string]string{"message": "playlist updated"})
}

func (a *API) deletePlaylist(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	playlistID := chi.URLParam(r, "id")

	// Check ownership
	var userID int64
	err := a.storage.DB.DB.QueryRow(`SELECT user_id FROM playlists WHERE id = $1`, playlistID).Scan(&userID)
	if err != nil {
		if err == sql.ErrNoRows {
			respondError(w, http.StatusNotFound, "playlist not found")
			return
		}
		respondError(w, http.StatusInternalServerError, "database error")
		return
	}

	if userID != claims.UserID {
		respondError(w, http.StatusForbidden, "not authorized to delete this playlist")
		return
	}

	_, err = a.storage.DB.DB.Exec(`DELETE FROM playlists WHERE id = $1`, playlistID)
	if err != nil {
		respondError(w, http.StatusInternalServerError, "failed to delete playlist")
		return
	}

	respondJSON(w, http.StatusOK, map[string]string{"message": "playlist deleted"})
}

func (a *API) addTrackToPlaylist(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	playlistID := chi.URLParam(r, "id")

	var req models.AddTrackToPlaylistRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	// Check ownership
	var userID int64
	err := a.storage.DB.DB.QueryRow(`SELECT user_id FROM playlists WHERE id = $1`, playlistID).Scan(&userID)
	if err != nil {
		if err == sql.ErrNoRows {
			respondError(w, http.StatusNotFound, "playlist not found")
			return
		}
		respondError(w, http.StatusInternalServerError, "database error")
		return
	}

	if userID != claims.UserID {
		respondError(w, http.StatusForbidden, "not authorized to modify this playlist")
		return
	}

	// Get max position
	var maxPosition sql.NullInt64
	err = a.storage.DB.DB.QueryRow(`
		SELECT MAX(position) FROM playlist_tracks WHERE playlist_id = $1
	`, playlistID).Scan(&maxPosition)

	if err != nil {
		respondError(w, http.StatusInternalServerError, "database error")
		return
	}

	newPosition := 0
	if maxPosition.Valid {
		newPosition = int(maxPosition.Int64) + 1
	}

	// Add track to playlist
	_, err = a.storage.DB.DB.Exec(`
		INSERT INTO playlist_tracks (playlist_id, track_id, position)
		VALUES ($1, $2, $3)
		ON CONFLICT (playlist_id, track_id) DO NOTHING
	`, playlistID, req.TrackID, newPosition)

	if err != nil {
		respondError(w, http.StatusInternalServerError, "failed to add track to playlist")
		return
	}

	// Update track count
	_, err = a.storage.DB.DB.Exec(`
		UPDATE playlists
		SET track_count = (SELECT COUNT(*) FROM playlist_tracks WHERE playlist_id = $1),
		    updated_at = CURRENT_TIMESTAMP
		WHERE id = $1
	`, playlistID)

	respondJSON(w, http.StatusCreated, map[string]string{"message": "track added to playlist"})
}

func (a *API) removeTrackFromPlaylist(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	playlistID := chi.URLParam(r, "id")
	trackID := chi.URLParam(r, "trackId")

	// Check ownership
	var userID int64
	err := a.storage.DB.DB.QueryRow(`SELECT user_id FROM playlists WHERE id = $1`, playlistID).Scan(&userID)
	if err != nil {
		if err == sql.ErrNoRows {
			respondError(w, http.StatusNotFound, "playlist not found")
			return
		}
		respondError(w, http.StatusInternalServerError, "database error")
		return
	}

	if userID != claims.UserID {
		respondError(w, http.StatusForbidden, "not authorized to modify this playlist")
		return
	}

	_, err = a.storage.DB.DB.Exec(`
		DELETE FROM playlist_tracks
		WHERE playlist_id = $1 AND track_id = $2
	`, playlistID, trackID)

	if err != nil {
		respondError(w, http.StatusInternalServerError, "failed to remove track from playlist")
		return
	}

	// Update track count
	_, err = a.storage.DB.DB.Exec(`
		UPDATE playlists
		SET track_count = (SELECT COUNT(*) FROM playlist_tracks WHERE playlist_id = $1),
		    updated_at = CURRENT_TIMESTAMP
		WHERE id = $1
	`, playlistID)

	respondJSON(w, http.StatusOK, map[string]string{"message": "track removed from playlist"})
}
