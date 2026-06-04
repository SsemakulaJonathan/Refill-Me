package com.simi.refillme.ui.screen

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.simi.refillme.data.entity.*
import com.simi.refillme.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllLogsScreen(
    trips: List<Trip>,
    refills: List<Refill>,
    services: List<Service>,
    expenses: List<Expense>,
    vehicles: List<Vehicle>,
    onNavigateBack: () -> Unit,
    onEditRefill: (Refill) -> Unit = {},
    onDeleteRefill: (Refill) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    // Group all logs by date
    data class LogEntry(
        val date: Long,
        val type: String, // "trip", "refill", "service", "expense"
        val data: Any
    )
    
    val allLogs = remember(trips, refills, services, expenses) {
        val logs = mutableListOf<LogEntry>()
        
        trips.forEach { trip ->
            logs.add(LogEntry(trip.startTime, "trip", trip))
        }
        
        refills.forEach { refill ->
            logs.add(LogEntry(refill.date, "refill", refill))
        }
        
        services.forEach { service ->
            logs.add(LogEntry(service.date, "service", service))
        }
        
        expenses.forEach { expense ->
            logs.add(LogEntry(expense.date, "expense", expense))
        }
        
        logs.sortedByDescending { it.date }
    }
    
    // Group by date for headers
    val logsByDate = remember(allLogs) {
        allLogs.groupBy { entry ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = entry.date
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Activity Logs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = OnPrimary,
                    navigationIconContentColor = OnPrimary
                )
            )
        }
    ) { paddingValues ->
        if (allLogs.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Article,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = OnSurfaceVariant
                    )
                    Text(
                        "No Activity Logs",
                        style = MaterialTheme.typography.titleLarge,
                        color = OnSurface
                    )
                    Text(
                        "Start tracking trips, refills, services, and expenses to see them here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Summary card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Activity Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                SummaryItem(
                                    icon = Icons.Default.DirectionsCar,
                                    count = trips.size,
                                    label = "Trips",
                                    color = Primary
                                )
                                SummaryItem(
                                    icon = Icons.Default.LocalGasStation,
                                    count = refills.size,
                                    label = "Refills",
                                    color = Success
                                )
                                SummaryItem(
                                    icon = Icons.Default.Build,
                                    count = services.size,
                                    label = "Services",
                                    color = Warning
                                )
                                SummaryItem(
                                    icon = Icons.Default.Receipt,
                                    count = expenses.size,
                                    label = "Expenses",
                                    color = Error
                                )
                            }
                        }
                    }
                }
                
                // Logs grouped by date
                logsByDate.forEach { (dateMillis, logs) ->
                    item {
                        Text(
                            text = dateFormat.format(Date(dateMillis)),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    
                    items(logs) { log ->
                        when (log.type) {
                            "trip" -> {
                                val trip = log.data as Trip
                                TripLogCard(trip, vehicles, dateFormat, timeFormat)
                            }
                            "refill" -> {
                                val refill = log.data as Refill
                                RefillLogCard(refill, vehicles, dateFormat, timeFormat)
                            }
                            "service" -> {
                                val service = log.data as Service
                                ServiceLogCard(service, vehicles, dateFormat, timeFormat)
                            }
                            "expense" -> {
                                val expense = log.data as Expense
                                ExpenseLogCard(expense, vehicles, dateFormat, timeFormat)
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun SummaryItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    label: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = OnSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )
    }
}

@Composable
private fun TripLogCard(
    trip: Trip,
    vehicles: List<Vehicle>,
    dateFormat: SimpleDateFormat,
    timeFormat: SimpleDateFormat
) {
    val vehicle = vehicles.find { it.id == trip.vehicleId }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(40.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Trip",
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    vehicle?.name ?: "Unknown Vehicle",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${timeFormat.format(Date(trip.startTime))} - ${if (trip.endTime != null) timeFormat.format(Date(trip.endTime)) else "Ongoing"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "📍 ${"%.1f".format(trip.distanceKm)} km",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (trip.durationMinutes > 0) {
                        val hours = trip.durationMinutes / 60
                        val mins = trip.durationMinutes % 60
                        Text(
                            "⏱️ ${if (hours > 0) "${hours}h " else ""}${mins}m",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RefillLogCard(
    refill: Refill,
    vehicles: List<Vehicle>,
    dateFormat: SimpleDateFormat,
    timeFormat: SimpleDateFormat
) {
    val vehicle = vehicles.find { it.id == refill.vehicleId }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Success.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.LocalGasStation,
                contentDescription = null,
                tint = Success,
                modifier = Modifier.size(40.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Fuel Fill-Up",
                    style = MaterialTheme.typography.labelMedium,
                    color = Success,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    vehicle?.name ?: "Unknown Vehicle",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    timeFormat.format(Date(refill.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "⛽ ${"%.2f".format(refill.refillAmount)} L",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "💰 UGX ${"%.0f".format(refill.totalPrice)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                refill.location?.let {
                    Text(
                        "📍 $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceLogCard(
    service: Service,
    vehicles: List<Vehicle>,
    dateFormat: SimpleDateFormat,
    timeFormat: SimpleDateFormat
) {
    val vehicle = vehicles.find { it.id == service.vehicleId }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Warning.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Build,
                contentDescription = null,
                tint = Warning,
                modifier = Modifier.size(40.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Service & Maintenance",
                    style = MaterialTheme.typography.labelMedium,
                    color = Warning,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    service.serviceCenter,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    vehicle?.name ?: "Unknown Vehicle",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
                Text(
                    timeFormat.format(Date(service.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    service.odometerReading?.let {
                        Text(
                            "📏 ${it.toInt()} km",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        "💰 UGX ${"%.0f".format(service.totalCost)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseLogCard(
    expense: Expense,
    vehicles: List<Vehicle>,
    dateFormat: SimpleDateFormat,
    timeFormat: SimpleDateFormat
) {
    val vehicle = vehicles.find { it.id == expense.vehicleId }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Error.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Receipt,
                contentDescription = null,
                tint = Error,
                modifier = Modifier.size(40.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Expense",
                    style = MaterialTheme.typography.labelMedium,
                    color = Error,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    expense.vendor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    vehicle?.name ?: "Unknown Vehicle",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
                Text(
                    timeFormat.format(Date(expense.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
                Text(
                    "💰 UGX ${"%.0f".format(expense.totalCost)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                expense.notes?.let {
                    if (it.isNotBlank()) {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}
