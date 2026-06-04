package com.simi.refillme.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.simi.refillme.data.entity.Refill
import com.simi.refillme.data.entity.Vehicle
import com.simi.refillme.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    vehicles: List<Vehicle>,
    recentRefills: List<Refill>,
    allServices: List<com.simi.refillme.data.entity.Service> = emptyList(),
    allExpenses: List<com.simi.refillme.data.entity.Expense> = emptyList(),
    allTrips: List<com.simi.refillme.data.entity.Trip> = emptyList(),
    preferences: com.simi.refillme.data.entity.UserPreferences,
    activeVehicleId: Long? = null,
    onVehicleSelected: (Long?) -> Unit = {},
    onNavigateToAddRefill: (vehicleId: Long) -> Unit,
    onNavigateToAddService: (vehicleId: Long) -> Unit = {},
    onNavigateToAddExpense: (vehicleId: Long) -> Unit = {},
    onEditRefill: (Refill) -> Unit,
    onDeleteRefill: (Refill) -> Unit,
    onNavigateToVehicles: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAutoTripLogging: () -> Unit = {},
    onNavigateToMaps: () -> Unit = {},
    onNavigateToPersonalExpenses: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Dashboard", "Stats & Charts")
    var showMenu by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }
    var showVehicleSelectorMenu by remember { mutableStateOf(false) }

    // Global vehicle selector – initialised from the persisted active vehicle preference
    var selectedVehicle by remember(vehicles, activeVehicleId) {
        mutableStateOf(
            vehicles.find { it.id == activeVehicleId } ?: vehicles.firstOrNull()
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Backdrop overlay when FAB menu is open (drawn first, behind everything)
        if (showFabMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showFabMenu = false }
            )
        }

        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Refill Me",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Vehicles") },
                            onClick = {
                                showMenu = false
                                onNavigateToVehicles()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Reminders") },
                            onClick = {
                                showMenu = false
                                // TODO: Navigate to reminders
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Notifications, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Auto Trip Logging") },
                            onClick = {
                                showMenu = false
                                onNavigateToAutoTripLogging()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Route, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Maps") },
                            onClick = {
                                showMenu = false
                                onNavigateToMaps()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Map, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Report") },
                            onClick = {
                                showMenu = false
                                // TODO: Navigate to report
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Assessment, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Personal Expenses") },
                            onClick = {
                                showMenu = false
                                onNavigateToPersonalExpenses()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.AccountBalance, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                showMenu = false
                                onNavigateToSettings()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Settings, contentDescription = null)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Menu items (shown when expanded)
                if (showFabMenu) {
                    FabMenuItem(
                        icon = Icons.Default.LocalGasStation,
                        label = "Add Fill-Up",
                        onClick = {
                            showFabMenu = false
                            onNavigateToAddRefill(0L)
                        }
                    )
                    FabMenuItem(
                        icon = Icons.Default.Build,
                        label = "Add Service",
                        onClick = {
                            showFabMenu = false
                            onNavigateToAddService(0L)
                        }
                    )
                    FabMenuItem(
                        icon = Icons.Default.AttachMoney,
                        label = "Add Expense",
                        onClick = {
                            showFabMenu = false
                            onNavigateToAddExpense(0L)
                        }
                    )
                    FabMenuItem(
                        icon = Icons.Default.DirectionsCar,
                        label = "Add Trip",
                        onClick = {
                            showFabMenu = false
                            onNavigateToAutoTripLogging()
                        }
                    )
                    FabMenuItem(
                        icon = Icons.Default.Speed,
                        label = "Add Odometer",
                        onClick = {
                            showFabMenu = false
                            // TODO: Navigate to Add Odometer
                        }
                    )
                }

                // Main FAB
                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = Secondary,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (showFabMenu) "Close Menu" else "Add"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Background)
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Surface,
                contentColor = Primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Vehicle Selector Row – shown only when there are multiple vehicles
            if (vehicles.size > 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showVehicleSelectorMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                        border = BorderStroke(1.dp, Primary)
                    ) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = selectedVehicle?.let { "${it.make} ${it.model}" } ?: "Select Vehicle",
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = showVehicleSelectorMenu,
                        onDismissRequest = { showVehicleSelectorMenu = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        vehicles.forEach { vehicle ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.DirectionsCar,
                                            contentDescription = null,
                                            tint = if (vehicle.id == selectedVehicle?.id) Primary else OnSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "${vehicle.make} ${vehicle.model}",
                                            color = if (vehicle.id == selectedVehicle?.id) Primary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (vehicle.id == selectedVehicle?.id) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                },
                                onClick = {
                                    selectedVehicle = vehicle
                                    onVehicleSelected(vehicle.id)
                                    showVehicleSelectorMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> DashboardTab(
                    vehicles = vehicles,
                    recentRefills = recentRefills,
                    allServices = allServices,
                    allExpenses = allExpenses,
                    preferences = preferences,
                    dateFormat = dateFormat,
                    selectedVehicle = selectedVehicle,
                    onNavigateToAddRefill = onNavigateToAddRefill,
                    onNavigateToVehicles = onNavigateToVehicles,
                    onNavigateToHistory = onNavigateToHistory,
                    onEditRefill = onEditRefill,
                    onDeleteRefill = onDeleteRefill
                )
                1 -> StatsAndChartsTab(
                    vehicles = vehicles,
                    recentRefills = recentRefills,
                    allServices = allServices,
                    allExpenses = allExpenses,
                    allTrips = allTrips,
                    preferences = preferences,
                    selectedVehicle = selectedVehicle
                )
            }
        }
    }
    }
}

@Composable
fun DashboardTab(
    vehicles: List<Vehicle>,
    recentRefills: List<Refill>,
    allServices: List<com.simi.refillme.data.entity.Service> = emptyList(),
    allExpenses: List<com.simi.refillme.data.entity.Expense> = emptyList(),
    preferences: com.simi.refillme.data.entity.UserPreferences,
    dateFormat: SimpleDateFormat,
    selectedVehicle: Vehicle? = null,
    onNavigateToAddRefill: (vehicleId: Long) -> Unit,
    onNavigateToVehicles: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onEditRefill: (Refill) -> Unit,
    onDeleteRefill: (Refill) -> Unit,
    modifier: Modifier = Modifier
) {
    // Filter data to the selected vehicle when one is chosen
    val vehicleRefills = remember(selectedVehicle, recentRefills) {
        if (selectedVehicle != null) recentRefills.filter { it.vehicleId == selectedVehicle.id }
        else recentRefills
    }
    val vehicleServices = remember(selectedVehicle, allServices) {
        if (selectedVehicle != null) allServices.filter { it.vehicleId == selectedVehicle.id }
        else allServices
    }
    val vehicleExpenses = remember(selectedVehicle, allExpenses) {
        if (selectedVehicle != null) allExpenses.filter { it.vehicleId == selectedVehicle.id }
        else allExpenses
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Statistics Circles
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Average km/l Circle
                val averageKmL = calculateAverageKmL(vehicleRefills)

                StatCircle(
                    value = if (averageKmL > 0) String.format("%.2f", averageKmL) else "--",
                    label = "Average\nkm/L",
                    gradient = Brush.linearGradient(
                        colors = listOf(Primary, PrimaryVariant)
                    )
                )

                // Fuel cost last 30 days
                val last30DaysCost = calculateLast30DaysCost(vehicleRefills)

                StatCircle(
                    value = String.format("%,d", last30DaysCost.toInt()),
                    label = "Fuel cost in\nlast 30 days",
                    gradient = Brush.linearGradient(
                        colors = listOf(Success, Color(0xFF388E3C))
                    )
                )

                // Monthly total cost (refills + services + expenses)
                val monthlyCost = calculateMonthlyCost(vehicleRefills, vehicleServices, vehicleExpenses)

                StatCircle(
                    value = String.format("%,d", monthlyCost.toInt()),
                    label = "Current\nMonth",
                    gradient = Brush.linearGradient(
                        colors = listOf(Secondary, Color(0xFFFF6F00))
                    )
                )
            }
        }

        // Recent Log Entries Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Log Entries",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToHistory) {
                    Text("Show All")
                }
            }
        }

        // Create combined list of all entries sorted by date
        val allEntries = buildList {
            addAll(vehicleRefills.map { Triple("refill", it.date, it) })
            addAll(vehicleServices.map { Triple("service", it.date, it) })
            addAll(vehicleExpenses.map { Triple("expense", it.date, it) })
        }.sortedByDescending { it.second }.take(5)

        if (allEntries.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.LocalGasStation,
                    message = "No entries recorded yet",
                    actionText = "Add Refill",
                    onAction = { onNavigateToAddRefill(0L) }
                )
            }
        } else {
            items(allEntries) { (type, _, entry) ->
                when (type) {
                    "refill" -> {
                        val refill = entry as Refill
                        val previousRefill = vehicleRefills
                            .filter { it.vehicleId == refill.vehicleId && it.date < refill.date }
                            .maxByOrNull { it.date }

                        RefillLogEntry(
                            refill = refill,
                            previousRefill = previousRefill,
                            vehicle = vehicles.find { it.id == refill.vehicleId },
                            preferences = preferences,
                            dateFormat = dateFormat,
                            onEdit = { onEditRefill(refill) },
                            onDelete = { onDeleteRefill(refill) }
                        )
                    }
                    "service" -> {
                        val service = entry as com.simi.refillme.data.entity.Service
                        ServiceLogEntry(
                            service = service,
                            vehicle = vehicles.find { it.id == service.vehicleId },
                            dateFormat = dateFormat
                        )
                    }
                    "expense" -> {
                        val expense = entry as com.simi.refillme.data.entity.Expense
                        ExpenseLogEntry(
                            expense = expense,
                            vehicle = vehicles.find { it.id == expense.vehicleId },
                            dateFormat = dateFormat
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsAndChartsTab(
    vehicles: List<Vehicle>,
    recentRefills: List<Refill>,
    allServices: List<com.simi.refillme.data.entity.Service> = emptyList(),
    allExpenses: List<com.simi.refillme.data.entity.Expense> = emptyList(),
    allTrips: List<com.simi.refillme.data.entity.Trip> = emptyList(),
    preferences: com.simi.refillme.data.entity.UserPreferences,
    selectedVehicle: Vehicle? = null,
    modifier: Modifier = Modifier
) {
    // Use the vehicle passed from parent; fall back to first vehicle only as a safety net
    val effectiveVehicle = selectedVehicle ?: vehicles.firstOrNull()
    var showDateRangeMenu by remember { mutableStateOf(false) }
    var selectedDateRange by remember { mutableStateOf("All Time") }
    var showCustomDatePicker by remember { mutableStateOf(false) }
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    val dateFormat = remember { SimpleDateFormat("d MMM yy", Locale.getDefault()) }

    // Filter refills based on selected vehicle and date range
    val filteredRefills = remember(effectiveVehicle, recentRefills, selectedDateRange, customStartDate, customEndDate) {
        val vehicleRefills = recentRefills.filter { it.vehicleId == effectiveVehicle?.id }

        when (selectedDateRange) {
            "All Time" -> vehicleRefills
            "This Month" -> {
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)
                vehicleRefills.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
                }
            }
            "Last Month" -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MONTH, -1)
                val lastMonth = calendar.get(Calendar.MONTH)
                val lastMonthYear = calendar.get(Calendar.YEAR)
                vehicleRefills.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.MONTH) == lastMonth && calendar.get(Calendar.YEAR) == lastMonthYear
                }
            }
            "This Year" -> {
                val calendar = Calendar.getInstance()
                val currentYear = calendar.get(Calendar.YEAR)
                vehicleRefills.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.YEAR) == currentYear
                }
            }
            "Custom Dates" -> {
                if (customStartDate != null && customEndDate != null) {
                    vehicleRefills.filter { it.date >= customStartDate!! && it.date <= customEndDate!! }
                } else {
                    vehicleRefills
                }
            }
            else -> vehicleRefills
        }
    }

    // Filter services based on same criteria
    val filteredServices = remember(effectiveVehicle, allServices, selectedDateRange, customStartDate, customEndDate) {
        val vehicleServices = allServices.filter { it.vehicleId == effectiveVehicle?.id }

        when (selectedDateRange) {
            "All Time" -> vehicleServices
            "This Month" -> {
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)
                vehicleServices.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
                }
            }
            "Last Month" -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MONTH, -1)
                val lastMonth = calendar.get(Calendar.MONTH)
                val lastMonthYear = calendar.get(Calendar.YEAR)
                vehicleServices.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.MONTH) == lastMonth && calendar.get(Calendar.YEAR) == lastMonthYear
                }
            }
            "This Year" -> {
                val calendar = Calendar.getInstance()
                val currentYear = calendar.get(Calendar.YEAR)
                vehicleServices.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.YEAR) == currentYear
                }
            }
            "Custom Dates" -> {
                if (customStartDate != null && customEndDate != null) {
                    vehicleServices.filter { it.date >= customStartDate!! && it.date <= customEndDate!! }
                } else {
                    vehicleServices
                }
            }
            else -> vehicleServices
        }
    }

    // Filter expenses based on same criteria
    val filteredExpenses = remember(effectiveVehicle, allExpenses, selectedDateRange, customStartDate, customEndDate) {
        val vehicleExpenses = allExpenses.filter { it.vehicleId == effectiveVehicle?.id }

        when (selectedDateRange) {
            "All Time" -> vehicleExpenses
            "This Month" -> {
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)
                vehicleExpenses.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
                }
            }
            "Last Month" -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MONTH, -1)
                val lastMonth = calendar.get(Calendar.MONTH)
                val lastMonthYear = calendar.get(Calendar.YEAR)
                vehicleExpenses.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.MONTH) == lastMonth && calendar.get(Calendar.YEAR) == lastMonthYear
                }
            }
            "This Year" -> {
                val calendar = Calendar.getInstance()
                val currentYear = calendar.get(Calendar.YEAR)
                vehicleExpenses.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.YEAR) == currentYear
                }
            }
            "Custom Dates" -> {
                if (customStartDate != null && customEndDate != null) {
                    vehicleExpenses.filter { it.date >= customStartDate!! && it.date <= customEndDate!! }
                } else {
                    vehicleExpenses
                }
            }
            else -> vehicleExpenses
        }
    }

    // Filter trips based on same criteria
    val filteredTrips = remember(effectiveVehicle, allTrips, selectedDateRange, customStartDate, customEndDate) {
        android.util.Log.d("DashboardScreen", "Filtering trips - allTrips size: ${allTrips.size}, effectiveVehicle: ${effectiveVehicle?.id}")
        val vehicleTrips = allTrips.filter { it.vehicleId == effectiveVehicle?.id }
        android.util.Log.d("DashboardScreen", "Vehicle trips after filter: ${vehicleTrips.size}")

        when (selectedDateRange) {
            "All Time" -> vehicleTrips
            "This Month" -> {
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)
                vehicleTrips.filter {
                    calendar.timeInMillis = it.startTime
                    calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
                }
            }
            "Last Month" -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MONTH, -1)
                val lastMonth = calendar.get(Calendar.MONTH)
                val lastMonthYear = calendar.get(Calendar.YEAR)
                vehicleTrips.filter {
                    calendar.timeInMillis = it.startTime
                    calendar.get(Calendar.MONTH) == lastMonth && calendar.get(Calendar.YEAR) == lastMonthYear
                }
            }
            "This Year" -> {
                val calendar = Calendar.getInstance()
                val currentYear = calendar.get(Calendar.YEAR)
                vehicleTrips.filter {
                    calendar.timeInMillis = it.startTime
                    calendar.get(Calendar.YEAR) == currentYear
                }
            }
            "Custom Dates" -> {
                if (customStartDate != null && customEndDate != null) {
                    vehicleTrips.filter { it.startTime >= customStartDate!! && it.startTime <= customEndDate!! }
                } else {
                    vehicleTrips
                }
            }
            else -> vehicleTrips
        }
    }

    // Calculate date range display
    val dateRangeText = remember(selectedDateRange, customStartDate, customEndDate) {
        val calendar = Calendar.getInstance()

        when (selectedDateRange) {
            "This Month" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val startDate = dateFormat.format(calendar.time)
                calendar.setTimeInMillis(System.currentTimeMillis())
                val endDate = dateFormat.format(calendar.time)
                "$startDate - $endDate"
            }
            "Last Month" -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val startDate = dateFormat.format(calendar.time)
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                val endDate = dateFormat.format(calendar.time)
                "$startDate - $endDate"
            }
            "This Year" -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                val startDate = dateFormat.format(calendar.time)
                calendar.setTimeInMillis(System.currentTimeMillis())
                val endDate = dateFormat.format(calendar.time)
                "$startDate - $endDate"
            }
            "Custom Dates" -> {
                val start = customStartDate
                val end = customEndDate
                if (start != null && end != null) {
                    val startDate = dateFormat.format(Date(start))
                    val endDate = dateFormat.format(Date(end))
                    "$startDate - $endDate"
                } else {
                    "Select dates"
                }
            }
            else -> { // All Time
                if (recentRefills.isEmpty()) {
                    "No data"
                } else {
                    val sortedDates = recentRefills.map { it.date }.sorted()
                    val startDate = dateFormat.format(Date(sortedDates.first()))
                    val endDate = dateFormat.format(Date(sortedDates.last()))
                    "$startDate - $endDate"
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Vehicle Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Vehicle avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Make and Model
                Text(
                    text = effectiveVehicle?.let { "${it.make} ${it.model}" } ?: "No Vehicle",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Date Range Selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateRangeText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )

                Box {
                    OutlinedButton(
                        onClick = { showDateRangeMenu = true }
                    ) {
                        Text(selectedDateRange)
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showDateRangeMenu,
                        onDismissRequest = { showDateRangeMenu = false }
                    ) {
                        listOf("All Time", "This Month", "Last Month", "This Year", "Custom Dates").forEach { range ->
                            DropdownMenuItem(
                                text = { Text(range) },
                                onClick = {
                                    selectedDateRange = range
                                    showDateRangeMenu = false
                                    if (range == "Custom Dates") {
                                        showCustomDatePicker = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Stats sections
        if (filteredRefills.isNotEmpty()) {
            // Total Stats
            item {
                StatsSection(
                    title = "Total",
                    icon = Icons.Default.BarChart,
                    stats = calculateTotalStats(filteredRefills, filteredServices, filteredExpenses, filteredTrips, effectiveVehicle, preferences)
                )
            }

            // Average Fuel Stats
            item {
                StatsSection(
                    title = "Average Fuel Stats",
                    icon = Icons.Default.LocalGasStation,
                    stats = calculateAverageFuelStats(filteredRefills, preferences)
                )
            }

            // Average Services Stats
            item {
                StatsSection(
                    title = "Average Services Stats",
                    icon = Icons.Default.Build,
                    stats = calculateAverageServicesStats(filteredServices)
                )
            }

            // Average Other Expense Stats
            item {
                StatsSection(
                    title = "Average Other Expense Stats",
                    icon = Icons.Default.AttachMoney,
                    stats = calculateAverageExpenseStats(filteredExpenses)
                )
            }

            // Trip Stats
            item {
                StatsSection(
                    title = "Trip Stats",
                    icon = Icons.Default.DirectionsCar,
                    stats = calculateTripStats(filteredTrips)
                )
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "No data available for selected date range",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Custom Date Picker Dialog
    if (showCustomDatePicker) {
        var selectingStartDate by remember { mutableStateOf(true) }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (selectingStartDate) customStartDate ?: System.currentTimeMillis()
                                        else customEndDate ?: System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = {
                showCustomDatePicker = false
                selectingStartDate = true
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectingStartDate) {
                            customStartDate = datePickerState.selectedDateMillis
                            selectingStartDate = false
                        } else {
                            customEndDate = datePickerState.selectedDateMillis
                            showCustomDatePicker = false
                            selectingStartDate = true
                        }
                    }
                ) {
                    Text(if (selectingStartDate) "Next" else "OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCustomDatePicker = false
                        selectingStartDate = true
                    }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            Column {
                Text(
                    text = if (selectingStartDate) "Select Start Date" else "Select End Date",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                DatePicker(state = datePickerState)
            }
        }
    }
}


@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
fun VehicleCard(
    vehicle: Vehicle, 
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vehicle.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = vehicle.model,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = (vehicle.currentFuelLevel / vehicle.tankCapacity).toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        vehicle.currentFuelLevel / vehicle.tankCapacity > 0.5 -> Success
                        vehicle.currentFuelLevel / vehicle.tankCapacity > 0.25 -> Warning
                        else -> Error
                    },
                    trackColor = Color.LightGray.copy(alpha = 0.3f)
                )
                Text(
                    text = "${String.format("%.1f", vehicle.currentFuelLevel)}L / ${vehicle.tankCapacity}L",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RefillCard(
    refill: Refill,
    vehicle: Vehicle?,
    dateFormat: SimpleDateFormat,
    preferences: com.simi.refillme.data.entity.UserPreferences,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { showMenu = true }
                    )
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = vehicle?.name ?: "Unknown Vehicle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormat.format(Date(refill.date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
                Text(
                    text = com.simi.refillme.ui.screen.formatPrice(refill.totalPrice, preferences),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip(
                    icon = Icons.Default.LocalGasStation,
                    label = "${String.format("%.2f", refill.refillAmount)}L"
                )
                InfoChip(
                    icon = Icons.Default.AttachMoney,
                    label = "${com.simi.refillme.ui.screen.formatPrice(refill.unitPrice, preferences)}/L"
                )
            }
        }
        
        // Dropdown menu for edit/delete
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    showMenu = false
                    onEdit()
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit"
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    showMenu = false
                    showDeleteDialog = true
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Error
                    )
                }
            )
        }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Delete Refill?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("Are you sure you want to delete this refill record?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${String.format("%.2f", refill.refillAmount)}L • ${formatPrice(refill.totalPrice, preferences)} • ${dateFormat.format(Date(refill.date))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This action cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = SurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = OnSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = OnSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(actionText)
            }
        }
    }
}

