package com.simi.refillme.data.repository

import com.simi.refillme.data.dao.VehicleDao
import com.simi.refillme.data.entity.Vehicle
import kotlinx.coroutines.flow.Flow

class VehicleRepository(private val vehicleDao: VehicleDao) {
    
    fun getAllVehicles(): Flow<List<Vehicle>> = vehicleDao.getAllVehicles()
    
    fun getVehicleById(vehicleId: Long): Flow<Vehicle?> = vehicleDao.getVehicleById(vehicleId)
    
    suspend fun getVehicleByIdSync(vehicleId: Long): Vehicle? = vehicleDao.getVehicleByIdSync(vehicleId)
    
    suspend fun insertVehicle(vehicle: Vehicle): Long = vehicleDao.insertVehicle(vehicle)
    
    suspend fun updateVehicle(vehicle: Vehicle) = vehicleDao.updateVehicle(vehicle)
    
    suspend fun deactivateVehicle(vehicleId: Long) = vehicleDao.deactivateVehicle(vehicleId)
    
    suspend fun deleteVehicle(vehicle: Vehicle) = vehicleDao.deleteVehicle(vehicle)
    
    suspend fun updateFuelLevel(vehicleId: Long, fuelLevel: Double) = 
        vehicleDao.updateFuelLevel(vehicleId, fuelLevel)
}
