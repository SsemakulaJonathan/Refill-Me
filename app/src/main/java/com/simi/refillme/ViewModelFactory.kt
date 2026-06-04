package com.simi.refillme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.simi.refillme.data.api.ExpenseApiService
import com.simi.refillme.data.api.RefillApiService
import com.simi.refillme.data.api.ServiceApiService
import com.simi.refillme.data.api.SpecificationApiService
import com.simi.refillme.data.api.TripApiService
import com.simi.refillme.data.api.VehicleApiService
import com.simi.refillme.data.auth.AuthManager
import com.simi.refillme.data.repository.TripRepository
import com.simi.refillme.ui.viewmodel.ExpenseViewModel
import com.simi.refillme.ui.viewmodel.RefillViewModel
import com.simi.refillme.ui.viewmodel.ServiceViewModel
import com.simi.refillme.ui.viewmodel.SpecificationViewModel
import com.simi.refillme.ui.viewmodel.TripViewModel
import com.simi.refillme.ui.viewmodel.VehicleViewModel

class ViewModelFactory(
    private val vehicleApiService: VehicleApiService,
    private val refillApiService: RefillApiService,
    private val specificationApiService: SpecificationApiService,
    private val serviceApiService: ServiceApiService,
    private val expenseApiService: ExpenseApiService,
    private val tripApiService: TripApiService,
    private val authManager: AuthManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(VehicleViewModel::class.java) -> {
                VehicleViewModel(vehicleApiService, authManager) as T
            }
            modelClass.isAssignableFrom(RefillViewModel::class.java) -> {
                RefillViewModel(refillApiService, vehicleApiService, authManager) as T
            }
            modelClass.isAssignableFrom(SpecificationViewModel::class.java) -> {
                SpecificationViewModel(specificationApiService, authManager) as T
            }
            modelClass.isAssignableFrom(ServiceViewModel::class.java) -> {
                ServiceViewModel(serviceApiService, authManager) as T
            }
            modelClass.isAssignableFrom(ExpenseViewModel::class.java) -> {
                ExpenseViewModel(expenseApiService, authManager) as T
            }
            modelClass.isAssignableFrom(TripViewModel::class.java) -> {
                val tripRepository = TripRepository(tripApiService, authManager)
                TripViewModel(tripRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