// New Components for Enhanced Dashboard

@Composable
fun StatCircle(
    value: String,
    label: String,
    gradient: Brush,
    size: Dp = 90.dp,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        // Circle with just the number
        Box(
            modifier = Modifier
                .size(size)
                .background(gradient, RoundedCornerShape(size / 2))
                .shadow(4.dp, RoundedCornerShape(size / 2)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Label below the circle (can be multi-line)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RefillLogEntry(
    refill: Refill,
    previousRefill: Refill?,
    vehicle: Vehicle?,
    preferences: com.simi.refillme.data.entity.UserPreferences,
    dateFormat: SimpleDateFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as com.simi.refillme.RefillMeApplication
    val authManager = app.authManager
    val authApiService = com.simi.refillme.data.auth.AuthApiService.getInstance()
    val scope = rememberCoroutineScope()

    var showMenu by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }

    // Calculate distance and consumption
    val distanceTraveled = if (previousRefill != null && refill.odometerReading != null && previousRefill.odometerReading != null) {
        refill.odometerReading - previousRefill.odometerReading
    } else {
        0.0
    }

    // Calculate fuel consumed: previousRefill.fuelAfter - currentRefill.fuelBefore
    val fuelConsumed = if (previousRefill != null) {
        previousRefill.fuelAfter - refill.fuelBefore
    } else {
        0.0
    }

    val kmPerLiter = if (distanceTraveled > 0 && fuelConsumed > 0) {
        distanceTraveled / fuelConsumed
    } else {
        0.0
    }

    // Debug logging
    android.util.Log.d("RefillLogEntry", "Refill date: ${refill.date}, " +
            "previousRefill: ${previousRefill != null}, " +
            "distanceTraveled: $distanceTraveled, " +
            "fuelConsumed: $fuelConsumed, " +
            "kmPerLiter: $kmPerLiter, " +
            "refill.fuelBefore: ${refill.fuelBefore}, " +
            "previousRefill.fuelAfter: ${previousRefill?.fuelAfter}")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { showMenu = true }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Column 1: Pump icon, date, odometer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.LocalGasStation,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(refill.date)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (refill.odometerReading != null) {
                        Text(
                            text = "${String.format("%.0f", refill.odometerReading)} km",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }

                // Column 2: Refill amount, distance traveled
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${String.format("%.2f", refill.refillAmount)}L",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Success
                    )
                    if (distanceTraveled > 0) {
                        Text(
                            text = "(+${String.format("%.0f", distanceTraveled)} km)",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }

                // Column 3: Total price, km/L
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = formatPrice(refill.totalPrice, preferences),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    if (kmPerLiter > 0) {
                        Text(
                            text = "${String.format("%.2f", kmPerLiter)} km/L",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            // Dropdown menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        showMenu = false
                        onEdit()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        showMenu = false
                        showPasswordDialog = true
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Error)
                    }
                )
            }
        }
    }

    // Password verification dialog
    if (showPasswordDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                password = ""
                passwordError = false
                isVerifying = false
            },
            title = { Text("Verify Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Please enter your password to confirm deletion")
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = false
                        },
                        label = { Text("Password") },
                        isError = passwordError,
                        enabled = !isVerifying,
                        singleLine = true
                    )
                    if (passwordError) {
                        Text(
                            "Incorrect password",
                            color = Error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (password.isNotBlank()) {
                            isVerifying = true
                            scope.launch {
                                try {
                                    val token = authManager.getToken()
                                    if (token != null) {
                                        val result = authApiService.verifyPassword(token, password)
                                        result.onSuccess { response ->
                                            if (response.success && response.valid) {
                                                onDelete()
                                                showPasswordDialog = false
                                                password = ""
                                                passwordError = false
                                            } else {
                                                passwordError = true
                                            }
                                        }.onFailure {
                                            passwordError = true
                                        }
                                    } else {
                                        passwordError = true
                                    }
                                } catch (e: Exception) {
                                    passwordError = true
                                } finally {
                                    isVerifying = false
                                }
                            }
                        }
                    },
                    enabled = !isVerifying && password.isNotBlank()
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Confirm Delete", color = Error)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasswordDialog = false
                        password = ""
                        passwordError = false
                        isVerifying = false
                    },
                    enabled = !isVerifying
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    stats: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats List
            stats.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// Helper functions for calculations

