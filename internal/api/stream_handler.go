package api

import (
	"context"
	"database/sql"
	"fmt"
	"io"
	"net/http"
	"strconv"
	"strings"

	"github.com/go-chi/chi/v5"
)

func (a *API) streamTrack(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	trackID := chi.URLParam(r, "id")

	// Get track from database
	var filePath string
	var fileSize int64
	err := a.storage.DB.DB.QueryRow(`
		SELECT file_path, file_size FROM tracks WHERE id = $1
	`, trackID).Scan(&filePath, &fileSize)

	if err != nil {
		if err == sql.ErrNoRows {
			respondError(w, http.StatusNotFound, "track not found")
			return
		}
		respondError(w, http.StatusInternalServerError, "database error")
		return
	}

	// Get file from MinIO
	ctx := context.Background()
	object, err := a.storage.Minio.Get(ctx, filePath)
	if err != nil {
		respondError(w, http.StatusInternalServerError, "failed to retrieve audio file")
		return
	}
	defer object.Close()

	// Parse Range header for partial content support
	rangeHeader := r.Header.Get("Range")

	if rangeHeader == "" {
		// No range header - stream entire file
		w.Header().Set("Content-Type", "audio/aac")
		w.Header().Set("Content-Length", strconv.FormatInt(fileSize, 10))
		w.Header().Set("Accept-Ranges", "bytes")
		w.WriteHeader(http.StatusOK)

		// Stream the file
		io.Copy(w, object)

		// Update play count asynchronously
		go a.incrementPlayCount(trackID, claims.UserID)
		return
	}

	// Handle range request
	ranges, err := parseRange(rangeHeader, fileSize)
	if err != nil || len(ranges) != 1 {
		w.Header().Set("Content-Range", fmt.Sprintf("bytes */%d", fileSize))
		respondError(w, http.StatusRequestedRangeNotSatisfiable, "invalid range")
		return
	}

	start := ranges[0].start
	end := ranges[0].end
	contentLength := end - start + 1

	// Seek to start position
	_, err = object.Seek(start, io.SeekStart)
	if err != nil {
		respondError(w, http.StatusInternalServerError, "seek failed")
		return
	}

	// Set headers for partial content
	w.Header().Set("Content-Type", "audio/aac")
	w.Header().Set("Content-Range", fmt.Sprintf("bytes %d-%d/%d", start, end, fileSize))
	w.Header().Set("Content-Length", strconv.FormatInt(contentLength, 10))
	w.Header().Set("Accept-Ranges", "bytes")
	w.WriteHeader(http.StatusPartialContent)

	// Stream the requested range
	io.CopyN(w, object, contentLength)

	// Update play count if streaming from the beginning
	if start == 0 {
		go a.incrementPlayCount(trackID, claims.UserID)
	}
}

type byteRange struct {
	start int64
	end   int64
}

func parseRange(rangeHeader string, size int64) ([]byteRange, error) {
	if !strings.HasPrefix(rangeHeader, "bytes=") {
		return nil, fmt.Errorf("invalid range header")
	}

	rangeHeader = strings.TrimPrefix(rangeHeader, "bytes=")
	parts := strings.Split(rangeHeader, ",")

	if len(parts) > 1 {
		// We only support single range requests for simplicity
		return nil, fmt.Errorf("multiple ranges not supported")
	}

	rangePart := strings.TrimSpace(parts[0])
	dashIndex := strings.Index(rangePart, "-")

	if dashIndex < 0 {
		return nil, fmt.Errorf("invalid range format")
	}

	startStr := rangePart[:dashIndex]
	endStr := rangePart[dashIndex+1:]

	var start, end int64
	var err error

	if startStr == "" {
		// suffix-byte-range-spec: "-500" means last 500 bytes
		if endStr == "" {
			return nil, fmt.Errorf("invalid range")
		}
		suffix, err := strconv.ParseInt(endStr, 10, 64)
		if err != nil || suffix > size {
			return nil, err
		}
		start = size - suffix
		end = size - 1
	} else {
		start, err = strconv.ParseInt(startStr, 10, 64)
		if err != nil {
			return nil, err
		}

		if endStr == "" {
			// "500-" means from byte 500 to end
			end = size - 1
		} else {
			end, err = strconv.ParseInt(endStr, 10, 64)
			if err != nil {
				return nil, err
			}
		}
	}

	if start < 0 || start >= size || end < start || end >= size {
		return nil, fmt.Errorf("range out of bounds")
	}

	return []byteRange{{start: start, end: end}}, nil
}

func (a *API) incrementPlayCount(trackID string, userID int64) {
	// Update track play count
	_, err := a.storage.DB.DB.Exec(`
		UPDATE tracks SET play_count = play_count + 1 WHERE id = $1
	`, trackID)

	if err != nil {
		println("Failed to increment play count:", err.Error())
	}
}

func (a *API) recordPlayHistory(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	var req struct {
		TrackID   int64 `json:"track_id"`
		Duration  int   `json:"duration"`
		Completed bool  `json:"completed"`
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	_, err := a.storage.DB.DB.Exec(`
		INSERT INTO play_history (user_id, track_id, duration, completed)
		VALUES ($1, $2, $3, $4)
	`, claims.UserID, req.TrackID, req.Duration, req.Completed)

	if err != nil {
		respondError(w, http.StatusInternalServerError, "failed to record play history")
		return
	}

	respondJSON(w, http.StatusCreated, map[string]string{"message": "play recorded"})
}

func (a *API) getPlayHistory(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	limit := r.URL.Query().Get("limit")
	limitInt := 50
	if limit != "" {
		if l, err := strconv.Atoi(limit); err == nil && l > 0 && l <= 200 {
			limitInt = l
		}
	}

	rows, err := a.storage.DB.DB.Query(`
		SELECT ph.id, ph.track_id, t.title, t.artist, t.album, ph.played_at, ph.duration, ph.completed
		FROM play_history ph
		JOIN tracks t ON ph.track_id = t.id
		WHERE ph.user_id = $1
		ORDER BY ph.played_at DESC
		LIMIT $2
	`, claims.UserID, limitInt)

	if err != nil {
		respondError(w, http.StatusInternalServerError, "database error")
		return
	}
	defer rows.Close()

	type HistoryEntry struct {
		ID        int64  `json:"id"`
		TrackID   int64  `json:"track_id"`
		Title     string `json:"title"`
		Artist    string `json:"artist"`
		Album     string `json:"album"`
		PlayedAt  string `json:"played_at"`
		Duration  int    `json:"duration"`
		Completed bool   `json:"completed"`
	}

	history := []HistoryEntry{}
	for rows.Next() {
		var entry HistoryEntry
		err := rows.Scan(&entry.ID, &entry.TrackID, &entry.Title, &entry.Artist, &entry.Album,
			&entry.PlayedAt, &entry.Duration, &entry.Completed)
		if err != nil {
			continue
		}
		history = append(history, entry)
	}

	respondJSON(w, http.StatusOK, history)
}
