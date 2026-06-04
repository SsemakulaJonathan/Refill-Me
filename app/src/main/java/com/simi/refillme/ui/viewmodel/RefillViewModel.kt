package com.simi.refillme.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simi.refillme.data.api.RefillApiService
import com.simi.refillme.data.api.VehicleApiService
import com.simi.refillme.data.api.toRefill
import com.simi.refillme.data.api.toVehicle
import com.simi.refillme.data.auth.AuthManager
import com.simi.refillme.data.entity.Refill
import com.simi.refillme.data.entity.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RefillFormState(
    val id: Long? = null, // null for new refill, non-null for editing
    val vehicleId: Long? = null,
    val date: Long = System.currentTimeMillis(), // Timestamp for the refill date
    val fuelBefore: String = "",
    val refillAmount: String = "",
    val unitPrice: String = "",
    val totalPrice: String = "",
    val odometerReading: String = "",
    val notes: String = "",
    val location: String = ""
)

class RefillViewModel(
    private val refillApiService: RefillApiService,
    private val vehicleApiService: VehicleApiService,
    private val authManager: AuthManager
) : ViewModel() {

    private val _formState = MutableStateFlow(RefillFormState())
    val formState: StateFlow<RefillFormState> = _formState

    private val _allRefills = MutableStateFlow<List<Refill>>(emptyList())
    val allRefills: StateFlow<List<Refill>> = _allRefills
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())

    init {
        loadRefills()
        loadVehicles()
    }

    fun loadRefills(vehicleId: Long? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = authManager.getToken()
                if (token != null) {
                    val result = refillApiService.getRefills(token, vehicleId)
                    result.onSuccess { response ->
                        if (response.success) {
                            _allRefills.value = response.refills.map { it.toRefill() }
                        } else {
                            authManager.handleApiError(response.message)
                            _error.value = response.message
                        }
                    }.onFailure { e ->
                        authManager.handleApiError(e.message ?: "")
                        _error.value = e.message
                        Log.e("RefillViewModel", "Failed to load refills", e)
                    }
                } else {
                    _error.value = "Not authenticated"
                }
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("RefillViewModel", "Error loading refills", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun loadVehicles() {
        viewModelScope.launch {
            try {
                val token = authManager.getToken()
                if (token != null) {
                    val result = vehicleApiService.getVehicles(token)
                    result.onSuccess { response ->
                        if (response.success) {
                            _vehicles.value = response.vehicles.map { it.toVehicle() }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("RefillViewModel", "Error loading vehicles", e)
            }
        }
    }

    fun getRefillsByVehicle(vehicleId: Long): StateFlow<List<Refill>> {
        val filtered = MutableStateFlow<List<Refill>>(emptyList())
        viewModelScope.launch {
            _allRefills.collect { refills ->
                filtered.value = refills.filter { it.vehicleId == vehicleId }
            }
        }
        return filtered
    }

    fun updateFormState(newState: RefillFormState) {
        _formState.value = newState
    }

    /**
     * Smart calculation for refill fields
     * When any 2 of the 3 fields (refillAmount, unitPrice, totalPrice) are provided,
     * automatically calculate the third one
     */
    fun calculateMissingField(
        refillAmount: String,
        unitPrice: String,
        totalPrice: String
    ): RefillFormState {
        val amount = refillAmount.toDoubleOrNull()
        val unit = unitPrice.toDoubleOrNull()
        val total = totalPrice.toDoubleOrNull()

        val newState = _formState.value

        return when {
            // Calculate refill amount: totalPrice / unitPrice
            amount == null && unit != null && total != null && unit > 0 -> {
                newState.copy(refillAmount = String.format("%.2f", total / unit))
            }
            // Calculate unit price: totalPrice / refillAmount
            unit == null && amount != null && total != null && amount > 0 -> {
                newState.copy(unitPrice = String.format("%.2f", total / amount))
            }
            // Calculate total price: refillAmount * unitPrice
            total == null && amount != null && unit != null -> {
                newState.copy(totalPrice = String.format("%.2f", amount * unit))
            }
            else -> newState
        }
    }

    fun saveRefill(onComplete: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val state = _formState.value
                
                // Validation
                val vehicleId = state.vehicleId ?: run {
                    onError("Please select a vehicle")
                    _isLoading.value = false
                    return@launch
                }

                val fuelBefore = state.fuelBefore.toDoubleOrNull() ?: run {
                    onError("Invalid fuel before value")
                    _isLoading.value = false
                    return@launch
                }

                val refillAmount = state.refillAmount.toDoubleOrNull() ?: run {
                    onError("Invalid refill amount")
                    _isLoading.value = false
                    return@launch
                }

                val unitPrice = state.unitPrice.toDoubleOrNull() ?: run {
                    onError("Invalid unit price")
                    _isLoading.value = false
                    return@launch
                }

                val totalPrice = state.totalPrice.toDoubleOrNull() ?: run {
                    onError("Invalid total price")
                    _isLoading.value = false
                    return@launch
                }

                val odometerReading = state.odometerReading.toDoubleOrNull() ?: run {
                    onError("Odometer reading is required")
                    _isLoading.value = false
                    return@launch
                }

                val fuelAfter = fuelBefore + refillAmount

                // Check if fuel after exceeds tank capacity
                val vehicle = _vehicles.value.find { it.id == vehicleId }
                if (vehicle != null && fuelAfter > vehicle.tankCapacity) {
                    onError("Total fuel (${String.format("%.2f", fuelAfter)}L) exceeds tank capacity (${vehicle.tankCapacity}L)")
                    _isLoading.value = false
                    return@launch
                }

                val refill = Refill(
                    id = state.id ?: 0L, // Use existing ID or 0 for new refill
                    vehicleId = vehicleId,
                    date = state.date,
                    fuelBefore = fuelBefore,
                    refillAmount = refillAmount,
                    fuelAfter = fuelAfter,
                    unitPrice = unitPrice,
                    totalPrice = totalPrice,
                    odometerReading = odometerReading,
                    notes = state.notes.ifBlank { null },
                    location = state.location.ifBlank { null }
                )

                val token = authManager.getToken()
                if (token != null) {
                    // Check if we're editing or creating
                    val result = if (state.id != null && state.id > 0L) {
                        refillApiService.updateRefill(token, state.id, refill)
                    } else {
                        refillApiService.createRefill(token, refill)
                    }

                    result.onSuccess { response ->
                        if (response.success && response.refill != null) {
                            // Backend automatically updates vehicle's fuel level based on most recent refill by date

                            // Reload refills
                            loadRefills()

                            // Reset form
                            _formState.value = RefillFormState()
                            onComplete()
                        } else {
                            onError(response.message)
                        }
                    }.onFailure { e ->
                        onError(e.message ?: "Failed to save refill")
                        Log.e("RefillViewModel", "Failed to save refill", e)
                    }
                } else {
                    onError("Not authenticated")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to save refill")
                Log.e("RefillViewModel", "Error saving refill", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateRefill(refill: Refill, onComplete: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = authManager.getToken()
                if (token != null) {
                    val result = refillApiService.updateRefill(token, refill.id, refill)
                    result.onSuccess { response ->
                        if (response.success) {
                            loadRefills() // Reload to get updated list
                            onComplete()
                        } else {
                            onError(response.message)
                        }
                    }.onFailure { e ->
                        onError(e.message ?: "Failed to update refill")
                        Log.e("RefillViewModel", "Failed to update refill", e)
                    }
                } else {
                    onError("Not authenticated")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to update refill")
                Log.e("RefillViewModel", "Error updating refill", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteRefill(refill: Refill, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = authManager.getToken()
                if (token != null) {
                    val result = refillApiService.deleteRefill(token, refill.id)
                    result.onSuccess { response ->
                        if (response.success) {
                            loadRefills() // Reload to get updated list
                            onComplete()
                        } else {
                            _error.value = response.message
                        }
                    }.onFailure { e ->
                        _error.value = e.message
                        Log.e("RefillViewModel", "Failed to delete refill", e)
                    }
                } else {
                    _error.value = "Not authenticated"
                }
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("RefillViewModel", "Error deleting refill", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadVehicleFuelLevel(vehicleId: Long) {
        viewModelScope.launch {
            val vehicle = _vehicles.value.find { it.id == vehicleId }
            vehicle?.let {
                _formState.value = _formState.value.copy(
                    vehicleId = vehicleId,
                    fuelBefore = String.format("%.2f", it.currentFuelLevel)
                )
            }
        }
    }

    fun loadRefill(refillId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Load all refills if not already loaded
                if (_allRefills.value.isEmpty()) {
                    loadRefills()
                }

                // Find the refill by ID
                val refill = _allRefills.value.find { it.id == refillId }
                if (refill != null) {
                    // Populate form state with existing refill data
                    _formState.value = RefillFormState(
                        id = refill.id,
                        vehicleId = refill.vehicleId,
                        date = refill.date,
                        fuelBefore = refill.fuelBefore.toString(),
                        refillAmount = refill.refillAmount.toString(),
                        unitPrice = refill.unitPrice.toString(),
                        totalPrice = refill.totalPrice.toString(),
                        odometerReading = refill.odometerReading.toString(),
                        notes = refill.notes ?: "",
                        location = refill.location ?: ""
                    )
                } else {
                    _error.value = "Refill not found"
                }
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("RefillViewModel", "Error loading refill", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