fun calculateAverageKmL(refills: List<Refill>): Double {
    if (refills.size < 2) return 0.0
    
    // Sort refills by date
    val sortedRefills = refills.sortedBy { it.date }
    var totalKm = 0.0
    var totalFuel = 0.0
    
    for (i in 1 until sortedRefills.size) {
        val current = sortedRefills[i]
        val previous = sortedRefills[i - 1]
        
        // Both must have odometer readings
        if (current.odometerReading != null && previous.odometerReading != null) {
            val distance = current.odometerReading - previous.odometerReading
            if (distance > 0) {
                totalKm += distance
                totalFuel += current.refillAmount
            }
        }
    }
    
    return if (totalFuel > 0) totalKm / totalFuel else 0.0
}

fun calculateLast30DaysCost(refills: List<Refill>): Double {
    val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
    return refills
        .filter { it.date >= thirtyDaysAgo }
        .sumOf { it.totalPrice }
}

fun calculateMonthlyCost(
    refills: List<Refill>,
    services: List<com.simi.refillme.data.entity.Service> = emptyList(),
    expenses: List<com.simi.refillme.data.entity.Expense> = emptyList()
): Double {
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)

    val refillCost = refills.filter {
        calendar.timeInMillis = it.date
        calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
    }.sumOf { it.totalPrice }

    val serviceCost = services.filter {
        calendar.timeInMillis = it.date
        calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
    }.sumOf { it.totalCost }

    val expenseCost = expenses.filter {
        calendar.timeInMillis = it.date
        calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
    }.sumOf { it.totalCost }

    return refillCost + serviceCost + expenseCost
}

