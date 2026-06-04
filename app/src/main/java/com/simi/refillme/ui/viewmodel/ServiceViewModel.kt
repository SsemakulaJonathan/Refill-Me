package com.simi.refillme.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simi.refillme.data.api.ServiceApiService
import com.simi.refillme.data.api.toService
import com.simi.refillme.data.api.toServiceItem
import com.simi.refillme.data.auth.AuthManager
import com.simi.refillme.data.entity.Service
import com.simi.refillme.data.entity.ServiceItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ServiceViewModel(
    private val serviceApiService: ServiceApiService,
    private val authManager: AuthManager
) : ViewModel() {

    private val _services = MutableStateFlow<List<Service>>(emptyList())
    val services: StateFlow<List<Service>> = _services

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadServices(vehicleId: Long? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = authManager.getToken()
                if (token != null) {
                    val result = serviceApiService.getServices(token, vehicleId)
                    result.onSuccess { serviceDtos ->
                        _services.value = serviceDtos.map { it.toService() }
                    }.onFailure { e ->
                        _error.value = e.message
                        Log.e("ServiceViewModel", "Failed to load services", e)
                    }
                } else {
                    _error.value = "Not authenticated"
                }
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("ServiceViewModel", "Error loading services", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addService(
        vehicleId: Long,
        date: Long,
        odometerReading: Int,
        serviceCenter: String,
        serviceItems: List<Pair<String, Double>>,
        totalCost: Double,
        notes: String,
        receiptUri: Uri?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = authManager.getToken()
                val user = authManager.currentUser.value

                if (token != null && user != null) {
                    // Save receipt image path if provided
                    val receiptPath = receiptUri?.toString()

                    // Create service record
                    val service = Service(
                        vehicleId = vehicleId,
                        date = date,
                        odometerReading = odometerReading,
                        serviceCenter = serviceCenter,
                        totalCost = totalCost,
                        notes = notes.ifBlank { null },
                        receiptImagePath = receiptPath
                    )

                    // Convert user ID from String to Int
                    val userId = user.id.toIntOrNull() ?: 0

                    // Call backend API
                    val result = serviceApiService.createService(token, userId, service, serviceItems)
                    result.onSuccess { serviceDto ->
                        Log.d("ServiceViewModel", "Service created successfully: $serviceDto")
                        loadServices(vehicleId) // Reload services
                        onSuccess()
                    }.onFailure { e ->
                        _error.value = e.message
                        onError(e.message ?: "Unknown error occurred")
                        Log.e("ServiceViewModel", "Failed to create service", e)
                    }
                } else {
                    val errorMsg = "Not authenticated"
                    _error.value = errorMsg
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                _error.value = e.message
                onError(e.message ?: "Unknown error occurred")
                Log.e("ServiceViewModel", "Error creating service", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateService(
        serviceId: Long,
        vehicleId: Long,
        date: Long,
        odometerReading: Int,
        serviceCenter: String,
        serviceItems: List<Pair<String, Double>>,
        totalCost: Double,
        notes: String,
        receiptUri: Uri?,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = authManager.getToken()
                val user = authManager.currentUser.value

                if (token != null && user != null) {
                    val receiptPath = receiptUri?.toString()

                    val service = Service(
                        id = serviceId,
                        vehicleId = vehicleId,
                        date = date,
                        odometerReading = odometerReading,
                        serviceCenter = serviceCenter,
                        totalCost = totalCost,
                        notes = notes.ifBlank { null },
                        receiptImagePath = receiptPath
                    )

                    // Convert user ID from String to Int
                    val userId = user.id.toIntOrNull() ?: 0

                    val result = serviceApiService.updateService(token, userId, serviceId, service, serviceItems)
                    result.onSuccess { serviceDto ->
                        Log.d("ServiceViewModel", "Service updated successfully: $serviceDto")
                        loadServices(vehicleId)
                        onSuccess()
                    }.onFailure { e ->
                        _error.value = e.message
                        onError(e.message ?: "Unknown error occurred")
                        Log.e("ServiceViewModel", "Failed to update service", e)
                    }
                } else {
                    val errorMsg = "Not authenticated"
                    _error.value = errorMsg
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                _error.value = e.message
                onError(e.message ?: "Unknown error occurred")
                Log.e("ServiceViewModel", "Error updating service", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteService(serviceId: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = authManager.getToken()
                if (token != null) {
                    val result = serviceApiService.deleteService(token, serviceId)
                    result.onSuccess { response ->
                        Log.d("ServiceViewModel", "Service deleted successfully")
                        loadServices() // Reload services
                        onComplete()
                    }.onFailure { e ->
                        _error.value = e.message
                        Log.e("ServiceViewModel", "Failed to delete service", e)
                    }
                } else {
                    _error.value = "Not authenticated"
                }
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("ServiceViewModel", "Error deleting service", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
