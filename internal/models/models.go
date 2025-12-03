package models

import (
	"time"
)

type User struct {
	ID           int64     `json:"id"`
	Username     string    `json:"username"`
	Email        string    `json:"email"`
	PasswordHash string    `json:"-"`
	DisplayName  string    `json:"display_name"`
	Role         string    `json:"role"` // "admin", "user"
	CreatedAt    time.Time `json:"created_at"`
	UpdatedAt    time.Time `json:"updated_at"`
}

// User role constants
const (
	RoleAdmin = "admin"
	RoleUser  = "user"
)

// Check if user is admin
func (u *User) IsAdmin() bool {
	return u.Role == RoleAdmin
}

type Track struct {
	ID          int64     `json:"id"`
	Title       string    `json:"title"`
	Artist      string    `json:"artist"`
	Album       string    `json:"album"`
	AlbumArtist string    `json:"album_artist,omitempty"`
	Genre       string    `json:"genre,omitempty"`
	Year        int       `json:"year,omitempty"`
	TrackNumber int       `json:"track_number,omitempty"`
	Duration    int       `json:"duration"` // in seconds
	FileSize    int64     `json:"file_size"`
	FilePath    string    `json:"-"` // MinIO object key
	FileFormat  string    `json:"file_format"`
	Bitrate     int       `json:"bitrate,omitempty"`
	SampleRate  int       `json:"sample_rate,omitempty"`
	UploadedBy  int64     `json:"uploaded_by"`
	PlayCount   int64     `json:"play_count"`
	CreatedAt   time.Time `json:"created_at"`
	UpdatedAt   time.Time `json:"updated_at"`
}

type Playlist struct {
	ID          int64     `json:"id"`
	Name        string    `json:"name"`
	Description string    `json:"description,omitempty"`
	UserID      int64     `json:"user_id"`
	IsPublic    bool      `json:"is_public"`
	TrackCount  int       `json:"track_count"`
	CreatedAt   time.Time `json:"created_at"`
	UpdatedAt   time.Time `json:"updated_at"`
}

type PlaylistTrack struct {
	ID         int64     `json:"id"`
	PlaylistID int64     `json:"playlist_id"`
	TrackID    int64     `json:"track_id"`
	Position   int       `json:"position"`
	AddedAt    time.Time `json:"added_at"`
}

type UserPreference struct {
	UserID          int64  `json:"user_id"`
	Language        string `json:"language"`
	Theme           string `json:"theme"`
	BitratePrefer   int    `json:"bitrate_prefer"`
	AutoPlay        bool   `json:"auto_play"`
	CrossfadeDur    int    `json:"crossfade_duration"`
	OfflineMode     bool   `json:"offline_mode"`
	NotifyNewTracks bool   `json:"notify_new_tracks"`
}

type PlayHistory struct {
	ID        int64     `json:"id"`
	UserID    int64     `json:"user_id"`
	TrackID   int64     `json:"track_id"`
	PlayedAt  time.Time `json:"played_at"`
	Duration  int       `json:"duration"` // How long the user listened
	Completed bool      `json:"completed"` // Did they listen to the end?
}

type LoginRequest struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

type RegisterRequest struct {
	Username    string `json:"username"`
	Email       string `json:"email"`
	Password    string `json:"password"`
	DisplayName string `json:"display_name"`
}

type AuthResponse struct {
	Token string `json:"token"`
	User  User   `json:"user"`
}

type CreatePlaylistRequest struct {
	Name        string `json:"name"`
	Description string `json:"description"`
	IsPublic    bool   `json:"is_public"`
}

type AddTrackToPlaylistRequest struct {
	TrackID int64 `json:"track_id"`
}
