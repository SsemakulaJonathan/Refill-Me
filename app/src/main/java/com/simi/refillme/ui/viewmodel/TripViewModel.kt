package com.simi.refillme.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simi.refillme.data.entity.Trip
import com.simi.refillme.data.repository.TripRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TripViewModel(
    private val tripRepository: TripRepository
) : ViewModel() {

    val trips: StateFlow<List<Trip>> = tripRepository.getAllTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTrip: StateFlow<Trip?> = tripRepository.observeActiveTrip()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun loadTrips(vehicleId: Long? = null) {
        viewModelScope.launch {
            tripRepository.loadTrips(vehicleId)
        }
    }
}
