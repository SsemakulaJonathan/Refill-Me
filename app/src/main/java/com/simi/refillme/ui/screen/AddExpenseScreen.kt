package com.simi.refillme.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.simi.refillme.data.entity.Vehicle
import com.simi.refillme.data.entity.Expense
import com.simi.refillme.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

data class ExpenseTaskEntry(
    val id: String = UUID.randomUUID().toString(),
    var taskName: String = "",
    var cost: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    vehicles: List<Vehicle>,
    selectedVehicleId: Long?,
    existingExpense: Expense? = null,
    onSave: (vehicleId: Long, date: Long, odometerReading: Int, vendor: String, expenseTasks: List<Pair<String, Double>>, totalCost: Double, notes: String, receiptUri: Uri?) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedVehicle by remember { 
        mutableStateOf(
            (existingExpense?.vehicleId?.let { id -> vehicles.find { it.id == id } }
                ?: selectedVehicleId?.let { id -> vehicles.find { it.id == id } }
                ?: vehicles.minByOrNull { it.id })
        ) 
    }
    var showVehicleDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(existingExpense?.date?.let { Date(it) } ?: Date()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var odometerReading by remember { mutableStateOf(existingExpense?.odometerReading?.toString() ?: "") }
    var vendor by remember { mutableStateOf(existingExpense?.vendor ?: "") }
    var expenseTasks by remember { mutableStateOf(listOf(ExpenseTaskEntry())) }
    var notes by remember { mutableStateOf(existingExpense?.notes ?: "") }
    var receiptUri by remember { mutableStateOf<Uri?>(existingExpense?.receiptImagePath?.let { Uri.parse(it) }) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    // Calculate total cost
    val totalCost = remember(expenseTasks) {
        expenseTasks.sumOf { item ->
            item.cost.toDoubleOrNull() ?: 0.0
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        receiptUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (existingExpense != null) "Edit Expense" else "Add Expense",
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
            // Vehicle Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Vehicle",
                            style = MaterialTheme.typography.labelLarge,
                            color = Primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showVehicleDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedVehicle?.name ?: "Select Vehicle",
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                }
            }

            // Date Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Expense Date",
                            style = MaterialTheme.typography.labelLarge,
                            color = Primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = dateFormat.format(selectedDate),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                        }
                    }
                }
            }

            // Odometer Reading
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Odometer Reading (km)",
                            style = MaterialTheme.typography.labelLarge,
                            color = Primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = odometerReading,
                            onValueChange = { odometerReading = it },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("e.g., 15000") },
                            singleLine = true
                        )
                    }
                }
            }

            // Vendor
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Vendor",
                            style = MaterialTheme.typography.labelLarge,
                            color = Primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = vendor,
                            onValueChange = { vendor = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g., Auto Parts Shop") },
                            singleLine = true
                        )
                    }
                }
            }

            // Expense Tasks Table
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
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
                                text = "Expense Tasks",
                                style = MaterialTheme.typography.labelLarge,
                                color = Primary
                            )
                            IconButton(
                                onClick = {
                                    expenseTasks = expenseTasks + ExpenseTaskEntry()
                                }
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add Expense Task",
                                    tint = Secondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Task Name",
                                modifier = Modifier.weight(1.5f),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Cost (UGX)",
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(40.dp))
                        }

                        // Expense Tasks Rows
                        expenseTasks.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.Gray.copy(alpha = 0.3f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = item.taskName,
                                    onValueChange = {
                                        val newItems = expenseTasks.toMutableList()
                                        newItems[index] = item.copy(taskName = it)
                                        expenseTasks = newItems
                                    },
                                    modifier = Modifier.weight(1.5f),
                                    placeholder = { Text("e.g., Car Wash", style = MaterialTheme.typography.bodySmall) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = item.cost,
                                    onValueChange = {
                                        val newItems = expenseTasks.toMutableList()
                                        newItems[index] = item.copy(cost = it)
                                        expenseTasks = newItems
                                    },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("0", style = MaterialTheme.typography.bodySmall) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (expenseTasks.size > 1) {
                                            expenseTasks = expenseTasks.filterIndexed { i, _ -> i != index }
                                        }
                                    },
                                    enabled = expenseTasks.size > 1
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = if (expenseTasks.size > 1) Color.Red else Color.Gray
                                    )
                                }
                            }
                        }

                        // Total Cost Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                                .background(Success.copy(alpha = 0.1f), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Cost:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "UGX ${String.format("%,.0f", totalCost)}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Primary
                            )
                        }
                    }
                }
            }

            // Notes
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Notes (Optional)",
                            style = MaterialTheme.typography.labelLarge,
                            color = Primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            placeholder = { Text("Add any additional notes...") },
                            maxLines = 5
                        )
                    }
                }
            }

            // Attach Receipt
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Attach Receipt (Optional)",
                            style = MaterialTheme.typography.labelLarge,
                            color = Primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (receiptUri != null) "Receipt attached" else "Attach receipt",
                                modifier = Modifier.weight(1f)
                            )
                            if (receiptUri != null) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Success)
                            }
                        }
                    }
                }
            }

            // Error Message
            errorMessage?.let { error ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
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
                                tint = Color(0xFFD32F2F)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = error,
                                color = Color(0xFFD32F2F),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Save Button
            item {
                Button(
                    onClick = {
                        // Validation
                        when {
                            selectedVehicle == null -> {
                                errorMessage = "Please select a vehicle"
                            }
                            odometerReading.isBlank() || odometerReading.toIntOrNull() == null -> {
                                errorMessage = "Please enter a valid odometer reading"
                            }
                            vendor.isBlank() -> {
                                errorMessage = "Please enter the vendor name"
                            }
                            expenseTasks.all { it.taskName.isBlank() } -> {
                                errorMessage = "Please add at least one expense task"
                            }
                            totalCost <= 0 -> {
                                errorMessage = "Total cost must be greater than zero"
                            }
                            else -> {
                                // Filter out empty expense tasks
                                val validExpenseTasks = expenseTasks
                                    .filter { it.taskName.isNotBlank() && (it.cost.toDoubleOrNull() ?: 0.0) > 0 }
                                    .map { it.taskName to (it.cost.toDoubleOrNull() ?: 0.0) }

                                onSave(
                                    selectedVehicle!!.id,
                                    selectedDate.time,
                                    odometerReading.toInt(),
                                    vendor,
                                    validExpenseTasks,
                                    totalCost,
                                    notes,
                                    receiptUri
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Expense Record", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Vehicle Selection Dialog
    if (showVehicleDialog) {
        AlertDialog(
            onDismissRequest = { showVehicleDialog = false },
            title = { Text("Select Vehicle") },
            text = {
                LazyColumn {
                    items(vehicles.size) { index ->
                        val vehicle = vehicles[index]
                        Text(
                            text = vehicle.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedVehicle = vehicle
                                    showVehicleDialog = false
                                }
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (index < vehicles.size - 1) {
                            Divider()
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
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.time
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = Date(it)
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
            DatePicker(state = datePickerState)
        }
    }
}
