package com.simi.refillme.ui.screen

import android.location.Geocoder
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.simi.refillme.RefillMeApplication
import com.simi.refillme.data.entity.Trip
import com.simi.refillme.data.entity.Vehicle
import com.simi.refillme.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTripsScreen(
    trips: List<Trip>,
    vehicles: List<Vehicle>,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as RefillMeApplication
    val tripRepository = app.tripRepository
    val scope = rememberCoroutineScope()
    
    val completedTrips = trips.filter { !it.isActive }
        .sortedByDescending { it.endTime ?: it.startTime }
    
    // Group trips by vehicle, ordered by most recent trip per vehicle
    val tripsByVehicle = completedTrips
        .groupBy { it.vehicleId }
        .entries
        .sortedByDescending { (_, vehicleTrips) -> vehicleTrips.first().endTime ?: vehicleTrips.first().startTime }

    // Expanded state per vehicle — default all expanded
    val expandedVehicles = remember { mutableStateMapOf<Long, Boolean>() }
    LaunchedEffect(tripsByVehicle) {
        tripsByVehicle.forEach { (vehicleId, _) ->
            if (!expandedVehicles.containsKey(vehicleId)) {
                expandedVehicles[vehicleId] = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Trips") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (completedTrips.isNotEmpty()) {
                item {
                    Text(
                        text = "${completedTrips.size} ${if (completedTrips.size == 1) "Trip" else "Trips"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                tripsByVehicle.forEach { (vehicleId, tripsForVehicle) ->
                    val vehicle = vehicles.find { it.id == vehicleId }
                    val isExpanded = expandedVehicles[vehicleId] ?: true

                    // Vehicle accordion header
                    item(key = "vehicle_header_$vehicleId") {
                        VehicleAccordionHeader(
                            vehicleName = vehicle?.let { "${it.make} ${it.model}" } ?: "Unknown Vehicle",
                            tripCount = tripsForVehicle.size,
                            isExpanded = isExpanded,
                            onClick = { expandedVehicles[vehicleId] = !isExpanded }
                        )
                    }

                    if (isExpanded) {
                        // Group by date within this vehicle's trips
                        val tripsByDate = tripsForVehicle.groupBy { trip ->
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = trip.endTime ?: trip.startTime
                            calendar.set(Calendar.HOUR_OF_DAY, 0)
                            calendar.set(Calendar.MINUTE, 0)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)
                            calendar.timeInMillis
                        }.toSortedMap(reverseOrder())

                        tripsByDate.forEach { (dateMillis, tripsForDate) ->
                            item(key = "date_header_${vehicleId}_$dateMillis") {
                                DateHeader(dateMillis)
                            }
                            items(tripsForDate, key = { it.id }) { trip ->
                                TripHistoryCard(
                                    trip = trip,
                                    vehicles = vehicles,
                                    allTrips = trips,
                                    onEdit = { updatedTrip ->
                                        scope.launch {
                                            tripRepository.updateTrip(updatedTrip)
                                            tripRepository.loadTrips()
                                        }
                                    },
                                    onDelete = { tripToDelete ->
                                        scope.launch {
                                            tripRepository.deleteTrip(tripToDelete)
                                            tripRepository.loadTrips()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No trips yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = OnSurfaceVariant
                            )
                            Text(
                                text = "Start your first trip to see it here",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateHeader(dateMillis: Long) {
    val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
    val dateString = dateFormat.format(Date(dateMillis))
    
    Text(
        text = dateString,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = Primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 8.dp)
    )
}

@Composable
private fun VehicleAccordionHeader(
    vehicleName: String,
    tripCount: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vehicleName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$tripCount ${if (tripCount == 1) "trip" else "trips"}",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripHistoryCard(
    trip: Trip,
    vehicles: List<Vehicle>,
    allTrips: List<Trip>,
    onEdit: (Trip) -> Unit,
    onDelete: (Trip) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vehicle = vehicles.find { it.id == trip.vehicleId }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showEndSearchDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var editEndAddress by remember(trip) { mutableStateOf(trip.endAddress ?: "") }
    var editEndLat by remember(trip) { mutableStateOf(trip.endLatitude) }
    var editEndLng by remember(trip) { mutableStateOf(trip.endLongitude) }
    var editEndDropdownExpanded by remember { mutableStateOf(false) }
    var editError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val app = context.applicationContext as RefillMeApplication
    val authManager = app.authManager
    val authApiService = com.simi.refillme.data.auth.AuthApiService.getInstance()

    // Top 5 frequent end addresses from all trips
    val endAddressOptions = remember(allTrips) {
        allTrips.mapNotNull { t ->
            val addr = t.endAddress; val lat = t.endLatitude; val lng = t.endLongitude
            if (addr != null && lat != null && lng != null) Triple(addr, lat, lng) else null
        }.groupBy { it.first }
            .toList().sortedByDescending { it.second.size }.take(5)
            .map { Triple(it.second.first().first, it.second.first().second, it.second.first().third) }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with vehicle info and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = vehicle?.let { "${it.make} ${it.model}" } ?: "Unknown Vehicle",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatTripTime(trip.startTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = {
                    editEndAddress = trip.endAddress ?: ""
                    editEndLat = trip.endLatitude
                    editEndLng = trip.endLongitude
                    editError = null
                    showEditDialog = true
                }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Trip", tint = Primary)
                }
                IconButton(
                    onClick = { showDeleteDialog = true }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Trip",
                        tint = Error
                    )
                }
            }

            // Start and End Locations
            if (trip.startAddress != null || trip.endAddress != null) {
                Divider()

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Start location
                    if (trip.startAddress != null) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Start",
                                tint = Success,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Start",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceVariant
                                )
                                Text(
                                    text = trip.startAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    // End location
                    if (trip.endAddress != null) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = "End",
                                tint = Error,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "End",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceVariant
                                )
                                Text(
                                    text = trip.endAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            Divider()

            // Trip stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TripStatItem(
                    icon = Icons.Default.Route,
                    label = "Distance",
                    value = "${"%.1f".format(trip.distanceKm)} km"
                )
                TripStatItem(
                    icon = Icons.Default.Schedule,
                    label = "Duration",
                    value = formatDurationTime(trip.durationMinutes)
                )
                TripStatItem(
                    icon = Icons.Default.Speed,
                    label = "Avg Speed",
                    value = "${"%.0f".format(trip.averageSpeedKmh)} km/h"
                )
            }
        }
    }

    // Edit trip dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showEditDialog = false },
            title = { Text("Edit Trip") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Update end address for this trip. Distance, duration and speed will remain unchanged.",
                        style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)

                    // End address dropdown
                    ExposedDropdownMenuBox(
                        expanded = editEndDropdownExpanded,
                        onExpandedChange = { editEndDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = editEndAddress,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("End Address") },
                            trailingIcon = {
                                Icon(
                                    if (editEndDropdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = editEndDropdownExpanded,
                            onDismissRequest = { editEndDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Use current location") },
                                onClick = {
                                    editEndDropdownExpanded = false
                                    scope.launch {
                                        try {
                                            val locationHelper = com.simi.refillme.utils.LocationHelper(context)
                                            val location = withContext(Dispatchers.IO) { locationHelper.getCurrentLocation() }
                                            if (location != null) {
                                                val geocoder = Geocoder(context, Locale.getDefault())
                                                val addresses = withContext(Dispatchers.IO) {
                                                    @Suppress("DEPRECATION")
                                                    geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                                }
                                                val addr = addresses?.firstOrNull()?.getAddressLine(0)
                                                if (addr != null) {
                                                    editEndAddress = addr
                                                    editEndLat = location.latitude
                                                    editEndLng = location.longitude
                                                } else { editError = "Could not resolve current location address" }
                                            } else { editError = "Unable to fetch current location" }
                                        } catch (e: Exception) { editError = "Location error: ${e.message}" }
                                    }
                                }
                            )
                            endAddressOptions.forEach { (addr, lat, lng) ->
                                DropdownMenuItem(
                                    text = { Text(addr, maxLines = 2) },
                                    onClick = {
                                        editEndAddress = addr
                                        editEndLat = lat
                                        editEndLng = lng
                                        editEndDropdownExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Search other location...") },
                                onClick = {
                                    editEndDropdownExpanded = false
                                    showEndSearchDialog = true
                                }
                            )
                        }
                    }

                    editError?.let { Text(it, color = Error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSaving && editEndAddress.isNotBlank(),
                    onClick = {
                        isSaving = true
                        editError = null
                        scope.launch {
                            try {
                                val updated = trip.copy(
                                    endAddress = editEndAddress.ifBlank { null },
                                    endLatitude = editEndLat,
                                    endLongitude = editEndLng,
                                    isActive = false
                                )
                                onEdit(updated)
                                showEditDialog = false
                            } catch (e: Exception) {
                                editError = "Failed to save: ${e.message}"
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                ) {
                    if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }, enabled = !isSaving) { Text("Cancel") }
            }
        )
    }

    // End location search dialog for edit
    if (showEndSearchDialog) {
        EditLocationSearchDialog(
            title = "Select End Location",
            onDismiss = { showEndSearchDialog = false },
            onLocationSelected = { addr, lat, lng ->
                editEndAddress = addr
                editEndLat = lat
                editEndLng = lng
                showEndSearchDialog = false
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Trip") },
            text = { Text("Are you sure you want to delete this trip? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        showPasswordDialog = true
                    }
                ) {
                    Text("Delete", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Password dialog
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
                                                password = ""
                                                passwordError = false
                                                isVerifying = false
                                                onDelete(trip)
                                            } else {
                                                passwordError = true
                                                isVerifying = false
                                            }
                                        }.onFailure {
                                            passwordError = true
                                            isVerifying = false
                                        }
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
private fun TripStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditLocationSearchDialog(
    title: String,
    onDismiss: () -> Unit,
    onLocationSelected: (String, Double, Double) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Triple<String, Double, Double>>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Triple<String, Double, Double>?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search location") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (query.isNotBlank()) {
                                isSearching = true
                                scope.launch {
                                    try {
                                        val geocoder = Geocoder(context, Locale.getDefault())
                                        val addresses = withContext(Dispatchers.IO) {
                                            @Suppress("DEPRECATION")
                                            geocoder.getFromLocationName(query, 5)
                                        }
                                        results = addresses?.mapNotNull { addr ->
                                            val line = addr.getAddressLine(0)
                                            if (line != null) Triple(line, addr.latitude, addr.longitude) else null
                                        } ?: emptyList()
                                    } catch (e: Exception) {
                                        results = emptyList()
                                    } finally {
                                        isSearching = false
                                    }
                                }
                            }
                        },
                        enabled = query.isNotBlank() && !isSearching
                    ) {
                        if (isSearching) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Search")
                    }
                }
                results.forEach { result ->
                    val isSelected = selected == result
                    Card(
                        onClick = { selected = result },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Primary.copy(alpha = 0.1f) else CardBackground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(result.first, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selected?.let { onLocationSelected(it.first, it.second, it.third) } },
                enabled = selected != null
            ) { Text("Select") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatTripTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDurationTime(minutes: Long): String {
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    
    return when {
        hours > 0 && remainingMinutes > 0 -> "${hours}h ${remainingMinutes}m"
        hours > 0 -> "${hours}h"
        else -> "${remainingMinutes}m"
    }
}
