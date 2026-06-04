package com.simi.refillme.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trips",
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
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val startTime: Long,
    val endTime: Long? = null,
    val startLatitude: Double?,
    val startLongitude: Double?,
    val startAddress: String? = null,
    val endLatitude: Double? = null,
    val endLongitude: Double? = null,
    val endAddress: String? = null,
    val distanceKm: Double = 0.0,
    val averageSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val durationMinutes: Long = 0,
    val tripType: TripType = TripType.PERSONAL,
    val notes: String? = null,
    val isActive: Boolean = true // true if trip is ongoing
)

enum class TripType {
    PERSONAL,
    BUSINESS,
    COMMUTE
}
