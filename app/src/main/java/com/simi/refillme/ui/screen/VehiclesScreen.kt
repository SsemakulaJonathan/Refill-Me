package com.simi.refillme.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.simi.refillme.data.entity.Refill
import com.simi.refillme.data.entity.Trip
import com.simi.refillme.data.entity.Vehicle
import com.simi.refillme.ui.theme.*
import kotlinx.coroutines.launch
import com.simi.refillme.ui.screen.calculateAverageKmL
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclesScreen(
    vehicles: List<Vehicle>,
    refills: List<Refill>,
    trips: List<Trip>,
    preferences: com.simi.refillme.data.entity.UserPreferences,
    onAddVehicle: () -> Unit,
    onEditVehicle: (Vehicle) -> Unit,
    onDeleteVehicle: (Vehicle) -> Unit,
    onViewSpecifications: (Vehicle) -> Unit = {},
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Vehicles",
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddVehicle,
                containerColor = Secondary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Vehicle")
            }
        }
    ) { paddingValues ->
        if (vehicles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = OnSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "No Vehicles Yet",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add your first vehicle to start tracking fuel expenses",
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(vehicles) { vehicle ->
                    val estimatedFuelLevel = remember(vehicle, refills, trips) {
                        estimateFuelLevel(vehicle, refills, trips)
                    }
                    VehicleDetailCard(
                        vehicle = vehicle,
                        displayFuelLevel = estimatedFuelLevel,
                        onEdit = { onEditVehicle(vehicle) },
                        onDelete = { onDeleteVehicle(vehicle) },
                        onViewSpecifications = { onViewSpecifications(vehicle) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VehicleDetailCard(
    vehicle: Vehicle,
    displayFuelLevel: Double = vehicle.currentFuelLevel,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onViewSpecifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as com.simi.refillme.RefillMeApplication
    val authManager = app.authManager
    val authApiService = com.simi.refillme.data.auth.AuthApiService.getInstance()
    val scope = rememberCoroutineScope()

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { showMenu = true }
                    )
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vehicle.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = vehicle.model,
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SurfaceVariant
                    ) {
                        Text(
                            text = vehicle.licensePlate,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Fuel Level Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = SurfaceVariant
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fuel Level",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${String.format("%.1f", displayFuelLevel)} / ${vehicle.tankCapacity}L",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val fuelPercentage = (displayFuelLevel / vehicle.tankCapacity).toFloat()
                    
                    LinearProgressIndicator(
                        progress = fuelPercentage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp),
                        color = when {
                            fuelPercentage > 0.5 -> Success
                            fuelPercentage > 0.25 -> Warning
                            else -> Error
                        },
                        trackColor = Color.LightGray.copy(alpha = 0.3f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "${String.format("%.1f", fuelPercentage * 100)}% Full",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatItemCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Speed,
                    label = "Tank",
                    value = "${vehicle.tankCapacity}L"
                )
                StatItemCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CalendarToday,
                    label = "Active",
                    value = if (vehicle.isActive) "Yes" else "No"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Specifications Button
            OutlinedButton(
                onClick = onViewSpecifications,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Primary
                )
            ) {
                Icon(
                    Icons.Default.AddCircleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "+ Specifications",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
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
                    showPasswordDialog = true
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

    if (showPasswordDialog) {
        AlertDialog(
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
                                                showPasswordDialog = false
                                                showDeleteDialog = true
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
                        Text("Confirm", color = Error)
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
                    "Delete Vehicle?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("Are you sure you want to delete this vehicle?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        vehicle.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    Text(
                        "${vehicle.make} ${vehicle.model} ${vehicle.year}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This will also delete all refill records and specifications for this vehicle. This action cannot be undone.",
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

private fun estimateFuelLevel(
    vehicle: Vehicle,
    refills: List<Refill>,
    trips: List<Trip>
): Double {
    val vehicleRefills = refills
        .filter { it.vehicleId == vehicle.id }
        .sortedByDescending { it.date }

    if (vehicleRefills.isEmpty()) {
        return vehicle.currentFuelLevel
    }

    val lastRefill = vehicleRefills.first()

    // Use the same average km/L calculation as the dashboard (all historical refills)
    val kmPerLiter = calculateAverageKmL(vehicleRefills).takeIf { it > 0 }
        ?: run {
            // Fallback: derive from last two refills if no odometer data
            val prev = vehicleRefills.getOrNull(1)
            if (prev?.odometerReading != null && lastRefill.odometerReading != null) {
                val dist = lastRefill.odometerReading - prev.odometerReading
                val fuel = prev.fuelAfter - lastRefill.fuelBefore
                if (dist > 0 && fuel > 0) dist / fuel else null
            } else null
        }

    val lastRefillTime = lastRefill.date
    val distanceSinceRefill = trips
        .filter { it.vehicleId == vehicle.id && !it.isActive }
        .filter { (it.endTime ?: it.startTime) > lastRefillTime }
        .sumOf { it.distanceKm }

    val estimated = if (kmPerLiter != null) {
        lastRefill.fuelAfter - (distanceSinceRefill / kmPerLiter)
    } else {
        lastRefill.fuelAfter
    }

    return min(vehicle.tankCapacity, max(0.0, estimated))
}

@Composable
fun StatItemCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Primary.copy(alpha = 0.05f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var tankCapacity by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Add New Vehicle",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Vehicle Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = licensePlate,
                    onValueChange = { licensePlate = it },
                    label = { Text("License Plate") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = tankCapacity,
                    onValueChange = { tankCapacity = it },
                    label = { Text("Tank Capacity (L)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val capacity = tankCapacity.toDoubleOrNull()
                    if (name.isNotBlank() && model.isNotBlank() && 
                        licensePlate.isNotBlank() && capacity != null && capacity > 0) {
                        onSave(name, model, licensePlate, capacity)
                        onDismiss()
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
