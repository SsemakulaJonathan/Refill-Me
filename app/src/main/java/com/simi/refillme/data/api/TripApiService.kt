package com.simi.refillme.data.api

import com.simi.refillme.data.entity.Trip
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

@Serializable
data class TripResponse(
    val success: Boolean,
    val message: String = "",
    val trip: TripDto? = null,
    val trips: List<TripDto> = emptyList()
)

@Serializable
data class TripDto(
    val id: Int,
    @SerialName("vehicle_id") val vehicle_id: Int,
    @SerialName("user_id") val user_id: Int,
    @SerialName("start_time") val start_time: Long,
    @SerialName("end_time") val end_time: Long? = null,
    @SerialName("start_latitude") val start_latitude: Double? = null,
    @SerialName("start_longitude") val start_longitude: Double? = null,
    @SerialName("start_address") val start_address: String? = null,
    @SerialName("end_latitude") val end_latitude: Double? = null,
    @SerialName("end_longitude") val end_longitude: Double? = null,
    @SerialName("end_address") val end_address: String? = null,
    @SerialName("distance_km") val distance_km: Double? = null,
    @SerialName("average_speed_kmh") val average_speed_kmh: Double? = null,
    @SerialName("max_speed_kmh") val max_speed_kmh: Double? = null,
    @SerialName("duration_minutes") val duration_minutes: Long? = null,
    @SerialName("trip_type") val trip_type: String? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("is_active") val is_active: Boolean? = null,
    @SerialName("created_at") val created_at: String,
    @SerialName("updated_at") val updated_at: String
)

@Serializable
data class TripUpdateRequest(
    @SerialName("startLatitude") val startLatitude: Double? = null,
    @SerialName("startLongitude") val startLongitude: Double? = null,
    @SerialName("startAddress") val startAddress: String? = null,
    @SerialName("endTime") val endTime: Long? = null,
    @SerialName("endLatitude") val endLatitude: Double? = null,
    @SerialName("endLongitude") val endLongitude: Double? = null,
    @SerialName("endAddress") val endAddress: String? = null,
    @SerialName("distanceKm") val distanceKm: Double = 0.0,
    @SerialName("averageSpeedKmh") val averageSpeedKmh: Double = 0.0,
    @SerialName("maxSpeedKmh") val maxSpeedKmh: Double = 0.0,
    @SerialName("durationMinutes") val durationMinutes: Long = 0,
    @SerialName("tripType") val tripType: String? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("isActive") val isActive: Boolean = true
)

@Serializable
data class TripCreateRequest(
    @SerialName("vehicleId") val vehicleId: Long,
    @SerialName("startTime") val startTime: Long,
    @SerialName("startLatitude") val startLatitude: Double? = null,
    @SerialName("startLongitude") val startLongitude: Double? = null,
    @SerialName("startAddress") val startAddress: String? = null,
    @SerialName("endTime") val endTime: Long? = null,
    @SerialName("endLatitude") val endLatitude: Double? = null,
    @SerialName("endLongitude") val endLongitude: Double? = null,
    @SerialName("endAddress") val endAddress: String? = null,
    @SerialName("distanceKm") val distanceKm: Double? = null,
    @SerialName("averageSpeedKmh") val averageSpeedKmh: Double? = null,
    @SerialName("maxSpeedKmh") val maxSpeedKmh: Double? = null,
    @SerialName("durationMinutes") val durationMinutes: Long? = null,
    @SerialName("tripType") val tripType: String? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("isActive") val isActive: Boolean = true
)

fun TripDto.toTrip(): Trip {
    val tripType = when (this.trip_type?.uppercase()) {
        "BUSINESS" -> com.simi.refillme.data.entity.TripType.BUSINESS
        "COMMUTE" -> com.simi.refillme.data.entity.TripType.COMMUTE
        "PERSONAL" -> com.simi.refillme.data.entity.TripType.PERSONAL
        else -> com.simi.refillme.data.entity.TripType.PERSONAL
    }

    return Trip(
        id = this.id.toLong(),
        vehicleId = this.vehicle_id.toLong(),
        startTime = this.start_time,
        endTime = this.end_time,
        startLatitude = this.start_latitude,
        startLongitude = this.start_longitude,
        startAddress = this.start_address,
        endLatitude = this.end_latitude,
        endLongitude = this.end_longitude,
        endAddress = this.end_address,
        distanceKm = this.distance_km ?: 0.0,
        averageSpeedKmh = this.average_speed_kmh ?: 0.0,
        maxSpeedKmh = this.max_speed_kmh ?: 0.0,
        durationMinutes = this.duration_minutes ?: 0,
        tripType = tripType,
        notes = this.notes,
        isActive = this.is_active ?: false
    )
}

