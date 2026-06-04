package com.simi.refillme.data.repository

import com.simi.refillme.data.dao.ServiceDao
import com.simi.refillme.data.dao.ServiceItemDao
import com.simi.refillme.data.entity.Service
import com.simi.refillme.data.entity.ServiceItem
import kotlinx.coroutines.flow.Flow

class ServiceRepository(
    private val serviceDao: ServiceDao,
    private val serviceItemDao: ServiceItemDao
) {

    fun getAllServices(): Flow<List<Service>> = serviceDao.getAllServices()

    fun getServicesByVehicle(vehicleId: Long): Flow<List<Service>> = serviceDao.getServicesByVehicle(vehicleId)

    suspend fun getServiceById(serviceId: Long): Service? = serviceDao.getServiceById(serviceId)

    suspend fun insertService(service: Service): Long = serviceDao.insertService(service)

    suspend fun updateService(service: Service) = serviceDao.updateService(service)

    suspend fun deleteService(service: Service) = serviceDao.deleteService(service)

    suspend fun getServiceCountByVehicle(vehicleId: Long): Int = serviceDao.getServiceCountByVehicle(vehicleId)

    suspend fun getTotalServiceCostByVehicle(vehicleId: Long): Double = serviceDao.getTotalServiceCostByVehicle(vehicleId) ?: 0.0

    // Service Items
    fun getServiceItemsByServiceId(serviceId: Long): Flow<List<ServiceItem>> = serviceItemDao.getServiceItemsByServiceId(serviceId)

    suspend fun getServiceItemsByServiceIdSync(serviceId: Long): List<ServiceItem> = serviceItemDao.getServiceItemsByServiceIdSync(serviceId)

    suspend fun insertServiceItem(serviceItem: ServiceItem): Long = serviceItemDao.insertServiceItem(serviceItem)

    suspend fun insertServiceItems(serviceItems: List<ServiceItem>) = serviceItemDao.insertServiceItems(serviceItems)

    suspend fun updateServiceItem(serviceItem: ServiceItem) = serviceItemDao.updateServiceItem(serviceItem)

    suspend fun deleteServiceItem(serviceItem: ServiceItem) = serviceItemDao.deleteServiceItem(serviceItem)

    suspend fun deleteServiceItemsByServiceId(serviceId: Long) = serviceItemDao.deleteServiceItemsByServiceId(serviceId)
}
