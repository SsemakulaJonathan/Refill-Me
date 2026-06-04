package com.simi.refillme.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String, // Vehicle nickname/name
    val make: String, // e.g., Toyota, Honda
    val model: String, // e.g., Camry, Civic
    val year: Int, // Year of manufacture
    val licensePlate: String,
    val vin: String? = null, // Vehicle Identification Number
    val insuranceNumber: String? = null,
    val fuelType: String, // Petrol, Diesel, Electric, Hybrid, etc.
    val tankCapacity: Double,
    val currentFuelLevel: Double = 0.0,
    val photoUri: String? = null, // Vehicle profile photo
    val document1Uri: String? = null, // Logbook, etc.
    val document2Uri: String? = null,
    val document3Uri: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

enum class FuelType(val displayName: String) {
    PETROL("Petrol"),
    DIESEL("Diesel"),
    ELECTRIC("Electric"),
    HYBRID("Hybrid"),
    LPG("LPG (Liquefied Petroleum Gas)"),
    CNG("CNG (Compressed Natural Gas)"),
    ETHANOL("Ethanol/E85")
}
