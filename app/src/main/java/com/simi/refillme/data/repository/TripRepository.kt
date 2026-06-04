package com.simi.refillme.data.repository

import android.util.Log
import com.simi.refillme.data.api.TripApiService
import com.simi.refillme.data.api.toTrip
import com.simi.refillme.data.auth.AuthManager
import com.simi.refillme.data.entity.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TripRepository(
    private val tripApiService: TripApiService,
    private val authManager: AuthManager
) {
    private val TAG = "TripRepository"
    
    // In-memory cache for trips
    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    private val _activeTrip = MutableStateFlow<Trip?>(null)

    fun getAllTrips(): Flow<List<Trip>> = _trips.asStateFlow()

    fun getTripsByVehicle(vehicleId: Long): Flow<List<Trip>> {
        val filtered = MutableStateFlow(_trips.value.filter { it.vehicleId == vehicleId })
        return filtered.asStateFlow()
    }

    fun getTripsInDateRange(vehicleId: Long, startTime: Long, endTime: Long): Flow<List<Trip>> {
        val filtered = MutableStateFlow(
            _trips.value.filter { 
                it.vehicleId == vehicleId && 
                it.startTime >= startTime && 
                it.startTime <= endTime 
            }
        )
        return filtered.asStateFlow()
    }

    suspend fun getTripById(tripId: Long): Trip? {
        val token = authManager.getToken() ?: return null
        return try {
            val result = tripApiService.getTripById(token, tripId)
            result.getOrNull()?.trip?.toTrip()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting trip by ID: ${e.message}")
            null
        }
    }

    suspend fun getActiveTrip(): Trip? {
        val token = authManager.getToken() ?: return null
        return try {
            val result = tripApiService.getActiveTrip(token)
            val activeTrip = result.getOrNull()
            _activeTrip.value = activeTrip
            activeTrip
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active trip: ${e.message}")
            null
        }
    }

    fun observeActiveTrip(): Flow<Trip?> = _activeTrip.asStateFlow()

    suspend fun insertTrip(trip: Trip): Long {
        val token = authManager.getToken() ?: return -1L
        return try {
            val result = tripApiService.createTrip(token, trip)
            result.fold(
                onSuccess = { response ->
                    if (response.success && response.trip != null) {
                        val newTrip = response.trip.toTrip()
                        _trips.value = _trips.value + newTrip
                        if (newTrip.isActive) {
                            _activeTrip.value = newTrip
                        }
                        Log.d(TAG, "Trip created successfully: id=${newTrip.id}")
                        newTrip.id
                    } else {
                        Log.e(TAG, "Insert trip failed: ${response.message}")
                        -1L
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "Error inserting trip: ${e.message}", e)
                    -1L
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception inserting trip: ${e.message}", e)
            -1L
        }
    }

    suspend fun updateTrip(trip: Trip) {
        val token = authManager.getToken() ?: return
        try {
            val result = tripApiService.updateTrip(token, trip.id, trip)
            result.onSuccess { response ->
                if (response.success && response.trip != null) {
                    val updatedTrip = response.trip.toTrip()
                    _trips.value = _trips.value.map { 
                        if (it.id == updatedTrip.id) updatedTrip else it 
                    }
                    if (updatedTrip.isActive) {
                        _activeTrip.value = updatedTrip
                    } else if (_activeTrip.value?.id == updatedTrip.id) {
                        _activeTrip.value = null
                    }
                    Log.d(TAG, "Trip updated successfully: ${updatedTrip.distanceKm}km")
                } else {
                    Log.e(TAG, "Update trip failed: ${response.message}")
                }
            }.onFailure { e ->
                Log.e(TAG, "Error updating trip: ${e.message}", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception updating trip: ${e.message}", e)
        }
    }

    suspend fun deleteTrip(trip: Trip) {
        val token = authManager.getToken() ?: return
        try {
            val result = tripApiService.deleteTrip(token, trip.id)
            result.onSuccess { response ->
                if (response.success) {
                    _trips.value = _trips.value.filter { it.id != trip.id }
                    if (_activeTrip.value?.id == trip.id) {
                        _activeTrip.value = null
                    }
                } else {
                    Log.e(TAG, "Delete trip failed: ${response.message}")
                }
            }.onFailure { e ->
                Log.e(TAG, "Error deleting trip: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception deleting trip: ${e.message}")
        }
    }

    suspend fun getTripCountByVehicle(vehicleId: Long): Int {
        return _trips.value.count { it.vehicleId == vehicleId }
    }

    suspend fun getTotalDistanceByVehicle(vehicleId: Long): Double {
        return _trips.value.filter { it.vehicleId == vehicleId }.sumOf { it.distanceKm }
    }
    
    suspend fun loadTrips(vehicleId: Long? = null) {
        val token = authManager.getToken() ?: return
        try {
            Log.d(TAG, "Loading trips from API...")
            val result = tripApiService.getTrips(token, vehicleId)
            result.onSuccess { response ->
                Log.d(TAG, "API Response - success: ${response.success}, message: ${response.message}")
                Log.d(TAG, "API Response - trips count: ${response.trips.size}")
                if (response.success) {
                    response.trips.forEachIndexed { index, tripDto ->
                        Log.d(TAG, "Trip $index DTO: id=${tripDto.id}, startLat=${tripDto.start_latitude}, startLng=${tripDto.start_longitude}, endLat=${tripDto.end_latitude}, endLng=${tripDto.end_longitude}")
                    }
                    _trips.value = response.trips.map { it.toTrip() }
                    _trips.value.forEachIndexed { index, trip ->
                        Log.d(TAG, "Trip $index Entity: id=${trip.id}, startLat=${trip.startLatitude}, startLng=${trip.startLongitude}, endLat=${trip.endLatitude}, endLng=${trip.endLongitude}")
                    }
                    _activeTrip.value = _trips.value.find { it.isActive }
                    Log.d(TAG, "Loaded ${_trips.value.size} trips successfully")
                } else {
                    Log.e(TAG, "Load trips failed: ${response.message}")
                }
            }.onFailure { e ->
                Log.e(TAG, "Error loading trips: ${e.message}", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading trips: ${e.message}", e)
        }
    }
}