fun calculateTotalStats(
    refills: List<Refill>,
    services: List<com.simi.refillme.data.entity.Service> = emptyList(),
    expenses: List<com.simi.refillme.data.entity.Expense> = emptyList(),
    trips: List<com.simi.refillme.data.entity.Trip> = emptyList(),
    vehicle: Vehicle?,
    preferences: com.simi.refillme.data.entity.UserPreferences
): List<Pair<String, String>> {
    val sortedRefills = refills.sortedBy { it.date }

    // Total Distance
    val totalDistance = if (sortedRefills.size >= 2) {
        val firstOdom = sortedRefills.first().odometerReading ?: 0.0
        val lastOdom = sortedRefills.last().odometerReading ?: 0.0
        lastOdom - firstOdom
    } else {
        0.0
    }

    // Fill-ups count
    val fillUpsCount = refills.size

    // Fuel Qty
    val totalFuelQty = refills.sumOf { it.refillAmount }

    // Fuel Cost
    val totalFuelCost = refills.sumOf { it.totalPrice }

    // Services
    val servicesCount = services.size
    val serviceCost = services.sumOf { it.totalCost }

    // Other Expenses
    val otherExpenses = expenses.sumOf { it.totalCost }

    // Total Cost
    val totalCost = totalFuelCost + serviceCost + otherExpenses

    // Total Cost/km
    val costPerKm = if (totalDistance > 0) totalCost / totalDistance else 0.0

    // Service Cost/km
    val serviceCostPerKm = if (totalDistance > 0) serviceCost / totalDistance else 0.0

    // Other Expenses/km
    val expenseCostPerKm = if (totalDistance > 0) otherExpenses / totalDistance else 0.0

    return listOf(
        "Distance" to "${String.format("%,.0f", totalDistance)} km",
        "Fill-ups" to "$fillUpsCount",
        "Fuel Qty" to "${String.format("%.2f", totalFuelQty)} Ltr",
        "Fuel Cost" to "${String.format("%,d", totalFuelCost.toInt())} UGX",
        "Services" to "$servicesCount",
        "Service Cost" to "${String.format("%,d", serviceCost.toInt())} UGX",
        "Service Cost/km" to "${String.format("%.2f", serviceCostPerKm)} UGX",
        "Other Expenses" to "${String.format("%,d", otherExpenses.toInt())} UGX",
        "Other Expenses/km" to "${String.format("%.2f", expenseCostPerKm)} UGX",
        "Total Cost" to "${String.format("%,d", totalCost.toInt())} UGX",
        "Total Cost/km" to "${String.format("%.2f", costPerKm)} UGX"
    )
}

