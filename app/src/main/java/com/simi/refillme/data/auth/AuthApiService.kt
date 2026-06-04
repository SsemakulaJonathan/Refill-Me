package com.simi.refillme.data.auth

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class AuthApiService {
    
    // Vercel backend API endpoint - Custom domain
    private val baseUrl = "https://refill-me.vercel.app/api"
    
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.BODY
        }
    }
    
    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = client.post("$baseUrl/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email, password))
            }
            Result.success(response.body<AuthResponse>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signup(email: String, password: String, fullName: String): Result<AuthResponse> {
        return try {
            val response = client.post("$baseUrl/auth/signup") {
                contentType(ContentType.Application.Json)
                setBody(SignupRequest(email, password, fullName))
            }
            Result.success(response.body<AuthResponse>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun verifyToken(token: String): Result<Boolean> {
        return try {
            val response = client.get("$baseUrl/auth/verify") {
                header("Authorization", "Bearer $token")
            }
            Result.success(response.status == HttpStatusCode.OK)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyPassword(token: String, password: String): Result<PasswordVerificationResponse> {
        return try {
            val response = client.post("$baseUrl/auth/verify-password") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(PasswordVerificationRequest(password))
            }
            Result.success(response.body<PasswordVerificationResponse>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AuthApiService? = null
        
        fun getInstance(): AuthApiService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthApiService().also { INSTANCE = it }
            }
        }
    }
}
