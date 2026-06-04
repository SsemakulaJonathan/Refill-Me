package com.simi.refillme.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PhotoSearchResponse(
    val success: Boolean,
    val photos: List<Photo>? = null,
    val message: String? = null
)

@Serializable
data class Photo(
    val id: String,
    val url: String,
    val thumbnail: String,
    val description: String? = null,
    val photographer: String,
    val photographerUrl: String
)

class PhotoApiService {
    
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
    
    suspend fun searchPhotos(query: String, page: Int = 1): Result<List<Photo>> {
        return try {
            val response = client.get("$baseUrl/search-photos") {
                parameter("query", query)
                parameter("page", page)
            }
            val result = response.body<PhotoSearchResponse>()
            if (result.success && result.photos != null) {
                Result.success(result.photos)
            } else {
                Result.failure(Exception(result.message ?: "Search failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: PhotoApiService? = null
        
        fun getInstance(): PhotoApiService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PhotoApiService().also { INSTANCE = it }
            }
        }
    }
}