fun calculateAverageFuelStats(
    refills: List<Refill>,
    preferences: com.simi.refillme.data.entity.UserPreferences
): List<Pair<String, String>> {
    if (refills.isEmpty()) return emptyList()

    val sortedRefills = refills.sortedBy { it.date }

    // Average Fuel Efficiency (km/L)
    val avgFuelEff = calculateAverageKmL(refills)

    // Distance Between Fill-Ups
    val distances = mutableListOf<Double>()
    for (i in 1 until sortedRefills.size) {
        val prev = sortedRefills[i - 1].odometerReading ?: 0.0
        val curr = sortedRefills[i].odometerReading ?: 0.0
        if (curr > prev) {
            distances.add(curr - prev)
        }
    }
    val avgDistBetweenFillUps = if (distances.isNotEmpty()) distances.average() else 0.0

    // Qty Per Fill-Up
    val avgQtyPerFillUp = refills.map { it.refillAmount }.average()

    // Cost Per Fill-Up
    val avgCostPerFillUp = refills.map { it.totalPrice }.average()

    // Avg Price/Ltr
    val avgPricePerLtr = refills.map { it.unitPrice }.average()

    // Fill-Ups per Month
    val fillUpsPerMonth = if (sortedRefills.size >= 2) {
        val daysBetweenFirstAndLast = (sortedRefills.last().date - sortedRefills.first().date) / (1000.0 * 60 * 60 * 24)
        if (daysBetweenFirstAndLast > 0) {
            (refills.size.toDouble() / daysBetweenFirstAndLast) * 30.0
        } else {
            refills.size.toDouble() // If all refills are on the same day, just show the count
        }
    } else {
        refills.size.toDouble() // If only 1 refill, show 1
    }

    // Fuel Cost/km
    val totalDistance = if (sortedRefills.size >= 2) {
        val firstOdom = sortedRefills.first().odometerReading ?: 0.0
        val lastOdom = sortedRefills.last().odometerReading ?: 0.0
        lastOdom - firstOdom
    } else {
        0.0
    }
    val fuelCostPerKm = if (totalDistance > 0) refills.sumOf { it.totalPrice } / totalDistance else 0.0

    // Fuel Cost/Day
    val daysBetweenFirstAndLast = if (sortedRefills.size >= 2) {
        (sortedRefills.last().date - sortedRefills.first().date) / (1000.0 * 60 * 60 * 24)
    } else {
        1.0
    }
    val fuelCostPerDay = if (daysBetweenFirstAndLast > 0) {
        refills.sumOf { it.totalPrice } / daysBetweenFirstAndLast
    } else {
        0.0
    }

    // Fuel Cost/Month
    val fuelCostPerMonth = fuelCostPerDay * 30

    return listOf(
        "Avg Fuel Eff" to String.format("%.2f km/Ltr", avgFuelEff),
        "Dist Btwn Fill-Ups" to String.format("%.0f km", avgDistBetweenFillUps),
        "Qty Per Fill-Up" to String.format("%.2f Ltr", avgQtyPerFillUp),
        "Cost Per Fill-Up" to "${String.format("%,d", avgCostPerFillUp.toInt())} UGX",
        "Avg Price/Ltr" to "${String.format("%.2f", avgPricePerLtr)} UGX",
        "Fill-Ups per Mth" to String.format("%.1f", fillUpsPerMonth),
        "Fuel Cost/km" to "${String.format("%.2f", fuelCostPerKm)} UGX",
        "Fuel Cost/Day" to "${String.format("%,d", fuelCostPerDay.toInt())} UGX",
        "Fuel Cost/Mth" to "${String.format("%,d", fuelCostPerMonth.toInt())} UGX"
    )
}

