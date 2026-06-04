package com.simi.refillme.ui.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.simi.refillme.data.entity.Vehicle
import com.simi.refillme.ui.theme.*
import com.simi.refillme.ui.viewmodel.RefillFormState
import com.simi.refillme.utils.LocationHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRefillScreen(
    vehicles: List<Vehicle>,
    formState: RefillFormState,
    preferences: com.simi.refillme.data.entity.UserPreferences,
    onFormStateChange: (RefillFormState) -> Unit,
    onSave: () -> Unit,
    onNavigateBack: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var showVehicleDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf<String?>(null) }
    var isFullTank by remember { mutableStateOf(false) }
    var lastModifiedField by remember { mutableStateOf<String?>(null) }
    var isDetectingLocation by remember { mutableStateOf(false) }

    // Update local error state when errorMessage changes
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            showError = errorMessage
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = formState.date
    )
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationHelper = remember { LocationHelper(context) }
    
    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            scope.launch {
                isDetectingLocation = true
                val location = locationHelper.detectFillingStation()
                isDetectingLocation = false
                location?.let {
                    onFormStateChange(formState.copy(location = it))
                }
            }
        } else {
            showError = "Location permission required to detect filling station"
        }
    }
    
    val selectedVehicle = vehicles.find { it.id == formState.vehicleId }

    // Auto-select first created vehicle if none is selected and vehicles are available
    LaunchedEffect(vehicles, formState.vehicleId) {
        if ((formState.vehicleId == null || formState.vehicleId == 0L) && vehicles.isNotEmpty()) {
            val firstVehicle = vehicles.minByOrNull { it.id }
            firstVehicle?.let {
                onFormStateChange(formState.copy(vehicleId = it.id))
            }
        }
    }

    // Handle full tank option: user enters refillAmount from fuel meter, fuelBefore is estimated
    LaunchedEffect(isFullTank, selectedVehicle) {
        if (isFullTank && selectedVehicle != null) {
            val amount = formState.refillAmount.toDoubleOrNull()
            if (amount != null) {
                val computedFuelBefore = (selectedVehicle.tankCapacity - amount).coerceAtLeast(0.0)
                onFormStateChange(formState.copy(
                    fuelBefore = String.format("%.2f", computedFuelBefore)
                ))
            }
        }
    }

    // Auto-calculate when fields change
    LaunchedEffect(formState.refillAmount, formState.unitPrice, formState.totalPrice, lastModifiedField) {
        
        val amount = formState.refillAmount.toDoubleOrNull()
        val unit = formState.unitPrice.toDoubleOrNull()
        val total = formState.totalPrice.toDoubleOrNull()

        // Count how many fields are filled
        val filledCount = listOfNotNull(amount, unit, total).size

        if (filledCount == 2) {
            val newState = when {
                // Calculate refill amount: totalPrice / unitPrice
                amount == null && unit != null && unit > 0 && total != null -> {
                    formState.copy(refillAmount = String.format("%.2f", total / unit))
                }
                // Calculate unit price: totalPrice / refillAmount
                unit == null && amount != null && amount > 0 && total != null -> {
                    formState.copy(unitPrice = String.format("%.2f", total / amount))
                }
                // Calculate total price: refillAmount * unitPrice
                total == null && amount != null && unit != null -> {
                    formState.copy(totalPrice = String.format("%.2f", amount * unit))
                }
                else -> null
            }
            
            if (newState != null && newState != formState) {
                onFormStateChange(newState)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (formState.id != null) "Edit Refill" else "Add Refill",
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
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Vehicle Selection Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                onClick = { showVehicleDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Vehicle",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                        Text(
                            text = selectedVehicle?.name ?: "Select a vehicle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedVehicle != null) {
                            Text(
                                text = "Current: ${String.format("%.1f", selectedVehicle.currentFuelLevel)}L / ${selectedVehicle.tankCapacity}L",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = OnSurfaceVariant
                    )
                }
            }

            // Date Selection Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                onClick = { showDatePicker = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Refill Date",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                        Text(
                            text = dateFormatter.format(Date(formState.date)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = OnSurfaceVariant
                    )
                }
            }

            // Refill Details Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Fuel Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = formState.fuelBefore,
                        onValueChange = { if (!isFullTank) onFormStateChange(formState.copy(fuelBefore = it)) },
                        label = { Text(if (isFullTank) "Fuel Before Refill (L) – estimated" else "Fuel Before Refill (L)") },
                        leadingIcon = {
                            Icon(Icons.Default.Speed, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isFullTank,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            focusedLabelColor = Primary
                        )
                    )

                    OutlinedTextField(
                        value = formState.refillAmount,
                        onValueChange = { value ->
                            lastModifiedField = "refillAmount"
                            if (isFullTank && selectedVehicle != null) {
                                val amount = value.toDoubleOrNull()
                                val newFuelBefore = if (amount != null)
                                    String.format("%.2f", (selectedVehicle.tankCapacity - amount).coerceAtLeast(0.0))
                                else formState.fuelBefore
                                onFormStateChange(formState.copy(refillAmount = value, fuelBefore = newFuelBefore))
                            } else {
                                onFormStateChange(formState.copy(refillAmount = value))
                            }
                        },
                        label = { Text("Refill Amount (L)") },
                        leadingIcon = {
                            Icon(Icons.Default.LocalGasStation, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            focusedLabelColor = Primary
                        ),
                        supportingText = {
                            if (selectedVehicle != null) {
                                if (isFullTank) {
                                    Text("After refill: ${String.format("%.1f", selectedVehicle.tankCapacity)}L (Full)")
                                } else {
                                    val fuelBefore = formState.fuelBefore.toDoubleOrNull() ?: 0.0
                                    val refillAmount = formState.refillAmount.toDoubleOrNull() ?: 0.0
                                    val fuelAfter = fuelBefore + refillAmount
                                    Text("After refill: ${String.format("%.2f", fuelAfter)}L")
                                }
                            }
                        }
                    )

                    // Full Tank Checkbox
                    if (selectedVehicle != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isFullTank,
                                onCheckedChange = { isFullTank = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Primary
                                )
                            )
                            Text(
                                text = "Full tank – enter litres from fuel meter, fuel before is estimated",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = formState.unitPrice,
                        onValueChange = { 
                            lastModifiedField = "unitPrice"
                            onFormStateChange(formState.copy(unitPrice = it))
                        },
                        label = { Text("Unit Price (${preferences.currencySymbol}/L)") },
                        leadingIcon = {
                            Icon(Icons.Default.AttachMoney, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            focusedLabelColor = Primary
                        )
                    )

                    OutlinedTextField(
                        value = formState.totalPrice,
                        onValueChange = { 
                            lastModifiedField = "totalPrice"
                            onFormStateChange(formState.copy(totalPrice = it))
                        },
                        label = { Text("Total Price (${preferences.currencySymbol})") },
                        leadingIcon = {
                            Icon(Icons.Default.Paid, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            focusedLabelColor = Primary
                        )
                    )
                    
                    // Calculation hint
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Info.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Info,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enter any 2 of the 3 values above, and the third will be calculated automatically",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurface
                            )
                        }
                    }
                }
            }

            // Additional Information Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Additional Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = formState.odometerReading,
                        onValueChange = { onFormStateChange(formState.copy(odometerReading = it)) },
                        label = { Text("Odometer Reading *") },
                        leadingIcon = {
                            Icon(Icons.Default.Speed, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = formState.location,
                        onValueChange = { onFormStateChange(formState.copy(location = it)) },
                        label = { Text("Filling Station") },
                        leadingIcon = {
                            Icon(Icons.Default.LocalGasStation, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (locationHelper.hasLocationPermission()) {
                                        scope.launch {
                                            isDetectingLocation = true
                                            val location = locationHelper.detectFillingStation()
                                            isDetectingLocation = false
                                            location?.let {
                                                onFormStateChange(formState.copy(location = it))
                                            } ?: run {
                                                showError = "Unable to detect location. Please check GPS."
                                            }
                                        }
                                    } else {
                                        locationPermissionLauncher.launch(LocationHelper.REQUIRED_PERMISSIONS)
                                    }
                                },
                                enabled = !isDetectingLocation
                            ) {
                                if (isDetectingLocation) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = Primary
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.MyLocation,
                                        contentDescription = "Detect Location",
                                        tint = Primary
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("e.g., Shell Kampala Road") }
                    )

                    OutlinedTextField(
                        value = formState.notes,
                        onValueChange = { onFormStateChange(formState.copy(notes = it)) },
                        label = { Text("Notes") },
                        leadingIcon = {
                            Icon(Icons.Default.Notes, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Error Display Card
            if (showError != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = Error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = showError!!,
                            color = Error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showError = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Error)
                        }
                    }
                }
            }

            // Save Button
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isLoading) "Saving..." else "Save Refill",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Vehicle Selection Dialog
    if (showVehicleDialog) {
        AlertDialog(
            onDismissRequest = { showVehicleDialog = false },
            title = { Text("Select Vehicle") },
            text = {
                Column {
                    vehicles.forEach { vehicle ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            onClick = {
                                onFormStateChange(
                                    formState.copy(
                                        vehicleId = vehicle.id,
                                        fuelBefore = String.format("%.2f", vehicle.currentFuelLevel)
                                    )
                                )
                                showVehicleDialog = false
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = Primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = vehicle.name,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = vehicle.model,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVehicleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate ->
                            onFormStateChange(formState.copy(date = selectedDate))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = true
            )
        }
    }

    // Error Snackbar
    showError?.let { error ->
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { showError = null }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(error)
        }
    }
}
