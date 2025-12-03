package main

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"musicstream/internal/api"
	"musicstream/internal/auth"
	"musicstream/internal/config"
	"musicstream/internal/storage"
)

func main() {
	// Load configuration
	cfg := config.Load()

	// Initialize database
	db, err := storage.NewPostgres(cfg.Database)
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}
	defer db.Close()

	// Run migrations
	if err := db.Migrate(); err != nil {
		log.Fatalf("Failed to run migrations: %v", err)
	}

	// Seed super admin user
	if err := seedSuperAdmin(db); err != nil {
		log.Printf("Warning: Failed to seed super admin: %v", err)
	} else {
		log.Println("✓ Super admin user ready (username: kaman)")
	}

	// Initialize Redis
	redisClient := storage.NewRedis(cfg.Redis)
	defer redisClient.Close()

	// Initialize MinIO
	minioClient, err := storage.NewMinio(cfg.Minio)
	if err != nil {
		log.Fatalf("Failed to connect to MinIO: %v", err)
	}

	// Initialize storage
	store := &storage.Storage{
		DB:    db,
		Redis: redisClient,
		Minio: minioClient,
	}

	// Create API router
	router := api.NewRouter(store, cfg)

	// Create HTTP server
	server := &http.Server{
		Addr:         fmt.Sprintf("%s:%s", cfg.Server.Host, cfg.Server.Port),
		Handler:      router,
		ReadTimeout:  30 * time.Second,
		WriteTimeout: 30 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	// Start server in goroutine
	go func() {
		log.Printf("🎵 Music Streaming Server starting on %s", server.Addr)
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("Server failed to start: %v", err)
		}
	}()

	// Wait for interrupt signal to gracefully shut down the server
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	log.Println("Shutting down server...")

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	if err := server.Shutdown(ctx); err != nil {
		log.Fatalf("Server forced to shutdown: %v", err)
	}

	log.Println("Server stopped")
}

// seedSuperAdmin creates the super admin user if it doesn't exist
func seedSuperAdmin(db *storage.Postgres) error {
	username := "kaman"
	password := "Johnedoms2@"
	email := "kaman@musicstream.local"
	displayName := "Super Admin"

	// Check if user already exists
	var exists bool
	err := db.DB.QueryRow(`SELECT EXISTS(SELECT 1 FROM users WHERE username = $1)`, username).Scan(&exists)
	if err != nil {
		return fmt.Errorf("failed to check admin existence: %w", err)
	}

	if exists {
		// Admin already exists
		return nil
	}

	// Hash the password
	hashedPassword, err := auth.HashPassword(password)
	if err != nil {
		return fmt.Errorf("failed to hash password: %w", err)
	}

	// Create the super admin user
	_, err = db.DB.Exec(`
		INSERT INTO users (username, email, password_hash, display_name, role)
		VALUES ($1, $2, $3, $4, 'admin')
	`, username, email, hashedPassword, displayName)

	if err != nil {
		return fmt.Errorf("failed to create super admin: %w", err)
	}

	// Create default preferences for super admin
	_, err = db.DB.Exec(`
		INSERT INTO user_preferences (user_id)
		SELECT id FROM users WHERE username = $1
	`, username)

	if err != nil {
		log.Printf("Warning: Failed to create preferences for super admin: %v", err)
	}

	return nil
}