fun calculateAverageServicesStats(services: List<com.simi.refillme.data.entity.Service>): List<Pair<String, String>> {
    if (services.isEmpty()) {
        return listOf(
            "Services" to "0",
            "Total Cost" to "0 UGX",
            "Avg Service Cost" to "0 UGX",
            "Highest Cost" to "0 UGX",
            "Lowest Cost" to "0 UGX"
        )
    }

    val totalCost = services.sumOf { it.totalCost }
    val avgCost = totalCost / services.size
    val highestCost = services.maxOfOrNull { it.totalCost } ?: 0.0
    val lowestCost = services.minOfOrNull { it.totalCost } ?: 0.0

    return listOf(
        "Services" to "${services.size}",
        "Total Cost" to "${String.format("%,d", totalCost.toInt())} UGX",
        "Avg Service Cost" to "${String.format("%,d", avgCost.toInt())} UGX",
        "Highest Cost" to "${String.format("%,d", highestCost.toInt())} UGX",
        "Lowest Cost" to "${String.format("%,d", lowestCost.toInt())} UGX"
    )
}

fun calculateAverageExpenseStats(expenses: List<com.simi.refillme.data.entity.Expense>): List<Pair<String, String>> {
    if (expenses.isEmpty()) {
        return listOf(
            "Expenses" to "0",
            "Total Cost" to "0 UGX",
            "Avg Expense" to "0 UGX",
            "Highest Cost" to "0 UGX",
            "Lowest Cost" to "0 UGX"
        )
    }

    val totalCost = expenses.sumOf { it.totalCost }
    val avgCost = totalCost / expenses.size
    val highestCost = expenses.maxOfOrNull { it.totalCost } ?: 0.0
    val lowestCost = expenses.minOfOrNull { it.totalCost } ?: 0.0

    return listOf(
        "Expenses" to "${expenses.size}",
        "Total Cost" to "${String.format("%,d", totalCost.toInt())} UGX",
        "Avg Expense" to "${String.format("%,d", avgCost.toInt())} UGX",
        "Highest Cost" to "${String.format("%,d", highestCost.toInt())} UGX",
        "Lowest Cost" to "${String.format("%,d", lowestCost.toInt())} UGX"
    )
}

