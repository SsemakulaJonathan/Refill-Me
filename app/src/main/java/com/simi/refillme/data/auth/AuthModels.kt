package com.simi.refillme.data.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class SignupRequest(
    val email: String,
    val password: String,
    val fullName: String
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String? = null,
    val token: String? = null,
    val user: User? = null
)

@Serializable
data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val createdAt: String? = null
)

@Serializable
data class PasswordVerificationRequest(
    val password: String
)

@Serializable
data class PasswordVerificationResponse(
    val success: Boolean,
    val valid: Boolean,
    val message: String
)
