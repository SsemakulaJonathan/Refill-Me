package com.simi.refillme.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.simi.refillme.data.entity.Refill
import com.simi.refillme.data.entity.Vehicle
import com.simi.refillme.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefillHistoryScreen(
    refills: List<Refill>,
    allServices: List<com.simi.refillme.data.entity.Service> = emptyList(),
    allExpenses: List<com.simi.refillme.data.entity.Expense> = emptyList(),
    vehicles: List<Vehicle>,
    preferences: com.simi.refillme.data.entity.UserPreferences,
    onEditRefill: (Refill) -> Unit,
    onDeleteRefill: (Refill) -> Unit,
    onEditService: (com.simi.refillme.data.entity.Service) -> Unit,
    onDeleteService: (com.simi.refillme.data.entity.Service) -> Unit,
    onEditExpense: (com.simi.refillme.data.entity.Expense) -> Unit,
    onDeleteExpense: (com.simi.refillme.data.entity.Expense) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val sortedRefills = remember(refills) {
        refills.sortedByDescending { it.date }
    }
    val sortedServices = remember(allServices) {
        allServices.sortedByDescending { it.date }
    }
    val sortedExpenses = remember(allExpenses) {
        allExpenses.sortedByDescending { it.date }
    }
    val isEmpty = sortedRefills.isEmpty() && sortedServices.isEmpty() && sortedExpenses.isEmpty()

    // Group by vehicle
    val refillsByVehicle = remember(sortedRefills) { sortedRefills.groupBy { it.vehicleId } }
    val servicesByVehicle = remember(sortedServices) { sortedServices.groupBy { it.vehicleId } }
    val expensesByVehicle = remember(sortedExpenses) { sortedExpenses.groupBy { it.vehicleId } }

    // Expanded states per section
    val expandedRefillVehicles = remember { mutableStateMapOf<Long, Boolean>() }
    val expandedServiceVehicles = remember { mutableStateMapOf<Long, Boolean>() }
    val expandedExpenseVehicles = remember { mutableStateMapOf<Long, Boolean>() }
    LaunchedEffect(refillsByVehicle.keys, servicesByVehicle.keys, expensesByVehicle.keys) {
        refillsByVehicle.keys.forEach { if (!expandedRefillVehicles.containsKey(it)) expandedRefillVehicles[it] = true }
        servicesByVehicle.keys.forEach { if (!expandedServiceVehicles.containsKey(it)) expandedServiceVehicles[it] = true }
        expensesByVehicle.keys.forEach { if (!expandedExpenseVehicles.containsKey(it)) expandedExpenseVehicles[it] = true }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Log History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        if (isEmpty) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Background)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = OnSurfaceVariant
                    )
                    Text(
                        "No entries recorded yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .background(Background)
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ── Fuel Fill-Ups ──────────────────────────────────
                if (refillsByVehicle.isNotEmpty()) {
                    item {
                        Text(
                            text = "Fuel Fill-Ups",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Success,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    refillsByVehicle.entries
                        .sortedByDescending { it.value.first().date }
                        .forEach { (vehicleId, vehicleRefills) ->
                            val vehicle = vehicles.find { it.id == vehicleId }
                            val isExpanded = expandedRefillVehicles[vehicleId] ?: true
                            item(key = "refill_vh_$vehicleId") {
                                LogVehicleHeader(
                                    vehicleName = vehicle?.let { "${it.make} ${it.model}" } ?: "Unknown Vehicle",
                                    itemCount = vehicleRefills.size,
                                    isExpanded = isExpanded,
                                    onClick = { expandedRefillVehicles[vehicleId] = !isExpanded }
                                )
                            }
                            if (isExpanded) {
                                items(vehicleRefills, key = { "refill_${it.id}" }) { refill ->
                                    val previousRefill = refills
                                        .filter { it.vehicleId == refill.vehicleId && it.date < refill.date }
                                        .maxByOrNull { it.date }
                                    RefillLogEntry(
                                        refill = refill,
                                        previousRefill = previousRefill,
                                        vehicle = vehicle,
                                        preferences = preferences,
                                        dateFormat = dateFormat,
                                        onEdit = { onEditRefill(refill) },
                                        onDelete = { onDeleteRefill(refill) }
                                    )
                                }
                            }
                        }
                }

                // ── Service & Maintenance ───────────────────────────
                if (servicesByVehicle.isNotEmpty()) {
                    item {
                        Text(
                            text = "Service & Maintenance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Warning,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    servicesByVehicle.entries
                        .sortedByDescending { it.value.first().date }
                        .forEach { (vehicleId, vehicleServices) ->
                            val vehicle = vehicles.find { it.id == vehicleId }
                            val isExpanded = expandedServiceVehicles[vehicleId] ?: true
                            item(key = "service_vh_$vehicleId") {
                                LogVehicleHeader(
                                    vehicleName = vehicle?.let { "${it.make} ${it.model}" } ?: "Unknown Vehicle",
                                    itemCount = vehicleServices.size,
                                    isExpanded = isExpanded,
                                    onClick = { expandedServiceVehicles[vehicleId] = !isExpanded }
                                )
                            }
                            if (isExpanded) {
                                items(vehicleServices, key = { "service_${it.id}" }) { service ->
                                    ServiceLogEntry(
                                        service = service,
                                        vehicle = vehicle,
                                        dateFormat = dateFormat,
                                        onEdit = { onEditService(service) },
                                        onDelete = { onDeleteService(service) }
                                    )
                                }
                            }
                        }
                }

                // ── Expenses ────────────────────────────────────────
                if (expensesByVehicle.isNotEmpty()) {
                    item {
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Error,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    expensesByVehicle.entries
                        .sortedByDescending { it.value.first().date }
                        .forEach { (vehicleId, vehicleExpenses) ->
                            val vehicle = vehicles.find { it.id == vehicleId }
                            val isExpanded = expandedExpenseVehicles[vehicleId] ?: true
                            item(key = "expense_vh_$vehicleId") {
                                LogVehicleHeader(
                                    vehicleName = vehicle?.let { "${it.make} ${it.model}" } ?: "Unknown Vehicle",
                                    itemCount = vehicleExpenses.size,
                                    isExpanded = isExpanded,
                                    onClick = { expandedExpenseVehicles[vehicleId] = !isExpanded }
                                )
                            }
                            if (isExpanded) {
                                items(vehicleExpenses, key = { "expense_${it.id}" }) { expense ->
                                    ExpenseLogEntry(
                                        expense = expense,
                                        vehicle = vehicle,
                                        dateFormat = dateFormat,
                                        onEdit = { onEditExpense(expense) },
                                        onDelete = { onDeleteExpense(expense) }
                                    )
                                }
                            }
                        }
                }
            }
        }
    }
}
@Composable
private fun LogVehicleHeader(
    vehicleName: String,
    itemCount: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vehicleName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$itemCount ${if (itemCount == 1) "entry" else "entries"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = OnSurfaceVariant
            )
        }
    }
}