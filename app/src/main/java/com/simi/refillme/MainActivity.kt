package com.simi.refillme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.simi.refillme.data.auth.AuthManager
import com.simi.refillme.data.auth.AuthState
import com.simi.refillme.ui.navigation.AppNavigation
import com.simi.refillme.ui.navigation.Screen
import com.simi.refillme.ui.theme.RefillMeTheme
import com.simi.refillme.ui.viewmodel.RefillViewModel
import com.simi.refillme.ui.viewmodel.SpecificationViewModel
import com.simi.refillme.ui.viewmodel.VehicleViewModel
import com.simi.refillme.ui.viewmodel.PersonalExpenseViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val application = application as RefillMeApplication
        val authManager = AuthManager.getInstance(this)

        setContent {
            RefillMeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val preferences by application.preferencesManager.userPreferencesFlow.collectAsState(
                        initial = com.simi.refillme.data.entity.UserPreferences()
                    )
                    val authState by authManager.authState.collectAsState()

                    val vehicleViewModel: VehicleViewModel = viewModel(
                        factory = application.viewModelFactory
                    )

                    val refillViewModel: RefillViewModel = viewModel(
                        factory = application.viewModelFactory
                    )

                    val specificationViewModel: SpecificationViewModel = viewModel(
                        factory = application.viewModelFactory
                    )

                    val serviceViewModel: com.simi.refillme.ui.viewmodel.ServiceViewModel = viewModel(
                        factory = application.viewModelFactory
                    )

                    val expenseViewModel: com.simi.refillme.ui.viewmodel.ExpenseViewModel = viewModel(
                        factory = application.viewModelFactory
                    )

                    val tripViewModel: com.simi.refillme.ui.viewmodel.TripViewModel = viewModel(
                        factory = application.viewModelFactory
                    )

                    val personalExpenseViewModel: PersonalExpenseViewModel = viewModel(
                        factory = PersonalExpenseViewModel.factory(
                            application.personalExpenseApiService,
                            application.authManager
                        )
                    )

                    // Start Bluetooth monitoring service if any vehicle has auto-trip enabled
                    LaunchedEffect(preferences.vehicleAutoTripConfigs) {
                        val anyEnabled = preferences.vehicleAutoTripConfigs.any { it.autoTripEnabled }
                        if (anyEnabled) {
                            com.simi.refillme.service.BluetoothMonitoringService.start(this@MainActivity)
                        }
                    }

                    // Determine start destination based on auth state
                    val startDestination = when (authState) {
                        AuthState.Authenticated -> Screen.Dashboard.route
                        AuthState.Unauthenticated -> Screen.Login.route
                        AuthState.Loading -> Screen.Login.route // Show login while loading
                    }

                    AppNavigation(
                        navController = navController,
                        vehicleViewModel = vehicleViewModel,
                        refillViewModel = refillViewModel,
                        specificationViewModel = specificationViewModel,
                        serviceViewModel = serviceViewModel,
                        expenseViewModel = expenseViewModel,
                        tripViewModel = tripViewModel,
                        personalExpenseViewModel = personalExpenseViewModel,
                        preferencesManager = application.preferencesManager,
                        authManager = authManager,
                        preferences = preferences,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
