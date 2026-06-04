package com.simi.refillme.data.api

import android.util.Log
import com.simi.refillme.data.entity.Service
import com.simi.refillme.data.entity.ServiceItem
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Serializable
data class ServiceResponse(
    val success: Boolean = true,
    val message: String = "",
    val service: ServiceDto? = null,
    val services: List<ServiceDto> = emptyList()
)

@Serializable
data class ServiceDto(
    val id: Int,
    val vehicle_id: Int,
    val user_id: Int,
    val date: String,
    val odometer_reading: Int,
    val service_center: String,
    val total_cost: Double,
    val notes: String?,
    val receipt_image_path: String?,
    val service_items: List<ServiceItemDto>?,
    val created_at: String
)

@Serializable
data class ServiceItemDto(
    val id: Int,
    val service_name: String,
    val cost: Double
)

fun ServiceDto.toService(): Service {
    return Service(
        id = this.id.toLong(),
        vehicleId = this.vehicle_id.toLong(),
        date = java.time.Instant.parse(this.date).toEpochMilli(),
        odometerReading = this.odometer_reading,
        serviceCenter = this.service_center,
        totalCost = this.total_cost,
        notes = this.notes,
        receiptImagePath = this.receipt_image_path,
        createdAt = java.time.Instant.parse(this.created_at).toEpochMilli()
    )
}

fun ServiceItemDto.toServiceItem(serviceId: Long): ServiceItem {
    return ServiceItem(
        id = this.id.toLong(),
        serviceId = serviceId,
        serviceName = this.service_name,
        cost = this.cost
    )
}

@Serializable
data class CreateServiceRequest(
    val vehicle_id: Long,
    val user_id: Int,
    val date: String,
    val odometer_reading: Int,
    val service_center: String,
    val total_cost: Double,
    val notes: String?,
    val receipt_image_path: String?,
    val service_items: List<ServiceItemRequest>
)

@Serializable
data class ServiceItemRequest(
    val service_name: String,
    val cost: Double
)

class ServiceApiService {

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
        private var instance: ServiceApiService? = null

        fun getInstance(): ServiceApiService {
            return instance ?: synchronized(this) {
                instance ?: ServiceApiService().also { instance = it }
            }
        }
    }

    suspend fun getServices(token: String, vehicleId: Long? = null): Result<List<ServiceDto>> {
        return try {
            val url = if (vehicleId != null) {
                "$baseUrl/services?vehicle_id=$vehicleId"
            } else {
                "$baseUrl/services"
            }

            val response = client.get(url) {
                header("Authorization", "Bearer $token")
            }
            Result.success(response.body<List<ServiceDto>>())
        } catch (e: Exception) {
            Log.e("ServiceApiService", "Failed to get services", e)
            Result.failure(e)
        }
    }

    suspend fun createService(
        token: String,
        userId: Int,
        service: Service,
        serviceItems: List<Pair<String, Double>>
    ): Result<ServiceDto> {
        return try {
            // Format date as ISO-8601 timestamp (PostgreSQL compatible)
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            dateFormatter.timeZone = TimeZone.getTimeZone("UTC")
            val formattedDate = dateFormatter.format(Date(service.date))

            val request = CreateServiceRequest(
                vehicle_id = service.vehicleId,
                user_id = userId,
                date = formattedDate,
                odometer_reading = service.odometerReading,
                service_center = service.serviceCenter,
                total_cost = service.totalCost,
                notes = service.notes,
                receipt_image_path = service.receiptImagePath,
                service_items = serviceItems.map { (name, cost) ->
                    ServiceItemRequest(service_name = name, cost = cost)
                }
            )

            Log.d("ServiceApiService", "Creating service: $request")

            val response = client.post("$baseUrl/services") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            val result = response.body<ServiceDto>()
            Log.d("ServiceApiService", "Create service response: $result")
            Result.success(result)
        } catch (e: Exception) {
            Log.e("ServiceApiService", "Failed to create service", e)
            Result.failure(e)
        }
    }

    suspend fun updateService(
        token: String,
        userId: Int,
        id: Long,
        service: Service,
        serviceItems: List<Pair<String, Double>>
    ): Result<ServiceDto> {
        return try {
            // Format date as ISO-8601 timestamp (PostgreSQL compatible)
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            dateFormatter.timeZone = TimeZone.getTimeZone("UTC")
            val formattedDate = dateFormatter.format(Date(service.date))

            val request = CreateServiceRequest(
                vehicle_id = service.vehicleId,
                user_id = userId,
                date = formattedDate,
                odometer_reading = service.odometerReading,
                service_center = service.serviceCenter,
                total_cost = service.totalCost,
                notes = service.notes,
                receipt_image_path = service.receiptImagePath,
                service_items = serviceItems.map { (name, cost) ->
                    ServiceItemRequest(service_name = name, cost = cost)
                }
            )

            val response = client.put("$baseUrl/services?id=$id") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(response.body<ServiceDto>())
        } catch (e: Exception) {
            Log.e("ServiceApiService", "Failed to update service", e)
            Result.failure(e)
        }
    }

    suspend fun deleteService(token: String, id: Long): Result<ServiceResponse> {
        return try {
            val response = client.delete("$baseUrl/services?id=$id") {
                header("Authorization", "Bearer $token")
            }
            Result.success(response.body<ServiceResponse>())
        } catch (e: Exception) {
            Log.e("ServiceApiService", "Failed to delete service", e)
            Result.failure(e)
        }
    }
}
