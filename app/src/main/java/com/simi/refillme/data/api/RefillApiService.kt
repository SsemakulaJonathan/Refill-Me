package com.simi.refillme.data.api

import android.util.Log
import com.simi.refillme.data.entity.Refill
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
data class RefillResponse(
    val success: Boolean,
    val message: String = "",
    val refill: RefillDto? = null,
    val refills: List<RefillDto> = emptyList()
)

@Serializable
data class RefillDto(
    val id: Int,
    val vehicle_id: Int,
    val user_id: Int,
    val date: String,
    val odometer_reading: Double,
    val fuel_amount: Double,
    val fuel_after: Double?,
    val price_per_unit: Double,
    val total_cost: Double,
    val filling_station: String?,
    val is_full_tank: Boolean,
    val notes: String?,
    val created_at: String,
    val updated_at: String
)

fun RefillDto.toRefill(): Refill {
    // Calculate fuelBefore from fuelAfter - fuelAmount
    val fuelAfter = this.fuel_after ?: this.fuel_amount
    val fuelBefore = maxOf(0.0, fuelAfter - this.fuel_amount)

    return Refill(
        id = this.id.toLong(),
        vehicleId = this.vehicle_id.toLong(),
        date = java.time.Instant.parse(this.date).toEpochMilli(),
        fuelBefore = fuelBefore,
        refillAmount = this.fuel_amount,
        fuelAfter = fuelAfter,
        unitPrice = this.price_per_unit,
        totalPrice = this.total_cost,
        odometerReading = this.odometer_reading,
        notes = this.notes,
        location = this.filling_station
    )
}

@Serializable
data class CreateRefillRequest(
    val vehicleId: Long,
    val date: String,
    val odometerReading: Double? = null,
    val fuelAmount: Double,
    val fuelAfter: Double? = null,
    val pricePerUnit: Double,
    val totalCost: Double,
    val fillingStation: String? = null,
    val isFullTank: Boolean = false,
    val notes: String? = null
)

class RefillApiService {
    
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
        private var instance: RefillApiService? = null

        fun getInstance(): RefillApiService {
            return instance ?: synchronized(this) {
                instance ?: RefillApiService().also { instance = it }
            }
        }
    }
    
    suspend fun getRefills(token: String, vehicleId: Long? = null): Result<RefillResponse> {
        return try {
            val url = if (vehicleId != null) {
                "$baseUrl/refills?vehicleId=$vehicleId"
            } else {
                "$baseUrl/refills"
            }
            
            val response = client.get(url) {
                header("Authorization", "Bearer $token")
            }
            Result.success(response.body<RefillResponse>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createRefill(token: String, refill: Refill): Result<RefillResponse> {
        return try {
            // Format date as ISO-8601 timestamp (PostgreSQL compatible)
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            dateFormatter.timeZone = TimeZone.getTimeZone("UTC")
            val formattedDate = dateFormatter.format(Date(refill.date))

            val request = CreateRefillRequest(
                vehicleId = refill.vehicleId,
                date = formattedDate,
                odometerReading = refill.odometerReading,
                fuelAmount = refill.refillAmount,
                fuelAfter = refill.fuelAfter,
                pricePerUnit = refill.unitPrice,
                totalCost = refill.totalPrice,
                fillingStation = refill.location,
                isFullTank = false,
                notes = refill.notes
            )

            Log.d("RefillApiService", "Creating refill: $request")

            val response = client.post("$baseUrl/refills") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            val result = response.body<RefillResponse>()
            Log.d("RefillApiService", "Create refill response: $result")
            Result.success(result)
        } catch (e: Exception) {
            Log.e("RefillApiService", "Failed to create refill", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateRefill(token: String, id: Long, refill: Refill): Result<RefillResponse> {
        return try {
            // Format date as ISO-8601 timestamp (PostgreSQL compatible)
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            dateFormatter.timeZone = TimeZone.getTimeZone("UTC")
            val formattedDate = dateFormatter.format(Date(refill.date))

            val request = CreateRefillRequest(
                vehicleId = refill.vehicleId,
                date = formattedDate,
                odometerReading = refill.odometerReading,
                fuelAmount = refill.refillAmount,
                fuelAfter = refill.fuelAfter,
                pricePerUnit = refill.unitPrice,
                totalCost = refill.totalPrice,
                fillingStation = refill.location,
                isFullTank = false,
                notes = refill.notes
            )

            val response = client.put("$baseUrl/refills?id=$id") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(response.body<RefillResponse>())
        } catch (e: Exception) {
            Log.e("RefillApiService", "Failed to update refill", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteRefill(token: String, id: Long): Result<RefillResponse> {
        return try {
            val response = client.delete("$baseUrl/refills?id=$id") {
                header("Authorization", "Bearer $token")
            }
            Result.success(response.body<RefillResponse>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
