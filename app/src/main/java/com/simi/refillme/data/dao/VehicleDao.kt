package com.simi.refillme.data.dao

import androidx.room.*
import com.simi.refillme.data.entity.Vehicle
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :vehicleId")
    fun getVehicleById(vehicleId: Long): Flow<Vehicle?>

    @Query("SELECT * FROM vehicles WHERE id = :vehicleId")
    suspend fun getVehicleByIdSync(vehicleId: Long): Vehicle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Update
    suspend fun updateVehicle(vehicle: Vehicle)

    @Query("UPDATE vehicles SET isActive = 0 WHERE id = :vehicleId")
    suspend fun deactivateVehicle(vehicleId: Long)

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    @Query("UPDATE vehicles SET currentFuelLevel = :fuelLevel WHERE id = :vehicleId")
    suspend fun updateFuelLevel(vehicleId: Long, fuelLevel: Double)
}
