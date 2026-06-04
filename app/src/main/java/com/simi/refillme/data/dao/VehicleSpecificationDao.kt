package com.simi.refillme.data.dao

import androidx.room.*
import com.simi.refillme.data.entity.VehicleSpecification
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleSpecificationDao {
    @Query("SELECT * FROM vehicle_specifications WHERE vehicleId = :vehicleId ORDER BY createdAt ASC")
    fun getSpecificationsForVehicle(vehicleId: Long): Flow<List<VehicleSpecification>>

    @Query("SELECT * FROM vehicle_specifications WHERE vehicleId = :vehicleId ORDER BY createdAt ASC")
    suspend fun getSpecificationsForVehicleSync(vehicleId: Long): List<VehicleSpecification>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpecification(specification: VehicleSpecification): Long

    @Update
    suspend fun updateSpecification(specification: VehicleSpecification)

    @Delete
    suspend fun deleteSpecification(specification: VehicleSpecification)

    @Query("DELETE FROM vehicle_specifications WHERE vehicleId = :vehicleId")
    suspend fun deleteAllSpecificationsForVehicle(vehicleId: Long)
}
