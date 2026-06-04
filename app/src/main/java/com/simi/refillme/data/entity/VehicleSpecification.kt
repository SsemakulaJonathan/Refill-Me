package com.simi.refillme.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vehicle_specifications",
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
data class VehicleSpecification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val name: String, // e.g., "Engine Size", "Car Weight", "Horsepower"
    val value: String, // e.g., "1998cc", "1460kg", "180hp"
    val createdAt: Long = System.currentTimeMillis()
)
