package com.simi.refillme.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simi.refillme.data.api.SpecificationApiService
import com.simi.refillme.data.api.toVehicleSpecification
import com.simi.refillme.data.auth.AuthManager
import com.simi.refillme.data.entity.VehicleSpecification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SpecificationViewModel(
    private val specificationApiService: SpecificationApiService,
    private val authManager: AuthManager
) : ViewModel() {

    private val _specifications = MutableStateFlow<Map<Long, List<VehicleSpecification>>>(emptyMap())
    val specifications: StateFlow<Map<Long, List<VehicleSpecification>>> = _specifications

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadSpecificationsForVehicle(vehicleId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = authManager.getToken()
                if (token != null) {
                    val result = specificationApiService.getSpecifications(token, vehicleId)
                    result.onSuccess { response ->
                        if (response.success) {
                            val specs = response.specifications.map { it.toVehicleSpecification() }
                            _specifications.value = _specifications.value.toMutableMap().apply {
                                put(vehicleId, specs)
                            }
                        } else {
                            _error.value = response.message
                        }
                    }.onFailure { e ->
                        _error.value = e.message
                        Log.e("SpecificationViewModel", "Failed to load specifications", e)
                    }
                } else {
                    _error.value = "Not authenticated"
                }
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("SpecificationViewModel", "Error loading specifications", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addSpecification(vehicleId: Long, name: String, value: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = authManager.getToken()
                if (token != null) {
                    val specification = VehicleSpecification(
                        vehicleId = vehicleId,
                        name = name,
                        value = value
                    )
                    val result = specificationApiService.createSpecification(token, specification)
                    result.onSuccess { response ->
                        if (response.success) {
                            loadSpecificationsForVehicle(vehicleId) // Reload
                            onComplete()
                        } else {
                            _error.value = response.message
                        }
                    }.onFailure { e ->
                        _error.value = e.message
                        Log.e("SpecificationViewModel", "Failed to add specification", e)
                    }
                } else {
                    _error.value = "Not authenticated"
                }
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("SpecificationViewModel", "Error adding specification", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteSpecification(specification: VehicleSpecification, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = authManager.getToken()
                if (token != null) {
                    val result = specificationApiService.deleteSpecification(token, specification.id)
                    result.onSuccess { response ->
                        if (response.success) {
                            loadSpecificationsForVehicle(specification.vehicleId) // Reload
                            onComplete()
                        } else {
                            _error.value = response.message
                        }
                    }.onFailure { e ->
                        _error.value = e.message
                        Log.e("SpecificationViewModel", "Failed to delete specification", e)
                    }
                } else {
                    _error.value = "Not authenticated"
                }
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("SpecificationViewModel", "Error deleting specification", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getSpecificationsFor(vehicleId: Long): List<VehicleSpecification> {
        return _specifications.value[vehicleId] ?: emptyList()
    }
}
