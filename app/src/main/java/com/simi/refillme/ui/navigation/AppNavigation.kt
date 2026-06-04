package com.simi.refillme.ui.navigation

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.simi.refillme.data.auth.AuthApiService
import com.simi.refillme.data.auth.AuthManager
import com.simi.refillme.data.entity.UserPreferences
import com.simi.refillme.data.entity.Vehicle
import com.simi.refillme.data.preferences.PreferencesManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.simi.refillme.ui.screen.AddExpenseScreen
import com.simi.refillme.ui.screen.AddPersonalExpenseScreen
import com.simi.refillme.ui.screen.AddRefillScreen
import com.simi.refillme.ui.screen.AddServiceScreen
import com.simi.refillme.ui.screen.AddVehicleScreen
import com.simi.refillme.ui.screen.AllTripsScreen
import com.simi.refillme.ui.screen.AutoTripLoggingScreen
import com.simi.refillme.ui.screen.DashboardScreen
import com.simi.refillme.ui.screen.MapsScreen
import com.simi.refillme.ui.screen.PersonalExpensesScreen
import com.simi.refillme.ui.screen.RefillHistoryScreen
import com.simi.refillme.ui.screen.SettingsScreen
import com.simi.refillme.ui.screen.VehiclesScreen
import com.simi.refillme.ui.screen.auth.LoginScreen
import com.simi.refillme.ui.screen.auth.SignupScreen
import com.simi.refillme.ui.components.SpecificationsDialog
import com.simi.refillme.ui.viewmodel.ExpenseViewModel
import com.simi.refillme.ui.viewmodel.PersonalExpenseViewModel
import com.simi.refillme.ui.viewmodel.RefillViewModel
import com.simi.refillme.ui.viewmodel.ServiceViewModel
import com.simi.refillme.ui.viewmodel.SpecificationViewModel
import com.simi.refillme.ui.viewmodel.VehicleViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    navController: NavHostController,
    vehicleViewModel: VehicleViewModel,
    refillViewModel: RefillViewModel,
    specificationViewModel: SpecificationViewModel,
    serviceViewModel: ServiceViewModel,
    expenseViewModel: ExpenseViewModel,
    tripViewModel: com.simi.refillme.ui.viewmodel.TripViewModel,
    personalExpenseViewModel: PersonalExpenseViewModel,
    preferencesManager: PreferencesManager,
    authManager: AuthManager,
    preferences: UserPreferences,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    val vehicles by vehicleViewModel.vehicles.collectAsState()
    val allRefills by refillViewModel.allRefills.collectAsState()
    val refillFormState by refillViewModel.formState.collectAsState()
    val specifications by specificationViewModel.specifications.collectAsState()
    val allServices by serviceViewModel.services.collectAsState()
    val allExpenses by expenseViewModel.expenses.collectAsState()
    val allTrips by tripViewModel.trips.collectAsState()
    val activeTrip by tripViewModel.activeTrip.collectAsState()
    val personalExpenses by personalExpenseViewModel.expenses.collectAsState()
    val personalCategories by personalExpenseViewModel.categories.collectAsState()
    val personalAllItems by personalExpenseViewModel.allItems.collectAsState()
    val scope = rememberCoroutineScope()
    val authApi = remember { AuthApiService.getInstance() }

    // Load services, expenses, and trips when authenticated
    LaunchedEffect(authManager.authState.collectAsState().value) {
        if (authManager.isAuthenticated()) {
            Log.d("AppNavigation", "User authenticated, loading data...")
            serviceViewModel.loadServices()
            expenseViewModel.loadExpenses()
            tripViewModel.loadTrips()
            Log.d("AppNavigation", "Data load triggered")
        }
    }
    
    // Log trips count whenever it changes
    LaunchedEffect(allTrips) {
        Log.d("AppNavigation", "allTrips updated: size=${allTrips.size}")
    }

    var selectedVehicleForSpecs by remember { mutableStateOf<Vehicle?>(null) }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignup = {
                    navController.navigate(Screen.Signup.route)
                },
                onLogin = { email, password ->
                    val result = authApi.login(email, password)
                    result.onSuccess { response ->
                        if (response.success && response.token != null && response.user != null) {
                            authManager.saveAuth(response.token, response.user)
                        }
                    }
                    if (result.isSuccess && result.getOrNull()?.success == true) {
                        Result.success("Login successful")
                    } else {
                        Result.failure(Exception(result.getOrNull()?.message ?: "Login failed"))
                    }
                }
            )
        }
        
        composable(Screen.Signup.route) {
            SignupScreen(
                onSignupSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onSignup = { email, password, fullName ->
                    val result = authApi.signup(email, password, fullName)
                    result.onSuccess { response ->
                        if (response.success && response.token != null && response.user != null) {
                            authManager.saveAuth(response.token, response.user)
                        }
                    }
                    if (result.isSuccess && result.getOrNull()?.success == true) {
                        Result.success("Signup successful")
                    } else {
                        Result.failure(Exception(result.getOrNull()?.message ?: "Signup failed"))
                    }
                }
            )
        }
        
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                vehicles = vehicles,
                recentRefills = allRefills,
                allServices = allServices,
                allExpenses = allExpenses,
                allTrips = allTrips,
                preferences = preferences,
                activeVehicleId = preferences.activeViewingVehicleId,
                onVehicleSelected = { vehicleId ->
                    scope.launch { preferencesManager.updateActiveViewingVehicle(vehicleId) }
                },
                onNavigateToAddRefill = { vehicleId ->
                    if (vehicles.isNotEmpty()) {
                        navController.navigate(Screen.AddRefill.createRoute(vehicleId))
                    } else {
                        navController.navigate(Screen.AddVehicle.createRoute())
                    }
                },
                onNavigateToAddService = { vehicleId ->
                    if (vehicles.isNotEmpty()) {
                        navController.navigate(Screen.AddService.createRoute(vehicleId))
                    } else {
                        navController.navigate(Screen.AddVehicle.createRoute())
                    }
                },
                onNavigateToAddExpense = { vehicleId ->
                    if (vehicles.isNotEmpty()) {
                        navController.navigate(Screen.AddExpense.createRoute(vehicleId))
                    } else {
                        navController.navigate(Screen.AddVehicle.createRoute())
                    }
                },
                onEditRefill = { refill ->
                    navController.navigate(Screen.AddRefill.createRoute(refill.vehicleId, refill.id))
                },
                onDeleteRefill = { refill ->
                    scope.launch {
                        refillViewModel.deleteRefill(refill)
                    }
                },
                onNavigateToVehicles = {
                    navController.navigate(Screen.Vehicles.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToAutoTripLogging = {
                    navController.navigate(Screen.AutoTripLogging.route)
                },
                onNavigateToMaps = {
                    navController.navigate(Screen.Maps.route)
                },
                onNavigateToPersonalExpenses = {
                    navController.navigate(Screen.PersonalExpenses.route)
                }
            )
        }

        composable(
            route = Screen.AddRefill.route,
            arguments = listOf(
                navArgument("vehicleId") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
                navArgument("refillId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: 0L
            val refillIdString = backStackEntry.arguments?.getString("refillId")
            val refillId = refillIdString?.toLongOrNull()

            val isLoading by refillViewModel.isLoading.collectAsState()
            val error by refillViewModel.error.collectAsState()
            var saveError by remember { mutableStateOf<String?>(null) }

            // Load existing refill if refillId is provided
            LaunchedEffect(refillId) {
                if (refillId != null && refillId > 0L) {
                    refillViewModel.loadRefill(refillId)
                } else {
                    // Pre-select vehicle if vehicleId is provided and not editing
                    if (vehicleId > 0L && refillFormState.vehicleId != vehicleId) {
                        refillViewModel.updateFormState(refillFormState.copy(vehicleId = vehicleId))
                    }
                }
            }

            AddRefillScreen(
                vehicles = vehicles,
                formState = refillFormState,
                preferences = preferences,
                onFormStateChange = { refillViewModel.updateFormState(it) },
                onSave = {
                    refillViewModel.saveRefill(
                        onComplete = {
                            saveError = null
                            // Reload vehicles to get updated fuel levels
                            vehicleViewModel.loadVehicles()
                            navController.popBackStack()
                        },
                        onError = { errorMsg ->
                            saveError = errorMsg
                        }
                    )
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                isLoading = isLoading,
                errorMessage = saveError ?: error
            )
        }

        composable(
            route = Screen.AddService.route,
            arguments = listOf(
                navArgument("vehicleId") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
                navArgument("serviceId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: 0L
            val serviceIdString = backStackEntry.arguments?.getString("serviceId")
            val serviceId = serviceIdString?.toLongOrNull()
            val context = LocalContext.current

            // Load existing service if serviceId is provided
            val existingService = serviceId?.let { id ->
                allServices.find { it.id == id }
            }

            AddServiceScreen(
                vehicles = vehicles,
                selectedVehicleId = if (vehicleId > 0L) vehicleId else null,
                existingService = existingService,
                onSave = { vehId, date, odometer, serviceCenter, serviceItems, totalCost, notes, receiptUri ->
                    if (existingService != null) {
                        // Update existing service
                        serviceViewModel.updateService(
                            serviceId = existingService.id,
                            vehicleId = vehId,
                            date = date,
                            odometerReading = odometer,
                            serviceCenter = serviceCenter,
                            serviceItems = serviceItems,
                            totalCost = totalCost,
                            notes = notes,
                            receiptUri = receiptUri,
                            onSuccess = {
                                android.widget.Toast.makeText(context, "Service record updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { error ->
                                android.widget.Toast.makeText(context, "Error updating service: $error", android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        // Create new service
                        serviceViewModel.addService(
                            vehicleId = vehId,
                            date = date,
                            odometerReading = odometer,
                            serviceCenter = serviceCenter,
                            serviceItems = serviceItems,
                            totalCost = totalCost,
                            notes = notes,
                            receiptUri = receiptUri,
                            onSuccess = {
                                android.widget.Toast.makeText(context, "Service record saved successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { error ->
                                android.widget.Toast.makeText(context, "Error saving service: $error", android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.AddExpense.route,
            arguments = listOf(
                navArgument("vehicleId") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
                navArgument("expenseId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: 0L
            val expenseIdString = backStackEntry.arguments?.getString("expenseId")
            val expenseId = expenseIdString?.toLongOrNull()
            val context = LocalContext.current

            // Load existing expense if expenseId is provided
            val existingExpense = expenseId?.let { id ->
                allExpenses.find { it.id == id }
            }

            AddExpenseScreen(
                vehicles = vehicles,
                selectedVehicleId = if (vehicleId > 0L) vehicleId else null,
                existingExpense = existingExpense,
                onSave = { vehId, date, odometer, vendor, expenseTasks, totalCost, notes, receiptUri ->
                    if (existingExpense != null) {
                        // Update existing expense
                        expenseViewModel.updateExpense(
                            expenseId = existingExpense.id,
                            vehicleId = vehId,
                            date = date,
                            odometerReading = odometer,
                            vendor = vendor,
                            expenseTasks = expenseTasks,
                            totalCost = totalCost,
                            notes = notes,
                            receiptUri = receiptUri,
                            onSuccess = {
                                android.widget.Toast.makeText(context, "Expense record updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { error ->
                                android.widget.Toast.makeText(context, "Error updating expense: $error", android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        // Create new expense
                        expenseViewModel.addExpense(
                            vehicleId = vehId,
                            date = date,
                            odometerReading = odometer,
                            vendor = vendor,
                            expenseTasks = expenseTasks,
                            totalCost = totalCost,
                            notes = notes,
                            receiptUri = receiptUri,
                            onSuccess = {
                                android.widget.Toast.makeText(context, "Expense record saved successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { error ->
                                android.widget.Toast.makeText(context, "Error saving expense: $error", android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Vehicles.route) {
            VehiclesScreen(
                vehicles = vehicles,
                refills = allRefills,
                trips = allTrips,
                preferences = preferences,
                onAddVehicle = {
                    navController.navigate(Screen.AddVehicle.createRoute())
                },
                onEditVehicle = { vehicle ->
                    navController.navigate(Screen.AddVehicle.createRoute(vehicle.id))
                },
                onDeleteVehicle = { vehicle ->
                    scope.launch {
                        vehicleViewModel.deleteVehicle(vehicle)
                    }
                },
                onViewSpecifications = { vehicle ->
                    selectedVehicleForSpecs = vehicle
                    specificationViewModel.loadSpecificationsForVehicle(vehicle.id)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.AddVehicle.route,
            arguments = listOf(
                navArgument("vehicleId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            val vehicleId = it.arguments?.getLong("vehicleId") ?: -1L
            val vehicleToEdit = vehicles.find { vehicle -> vehicle.id == vehicleId }
            val isLoading by vehicleViewModel.isLoading.collectAsState()
            val error by vehicleViewModel.error.collectAsState()

            AddVehicleScreen(
                vehicleToEdit = vehicleToEdit,
                onSave = { name, make, model, year, licensePlate, vin, insuranceNumber,
                          fuelType, tankCapacity, photoUri, doc1Uri, doc2Uri, doc3Uri, notes ->
                    if (vehicleToEdit != null) {
                        val updatedVehicle = vehicleToEdit.copy(
                            name = name,
                            make = make,
                            model = model,
                            year = year,
                            licensePlate = licensePlate,
                            vin = vin,
                            insuranceNumber = insuranceNumber,
                            fuelType = fuelType,
                            tankCapacity = tankCapacity,
                            photoUri = photoUri?.toString() ?: vehicleToEdit.photoUri,
                            document1Uri = doc1Uri?.toString() ?: vehicleToEdit.document1Uri,
                            document2Uri = doc2Uri?.toString() ?: vehicleToEdit.document2Uri,
                            document3Uri = doc3Uri?.toString() ?: vehicleToEdit.document3Uri,
                            notes = notes
                        )
                        vehicleViewModel.updateVehicle(updatedVehicle) {
                            navController.popBackStack()
                        }
                    } else {
                        val vehicle = Vehicle(
                            name = name,
                            make = make,
                            model = model,
                            year = year,
                            licensePlate = licensePlate,
                            vin = vin,
                            insuranceNumber = insuranceNumber,
                            fuelType = fuelType,
                            tankCapacity = tankCapacity,
                            currentFuelLevel = 0.0,
                            photoUri = photoUri?.toString(),
                            document1Uri = doc1Uri?.toString(),
                            document2Uri = doc2Uri?.toString(),
                            document3Uri = doc3Uri?.toString(),
                            notes = notes
                        )
                        vehicleViewModel.insertVehicle(vehicle) {
                            // Navigate back only after successful save
                            navController.popBackStack()
                        }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                isLoading = isLoading,
                apiError = error
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                preferences = preferences,
                onUpdateCurrency = { currencyInfo ->
                    scope.launch {
                        preferencesManager.updateCurrency(
                            symbol = currencyInfo.symbol,
                            code = currencyInfo.code,
                            decimalPlaces = currencyInfo.decimalPlaces,
                            format = currencyInfo.format
                        )
                    }
                },
                onLogout = {
                    authManager.clearAuth()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.History.route) {
            RefillHistoryScreen(
                refills = allRefills,
                allServices = allServices,
                allExpenses = allExpenses,
                vehicles = vehicles,
                preferences = preferences,
                onEditRefill = { refill ->
                    navController.navigate(Screen.AddRefill.createRoute(refill.vehicleId, refill.id))
                },
                onDeleteRefill = { refill ->
                    scope.launch {
                        refillViewModel.deleteRefill(refill)
                    }
                },
                onEditService = { service ->
                    navController.navigate(Screen.AddService.createRoute(service.vehicleId, service.id))
                },
                onDeleteService = { service ->
                    serviceViewModel.deleteService(service.id)
                },
                onEditExpense = { expense ->
                    navController.navigate(Screen.AddExpense.createRoute(expense.vehicleId, expense.id))
                },
                onDeleteExpense = { expense ->
                    expenseViewModel.deleteExpense(expense.id)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AutoTripLogging.route) {
            AutoTripLoggingScreen(
                vehicles = vehicles,
                vehicleAutoTripConfigs = preferences.vehicleAutoTripConfigs,
                onUpdateVehicleConfig = { config ->
                    scope.launch {
                        preferencesManager.upsertVehicleAutoTripConfig(config, preferences.vehicleAutoTripConfigs)
                    }
                },
                onRemoveVehicleConfig = { vehicleId ->
                    scope.launch {
                        preferencesManager.removeVehicleAutoTripConfig(vehicleId, preferences.vehicleAutoTripConfigs)
                    }
                },
                onStartService = {
                    val context = navController.context
                    com.simi.refillme.service.BluetoothMonitoringService.start(context)
                },
                onStopService = {
                    // Only stop if NO configs remain enabled
                    val anyEnabled = preferences.vehicleAutoTripConfigs.any { it.autoTripEnabled }
                    if (!anyEnabled) {
                        val context = navController.context
                        com.simi.refillme.service.BluetoothMonitoringService.stop(context)
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onShowAllLogs = {
                    navController.navigate(Screen.AllTrips.route)
                }
            )
        }

        composable(Screen.AllTrips.route) {
            AllTripsScreen(
                trips = allTrips,
                vehicles = vehicles,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Maps.route) {
            // Reload trips when entering Maps screen
            LaunchedEffect(Unit) {
                Log.d("AppNavigation", "Maps screen opened, reloading trips...")
                tripViewModel.loadTrips()
            }
            
            Log.d("AppNavigation", "Maps screen - allTrips size: ${allTrips.size}")
            allTrips.forEachIndexed { index, trip ->
                Log.d("AppNavigation", "Trip $index: startLat=${trip.startLatitude}, startLng=${trip.startLongitude}")
            }
            MapsScreen(
                trips = allTrips,
                activeTrip = activeTrip,
                refills = allRefills,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ── Personal Expenses ─────────────────────────────────────────────────
        composable(Screen.PersonalExpenses.route) {
            PersonalExpensesScreen(
                expenses   = personalExpenses,
                allItems   = personalAllItems,
                categories = personalCategories,
                allRefills = allRefills,
                allServices = allServices,
                allVehicleExpenses = allExpenses,
                preferences = preferences,
                onAddExpense = {
                    navController.navigate(Screen.AddPersonalExpense.createRoute())
                },
                onEditExpense = { id ->
                    navController.navigate(Screen.AddPersonalExpense.createRoute(id))
                },
                onDeleteExpense = { expense ->
                    personalExpenseViewModel.deleteExpense(expense)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AddPersonalExpense.route,
            arguments = listOf(
                navArgument("expenseId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId")?.toLongOrNull()
            val existingExpense = expenseId?.let { id -> personalExpenses.find { it.id == id } }
            val existingItems   = expenseId?.let { id -> personalAllItems.filter { it.expenseId == id } } ?: emptyList()

            AddPersonalExpenseScreen(
                categories     = personalCategories,
                existingExpense = existingExpense,
                existingItems   = existingItems,
                onSave = { expense, items ->
                    personalExpenseViewModel.saveExpense(expense, items) {
                        navController.popBackStack()
                    }
                },
                onAddCategory = { name, colorHex ->
                    personalExpenseViewModel.addCategory(name, colorHex)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }

    // Specifications Dialog
    selectedVehicleForSpecs?.let { vehicle ->
        SpecificationsDialog(
            vehicleName = vehicle.name,
            specifications = specifications[vehicle.id] ?: emptyList(),
            onDismiss = { selectedVehicleForSpecs = null },
            onAddSpecification = { name, value ->
                specificationViewModel.addSpecification(vehicle.id, name, value)
            },
            onDeleteSpecification = { specification ->
                specificationViewModel.deleteSpecification(specification)
            },
            onRefresh = {
                specificationViewModel.loadSpecificationsForVehicle(vehicle.id)
            }
        )
    }
}
