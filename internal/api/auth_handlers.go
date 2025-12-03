package api

import (
	"database/sql"
	"encoding/json"
	"net/http"
	"strings"

	"musicstream/internal/auth"
	"musicstream/internal/models"
)

func (a *API) register(w http.ResponseWriter, r *http.Request) {
	var req models.RegisterRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	// Validation
	if req.Username == "" || req.Email == "" || req.Password == "" {
		respondError(w, http.StatusBadRequest, "username, email, and password are required")
		return
	}

	if len(req.Password) < 6 {
		respondError(w, http.StatusBadRequest, "password must be at least 6 characters")
		return
	}

	// Hash password
	hashedPassword, err := auth.HashPassword(req.Password)
	if err != nil {
		respondError(w, http.StatusInternalServerError, "failed to hash password")
		return
	}

	// Create user
	var userID int64
	err = a.storage.DB.DB.QueryRow(`
		INSERT INTO users (username, email, password_hash, display_name)
		VALUES ($1, $2, $3, $4)
		RETURNING id
	`, req.Username, req.Email, hashedPassword, req.DisplayName).Scan(&userID)

	if err != nil {
		if strings.Contains(err.Error(), "duplicate key") {
			respondError(w, http.StatusConflict, "username or email already exists")
			return
		}
		respondError(w, http.StatusInternalServerError, "failed to create user")
		return
	}

	// Create default preferences
	_, err = a.storage.DB.DB.Exec(`
		INSERT INTO user_preferences (user_id)
		VALUES ($1)
	`, userID)
	if err != nil {
		// Log error but don't fail the registration
		println("Failed to create user preferences:", err.Error())
	}

	// Get created user
	var user models.User
	err = a.storage.DB.DB.QueryRow(`
		SELECT id, username, email, display_name, role, created_at, updated_at
		FROM users WHERE id = $1
	`, userID).Scan(&user.ID, &user.Username, &user.Email, &user.DisplayName, &user.Role, &user.CreatedAt, &user.UpdatedAt)

	if err != nil {
		respondError(w, http.StatusInternalServerError, "failed to retrieve user")
		return
	}

	// Generate JWT token
	token, err := a.jwtManager.Generate(user.ID, user.Username)
	if err != nil {
		respondError(w, http.StatusInternalServerError, "failed to generate token")
		return
	}

	respondJSON(w, http.StatusCreated, models.AuthResponse{
		Token: token,
		User:  user,
	})
}

func (a *API) login(w http.ResponseWriter, r *http.Request) {
	var req models.LoginRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	// Get user by username
	var user models.User
	err := a.storage.DB.DB.QueryRow(`
		SELECT id, username, email, password_hash, display_name, role, created_at, updated_at
		FROM users WHERE username = $1
	`, req.Username).Scan(&user.ID, &user.Username, &user.Email, &user.PasswordHash, &user.DisplayName, &user.Role, &user.CreatedAt, &user.UpdatedAt)

	if err != nil {
		if err == sql.ErrNoRows {
			respondError(w, http.StatusUnauthorized, "invalid credentials")
			return
		}
		respondError(w, http.StatusInternalServerError, "database error")
		return
	}

	// Verify password
	if !auth.ComparePassword(user.PasswordHash, req.Password) {
		respondError(w, http.StatusUnauthorized, "invalid credentials")
		return
	}

	// Generate JWT token
	token, err := a.jwtManager.Generate(user.ID, user.Username)
	if err != nil {
		respondError(w, http.StatusInternalServerError, "failed to generate token")
		return
	}

	// Clear password hash before sending
	user.PasswordHash = ""

	respondJSON(w, http.StatusOK, models.AuthResponse{
		Token: token,
		User:  user,
	})
}

func (a *API) getCurrentUser(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	var user models.User
	err := a.storage.DB.DB.QueryRow(`
		SELECT id, username, email, display_name, role, created_at, updated_at
		FROM users WHERE id = $1
	`, claims.UserID).Scan(&user.ID, &user.Username, &user.Email, &user.DisplayName, &user.Role, &user.CreatedAt, &user.UpdatedAt)

	if err != nil {
		if err == sql.ErrNoRows {
			respondError(w, http.StatusNotFound, "user not found")
			return
		}
		respondError(w, http.StatusInternalServerError, "database error")
		return
	}

	respondJSON(w, http.StatusOK, user)
}
