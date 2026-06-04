package com.simi.refillme.data.api

import com.simi.refillme.data.entity.VehicleSpecification
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SpecificationResponse(
    val success: Boolean,
    val message: String = "",
    val specification: SpecificationDto? = null,
    val specifications: List<SpecificationDto> = emptyList()
)

@Serializable
data class SpecificationDto(
    val id: Int,
    val vehicle_id: Int,
    val name: String,
    val value: String,
    val created_at: String
)

fun SpecificationDto.toVehicleSpecification(): VehicleSpecification {
    return VehicleSpecification(
        id = this.id.toLong(),
        vehicleId = this.vehicle_id.toLong(),
        name = this.name,
        value = this.value
    )
}

@Serializable
data class CreateSpecificationRequest(
    val vehicleId: Long,
    val name: String,
    val value: String
)

@Serializable
data class UpdateSpecificationRequest(
    val name: String,
    val value: String
)

class SpecificationApiService {

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

    companion object {
        @Volatile
        private var instance: SpecificationApiService? = null

        fun getInstance(): SpecificationApiService {
            return instance ?: synchronized(this) {
                instance ?: SpecificationApiService().also { instance = it }
            }
        }
    }

    suspend fun getSpecifications(token: String, vehicleId: Long): Result<SpecificationResponse> {
        return try {
            val response = client.get("$baseUrl/specifications?vehicleId=$vehicleId") {
                header("Authorization", "Bearer $token")
            }
            Result.success(response.body<SpecificationResponse>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createSpecification(
        token: String,
        specification: VehicleSpecification
    ): Result<SpecificationResponse> {
        return try {
            val request = CreateSpecificationRequest(
                vehicleId = specification.vehicleId,
                name = specification.name,
                value = specification.value
            )

            val response = client.post("$baseUrl/specifications") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(response.body<SpecificationResponse>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSpecification(
        token: String,
        id: Long,
        specification: VehicleSpecification
    ): Result<SpecificationResponse> {
        return try {
            val request = UpdateSpecificationRequest(
                name = specification.name,
                value = specification.value
            )

            val response = client.put("$baseUrl/specifications?id=$id") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(response.body<SpecificationResponse>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSpecification(token: String, id: Long): Result<SpecificationResponse> {
        return try {
            val response = client.delete("$baseUrl/specifications?id=$id") {
                header("Authorization", "Bearer $token")
            }
            Result.success(response.body<SpecificationResponse>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