fun calculateTripStats(trips: List<com.simi.refillme.data.entity.Trip>): List<Pair<String, String>> {
    android.util.Log.d("DashboardScreen", "calculateTripStats called with ${trips.size} trips")
    val completedTrips = trips.filter { !it.isActive }
    android.util.Log.d("DashboardScreen", "Completed trips: ${completedTrips.size}")

    if (completedTrips.isEmpty()) {
        return listOf(
            "Total Trips" to "0",
            "Total Distance" to "0 km",
            "Avg Trip Distance" to "0 km",
            "Longest Trip" to "0 km",
            "Shortest Trip" to "0 km",
            "Total Duration" to "0 min",
            "Avg Duration" to "0 min",
            "Avg Speed" to "0 km/h",
            "Max Speed" to "0 km/h"
        )
    }

    val totalDistance = completedTrips.sumOf { it.distanceKm }
    val avgDistance = totalDistance / completedTrips.size
    val longestTrip = completedTrips.maxOfOrNull { it.distanceKm } ?: 0.0
    val shortestTrip = completedTrips.minOfOrNull { it.distanceKm } ?: 0.0

    val totalDuration = completedTrips.sumOf { it.durationMinutes }
    val avgDuration = totalDuration / completedTrips.size

    val avgSpeed = completedTrips
        .filter { it.averageSpeedKmh > 0 }
        .map { it.averageSpeedKmh }
        .average()
        .takeIf { !it.isNaN() } ?: 0.0

    val maxSpeed = completedTrips.maxOfOrNull { it.maxSpeedKmh } ?: 0.0

    return listOf(
        "Total Trips" to "${completedTrips.size}",
        "Total Distance" to "${String.format("%.1f", totalDistance)} km",
        "Avg Trip Distance" to "${String.format("%.1f", avgDistance)} km",
        "Longest Trip" to "${String.format("%.1f", longestTrip)} km",
        "Shortest Trip" to "${String.format("%.1f", shortestTrip)} km",
        "Total Duration" to "${totalDuration} min",
        "Avg Duration" to "${avgDuration} min",
        "Avg Speed" to "${String.format("%.0f", avgSpeed)} km/h",
        "Max Speed" to "${String.format("%.0f", maxSpeed)} km/h"
    )
}

