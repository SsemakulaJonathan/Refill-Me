package com.simi.refillme.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class VehicleWithRefills(
    @Embedded val vehicle: Vehicle,
    @Relation(
        parentColumn = "id",
        entityColumn = "vehicleId"
    )
    val refills: List<Refill>
)
