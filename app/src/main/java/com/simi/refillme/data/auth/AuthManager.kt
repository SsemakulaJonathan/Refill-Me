package com.simi.refillme.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthManager(private val context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        "auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    init {
        checkAuthStatus()
    }
    
    private fun checkAuthStatus() {
        val token = getToken()
        val userJson = securePrefs.getString("user", null)
        
        if (token != null && userJson != null) {
            try {
                // TODO: Verify token validity with backend
                _authState.value = AuthState.Authenticated
                // Parse user from JSON
                _currentUser.value = parseUserFromJson(userJson)
            } catch (e: Exception) {
                clearAuth()
                _authState.value = AuthState.Unauthenticated
            }
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    fun saveAuth(token: String, user: User) {
        securePrefs.edit().apply {
            putString("auth_token", token)
            putString("user", userToJson(user))
            apply()
        }
        _currentUser.value = user
        _authState.value = AuthState.Authenticated
    }
    
    fun getToken(): String? {
        return securePrefs.getString("auth_token", null)
    }
    
    fun clearAuth() {
        securePrefs.edit().clear().apply()
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
    }
    
    fun isAuthenticated(): Boolean {
        return _authState.value == AuthState.Authenticated
    }

    /** Call this with any API error message. If the message indicates an expired/invalid token, clears auth and redirects to login. */
    fun handleApiError(message: String) {
        val lower = message.lowercase()
        if (lower.contains("token") || lower.contains("unauthorized") || lower.contains("expired") || lower.contains("authentication")) {
            clearAuth()
        }
    }
    
    private fun userToJson(user: User): String {
        return """{"id":"${user.id}","email":"${user.email}","fullName":"${user.fullName}","createdAt":"${user.createdAt}"}"""
    }
    
    private fun parseUserFromJson(json: String): User? {
        return try {
            // Simple JSON parsing - in production use kotlinx.serialization
            val id = json.substringAfter("\"id\":\"").substringBefore("\"")
            val email = json.substringAfter("\"email\":\"").substringBefore("\"")
            val fullName = json.substringAfter("\"fullName\":\"").substringBefore("\"")
            val createdAt = json.substringAfter("\"createdAt\":\"").substringBefore("\"")
            User(id, email, fullName, createdAt)
        } catch (e: Exception) {
            null
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: AuthManager? = null
        
        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
}
