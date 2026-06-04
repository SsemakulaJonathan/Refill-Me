package com.simi.refillme.data.api

import com.simi.refillme.data.entity.Vehicle
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
data class VehicleResponse(
    val success: Boolean,
    val message: String = "",
    val vehicle: VehicleDto? = null,
    val vehicles: List<VehicleDto> = emptyList()
)

@Serializable
data class VehicleDto(
    val id: Int,
    val user_id: Int,
    val name: String,
    val make: String?,
    val model: String?,
    val year: Int?,
    val license_plate: String?,
    val vin: String?,
    val insurance_number: String?,
    val fuel_type: String?,
    val fuel_capacity: Double,
    val current_fuel_level: Double? = null,
    val photo_uri: String?,
    val document1_uri: String?,
    val document2_uri: String?,
    val document3_uri: String?,
    val notes: String?,
    val created_at: String,
    val updated_at: String
)

@Serializable
data class CreateVehicleRequest(
    val name: String,
    val make: String,
    val model: String,
    val year: Int,
    val licensePlate: String,
    val vin: String? = null,
    val insuranceNumber: String? = null,
    val fuelType: String,
    val fuelCapacity: Double,
    val currentFuelLevel: Double? = null,
    val photoUri: String? = null,
    val document1Uri: String? = null,
    val document2Uri: String? = null,
    val document3Uri: String? = null,
    val notes: String? = null
)

fun VehicleDto.toVehicle(): Vehicle {
    return Vehicle(
        id = this.id.toLong(),
        name = this.name,
        make = this.make ?: "",
        model = this.model ?: "",
        year = this.year ?: 0,
        licensePlate = this.license_plate ?: "",
        vin = this.vin,
        insuranceNumber = this.insurance_number,
        fuelType = this.fuel_type ?: "PETROL",
        tankCapacity = this.fuel_capacity,
        currentFuelLevel = this.current_fuel_level ?: 0.0,
        photoUri = this.photo_uri,
        document1Uri = this.document1_uri,
        document2Uri = this.document2_uri,
        document3Uri = this.document3_uri,
        notes = this.notes
    )
}

class VehicleApiService {
    
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
        private var instance: VehicleApiService? = null

        fun getInstance(): VehicleApiService {
            return instance ?: synchronized(this) {
                instance ?: VehicleApiService().also { instance = it }
            }
        }
    }
    
    suspend fun getVehicles(token: String): Result<VehicleResponse> {
        return try {
            val response = client.get("$baseUrl/vehicles") {
                header("Authorization", "Bearer $token")
            }
            Result.success(response.body<VehicleResponse>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createVehicle(token: String, vehicle: Vehicle): Result<VehicleResponse> {
        return try {
            val request = CreateVehicleRequest(
                name = vehicle.name,
                make = vehicle.make,
                model = vehicle.model,
                year = vehicle.year,
                licensePlate = vehicle.licensePlate,
                vin = vehicle.vin,
                insuranceNumber = vehicle.insuranceNumber,
                fuelType = vehicle.fuelType,
                fuelCapacity = vehicle.tankCapacity,
                currentFuelLevel = vehicle.currentFuelLevel,
                photoUri = vehicle.photoUri,
                document1Uri = vehicle.document1Uri,
                document2Uri = vehicle.document2Uri,
                document3Uri = vehicle.document3Uri,
                notes = vehicle.notes
            )

            val response = client.post("$baseUrl/vehicles") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(response.body<VehicleResponse>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateVehicle(token: String, id: Long, vehicle: Vehicle): Result<VehicleResponse> {
        return try {
            val request = CreateVehicleRequest(
                name = vehicle.name,
                make = vehicle.make,
                model = vehicle.model,
                year = vehicle.year,
                licensePlate = vehicle.licensePlate,
                vin = vehicle.vin,
                insuranceNumber = vehicle.insuranceNumber,
                fuelType = vehicle.fuelType,
                fuelCapacity = vehicle.tankCapacity,
                currentFuelLevel = vehicle.currentFuelLevel,
                photoUri = vehicle.photoUri,
                document1Uri = vehicle.document1Uri,
                document2Uri = vehicle.document2Uri,
                document3Uri = vehicle.document3Uri,
                notes = vehicle.notes
            )

            val response = client.put("$baseUrl/vehicles?id=$id") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(response.body<VehicleResponse>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteVehicle(token: String, id: Long): Result<VehicleResponse> {
        return try {
            val response = client.delete("$baseUrl/vehicles?id=$id") {
                header("Authorization", "Bearer $token")
            }
            Result.success(response.body<VehicleResponse>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
