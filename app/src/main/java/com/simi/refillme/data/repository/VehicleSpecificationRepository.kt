package com.simi.refillme.data.repository

import com.simi.refillme.data.dao.VehicleSpecificationDao
import com.simi.refillme.data.entity.VehicleSpecification
import kotlinx.coroutines.flow.Flow

class VehicleSpecificationRepository(private val specificationDao: VehicleSpecificationDao) {

    fun getSpecificationsForVehicle(vehicleId: Long): Flow<List<VehicleSpecification>> =
        specificationDao.getSpecificationsForVehicle(vehicleId)

    suspend fun getSpecificationsForVehicleSync(vehicleId: Long): List<VehicleSpecification> =
        specificationDao.getSpecificationsForVehicleSync(vehicleId)

    suspend fun insertSpecification(specification: VehicleSpecification): Long =
        specificationDao.insertSpecification(specification)

    suspend fun updateSpecification(specification: VehicleSpecification) =
        specificationDao.updateSpecification(specification)

    suspend fun deleteSpecification(specification: VehicleSpecification) =
        specificationDao.deleteSpecification(specification)

    suspend fun deleteAllSpecificationsForVehicle(vehicleId: Long) =
        specificationDao.deleteAllSpecificationsForVehicle(vehicleId)
}
