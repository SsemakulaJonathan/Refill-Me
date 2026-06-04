package com.simi.refillme.data.dao

import androidx.room.*
import com.simi.refillme.data.entity.Service
import com.simi.refillme.data.entity.ServiceItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceDao {
    @Query("SELECT * FROM services WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getServicesByVehicle(vehicleId: Long): Flow<List<Service>>

    @Query("SELECT * FROM services ORDER BY date DESC")
    fun getAllServices(): Flow<List<Service>>

    @Query("SELECT * FROM services WHERE id = :serviceId")
    suspend fun getServiceById(serviceId: Long): Service?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: Service): Long

    @Update
    suspend fun updateService(service: Service)

    @Delete
    suspend fun deleteService(service: Service)

    @Query("DELETE FROM services WHERE vehicleId = :vehicleId")
    suspend fun deleteServicesByVehicle(vehicleId: Long)

    @Query("SELECT COUNT(*) FROM services WHERE vehicleId = :vehicleId")
    suspend fun getServiceCountByVehicle(vehicleId: Long): Int

    @Query("SELECT SUM(totalCost) FROM services WHERE vehicleId = :vehicleId")
    suspend fun getTotalServiceCostByVehicle(vehicleId: Long): Double?
}

@Dao
interface ServiceItemDao {
    @Query("SELECT * FROM service_items WHERE serviceId = :serviceId ORDER BY id ASC")
    fun getServiceItemsByServiceId(serviceId: Long): Flow<List<ServiceItem>>

    @Query("SELECT * FROM service_items WHERE serviceId = :serviceId ORDER BY id ASC")
    suspend fun getServiceItemsByServiceIdSync(serviceId: Long): List<ServiceItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceItem(serviceItem: ServiceItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceItems(serviceItems: List<ServiceItem>)

    @Update
    suspend fun updateServiceItem(serviceItem: ServiceItem)

    @Delete
    suspend fun deleteServiceItem(serviceItem: ServiceItem)

    @Query("DELETE FROM service_items WHERE serviceId = :serviceId")
    suspend fun deleteServiceItemsByServiceId(serviceId: Long)
}