@Composable
fun FabMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.75f),
            shadowElevation = 8.dp
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = Secondary,
            contentColor = Color.White
        ) {
            Icon(icon, contentDescription = label)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServiceLogEntry(
    service: com.simi.refillme.data.entity.Service,
    vehicle: Vehicle?,
    dateFormat: SimpleDateFormat,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as com.simi.refillme.RefillMeApplication
    val authManager = app.authManager
    val authApiService = com.simi.refillme.data.auth.AuthApiService.getInstance()
    val scope = rememberCoroutineScope()

    val hasActions = onEdit != null || onDelete != null
    var showMenu by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { if (hasActions) showMenu = true }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Column 1: Service icon, date, odometer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(service.date)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${service.odometerReading} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }

                // Column 2: Service center
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text(
                        text = service.serviceCenter,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        textAlign = TextAlign.Center
                    )
                    if (vehicle != null) {
                        Text(
                            text = vehicle.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }

                // Column 3: Total cost
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "UGX ${String.format("%,.0f", service.totalCost)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    Text(
                        text = "Service",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }

            if (hasActions) {
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (onEdit != null) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        )
                    }
                    if (onDelete != null) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                showPasswordDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Error)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showPasswordDialog && onDelete != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                password = ""
                passwordError = false
                isVerifying = false
            },
            title = { Text("Verify Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Please enter your password to confirm deletion")
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = false
                        },
                        label = { Text("Password") },
                        isError = passwordError,
                        enabled = !isVerifying,
                        singleLine = true
                    )
                    if (passwordError) {
                        Text(
                            "Incorrect password",
                            color = Error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (password.isNotBlank()) {
                            isVerifying = true
                            scope.launch {
                                try {
                                    val token = authManager.getToken()
                                    if (token != null) {
                                        val result = authApiService.verifyPassword(token, password)
                                        result.onSuccess { response ->
                                            if (response.success && response.valid) {
                                                onDelete()
                                                showPasswordDialog = false
                                                password = ""
                                                passwordError = false
                                            } else {
                                                passwordError = true
                                            }
                                        }.onFailure {
                                            passwordError = true
                                        }
                                    } else {
                                        passwordError = true
                                    }
                                } catch (e: Exception) {
                                    passwordError = true
                                } finally {
                                    isVerifying = false
                                }
                            }
                        }
                    },
                    enabled = !isVerifying && password.isNotBlank()
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Confirm Delete", color = Error)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasswordDialog = false
                        password = ""
                        passwordError = false
                        isVerifying = false
                    },
                    enabled = !isVerifying
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpenseLogEntry(
    expense: com.simi.refillme.data.entity.Expense,
    vehicle: Vehicle?,
    dateFormat: SimpleDateFormat,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as com.simi.refillme.RefillMeApplication
    val authManager = app.authManager
    val authApiService = com.simi.refillme.data.auth.AuthApiService.getInstance()
    val scope = rememberCoroutineScope()

    val hasActions = onEdit != null || onDelete != null
    var showMenu by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { if (hasActions) showMenu = true }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Column 1: Expense icon, date, odometer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(expense.date)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${expense.odometerReading} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }

                // Column 2: Vendor
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text(
                        text = expense.vendor,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        textAlign = TextAlign.Center
                    )
                    if (vehicle != null) {
                        Text(
                            text = vehicle.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }

                // Column 3: Total cost
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "UGX ${String.format("%,.0f", expense.totalCost)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Success
                    )
                    Text(
                        text = "Expense",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }

            if (hasActions) {
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (onEdit != null) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        )
                    }
                    if (onDelete != null) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                showPasswordDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Error)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showPasswordDialog && onDelete != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                password = ""
                passwordError = false
                isVerifying = false
            },
            title = { Text("Verify Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Please enter your password to confirm deletion")
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = false
                        },
                        label = { Text("Password") },
                        isError = passwordError,
                        enabled = !isVerifying,
                        singleLine = true
                    )
                    if (passwordError) {
                        Text(
                            "Incorrect password",
                            color = Error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (password.isNotBlank()) {
                            isVerifying = true
                            scope.launch {
                                try {
                                    val token = authManager.getToken()
                                    if (token != null) {
                                        val result = authApiService.verifyPassword(token, password)
                                        result.onSuccess { response ->
                                            if (response.success && response.valid) {
                                                onDelete()
                                                showPasswordDialog = false
                                                password = ""
                                                passwordError = false
                                            } else {
                                                passwordError = true
                                            }
                                        }.onFailure {
                                            passwordError = true
                                        }
                                    } else {
                                        passwordError = true
                                    }
                                } catch (e: Exception) {
                                    passwordError = true
                                } finally {
                                    isVerifying = false
                                }
                            }
                        }
                    },
                    enabled = !isVerifying && password.isNotBlank()
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Confirm Delete", color = Error)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasswordDialog = false
                        password = ""
                        passwordError = false
                        isVerifying = false
                    },
                    enabled = !isVerifying
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
