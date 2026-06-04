package com.simi.refillme.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "refills",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId")]
)
data class Refill(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val date: Long = System.currentTimeMillis(),
    val fuelBefore: Double, // Fuel level before refill
    val refillAmount: Double, // Amount refilled
    val fuelAfter: Double, // Fuel level after refill
    val unitPrice: Double, // Price per liter
    val totalPrice: Double, // Total cost
    val odometerReading: Double? = null, // Optional odometer reading
    val notes: String? = null,
    val location: String? = null
)
