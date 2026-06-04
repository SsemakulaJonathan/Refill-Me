package com.simi.refillme

import android.app.Application
import com.simi.refillme.data.api.ExpenseApiService
import com.simi.refillme.data.api.PersonalExpenseApiService
import com.simi.refillme.data.api.RefillApiService
import com.simi.refillme.data.api.ServiceApiService
import com.simi.refillme.data.api.SpecificationApiService
import com.simi.refillme.data.api.TripApiService
import com.simi.refillme.data.api.VehicleApiService
import com.simi.refillme.data.auth.AuthManager
import com.simi.refillme.data.database.RefillMeDatabase
import com.simi.refillme.data.preferences.PreferencesManager
import com.simi.refillme.data.repository.PersonalExpenseRepository
import com.simi.refillme.data.repository.TripRepository

class RefillMeApplication : Application() {

    val vehicleApiService by lazy { VehicleApiService.getInstance() }
    val refillApiService by lazy { RefillApiService.getInstance() }
    val specificationApiService by lazy { SpecificationApiService.getInstance() }
    val serviceApiService by lazy { ServiceApiService.getInstance() }
    val expenseApiService by lazy { ExpenseApiService.getInstance() }
    val tripApiService by lazy { TripApiService.getInstance() }
    val personalExpenseApiService by lazy { PersonalExpenseApiService.getInstance() }
    val authManager by lazy { AuthManager.getInstance(this) }
    val preferencesManager by lazy { PreferencesManager.getInstance(this) }

    val tripRepository by lazy { TripRepository(tripApiService, authManager) }

    val database by lazy { RefillMeDatabase.getDatabase(this) }
    val personalExpenseRepository by lazy { PersonalExpenseRepository(database) }

    val viewModelFactory by lazy {
        ViewModelFactory(
            vehicleApiService,
            refillApiService,
            specificationApiService,
            serviceApiService,
            expenseApiService,
            tripApiService,
            authManager
        )
    }
}
