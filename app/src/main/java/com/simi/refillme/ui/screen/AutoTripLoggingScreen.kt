package com.simi.refillme.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.wifi.WifiManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simi.refillme.RefillMeApplication
import com.simi.refillme.data.entity.Trip
import com.simi.refillme.data.entity.TripType
import com.simi.refillme.data.entity.Vehicle
import com.simi.refillme.data.entity.VehicleAutoTripConfig
import com.simi.refillme.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoTripLoggingScreen(
    vehicles: List<Vehicle>,
    vehicleAutoTripConfigs: List<VehicleAutoTripConfig> = emptyList(),
    onUpdateVehicleConfig: (VehicleAutoTripConfig) -> Unit = {},
    onRemoveVehicleConfig: (Long) -> Unit = {},
    onStartService: () -> Unit = {},
    onStopService: () -> Unit = {},
    onNavigateBack: () -> Unit,
    onShowAllLogs: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showBluetoothDeviceDialog by remember { mutableStateOf(false) }
    var showWifiNetworkDialog by remember { mutableStateOf(false) }
    var showVehicleSelectionDialog by remember { mutableStateOf(false) }
    var showManualTripVehicleDialog by remember { mutableStateOf(false) }
    var showManualVehicleEntryDialog by remember { mutableStateOf(false) }
    var manualVehicleMake by remember { mutableStateOf("") }
    var manualVehicleModel by remember { mutableStateOf("") }
    var manualVehiclePlate by remember { mutableStateOf("") }
    var showManualTripEntryDialog by remember { mutableStateOf(false) }
    var manualTripVehicleId by remember { mutableStateOf<Long?>(null) }
    var manualStartAddress by remember { mutableStateOf("") }
    var manualEndAddress by remember { mutableStateOf("") }
    var manualStartLat by remember { mutableStateOf<Double?>(null) }
    var manualStartLng by remember { mutableStateOf<Double?>(null) }
    var manualEndLat by remember { mutableStateOf<Double?>(null) }
    var manualEndLng by remember { mutableStateOf<Double?>(null) }
    var manualStartTimeText by remember { mutableStateOf("") }
    var manualEndTimeText by remember { mutableStateOf("") }
    var manualTripNotes by remember { mutableStateOf("") }
    var startAddressDropdownExpanded by remember { mutableStateOf(false) }
    var endAddressDropdownExpanded by remember { mutableStateOf(false) }
    var showStartSearchDialog by remember { mutableStateOf(false) }
    var showEndSearchDialog by remember { mutableStateOf(false) }
    var manualTripVehicleDropdownExpanded by remember { mutableStateOf(false) }
    var manualTripError by remember { mutableStateOf<String?>(null) }
    var manualDistanceOverride by remember { mutableStateOf("") }
    var isSavingManualTrip by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var availableWifiNetworks by remember { mutableStateOf<List<String>>(emptyList()) }
    var hasBluetoothPermission by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var permissionsChecked by remember { mutableStateOf(false) }
    var currentPermissionStep by remember { mutableStateOf(0) } // Track which permission to request

    // Multi-vehicle config state
    var editingConfigVehicleId by remember { mutableStateOf<Long?>(null) }
    var showAddVehicleConfigDialog by remember { mutableStateOf(false) }
    // Remove config password protection
    var configToRemove by remember { mutableStateOf<Long?>(null) }
    var showRemovePasswordDialog by remember { mutableStateOf(false) }
    var removePassword by remember { mutableStateOf("") }
    var removePasswordError by remember { mutableStateOf(false) }
    var isVerifyingRemove by remember { mutableStateOf(false) }

    // Get trip repository and observe active trip
    val app = context.applicationContext as RefillMeApplication
    val tripRepository = app.tripRepository
    val authManager = app.authManager
    val authApiService = com.simi.refillme.data.auth.AuthApiService.getInstance()
    val activeTrip by tripRepository.observeActiveTrip().collectAsStateWithLifecycle(initialValue = null)
    val allTrips by tripRepository.getAllTrips().collectAsStateWithLifecycle(initialValue = emptyList())
    
    // Coroutine scope for launching background tasks
    val scope = rememberCoroutineScope()
    
    // Load trips when screen opens
    LaunchedEffect(Unit) {
        tripRepository.loadTrips()
        // Periodically refresh active trip status
        while (true) {
            tripRepository.getActiveTrip()
            kotlinx.coroutines.delay(5000) // Refresh every 5 seconds
        }
    }
    
    // Separate trips into active and completed
    val completedTrips = remember(allTrips) {
        allTrips.filter { !it.isActive }.sortedByDescending { it.startTime }
    }
    val startAddressOptions = remember(completedTrips) {
        getTopAddressOptions(completedTrips, isStart = true)
    }
    val endAddressOptions = remember(completedTrips) {
        getTopAddressOptions(completedTrips, isStart = false)
    }

    // Step 1: Bluetooth permissions
    val bluetoothPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasBluetoothPermission = checkBluetoothPermissions(context)
        if (hasBluetoothPermission) {
            currentPermissionStep = 1 // Move to location permissions
        } else {
            errorMessage = "Bluetooth permission is required for Auto Trip Logging"
        }
    }

    // Step 2: Foreground location permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            currentPermissionStep = 2 // Move to background location
        } else {
            errorMessage = "Location permission is required for Auto Trip Logging"
        }
    }

    // Step 3: Background location permission (Android 10+)
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = checkLocationPermissions(context)
        if (hasLocationPermission || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            currentPermissionStep = 3 // Move to notifications
        } else {
            errorMessage = "Background location permission is required. Please select 'Allow all the time'"
        }
    }

    // Step 4: Notification permission (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = checkNotificationPermission(context)
        currentPermissionStep = 4 // All done
        if (hasBluetoothPermission && hasLocationPermission && hasNotificationPermission) {
            pairedDevices = getPairedBluetoothDevices(context)
            successMessage = "All permissions granted successfully!"
            showPermissionDialog = false
        }
    }

    // Check permissions on launch and trigger permission flow
    LaunchedEffect(Unit) {
        hasBluetoothPermission = checkBluetoothPermissions(context)
        hasLocationPermission = checkLocationPermissions(context)
        hasNotificationPermission = checkNotificationPermission(context)
        permissionsChecked = true

        // Show dialog if permissions not granted
        if (!hasBluetoothPermission || !hasLocationPermission || !hasNotificationPermission) {
            showPermissionDialog = true
            currentPermissionStep = 0 // Start from beginning
        } else if (hasBluetoothPermission) {
            pairedDevices = getPairedBluetoothDevices(context)
        }
    }

    // Trigger sequential permission requests
    LaunchedEffect(currentPermissionStep) {
        when (currentPermissionStep) {
            0 -> {
                // Request Bluetooth permissions first
                if (!hasBluetoothPermission) {
                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                        )
                    } else {
                        arrayOf(
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN
                        )
                    }
                    bluetoothPermissionsLauncher.launch(permissions)
                } else {
                    currentPermissionStep = 1
                }
            }
            1 -> {
                // Request foreground location
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                } else {
                    currentPermissionStep = 2
                }
            }
            2 -> {
                // Request background location (Android 10+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    } else {
                        currentPermissionStep = 3
                    }
                } else {
                    currentPermissionStep = 3
                }
            }
            3 -> {
                // Request notification permission (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!hasNotificationPermission) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        currentPermissionStep = 4
                    }
                } else {
                    currentPermissionStep = 4
                    if (hasBluetoothPermission && hasLocationPermission) {
                        pairedDevices = getPairedBluetoothDevices(context)
                        successMessage = "All permissions granted successfully!"
                        showPermissionDialog = false
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Auto Trip Logging",
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
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(Background)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ━━━ Multi-Vehicle Auto Trip Configurations ━━━

            // Section header
            item {
                val unconfiguredVehicles = vehicles.filter { v -> vehicleAutoTripConfigs.none { it.vehicleId == v.id } }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Auto Trip Configurations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (unconfiguredVehicles.isNotEmpty()) {
                        IconButton(onClick = { showAddVehicleConfigDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add vehicle configuration", tint = Primary)
                        }
                    }
                }
            }

            // Empty state when no configs
            if (vehicleAutoTripConfigs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null,
                                tint = OnSurfaceVariant, modifier = Modifier.size(40.dp))
                            Text("No vehicles configured", style = MaterialTheme.typography.bodyLarge, color = OnSurfaceVariant)
                            Text("Tap + to add a vehicle and pair it with a Bluetooth device",
                                style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                        }
                    }
                }
            }

            // Per-vehicle config cards
            vehicleAutoTripConfigs.forEach { config ->
                item(key = config.vehicleId) {
                    val configVehicle = vehicles.find { it.id == config.vehicleId }
                    if (configVehicle != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (config.autoTripEnabled) Primary.copy(alpha = 0.07f) else CardBackground
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Vehicle name + enable toggle row
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
                                        Icon(Icons.Default.DirectionsCar, contentDescription = null,
                                            tint = if (config.autoTripEnabled) Primary else OnSurfaceVariant,
                                            modifier = Modifier.size(22.dp))
                                        Column {
                                            Text(
                                                "${configVehicle.make} ${configVehicle.model}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (configVehicle.licensePlate.isNotBlank()) {
                                                Text(configVehicle.licensePlate, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                            }
                                        }
                                    }
                                    Switch(
                                        checked = config.autoTripEnabled,
                                        onCheckedChange = { enabled ->
                                            val updated = config.copy(autoTripEnabled = enabled)
                                            onUpdateVehicleConfig(updated)
                                            val anyEnabled = vehicleAutoTripConfigs
                                                .map { if (it.vehicleId == config.vehicleId) updated else it }
                                                .any { it.autoTripEnabled }
                                            if (anyEnabled) onStartService() else onStopService()
                                        },
                                        enabled = config.bluetoothDevice != null
                                    )
                                }

                                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                                // Bluetooth device row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (hasBluetoothPermission) {
                                                pairedDevices = getPairedBluetoothDevices(context)
                                                editingConfigVehicleId = config.vehicleId
                                                showBluetoothDeviceDialog = true
                                            } else {
                                                showPermissionDialog = true
                                                currentPermissionStep = 0
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Bluetooth, contentDescription = null,
                                        tint = if (config.bluetoothDevice != null) Primary else OnSurfaceVariant,
                                        modifier = Modifier.size(20.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Bluetooth Device", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                        Text(
                                            config.bluetoothDeviceName ?: "Tap to select",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (config.bluetoothDevice != null) Primary else OnSurfaceVariant
                                        )
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                                }

                                // WiFi SSID row (optional)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (hasLocationPermission) {
                                                availableWifiNetworks = getAvailableWifiNetworks(context)
                                                editingConfigVehicleId = config.vehicleId
                                                showWifiNetworkDialog = true
                                            } else {
                                                showPermissionDialog = true
                                                currentPermissionStep = 1
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Wifi, contentDescription = null,
                                        tint = if (config.wifiSsid != null) Primary else OnSurfaceVariant,
                                        modifier = Modifier.size(20.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("WiFi Network (optional)", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                        Text(
                                            config.wifiSsid ?: "Not set",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (config.wifiSsid != null) Primary else OnSurfaceVariant
                                        )
                                    }
                                    if (config.wifiSsid != null) {
                                        IconButton(
                                            onClick = { onUpdateVehicleConfig(config.copy(wifiSsid = null)) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear WiFi", tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
                                        }
                                    } else {
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                                    }
                                }

                                // Remove config button
                                TextButton(
                                    onClick = {
                                        configToRemove = config.vehicleId
                                        showRemovePasswordDialog = true
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Error)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Remove", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            // Permissions Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasBluetoothPermission && hasLocationPermission && hasNotificationPermission)
                            Success.copy(alpha = 0.1f)
                        else
                            Warning.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Permissions Status",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        PermissionStatusRow("Bluetooth", hasBluetoothPermission)
                        PermissionStatusRow("Location", hasLocationPermission)
                        PermissionStatusRow("Notifications", hasNotificationPermission)

                        if (!hasBluetoothPermission || !hasLocationPermission || !hasNotificationPermission) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // Info text for background location (Android 10+)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasLocationPermission) {
                                Text(
                                    text = "⚠️ Background location is required for auto trip tracking. When prompted, select 'Allow all the time' for location access.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Warning,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    showPermissionDialog = true
                                    currentPermissionStep = 0 // Start permission flow
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Grant All Permissions")
                            }
                        }
                    }
                }
            }

            // Active Trip Tracking Display
            activeTrip?.let { trip ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Active Trip",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                                // Pulsing indicator
                                Icon(
                                    imageVector = Icons.Default.FiberManualRecord,
                                    contentDescription = "Recording",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Divider(modifier = Modifier.padding(vertical = 4.dp))

                            // Start Time
                            TripInfoRow(
                                label = "Started",
                                value = formatTime(trip.startTime)
                            )

                            // Duration
                            val durationMillis = System.currentTimeMillis() - trip.startTime
                            val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
                            val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
                            TripInfoRow(
                                label = "Duration",
                                value = "${hours}h ${minutes}m"
                            )

                            // Distance
                            if (trip.distanceKm > 0) {
                                TripInfoRow(
                                    label = "Distance",
                                    value = "${"%.2f".format(trip.distanceKm)} km"
                                )
                            }

                            // Average Speed
                            if (trip.averageSpeedKmh > 0) {
                                TripInfoRow(
                                    label = "Avg Speed",
                                    value = "${"%.0f".format(trip.averageSpeedKmh)} km/h"
                                )
                            }

                            // Max Speed
                            if (trip.maxSpeedKmh > 0) {
                                TripInfoRow(
                                    label = "Max Speed",
                                    value = "${"%.0f".format(trip.maxSpeedKmh)} km/h"
                                )
                            }

                            Divider(modifier = Modifier.padding(vertical = 4.dp))

                            // Stop Trip Button
                            Button(
                                onClick = {
                                    val intent = Intent(context, com.simi.refillme.service.TripTrackingService::class.java).apply {
                                        action = com.simi.refillme.service.TripTrackingService.ACTION_STOP_TRIP
                                    }
                                    context.startService(intent)
                                    successMessage = "Trip stopped successfully!"
                                    
                                    // Reload trips after a short delay to show the completed trip
                                    scope.launch {
                                        delay(2000) // Wait 2 seconds for trip to be saved
                                        tripRepository.loadTrips()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF5252)
                                )
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Stop Trip")
                            }
                        }
                    }
                }
            }

            // Test Trip Button (for debugging)
            val firstEnabledConfig = vehicleAutoTripConfigs.firstOrNull { it.autoTripEnabled }
            if (firstEnabledConfig != null && activeTrip == null) {
                item {
                    OutlinedButton(
                        onClick = {
                            val deviceAddress = firstEnabledConfig.bluetoothDevice
                            val intent = Intent(context, com.simi.refillme.service.TripTrackingService::class.java).apply {
                                action = com.simi.refillme.service.TripTrackingService.ACTION_START_TRIP
                                putExtra(com.simi.refillme.service.TripTrackingService.EXTRA_VEHICLE_ID, firstEnabledConfig.vehicleId)
                                deviceAddress?.let {
                                    putExtra(com.simi.refillme.service.TripTrackingService.EXTRA_DEVICE_ADDRESS, it)
                                }
                            }
                            try {
                                context.startForegroundService(intent)
                                successMessage = "Test trip started! Check your notifications."
                            } catch (e: Exception) {
                                errorMessage = "Error starting trip: ${e.message}"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Test Trip")
                    }
                }
            }
            
            // Quick Start Trip Button (available even when auto-trip is disabled)
            if (activeTrip == null) {
                item {
                    Button(
                        onClick = {
                            // Show vehicle selection dialog
                            showManualTripVehicleDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Trip Now")
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { showManualTripEntryDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Manual Trip")
                }
            }
            
            // Trip History Section
            if (completedTrips.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Trip History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        TextButton(
                            onClick = onShowAllLogs
                        ) {
                            Text("Show All")
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                
                items(completedTrips.take(10)) { trip ->
                    TripHistoryCard(
                        trip = trip,
                        vehicles = vehicles,
                        onDelete = { tripToDelete ->
                            scope.launch {
                                tripRepository.deleteTrip(tripToDelete)
                                tripRepository.loadTrips()
                            }
                        }
                    )
                }
                
                if (completedTrips.size > 10) {
                    item {
                        Text(
                            text = "Showing last 10 trips",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            } else if (activeTrip == null) {
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

            // Success message
            successMessage?.let { message ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Success
                            )
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Success
                            )
                        }
                    }
                }
            }

            // Info Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "How it works",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                        Text(
                            text = "• Grant all required permissions (Bluetooth, Location, Notifications)\n" +
                                    "• IMPORTANT: Select 'Allow all the time' for location access\n" +
                                    "• Connect your phone to your car's Bluetooth\n" +
                                    "• Select the paired device and vehicle\n" +
                                    "• Enable Auto Trip Logging\n" +
                                    "• Trips are automatically tracked when Bluetooth connects\n" +
                                    "• Captures start/end location, distance, and average speed",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurface
                        )
                    }
                }
            }

            // Error message
            errorMessage?.let { error ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Error
                            )
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Error
                            )
                        }
                    }
                }
            }
        }
    }

    // Bluetooth Device Selection Dialog
    if (showBluetoothDeviceDialog) {
        AlertDialog(
            onDismissRequest = { showBluetoothDeviceDialog = false },
            title = { Text("Select Bluetooth Device") },
            text = {
                if (pairedDevices.isEmpty()) {
                    Text("No paired Bluetooth devices found. Please pair your car's Bluetooth in system settings first.")
                } else {
                    LazyColumn {
                        items(pairedDevices) { device ->
                            TextButton(
                                onClick = {
                                    val deviceName = device.name ?: "Unknown Device"
                                    val configId = editingConfigVehicleId
                                    if (configId != null) {
                                        val existing = vehicleAutoTripConfigs.find { it.vehicleId == configId }
                                            ?: VehicleAutoTripConfig(vehicleId = configId)
                                        onUpdateVehicleConfig(existing.copy(bluetoothDevice = device.address, bluetoothDeviceName = deviceName))
                                    }
                                    showBluetoothDeviceDialog = false
                                    editingConfigVehicleId = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = device.name ?: "Unknown Device",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBluetoothDeviceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // WiFi Network Selection Dialog
    if (showWifiNetworkDialog) {
        AlertDialog(
            onDismissRequest = { showWifiNetworkDialog = false },
            title = { Text("Select WiFi Network") },
            text = {
                Column {
                    Text(
                        text = "Select your car's WiFi hotspot network:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (availableWifiNetworks.isEmpty()) {
                        Text("No WiFi networks found. Make sure your car's WiFi is turned on and in range.")
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(availableWifiNetworks) { ssid ->
                                TextButton(
                                    onClick = {
                                        val configId = editingConfigVehicleId
                                        if (configId != null) {
                                            val existing = vehicleAutoTripConfigs.find { it.vehicleId == configId }
                                                ?: VehicleAutoTripConfig(vehicleId = configId)
                                            onUpdateVehicleConfig(existing.copy(wifiSsid = ssid))
                                        }
                                        showWifiNetworkDialog = false
                                        editingConfigVehicleId = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(ssid)
                                        Icon(Icons.Default.Wifi, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWifiNetworkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Manual Trip Vehicle Selection Dialog
    if (showManualTripVehicleDialog) {
        AlertDialog(
            onDismissRequest = { showManualTripVehicleDialog = false },
            title = { Text("Select Vehicle for Trip") },
            text = {
                LazyColumn {
                    if (vehicles.isEmpty()) {
                        item {
                            Text(
                                "No vehicles available. You can enter vehicle details manually.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    } else {
                        items(vehicles) { vehicle ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                onClick = {
                                    showManualTripVehicleDialog = false
                                    // Start trip with selected vehicle
                                    // Use the configured BT device for this specific vehicle, if any
                                    val vehicleConfig = vehicleAutoTripConfigs.find { it.vehicleId == vehicle.id }
                                    val deviceAddress = vehicleConfig?.bluetoothDevice

                                    val intent = Intent(context, com.simi.refillme.service.TripTrackingService::class.java).apply {
                                        action = com.simi.refillme.service.TripTrackingService.ACTION_START_TRIP
                                        putExtra(com.simi.refillme.service.TripTrackingService.EXTRA_VEHICLE_ID, vehicle.id)
                                        deviceAddress?.let {
                                            putExtra(com.simi.refillme.service.TripTrackingService.EXTRA_DEVICE_ADDRESS, it)
                                        }
                                    }
                                    try {
                                        context.startForegroundService(intent)
                                        successMessage = if (deviceAddress != null) {
                                            "Trip started for ${vehicle.make} ${vehicle.model}! Monitoring will stop if device disconnects for 2 minutes."
                                        } else {
                                            "Trip started for ${vehicle.make} ${vehicle.model} without Bluetooth monitoring!"
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Error starting trip: ${e.message}"
                                    }
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (vehicleAutoTripConfigs.any { it.vehicleId == vehicle.id })
                                        Primary.copy(alpha = 0.1f)
                                    else
                                        CardBackground
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = if (vehicleAutoTripConfigs.any { it.vehicleId == vehicle.id }) Primary else OnSurfaceVariant
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${vehicle.make} ${vehicle.model}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = vehicle.licensePlate,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = OnSurfaceVariant
                                        )
                                    }
                                    if (vehicleAutoTripConfigs.any { it.vehicleId == vehicle.id }) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Configured vehicle",
                                            tint = Primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Other Vehicle option
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            onClick = {
                                showManualTripVehicleDialog = false
                                showManualVehicleEntryDialog = true
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Other Vehicle",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Primary
                                    )
                                    Text(
                                        text = "Enter vehicle details manually",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showManualTripVehicleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Manual Vehicle Entry Dialog
    if (showManualVehicleEntryDialog) {
        AlertDialog(
            onDismissRequest = {
                showManualVehicleEntryDialog = false
                manualVehicleMake = ""
                manualVehicleModel = ""
                manualVehiclePlate = ""
            },
            title = { Text("Enter Vehicle Details") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = manualVehicleMake,
                        onValueChange = { manualVehicleMake = it },
                        label = { Text("Make (e.g., Toyota)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = manualVehicleModel,
                        onValueChange = { manualVehicleModel = it },
                        label = { Text("Model (e.g., Camry)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = manualVehiclePlate,
                        onValueChange = { manualVehiclePlate = it },
                        label = { Text("License Plate") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualVehicleMake.isNotBlank() && manualVehicleModel.isNotBlank()) {
                            showManualVehicleEntryDialog = false
                            
                            // Create a temporary vehicle and save it to the backend
                            scope.launch {
                                try {
                                    val vehicleApiService = app.vehicleApiService
                                    val token = authManager.getToken()
                                    
                                    if (token != null) {
                                        val vehicleName = "${manualVehicleMake.trim()} ${manualVehicleModel.trim()}"
                                        val newVehicle = com.simi.refillme.data.entity.Vehicle(
                                            id = 0,
                                            name = vehicleName,
                                            make = manualVehicleMake.trim(),
                                            model = manualVehicleModel.trim(),
                                            licensePlate = manualVehiclePlate.trim().ifBlank { "N/A" },
                                            year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
                                            fuelType = "Petrol",
                                            tankCapacity = 50.0, // Default 50L tank
                                            currentFuelLevel = 0.0
                                        )
                                        
                                        val result = vehicleApiService.createVehicle(token, newVehicle)
                                        result.onSuccess { response ->
                                            android.util.Log.d("ManualTrip", "Vehicle creation response: success=${response.success}, vehicle=${response.vehicle}")
                                            if (response.success && response.vehicle != null) {
                                                // Start trip with the newly created vehicle
                                                val vehicleId = response.vehicle.id
                                                android.util.Log.d("ManualTrip", "Starting trip with vehicleId=$vehicleId")
                                                val deviceAddress = vehicleAutoTripConfigs
                                                    .firstOrNull { it.autoTripEnabled }?.bluetoothDevice
                                                
                                                val intent = Intent(context, com.simi.refillme.service.TripTrackingService::class.java).apply {
                                                    action = com.simi.refillme.service.TripTrackingService.ACTION_START_TRIP
                                                    putExtra(com.simi.refillme.service.TripTrackingService.EXTRA_VEHICLE_ID, vehicleId)
                                                    deviceAddress?.let {
                                                        putExtra(com.simi.refillme.service.TripTrackingService.EXTRA_DEVICE_ADDRESS, it)
                                                    }
                                                }
                                                
                                                try {
                                                    context.startForegroundService(intent)
                                                    android.util.Log.d("ManualTrip", "Trip service started successfully")
                                                    successMessage = "Trip started for ${manualVehicleMake} ${manualVehicleModel}!"
                                                } catch (e: Exception) {
                                                    android.util.Log.e("ManualTrip", "Failed to start service", e)
                                                    errorMessage = "Failed to start trip: ${e.message}"
                                                }
                                            } else {
                                                android.util.Log.e("ManualTrip", "Vehicle creation failed: ${response.message}")
                                                errorMessage = response.message ?: "Failed to add vehicle"
                                            }
                                        }.onFailure { e ->
                                            android.util.Log.e("ManualTrip", "API call failed", e)
                                            errorMessage = "Error: ${e.message}"
                                        }
                                    } else {
                                        errorMessage = "Please log in again"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Error: ${e.message}"
                                }
                                
                                // Clear form
                                manualVehicleMake = ""
                                manualVehicleModel = ""
                                manualVehiclePlate = ""
                            }
                        } else {
                            errorMessage = "Please enter at least Make and Model"
                        }
                    },
                    enabled = manualVehicleMake.isNotBlank() && manualVehicleModel.isNotBlank()
                ) {
                    Text("Start Trip")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showManualVehicleEntryDialog = false
                        manualVehicleMake = ""
                        manualVehicleModel = ""
                        manualVehiclePlate = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Manual Trip Entry Dialog
    if (showManualTripEntryDialog) {
        val dateTimeFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
        val selectedManualVehicle = vehicles.find { it.id == manualTripVehicleId }
        val parsedStartTime = parseDateTime(manualStartTimeText)
        val parsedEndTime = parseDateTime(manualEndTimeText)
        val durationMinutes = if (parsedStartTime != null && parsedEndTime != null && parsedEndTime > parsedStartTime) {
            TimeUnit.MILLISECONDS.toMinutes(parsedEndTime - parsedStartTime)
        } else {
            null
        }
        val calculatedDistanceKm = if (manualStartLat != null && manualStartLng != null && manualEndLat != null && manualEndLng != null) {
            calculateDistanceKm(manualStartLat!!, manualStartLng!!, manualEndLat!!, manualEndLng!!)
        } else {
            null
        }
        val overrideDistanceKm = manualDistanceOverride.toDoubleOrNull()
        val distanceKm = overrideDistanceKm ?: calculatedDistanceKm
        val averageSpeedKmh = if (distanceKm != null && durationMinutes != null && durationMinutes > 0) {
            (distanceKm / (durationMinutes / 60.0))
        } else {
            null
        }

        AlertDialog(
            onDismissRequest = { showManualTripEntryDialog = false },
            title = { Text("Add Manual Trip") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ExposedDropdownMenuBox(
                        expanded = manualTripVehicleDropdownExpanded,
                        onExpandedChange = { manualTripVehicleDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedManualVehicle?.let { "${it.make} ${it.model}" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Vehicle *") },
                            trailingIcon = {
                                Icon(
                                    if (manualTripVehicleDropdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = manualTripVehicleDropdownExpanded,
                            onDismissRequest = { manualTripVehicleDropdownExpanded = false }
                        ) {
                            vehicles.forEach { vehicle ->
                                DropdownMenuItem(
                                    text = { Text("${vehicle.make} ${vehicle.model}") },
                                    onClick = {
                                        manualTripVehicleId = vehicle.id
                                        manualTripVehicleDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = startAddressDropdownExpanded,
                        onExpandedChange = { startAddressDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = manualStartAddress,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Start Address *") },
                            trailingIcon = {
                                Icon(
                                    if (startAddressDropdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = startAddressDropdownExpanded,
                            onDismissRequest = { startAddressDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Use current location") },
                                onClick = {
                                    startAddressDropdownExpanded = false
                                    scope.launch {
                                        val option = getCurrentLocationOption(context)
                                        if (option != null) {
                                            manualStartAddress = option.address
                                            manualStartLat = option.lat
                                            manualStartLng = option.lng
                                        } else {
                                            manualTripError = "Unable to fetch current location"
                                        }
                                    }
                                }
                            )
                            startAddressOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.address) },
                                    onClick = {
                                        manualStartAddress = option.address
                                        manualStartLat = option.lat
                                        manualStartLng = option.lng
                                        startAddressDropdownExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Other...") },
                                onClick = {
                                    startAddressDropdownExpanded = false
                                    showStartSearchDialog = true
                                }
                            )
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = endAddressDropdownExpanded,
                        onExpandedChange = { endAddressDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = manualEndAddress,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("End Address *") },
                            trailingIcon = {
                                Icon(
                                    if (endAddressDropdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = endAddressDropdownExpanded,
                            onDismissRequest = { endAddressDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Use current location") },
                                onClick = {
                                    endAddressDropdownExpanded = false
                                    scope.launch {
                                        val option = getCurrentLocationOption(context)
                                        if (option != null) {
                                            manualEndAddress = option.address
                                            manualEndLat = option.lat
                                            manualEndLng = option.lng
                                        } else {
                                            manualTripError = "Unable to fetch current location"
                                        }
                                    }
                                }
                            )
                            endAddressOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.address) },
                                    onClick = {
                                        manualEndAddress = option.address
                                        manualEndLat = option.lat
                                        manualEndLng = option.lng
                                        endAddressDropdownExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Other...") },
                                onClick = {
                                    endAddressDropdownExpanded = false
                                    showEndSearchDialog = true
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = manualStartTimeText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Start Time *") },
                        placeholder = { Text("Select start time") },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    showDateTimePicker(context, parsedStartTime ?: System.currentTimeMillis()) { time ->
                                        manualStartTimeText = dateTimeFormat.format(Date(time))
                                    }
                                }
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualEndTimeText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("End Time *") },
                        placeholder = { Text("Select end time") },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    showDateTimePicker(context, parsedEndTime ?: System.currentTimeMillis()) { time ->
                                        manualEndTimeText = dateTimeFormat.format(Date(time))
                                    }
                                }
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = manualTripNotes,
                        onValueChange = { manualTripNotes = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = manualDistanceOverride,
                        onValueChange = { manualDistanceOverride = it },
                        label = { Text("Distance (km) - optional") },
                        placeholder = { Text(calculatedDistanceKm?.let { String.format("%.2f", it) } ?: "Calculated") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text(
                        text = "Calculated distance: ${calculatedDistanceKm?.let { String.format("%.2f", it) } ?: "-"} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                    Text(
                        text = "Duration: ${durationMinutes?.let { "$it min" } ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                    Text(
                        text = "Avg Speed: ${averageSpeedKmh?.let { String.format("%.1f", it) } ?: "-"} km/h",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )

                    manualTripError?.let { error ->
                        Text(
                            text = error,
                            color = Error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSavingManualTrip,
                    onClick = {
                        manualTripError = null
                        if (manualTripVehicleId == null) {
                            manualTripError = "Please select a vehicle"
                            return@Button
                        }
                        if (manualStartAddress.isBlank() || manualEndAddress.isBlank()) {
                            manualTripError = "Please select start and end addresses"
                            return@Button
                        }
                        if (parsedStartTime == null || parsedEndTime == null) {
                            manualTripError = "Please enter valid start and end times"
                            return@Button
                        }
                        if (parsedEndTime <= parsedStartTime) {
                            manualTripError = "End time must be after start time"
                            return@Button
                        }

                        isSavingManualTrip = true
                        scope.launch {
                            try {
                                val vehicleId = manualTripVehicleId!!
                                val avgSpeed = averageSpeedKmh ?: 0.0
                                val finalDistanceKm = distanceKm ?: 0.0
                                val finalDurationMinutes = durationMinutes ?: 0L

                                // Step 1: Create trip as active (mirrors TripTrackingService)
                                val startTrip = Trip(
                                    id = 0,
                                    vehicleId = vehicleId,
                                    startTime = parsedStartTime,
                                    startLatitude = manualStartLat,
                                    startLongitude = manualStartLng,
                                    startAddress = manualStartAddress,
                                    isActive = true
                                )
                                val newTripId = tripRepository.insertTrip(startTrip)
                                if (newTripId > 0) {
                                    // Step 2: Update with full data and mark as complete
                                    val fullTrip = startTrip.copy(
                                        id = newTripId,
                                        endTime = parsedEndTime,
                                        endLatitude = manualEndLat,
                                        endLongitude = manualEndLng,
                                        endAddress = manualEndAddress,
                                        distanceKm = finalDistanceKm,
                                        averageSpeedKmh = avgSpeed,
                                        maxSpeedKmh = avgSpeed,
                                        durationMinutes = finalDurationMinutes,
                                        tripType = TripType.PERSONAL,
                                        notes = manualTripNotes.ifBlank { null },
                                        isActive = false
                                    )
                                    tripRepository.updateTrip(fullTrip)
                                    // Refresh from server so fuel level estimation uses correct distanceKm
                                    tripRepository.loadTrips()
                                    successMessage = "Manual trip added successfully"
                                    showManualTripEntryDialog = false
                                    manualTripVehicleId = null
                                    manualStartAddress = ""
                                    manualEndAddress = ""
                                    manualStartLat = null
                                    manualStartLng = null
                                    manualEndLat = null
                                    manualEndLng = null
                                    manualStartTimeText = ""
                                    manualEndTimeText = ""
                                    manualTripNotes = ""
                                    manualDistanceOverride = ""
                                } else {
                                    manualTripError = "Failed to save trip. Please try again."
                                }
                            } catch (e: Exception) {
                                manualTripError = "Error: ${e.message}"
                            } finally {
                                isSavingManualTrip = false
                            }
                        }
                    }
                ) {
                    if (isSavingManualTrip) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save Trip")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualTripEntryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showStartSearchDialog) {
        LocationSearchDialog(
            title = "Select Start Location",
            onDismiss = { showStartSearchDialog = false },
            onLocationSelected = { option ->
                manualStartAddress = option.address
                manualStartLat = option.lat
                manualStartLng = option.lng
                showStartSearchDialog = false
            }
        )
    }

    if (showEndSearchDialog) {
        LocationSearchDialog(
            title = "Select End Location",
            onDismiss = { showEndSearchDialog = false },
            onLocationSelected = { option ->
                manualEndAddress = option.address
                manualEndLat = option.lat
                manualEndLng = option.lng
                showEndSearchDialog = false
            }
        )
    }
    
    // Add Vehicle Configuration Dialog
    if (showAddVehicleConfigDialog) {
        val unconfiguredVehicles = vehicles.filter { v -> vehicleAutoTripConfigs.none { it.vehicleId == v.id } }
        AlertDialog(
            onDismissRequest = { showAddVehicleConfigDialog = false },
            title = { Text("Add Vehicle Configuration") },
            text = {
                if (unconfiguredVehicles.isEmpty()) {
                    Text("All vehicles are already configured for auto trip logging.")
                } else {
                    Column {
                        Text(
                            "Choose a vehicle to configure for auto trip logging:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn {
                            items(unconfiguredVehicles) { vehicle ->
                                TextButton(
                                    onClick = {
                                        // Create a new empty config for this vehicle
                                        onUpdateVehicleConfig(VehicleAutoTripConfig(vehicleId = vehicle.id))
                                        showAddVehicleConfigDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("${vehicle.make} ${vehicle.model}")
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddVehicleConfigDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Remove vehicle config password dialog
    if (showRemovePasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isVerifyingRemove) {
                    showRemovePasswordDialog = false
                    configToRemove = null
                    removePassword = ""
                    removePasswordError = false
                }
            },
            title = { Text("Verify Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your password to remove this vehicle configuration")
                    OutlinedTextField(
                        value = removePassword,
                        onValueChange = { removePassword = it; removePasswordError = false },
                        label = { Text("Password") },
                        isError = removePasswordError,
                        enabled = !isVerifyingRemove,
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                    if (removePasswordError) {
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
                        if (removePassword.isNotBlank()) {
                            isVerifyingRemove = true
                            scope.launch {
                                try {
                                    val token = authManager.getToken()
                                    if (token != null) {
                                        val result = authApiService.verifyPassword(token, removePassword)
                                        result.onSuccess { response ->
                                            if (response.success && response.valid) {
                                                val vehicleId = configToRemove!!
                                                showRemovePasswordDialog = false
                                                configToRemove = null
                                                removePassword = ""
                                                removePasswordError = false
                                                isVerifyingRemove = false
                                                onRemoveVehicleConfig(vehicleId)
                                                val stillEnabled = vehicleAutoTripConfigs
                                                    .filter { it.vehicleId != vehicleId }
                                                    .any { it.autoTripEnabled }
                                                if (!stillEnabled) onStopService()
                                            } else {
                                                removePasswordError = true
                                                isVerifyingRemove = false
                                            }
                                        }.onFailure {
                                            removePasswordError = true
                                            isVerifyingRemove = false
                                        }
                                    } else {
                                        removePasswordError = true
                                        isVerifyingRemove = false
                                    }
                                } catch (e: Exception) {
                                    removePasswordError = true
                                    isVerifyingRemove = false
                                }
                            }
                        }
                    },
                    enabled = !isVerifyingRemove && removePassword.isNotBlank()
                ) {
                    if (isVerifyingRemove) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Confirm Remove", color = Error)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRemovePasswordDialog = false
                        configToRemove = null
                        removePassword = ""
                        removePasswordError = false
                        isVerifyingRemove = false
                    },
                    enabled = !isVerifyingRemove
                ) { Text("Cancel") }
            }
        )
    }

    // Permission Request Dialog - Shows immediately if permissions not granted
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { /* Cannot dismiss without granting permissions */ },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Warning
                    )
                    Text("Granting Permissions...")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Auto Trip Logging requires the following permissions:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text("1️⃣ Bluetooth - To detect car connection")
                    Text("2️⃣ Location - To track trip routes")
                    Text("3️⃣ Background Location - For tracking while app is closed")
                    Text("4️⃣ Notifications - To show trip progress")

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "You will be prompted to grant each permission one by one.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ IMPORTANT: When prompted for location, select 'Allow all the time' for background tracking!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Warning,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onNavigateBack) {
                    Text("Cancel")
                }
            }
        )
    }
}

private data class AddressOption(
    val address: String,
    val lat: Double,
    val lng: Double
)

private fun getTopAddressOptions(trips: List<Trip>, isStart: Boolean): List<AddressOption> {
    val options = trips.mapNotNull { trip ->
        if (isStart) {
            val address = trip.startAddress
            val lat = trip.startLatitude
            val lng = trip.startLongitude
            if (address != null && lat != null && lng != null) AddressOption(address, lat, lng) else null
        } else {
            val address = trip.endAddress
            val lat = trip.endLatitude
            val lng = trip.endLongitude
            if (address != null && lat != null && lng != null) AddressOption(address, lat, lng) else null
        }
    }

    return options.groupBy { it.address }
        .toList()
        .sortedByDescending { it.second.size }
        .take(5)
        .map { it.second.first() }
}

private fun parseDateTime(text: String): Long? {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        format.parse(text)?.time
    } catch (e: Exception) {
        null
    }
}

private fun calculateDistanceKm(
    startLat: Double,
    startLng: Double,
    endLat: Double,
    endLng: Double
): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(endLat - startLat)
    val dLng = Math.toRadians(endLng - startLng)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(Math.toRadians(startLat)) * Math.cos(Math.toRadians(endLat)) *
        Math.sin(dLng / 2) * Math.sin(dLng / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return earthRadiusKm * c
}

@Composable
private fun LocationSearchDialog(
    title: String,
    onDismiss: () -> Unit,
    onLocationSelected: (AddressOption) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<AddressOption>>(emptyList()) }
    var selected by remember { mutableStateOf<AddressOption?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search location") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (query.isNotBlank()) {
                            isSearching = true
                            scope.launch {
                                results = searchLocations(context, query)
                                selected = results.firstOrNull()
                                isSearching = false
                            }
                        }
                    },
                    enabled = query.isNotBlank() && !isSearching
                ) {
                    Text(if (isSearching) "Searching..." else "Search")
                }

                if (results.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(results) { option ->
                            TextButton(
                                onClick = { selected = option },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(option.address)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selected?.let { onLocationSelected(it) }
                },
                enabled = selected != null
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun showDateTimePicker(
    context: Context,
    initialTime: Long,
    onSelected: (Long) -> Unit
) {
    val calendar = Calendar.getInstance().apply { timeInMillis = initialTime }
    val datePicker = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val timePicker = android.app.TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val selected = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onSelected(selected.timeInMillis)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            )
            timePicker.show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    datePicker.show()
}

private suspend fun getCurrentLocationOption(context: Context): AddressOption? {
    val helper = com.simi.refillme.utils.LocationHelper(context)
    val location = helper.getCurrentLocation() ?: return null
    val address = helper.getAddressFromLocation(location)
        ?: "Lat: ${String.format("%.6f", location.latitude)}, Lng: ${String.format("%.6f", location.longitude)}"
    return AddressOption(address, location.latitude, location.longitude)
}

private suspend fun searchLocations(context: Context, query: String): List<AddressOption> {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(query, 5) { list ->
                        continuation.resume(list) {}
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(query, 5) ?: emptyList()
            }
            addresses.mapNotNull { address ->
                val line = address.getAddressLine(0)
                if (line != null) AddressOption(line, address.latitude, address.longitude) else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

@Suppress("DEPRECATION")
private fun getPairedBluetoothDevices(context: Context): List<BluetoothDevice> {
    return try {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            emptyList()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    bluetoothAdapter.bondedDevices.toList()
                } else {
                    emptyList()
                }
            } else {
                bluetoothAdapter.bondedDevices.toList()
            }
        }
    } catch (e: Exception) {
        emptyList()
    }
}

@SuppressLint("MissingPermission")
private fun getAvailableWifiNetworks(context: Context): List<String> {
    return try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

        if (wifiManager == null || !wifiManager.isWifiEnabled) {
            return emptyList()
        }

        // Check location permission (required for WiFi scan on Android 6+)
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }

        // Start WiFi scan
        wifiManager.startScan()

        // Get scan results and extract SSIDs
        val scanResults = wifiManager.scanResults
        scanResults
            .mapNotNull { it.SSID }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    } catch (e: Exception) {
        emptyList()
    }
}

private fun checkBluetoothPermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_ADMIN
        ) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun PermissionStatusRow(name: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, style = MaterialTheme.typography.bodyMedium)
        Icon(
            imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (granted) Color(0xFF4CAF50) else Color(0xFFF44336)
        )
    }
}

@Composable
fun TripInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun checkLocationPermissions(context: Context): Boolean {
    val fineLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val backgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    return fineLocation && backgroundLocation
}

private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

@Suppress("MissingPermission")
private fun isBluetoothDeviceConnected(context: Context, deviceAddress: String): Boolean {
    return try {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        
        if (adapter == null || !adapter.isEnabled) {
            return false
        }
        
        val device = adapter.getRemoteDevice(deviceAddress)
        
        // Check if device is connected via Bluetooth profile
        // For a more accurate check, you would need to check specific profiles (A2DP, HFP, etc.)
        // This is a basic check
        val bondState = device.bondState
        bondState == BluetoothDevice.BOND_BONDED
    } catch (e: Exception) {
        false
    }
}

@Composable
private fun TripHistoryCard(
    trip: Trip,
    vehicles: List<Vehicle>,
    onDelete: (Trip) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vehicle = vehicles.find { it.id == trip.vehicleId }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val app = context.applicationContext as RefillMeApplication
    val authManager = app.authManager
    val authApiService = com.simi.refillme.data.auth.AuthApiService.getInstance()

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
                            text = formatTime(trip.startTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
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
                    value = formatDuration(trip.durationMinutes)
                )
                TripStatItem(
                    icon = Icons.Default.Speed,
                    label = "Avg Speed",
                    value = "${"%.0f".format(trip.averageSpeedKmh)} km/h"
                )
            }
        }
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
            },
            title = { Text("Enter Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Please enter your password to confirm deletion.")
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = false
                        },
                        label = { Text("Password") },
                        isError = passwordError,
                        supportingText = if (passwordError) {
                            { Text("Incorrect password", color = Error) }
                        } else null,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (password.isEmpty()) {
                            passwordError = true
                            return@TextButton
                        }

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
                                } else {
                                    passwordError = true
                                    isVerifying = false
                                }
                            } catch (e: Exception) {
                                passwordError = true
                                isVerifying = false
                            }
                        }
                    },
                    enabled = !isVerifying
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Error
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
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatDuration(minutes: Long): String {
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    
    return when {
        hours > 0 && remainingMinutes > 0 -> "${hours}h ${remainingMinutes}m"
        hours > 0 -> "${hours}h"
        else -> "${remainingMinutes}m"
    }
}

@Composable
private fun TripStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
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
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )
    }
}

