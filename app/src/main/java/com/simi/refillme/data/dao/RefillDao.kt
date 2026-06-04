package com.simi.refillme.data.dao

import androidx.room.*
import com.simi.refillme.data.entity.Refill
import com.simi.refillme.data.entity.VehicleWithRefills
import kotlinx.coroutines.flow.Flow

@Dao
interface RefillDao {
    @Query("SELECT * FROM refills ORDER BY date DESC")
    fun getAllRefills(): Flow<List<Refill>>

    @Query("SELECT * FROM refills WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getRefillsByVehicle(vehicleId: Long): Flow<List<Refill>>

    @Query("SELECT * FROM refills WHERE id = :refillId")
    fun getRefillById(refillId: Long): Flow<Refill?>

    @Transaction
    @Query("SELECT * FROM vehicles WHERE id = :vehicleId")
    fun getVehicleWithRefills(vehicleId: Long): Flow<VehicleWithRefills?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRefill(refill: Refill): Long

    @Update
    suspend fun updateRefill(refill: Refill)

    @Delete
    suspend fun deleteRefill(refill: Refill)

    @Query("SELECT * FROM refills WHERE vehicleId = :vehicleId ORDER BY date DESC LIMIT 1")
    suspend fun getLastRefill(vehicleId: Long): Refill?

    @Query("""
        SELECT SUM(totalPrice) FROM refills 
        WHERE vehicleId = :vehicleId 
        AND date >= :startDate 
        AND date <= :endDate
    """)
    suspend fun getTotalSpentInPeriod(vehicleId: Long, startDate: Long, endDate: Long): Double?

    @Query("""
        SELECT SUM(refillAmount) FROM refills 
        WHERE vehicleId = :vehicleId 
        AND date >= :startDate 
        AND date <= :endDate
    """)
    suspend fun getTotalLitersInPeriod(vehicleId: Long, startDate: Long, endDate: Long): Double?
}
