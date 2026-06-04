package com.simi.refillme.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simi.refillme.data.api.VehicleApiService
import com.simi.refillme.data.api.toVehicle
import com.simi.refillme.data.auth.AuthManager
import com.simi.refillme.data.entity.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VehicleViewModel(
    private val vehicleApiService: VehicleApiService,
    private val authManager: AuthManager
) : ViewModel() {

    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles

    private val _selectedVehicle = MutableStateFlow<Vehicle?>(null)
    val selectedVehicle: StateFlow<Vehicle?> = _selectedVehicle
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    private val _defaultVehicleId = MutableStateFlow<Long?>(null)
    val defaultVehicleId: StateFlow<Long?> = _defaultVehicleId

    init {
        loadVehicles()
    }

    fun loadVehicles() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = authManager.getToken()
                if (token != null) {
                    val result = vehicleApiService.getVehicles(token)
                    result.onSuccess { response ->
                        if (response.success) {
                            val vehicleList = response.vehicles.map { it.toVehicle() }
                            _vehicles.value = vehicleList
                            
                            // Set first vehicle as default if not already set
                            if (vehicleList.isNotEmpty() && _defaultVehicleId.value == null) {
                                // Sort by ID to get the first created vehicle (lowest ID)
                                val firstVehicle = vehicleList.minByOrNull { it.id }
                                firstVehicle?.let {
                                    _defaultVehicleId.value = it.id
                                    Log.d("VehicleViewModel", "Set default vehicle: ${it.name} (ID: ${it.id})")
                                }
                            }
                        } else {
                            authManager.handleApiError(response.message)
                            _error.value = response.message
                        }
                    }.onFailure { e ->
                        authManager.handleApiError(e.message ?: "")
                        _error.value = e.message
                        Log.e("VehicleViewModel", "Failed to load vehicles", e)
                    }
                } else {
                    _error.value = "Not authenticated"
                }
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("VehicleViewModel", "Error loading vehicles", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectVehicle(vehicleId: Long) {
        _selectedVehicle.value = _vehicles.value.find { it.id == vehicleId }
    }

    fun insertVehicle(vehicle: Vehicle, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = authManager.getToken()
                if (token != null) {
                    val result = vehicleApiService.createVehicle(token, vehicle)
                    result.onSuccess { response ->
                        if (response.success && response.vehicle != null) {
                            loadVehicles() // Reload to get updated list
                            onComplete(response.vehicle.id.toLong())
                        } else {
                            authManager.handleApiError(response.message)
                            _error.value = response.message
                        }
                    }.onFailure { e ->
                        authManager.handleApiError(e.message ?: "")
                        _error.value = e.message
                        Log.e("VehicleViewModel", "Failed to create vehicle", e)
                    }
                } else {
                    _error.value = "Not authenticated"
                }
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("VehicleViewModel", "Error creating vehicle", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateVehicle(vehicle: Vehicle, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = authManager.getToken()
                if (token != null) {
                    val result = vehicleApiService.updateVehicle(token, vehicle.id, vehicle)
                    result.onSuccess { response ->
                        if (response.success) {
                            loadVehicles() // Reload to get updated list
                            onComplete()
                        } else {
                            _error.value = response.message
                        }
                    }.onFailure { e ->
                        _error.value = e.message
                        Log.e("VehicleViewModel", "Failed to update vehicle", e)
                    }
                } else {
                    _error.value = "Not authenticated"
                }
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("VehicleViewModel", "Error updating vehicle", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteVehicle(vehicle: Vehicle, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = authManager.getToken()
                if (token != null) {
                    val result = vehicleApiService.deleteVehicle(token, vehicle.id)
                    result.onSuccess { response ->
                        if (response.success) {
                            loadVehicles() // Reload to get updated list
                            onComplete()
                        } else {
                            _error.value = response.message
                        }
                    }.onFailure { e ->
                        _error.value = e.message
                        Log.e("VehicleViewModel", "Failed to delete vehicle", e)
                    }
                } else {
                    _error.value = "Not authenticated"
                }
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("VehicleViewModel", "Error deleting vehicle", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateFuelLevel(vehicleId: Long, fuelLevel: Double) {
        viewModelScope.launch {
            val vehicle = _vehicles.value.find { it.id == vehicleId }
            if (vehicle != null) {
                updateVehicle(vehicle.copy(currentFuelLevel = fuelLevel))
            }
        }
    }
}
