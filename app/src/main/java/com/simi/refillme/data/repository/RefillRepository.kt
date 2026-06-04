package com.simi.refillme.data.repository

import com.simi.refillme.data.dao.RefillDao
import com.simi.refillme.data.entity.Refill
import com.simi.refillme.data.entity.VehicleWithRefills
import kotlinx.coroutines.flow.Flow

class RefillRepository(private val refillDao: RefillDao) {
    
    fun getAllRefills(): Flow<List<Refill>> = refillDao.getAllRefills()
    
    fun getRefillsByVehicle(vehicleId: Long): Flow<List<Refill>> = 
        refillDao.getRefillsByVehicle(vehicleId)
    
    fun getRefillById(refillId: Long): Flow<Refill?> = refillDao.getRefillById(refillId)
    
    fun getVehicleWithRefills(vehicleId: Long): Flow<VehicleWithRefills?> = 
        refillDao.getVehicleWithRefills(vehicleId)
    
    suspend fun insertRefill(refill: Refill): Long = refillDao.insertRefill(refill)
    
    suspend fun updateRefill(refill: Refill) = refillDao.updateRefill(refill)
    
    suspend fun deleteRefill(refill: Refill) = refillDao.deleteRefill(refill)
    
    suspend fun getLastRefill(vehicleId: Long): Refill? = refillDao.getLastRefill(vehicleId)
    
    suspend fun getTotalSpentInPeriod(vehicleId: Long, startDate: Long, endDate: Long): Double = 
        refillDao.getTotalSpentInPeriod(vehicleId, startDate, endDate) ?: 0.0
    
    suspend fun getTotalLitersInPeriod(vehicleId: Long, startDate: Long, endDate: Long): Double = 
        refillDao.getTotalLitersInPeriod(vehicleId, startDate, endDate) ?: 0.0
}
