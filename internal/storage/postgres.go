package storage

import (
	"database/sql"
	"fmt"

	_ "github.com/lib/pq"
	"musicstream/internal/config"
)

type Postgres struct {
	DB *sql.DB
}

func NewPostgres(cfg config.DatabaseConfig) (*Postgres, error) {
	connStr := fmt.Sprintf(
		"host=%s port=%s user=%s password=%s dbname=%s sslmode=disable",
		cfg.Host, cfg.Port, cfg.User, cfg.Password, cfg.DBName,
	)

	db, err := sql.Open("postgres", connStr)
	if err != nil {
		return nil, fmt.Errorf("failed to open database: %w", err)
	}

	// Configure connection pool for efficiency
	db.SetMaxOpenConns(25)
	db.SetMaxIdleConns(5)
	db.SetConnMaxLifetime(0)

	if err := db.Ping(); err != nil {
		return nil, fmt.Errorf("failed to ping database: %w", err)
	}

	return &Postgres{DB: db}, nil
}

func (p *Postgres) Close() error {
	return p.DB.Close()
}

func (p *Postgres) Migrate() error {
	queries := []string{
		// Users table
		`CREATE TABLE IF NOT EXISTS users (
			id SERIAL PRIMARY KEY,
			username VARCHAR(50) UNIQUE NOT NULL,
			email VARCHAR(255) UNIQUE NOT NULL,
			password_hash VARCHAR(255) NOT NULL,
			display_name VARCHAR(100) NOT NULL,
			role VARCHAR(20) DEFAULT 'user',
			created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
			updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
		)`,

		// Add role column if it doesn't exist (for existing databases)
		`DO $$
		BEGIN
			IF NOT EXISTS (SELECT 1 FROM information_schema.columns
				WHERE table_name='users' AND column_name='role') THEN
				ALTER TABLE users ADD COLUMN role VARCHAR(20) DEFAULT 'user';
			END IF;
		END $$`,

		// Tracks table
		`CREATE TABLE IF NOT EXISTS tracks (
			id SERIAL PRIMARY KEY,
			title VARCHAR(255) NOT NULL,
			artist VARCHAR(255) NOT NULL,
			album VARCHAR(255),
			album_artist VARCHAR(255),
			genre VARCHAR(100),
			year INTEGER,
			track_number INTEGER,
			duration INTEGER NOT NULL,
			file_size BIGINT NOT NULL,
			file_path VARCHAR(500) UNIQUE NOT NULL,
			file_format VARCHAR(10) NOT NULL,
			bitrate INTEGER,
			sample_rate INTEGER,
			uploaded_by INTEGER REFERENCES users(id) ON DELETE SET NULL,
			play_count BIGINT DEFAULT 0,
			created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
			updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
		)`,

		// Playlists table
		`CREATE TABLE IF NOT EXISTS playlists (
			id SERIAL PRIMARY KEY,
			name VARCHAR(255) NOT NULL,
			description TEXT,
			user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
			is_public BOOLEAN DEFAULT false,
			track_count INTEGER DEFAULT 0,
			created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
			updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
		)`,

		// Playlist tracks junction table
		`CREATE TABLE IF NOT EXISTS playlist_tracks (
			id SERIAL PRIMARY KEY,
			playlist_id INTEGER NOT NULL REFERENCES playlists(id) ON DELETE CASCADE,
			track_id INTEGER NOT NULL REFERENCES tracks(id) ON DELETE CASCADE,
			position INTEGER NOT NULL,
			added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
			UNIQUE(playlist_id, track_id)
		)`,

		// User preferences
		`CREATE TABLE IF NOT EXISTS user_preferences (
			user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
			language VARCHAR(10) DEFAULT 'en',
			theme VARCHAR(20) DEFAULT 'dark',
			bitrate_prefer INTEGER DEFAULT 320,
			auto_play BOOLEAN DEFAULT true,
			crossfade_duration INTEGER DEFAULT 0,
			offline_mode BOOLEAN DEFAULT false,
			notify_new_tracks BOOLEAN DEFAULT true
		)`,

		// Play history
		`CREATE TABLE IF NOT EXISTS play_history (
			id SERIAL PRIMARY KEY,
			user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
			track_id INTEGER NOT NULL REFERENCES tracks(id) ON DELETE CASCADE,
			played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
			duration INTEGER,
			completed BOOLEAN DEFAULT false
		)`,

		// Indexes for performance
		`CREATE INDEX IF NOT EXISTS idx_tracks_artist ON tracks(artist)`,
		`CREATE INDEX IF NOT EXISTS idx_tracks_album ON tracks(album)`,
		`CREATE INDEX IF NOT EXISTS idx_tracks_genre ON tracks(genre)`,
		`CREATE INDEX IF NOT EXISTS idx_playlists_user ON playlists(user_id)`,
		`CREATE INDEX IF NOT EXISTS idx_playlist_tracks_playlist ON playlist_tracks(playlist_id)`,
		`CREATE INDEX IF NOT EXISTS idx_play_history_user ON play_history(user_id)`,
		`CREATE INDEX IF NOT EXISTS idx_play_history_track ON play_history(track_id)`,
	}

	for _, query := range queries {
		if _, err := p.DB.Exec(query); err != nil {
			return fmt.Errorf("migration failed: %w", err)
		}
	}

	return nil
}

// SeedSuperAdmin creates the super admin user if it doesn't exist
func (p *Postgres) SeedSuperAdmin(username, password, email, displayName string) error {
	// Check if super admin already exists
	var exists bool
	err := p.DB.QueryRow(`SELECT EXISTS(SELECT 1 FROM users WHERE username = $1)`, username).Scan(&exists)
	if err != nil {
		return fmt.Errorf("failed to check admin existence: %w", err)
	}

	if exists {
		// Admin already exists, skip creation
		return nil
	}

	// Import password hasher (we'll need to handle this in the calling code)
	// For now, return an error to be handled by the caller
	return fmt.Errorf("super admin needs to be created by auth handler")
}
