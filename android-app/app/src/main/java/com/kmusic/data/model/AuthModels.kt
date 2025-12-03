package com.kmusic.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    @SerializedName("display_name")
    val displayName: String
)

data class AuthResponse(
    val token: String,
    val user: User
)

data class User(
    val id: Long,
    val username: String,
    val email: String,
    @SerializedName("display_name")
    val displayName: String,
    val role: String, // "admin" or "user"
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
) {
    fun isAdmin(): Boolean = role == "admin"
}
