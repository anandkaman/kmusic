package api

import (
	"net/http"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/go-chi/cors"

	"musicstream/internal/auth"
	"musicstream/internal/config"
	"musicstream/internal/storage"
)

type API struct {
	storage    *storage.Storage
	jwtManager *auth.JWTManager
	config     *config.Config
}

func NewRouter(store *storage.Storage, cfg *config.Config) http.Handler {
	r := chi.NewRouter()

	// Middleware
	r.Use(middleware.RequestID)
	r.Use(middleware.RealIP)
	r.Use(middleware.Recoverer)
	r.Use(middleware.Timeout(60 * time.Second))

	// CORS configuration
	r.Use(cors.Handler(cors.Options{
		AllowedOrigins:   []string{"*"},
		AllowedMethods:   []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
		AllowedHeaders:   []string{"Accept", "Authorization", "Content-Type"},
		ExposedHeaders:   []string{"Link", "Content-Range", "Accept-Ranges"},
		AllowCredentials: true,
		MaxAge:           300,
	}))

	api := &API{
		storage:    store,
		jwtManager: auth.NewJWTManager(cfg.JWT),
		config:     cfg,
	}

	r.Use(api.LoggingMiddleware)

	// Health check
	r.Get("/health", api.healthCheck)

	// Public routes
	r.Post("/api/auth/register", api.register)
	r.Post("/api/auth/login", api.login)

	// Protected routes
	r.Group(func(r chi.Router) {
		r.Use(api.AuthMiddleware)

		// User routes
		r.Get("/api/user/me", api.getCurrentUser)
		r.Get("/api/user/preferences", api.getUserPreferences)
		r.Put("/api/user/preferences", api.updateUserPreferences)

		// Track routes
		r.Post("/api/tracks/upload", api.uploadTrack)
		r.Get("/api/tracks", api.getTracks)
		r.Get("/api/tracks/{id}", api.getTrack)
		r.Delete("/api/tracks/{id}", api.deleteTrack)
		r.Get("/api/tracks/{id}/stream", api.streamTrack)

		// Playlist routes
		r.Post("/api/playlists", api.createPlaylist)
		r.Get("/api/playlists", api.getPlaylists)
		r.Get("/api/playlists/{id}", api.getPlaylist)
		r.Put("/api/playlists/{id}", api.updatePlaylist)
		r.Delete("/api/playlists/{id}", api.deletePlaylist)
		r.Post("/api/playlists/{id}/tracks", api.addTrackToPlaylist)
		r.Delete("/api/playlists/{id}/tracks/{trackId}", api.removeTrackFromPlaylist)

		// Search routes
		r.Get("/api/search", api.search)

		// Play history
		r.Post("/api/history", api.recordPlayHistory)
		r.Get("/api/history", api.getPlayHistory)

		// Statistics
		r.Get("/api/stats", api.getStats)
	})

	return r
}

func (a *API) healthCheck(w http.ResponseWriter, r *http.Request) {
	respondJSON(w, http.StatusOK, map[string]string{
		"status": "ok",
		"service": "music-streaming-api",
	})
}
