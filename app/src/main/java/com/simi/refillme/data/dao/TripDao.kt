package com.simi.refillme.data.dao

import androidx.room.*
import com.simi.refillme.data.entity.Trip
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips WHERE vehicleId = :vehicleId ORDER BY startTime DESC")
    fun getTripsByVehicle(vehicleId: Long): Flow<List<Trip>>

    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun getAllTrips(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripById(tripId: Long): Trip?

    @Query("SELECT * FROM trips WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTrip(): Trip?

    @Query("SELECT * FROM trips WHERE isActive = 1 LIMIT 1")
    fun observeActiveTrip(): Flow<Trip?>

    @Query("SELECT * FROM trips WHERE vehicleId = :vehicleId AND startTime >= :startTime AND startTime <= :endTime ORDER BY startTime DESC")
    fun getTripsInDateRange(vehicleId: Long, startTime: Long, endTime: Long): Flow<List<Trip>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip): Long

    @Update
    suspend fun updateTrip(trip: Trip)

    @Delete
    suspend fun deleteTrip(trip: Trip)

    @Query("DELETE FROM trips WHERE vehicleId = :vehicleId")
    suspend fun deleteTripsByVehicle(vehicleId: Long)

    @Query("SELECT COUNT(*) FROM trips WHERE vehicleId = :vehicleId")
    suspend fun getTripCountByVehicle(vehicleId: Long): Int

    @Query("SELECT SUM(distanceKm) FROM trips WHERE vehicleId = :vehicleId")
    suspend fun getTotalDistanceByVehicle(vehicleId: Long): Double?
}
