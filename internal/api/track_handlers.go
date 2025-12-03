package api

import (
	"context"
	"database/sql"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strconv"
	"time"

	"github.com/dhowden/tag"
	"github.com/go-chi/chi/v5"
	"musicstream/internal/models"
)

func (a *API) uploadTrack(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	// Check if user is admin
	var userRole string
	err := a.storage.DB.DB.QueryRow(`SELECT role FROM users WHERE id = $1`, claims.UserID).Scan(&userRole)
	if err != nil {
		respondError(w, http.StatusInternalServerError, "failed to verify permissions")
		return
	}

	if userRole != models.RoleAdmin {
		respondError(w, http.StatusForbidden, "only admins can upload tracks")
		return
	}

	// Parse multipart form
	err = r.ParseMultipartForm(a.config.Server.MaxUploadSize)
	if err != nil {
		respondError(w, http.StatusBadRequest, "file too large or invalid form")
		return
	}

	file, header, err := r.FormFile("file")
	if err != nil {
		respondError(w, http.StatusBadRequest, "missing file")
		return
	}
	defer file.Close()

	// Extract metadata from audio file
	metadata, err := tag.ReadFrom(file)
	if err != nil {
		respondError(w, http.StatusBadRequest, "failed to read audio metadata")
		return
	}

	// Reset file pointer for upload
	file.Seek(0, 0)

	// Generate unique file path
	fileName := fmt.Sprintf("tracks/%d_%d_%s", claims.UserID, time.Now().Unix(), header.Filename)

	// Upload to MinIO
	ctx := context.Background()
	err = a.storage.Minio.Upload(ctx, fileName, file, header.Size, "audio/aac")
	if err != nil {
		respondError(w, http.StatusInternalServerError, "failed to upload file")
		return
	}

	// Extract metadata
	title := metadata.Title()
	if title == "" {
		title = header.Filename
	}
	artist := metadata.Artist()
	if artist == "" {
		artist = "Unknown Artist"
	}
	album := metadata.Album()
	albumArtist := metadata.AlbumArtist()
	genre := metadata.Genre()
	year, _ := metadata.Year()
	trackNum, _ := metadata.Track()

	// Insert track into database
	var trackID int64
	err = a.storage.DB.DB.QueryRow(`
		INSERT INTO tracks (title, artist, album, album_artist, genre, year, track_number,
			duration, file_size, file_path, file_format, uploaded_by)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
		RETURNING id
	`, title, artist, album, albumArtist, genre, year, trackNum,
		0, header.Size, fileName, metadata.Format().String(), claims.UserID).Scan(&trackID)

	if err != nil {
		// Cleanup uploaded file
		a.storage.Minio.Delete(ctx, fileName)
		respondError(w, http.StatusInternalServerError, "failed to save track metadata")
		return
	}

	// Get created track
	var track models.Track
	err = a.storage.DB.DB.QueryRow(`
		SELECT id, title, artist, album, album_artist, genre, year, track_number,
			duration, file_size, file_format, uploaded_by, play_count, created_at, updated_at
		FROM tracks WHERE id = $1
	`, trackID).Scan(&track.ID, &track.Title, &track.Artist, &track.Album, &track.AlbumArtist,
		&track.Genre, &track.Year, &track.TrackNumber, &track.Duration, &track.FileSize,
		&track.FileFormat, &track.UploadedBy, &track.PlayCount, &track.CreatedAt, &track.UpdatedAt)

	if err != nil {
		respondError(w, http.StatusInternalServerError, "failed to retrieve track")
		return
	}

	respondJSON(w, http.StatusCreated, track)
}

func (a *API) getTracks(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	// Get query parameters
	artist := r.URL.Query().Get("artist")
	album := r.URL.Query().Get("album")
	genre := r.URL.Query().Get("genre")
	limit := r.URL.Query().Get("limit")
	offset := r.URL.Query().Get("offset")

	limitInt := 50
	offsetInt := 0

	if limit != "" {
		if l, err := strconv.Atoi(limit); err == nil && l > 0 && l <= 200 {
			limitInt = l
		}
	}

	if offset != "" {
		if o, err := strconv.Atoi(offset); err == nil && o >= 0 {
			offsetInt = o
		}
	}

	// Build query
	query := `
		SELECT id, title, artist, album, album_artist, genre, year, track_number,
			duration, file_size, file_format, uploaded_by, play_count, created_at, updated_at
		FROM tracks WHERE 1=1
	`
	args := []interface{}{}
	argCount := 1

	if artist != "" {
		query += fmt.Sprintf(" AND artist ILIKE $%d", argCount)
		args = append(args, "%"+artist+"%")
		argCount++
	}

	if album != "" {
		query += fmt.Sprintf(" AND album ILIKE $%d", argCount)
		args = append(args, "%"+album+"%")
		argCount++
	}

	if genre != "" {
		query += fmt.Sprintf(" AND genre ILIKE $%d", argCount)
		args = append(args, "%"+genre+"%")
		argCount++
	}

	query += " ORDER BY created_at DESC"
	query += fmt.Sprintf(" LIMIT $%d OFFSET $%d", argCount, argCount+1)
	args = append(args, limitInt, offsetInt)

	rows, err := a.storage.DB.DB.Query(query, args...)
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

func (a *API) getTrack(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	trackID := chi.URLParam(r, "id")

	var track models.Track
	err := a.storage.DB.DB.QueryRow(`
		SELECT id, title, artist, album, album_artist, genre, year, track_number,
			duration, file_size, file_format, uploaded_by, play_count, created_at, updated_at
		FROM tracks WHERE id = $1
	`, trackID).Scan(&track.ID, &track.Title, &track.Artist, &track.Album, &track.AlbumArtist,
		&track.Genre, &track.Year, &track.TrackNumber, &track.Duration, &track.FileSize,
		&track.FileFormat, &track.UploadedBy, &track.PlayCount, &track.CreatedAt, &track.UpdatedAt)

	if err != nil {
		if err == sql.ErrNoRows {
			respondError(w, http.StatusNotFound, "track not found")
			return
		}
		respondError(w, http.StatusInternalServerError, "database error")
		return
	}

	respondJSON(w, http.StatusOK, track)
}

func (a *API) deleteTrack(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	trackID := chi.URLParam(r, "id")

	// Get track to verify ownership and get file path
	var filePath string
	var uploadedBy int64
	err := a.storage.DB.DB.QueryRow(`
		SELECT file_path, uploaded_by FROM tracks WHERE id = $1
	`, trackID).Scan(&filePath, &uploadedBy)

	if err != nil {
		if err == sql.ErrNoRows {
			respondError(w, http.StatusNotFound, "track not found")
			return
		}
		respondError(w, http.StatusInternalServerError, "database error")
		return
	}

	// Only allow deletion if user uploaded the track
	if uploadedBy != claims.UserID {
		respondError(w, http.StatusForbidden, "not authorized to delete this track")
		return
	}

	// Delete from database
	_, err = a.storage.DB.DB.Exec(`DELETE FROM tracks WHERE id = $1`, trackID)
	if err != nil {
		respondError(w, http.StatusInternalServerError, "failed to delete track")
		return
	}

	// Delete from MinIO
	ctx := context.Background()
	err = a.storage.Minio.Delete(ctx, filePath)
	if err != nil {
		// Log error but don't fail the request
		println("Failed to delete file from storage:", err.Error())
	}

	respondJSON(w, http.StatusOK, map[string]string{"message": "track deleted"})
}
