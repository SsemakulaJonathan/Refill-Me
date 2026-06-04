package com.simi.refillme.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import coil.compose.AsyncImage
import com.simi.refillme.data.entity.FuelType
import com.simi.refillme.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleScreen(
    vehicleToEdit: com.simi.refillme.data.entity.Vehicle? = null,
    onSave: (
        name: String,
        make: String,
        model: String,
        year: Int,
        licensePlate: String,
        vin: String?,
        insuranceNumber: String?,
        fuelType: String,
        tankCapacity: Double,
        photoUri: Uri?,
        document1Uri: Uri?,
        document2Uri: Uri?,
        document3Uri: Uri?,
        notes: String?
    ) -> Unit,
    onNavigateBack: () -> Unit,
    isLoading: Boolean = false,
    apiError: String? = null,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var vin by remember { mutableStateOf("") }
    var insuranceNumber by remember { mutableStateOf("") }
    var selectedFuelType by remember { mutableStateOf(FuelType.PETROL) }
    var tankCapacity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showFuelTypeMenu by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf<String?>(null) }
    var showPhotoSearchDialog by remember { mutableStateOf(false) }
    
    // Photo and documents URIs
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var document1Uri by remember { mutableStateOf<Uri?>(null) }
    var document2Uri by remember { mutableStateOf<Uri?>(null) }
    var document3Uri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(vehicleToEdit) {
        vehicleToEdit?.let { vehicle ->
            name = vehicle.name
            make = vehicle.make
            model = vehicle.model
            year = vehicle.year.toString()
            licensePlate = vehicle.licensePlate
            vin = vehicle.vin ?: ""
            insuranceNumber = vehicle.insuranceNumber ?: ""
            selectedFuelType = runCatching { FuelType.valueOf(vehicle.fuelType) }
                .getOrDefault(FuelType.PETROL)
            tankCapacity = vehicle.tankCapacity.toString()
            notes = vehicle.notes ?: ""

            photoUrl = vehicle.photoUri
            photoUri = vehicle.photoUri?.let { Uri.parse(it) }
            document1Uri = vehicle.document1Uri?.let { Uri.parse(it) }
            document2Uri = vehicle.document2Uri?.let { Uri.parse(it) }
            document3Uri = vehicle.document3Uri?.let { Uri.parse(it) }
        }
    }
    
    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> photoUri = uri }
    
    // Document pickers
    val document1Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> document1Uri = uri }
    
    val document2Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> document2Uri = uri }
    
    val document3Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> document3Uri = uri }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // TODO: Convert bitmap to URI and save
        // For now, we'll just use photo picker
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (vehicleToEdit != null) "Edit Vehicle" else "Add Vehicle",
                        style = MaterialTheme.typography.headlineSmall,
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
                .padding(paddingValues)
                .background(Background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error message (local validation)
            showError?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = Error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(error, color = Error)
                    }
                }
            }

            // API Error message
            apiError?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = Error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Failed to save vehicle", color = Error, fontWeight = FontWeight.Bold)
                            Text(error, color = Error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            
            // Vehicle Photo Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Vehicle Photo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.1f))
                            .border(2.dp, Primary.copy(alpha = 0.3f), CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri != null) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Vehicle Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (photoUrl != null) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Vehicle Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gallery")
                        }
                        
                        OutlinedButton(
                            onClick = { showPhotoSearchDialog = true }
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Search Online")
                        }
                    }
                }
            }
            
            // Photo Search Dialog
            if (showPhotoSearchDialog) {
                com.simi.refillme.ui.components.PhotoSearchDialog(
                    onDismiss = { showPhotoSearchDialog = false },
                    onPhotoSelected = { url ->
                        photoUrl = url
                        photoUri = null // Clear local URI if online photo selected
                    }
                )
            }
            
            // Basic Information
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Basic Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Vehicle Name/Nickname *") },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("e.g., My Toyota") }
                    )
                    
                    OutlinedTextField(
                        value = make,
                        onValueChange = { make = it },
                        label = { Text("Make *") },
                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("e.g., Toyota, Honda") }
                    )
                    
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Model *") },
                        leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("e.g., Camry, Civic") }
                    )
                    
                    OutlinedTextField(
                        value = year,
                        onValueChange = { if (it.length <= 4) year = it },
                        label = { Text("Year *") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("e.g., 2020") }
                    )
                }
            }
            
            // Legal Information
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Legal Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedTextField(
                        value = licensePlate,
                        onValueChange = { licensePlate = it.uppercase() },
                        label = { Text("License Plate *") },
                        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("e.g., UAG 123A") }
                    )
                    
                    OutlinedTextField(
                        value = vin,
                        onValueChange = { vin = it.uppercase() },
                        label = { Text("VIN (Vehicle Identification Number)") },
                        leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("17-character VIN") }
                    )
                    
                    OutlinedTextField(
                        value = insuranceNumber,
                        onValueChange = { insuranceNumber = it },
                        label = { Text("Insurance Policy Number") },
                        leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
            
            // Fuel & Capacity
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Fuel Specifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Fuel Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = showFuelTypeMenu,
                        onExpandedChange = { showFuelTypeMenu = it }
                    ) {
                        OutlinedTextField(
                            value = selectedFuelType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Fuel Type *") },
                            leadingIcon = { Icon(Icons.Default.LocalGasStation, contentDescription = null) },
                            trailingIcon = {
                                Icon(
                                    if (showFuelTypeMenu) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                focusedLabelColor = Primary
                            )
                        )
                        
                        ExposedDropdownMenu(
                            expanded = showFuelTypeMenu,
                            onDismissRequest = { showFuelTypeMenu = false }
                        ) {
                            FuelType.values().forEach { fuelType ->
                                DropdownMenuItem(
                                    text = { Text(fuelType.displayName) },
                                    onClick = {
                                        selectedFuelType = fuelType
                                        showFuelTypeMenu = false
                                    },
                                    leadingIcon = {
                                        if (selectedFuelType == fuelType) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Primary)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = tankCapacity,
                        onValueChange = { tankCapacity = it },
                        label = { Text("Tank Capacity (Liters) *") },
                        leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("e.g., 60") }
                    )
                }
            }
            
            // Documents
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Documents (Optional)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        "Upload up to 3 documents (e.g., logbook, transfer documents, insurance)",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                    
                    DocumentUploadButton(
                        label = "Document 1",
                        uri = document1Uri,
                        onSelectDocument = { document1Launcher.launch("*/*") },
                        onRemove = { document1Uri = null }
                    )
                    
                    DocumentUploadButton(
                        label = "Document 2",
                        uri = document2Uri,
                        onSelectDocument = { document2Launcher.launch("*/*") },
                        onRemove = { document2Uri = null }
                    )
                    
                    DocumentUploadButton(
                        label = "Document 3",
                        uri = document3Uri,
                        onSelectDocument = { document3Launcher.launch("*/*") },
                        onRemove = { document3Uri = null }
                    )
                }
            }
            
            // Notes
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Additional Notes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("Any additional information about this vehicle...") }
                    )
                }
            }
            
            // Save Button
            Button(
                onClick = {
                    // Validation
                    when {
                        name.isBlank() -> showError = "Vehicle name is required"
                        make.isBlank() -> showError = "Make is required"
                        model.isBlank() -> showError = "Model is required"
                        year.isBlank() || year.toIntOrNull() == null -> showError = "Valid year is required"
                        licensePlate.isBlank() -> showError = "License plate is required"
                        tankCapacity.isBlank() || tankCapacity.toDoubleOrNull() == null || tankCapacity.toDouble() <= 0 ->
                            showError = "Valid tank capacity is required"
                        else -> {
                            // Use photoUrl if available, otherwise photoUri
                            val finalPhotoUri = if (photoUrl != null) {
                                android.net.Uri.parse(photoUrl)
                            } else {
                                photoUri
                            }

                            onSave(
                                name,
                                make,
                                model,
                                year.toInt(),
                                licensePlate,
                                vin.ifBlank { null },
                                insuranceNumber.ifBlank { null },
                                selectedFuelType.name,
                                tankCapacity.toDouble(),
                                finalPhotoUri,
                                document1Uri,
                                document2Uri,
                                document3Uri,
                                notes.ifBlank { null }
                            )
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...", style = MaterialTheme.typography.titleMedium)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Vehicle", style = MaterialTheme.typography.titleMedium)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DocumentUploadButton(
    label: String,
    uri: Uri?,
    onSelectDocument: () -> Unit,
    onRemove: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (uri != null) Icons.Default.CheckCircle else Icons.Default.Description,
                    contentDescription = null,
                    tint = if (uri != null) Success else OnSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (uri != null) {
                        Text(
                            "Document attached",
                            style = MaterialTheme.typography.bodySmall,
                            color = Success
                        )
                    }
                }
            }
            
            Row {
                if (uri != null) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Error)
                    }
                }
                OutlinedButton(onClick = onSelectDocument) {
                    Text(if (uri == null) "Upload" else "Change")
                }
            }
        }
    }
}