class TripApiService {
    
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
        private var instance: TripApiService? = null

        fun getInstance(): TripApiService {
            return instance ?: synchronized(this) {
                instance ?: TripApiService().also { instance = it }
            }
        }
    }
    
    suspend fun getTrips(token: String, vehicleId: Long? = null): Result<TripResponse> {
        return try {
            val url = if (vehicleId != null) {
                "$baseUrl/trips?vehicleId=$vehicleId"
            } else {
                "$baseUrl/trips"
            }
            
            val response = client.get(url) {
                header("Authorization", "Bearer $token")
            }
            Result.success(response.body<TripResponse>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTripById(token: String, id: Long): Result<TripResponse> {
        return try {
            val response = client.get("$baseUrl/trips?id=$id") {
                header("Authorization", "Bearer $token")
            }
            Result.success(response.body<TripResponse>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createTrip(token: String, trip: Trip): Result<TripResponse> {
        return try {
            val request = TripCreateRequest(
                vehicleId = trip.vehicleId,
                startTime = trip.startTime,
                startLatitude = trip.startLatitude,
                startLongitude = trip.startLongitude,
                startAddress = trip.startAddress,
                endTime = trip.endTime,
                endLatitude = trip.endLatitude,
                endLongitude = trip.endLongitude,
                endAddress = trip.endAddress,
                distanceKm = trip.distanceKm,
                averageSpeedKmh = trip.averageSpeedKmh,
                maxSpeedKmh = trip.maxSpeedKmh,
                durationMinutes = trip.durationMinutes,
                tripType = trip.tripType.name,
                notes = trip.notes,
                isActive = trip.isActive
            )
            android.util.Log.d("TripApiService", "Creating trip: vehicleId=${request.vehicleId}, startTime=${request.startTime}, endTime=${request.endTime}, distanceKm=${request.distanceKm}")
            val response = client.post("$baseUrl/trips") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            android.util.Log.d("TripApiService", "HTTP Status: ${response.status.value}")
            val result = response.body<TripResponse>()
            android.util.Log.d("TripApiService", "Create trip response: success=${result.success}, message=${result.message}")
            Result.success(result)
        } catch (e: Exception) {
            android.util.Log.e("TripApiService", "Error creating trip: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateTrip(token: String, id: Long, trip: Trip): Result<TripResponse> {
        return try {
            val request = TripUpdateRequest(
                startLatitude = trip.startLatitude,
                startLongitude = trip.startLongitude,
                startAddress = trip.startAddress,
                endTime = trip.endTime,
                endLatitude = trip.endLatitude,
                endLongitude = trip.endLongitude,
                endAddress = trip.endAddress,
                distanceKm = trip.distanceKm,
                averageSpeedKmh = trip.averageSpeedKmh,
                maxSpeedKmh = trip.maxSpeedKmh,
                durationMinutes = trip.durationMinutes,
                tripType = trip.tripType.name,
                notes = trip.notes,
                isActive = trip.isActive
            )
            android.util.Log.d("TripApiService", "Updating trip id=$id: distance=${request.distanceKm}km, active=${request.isActive}")
            val response = client.put("$baseUrl/trips?id=$id") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            val result = response.body<TripResponse>()
            android.util.Log.d("TripApiService", "Update trip response: success=${result.success}, message=${result.message}")
            Result.success(result)
        } catch (e: Exception) {
            android.util.Log.e("TripApiService", "Error updating trip: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteTrip(token: String, id: Long): Result<TripResponse> {
        return try {
            val response = client.delete("$baseUrl/trips?id=$id") {
                header("Authorization", "Bearer $token")
            }
            Result.success(response.body<TripResponse>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getActiveTrip(token: String): Result<Trip?> {
        return try {
            val response = client.get("$baseUrl/trips") {
                header("Authorization", "Bearer $token")
            }
            val result = response.body<TripResponse>()
            if (result.success) {
                val activeTrip = result.trips.find { it.is_active == true }
                Result.success(activeTrip?.toTrip())
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
