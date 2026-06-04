package com.simi.refillme.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.simi.refillme.data.entity.PersonalExpense
import com.simi.refillme.data.entity.PersonalExpenseCategory
import com.simi.refillme.data.entity.PersonalExpenseItem
import com.simi.refillme.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// Preset palette for new categories
private val CATEGORY_PALETTE = listOf(
    "#F44336", "#E91E63", "#9C27B0", "#673AB7",
    "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
    "#009688", "#4CAF50", "#8BC34A", "#FFC107",
    "#FF9800", "#FF5722", "#795548", "#607D8B",
    "#9E9E9E", "#000000"
)

private val AddAccent = Color(0xFF6C3483)

/** UI-only model for a line-item row in the form. */
data class PersonalItemEntry(
    val entryId: String = UUID.randomUUID().toString(),
    val name: String = "",
    val categoryId: Long? = null,
    val costText: String = ""
)

// ─────────────────────────────────────────────────────────────────────────────
// Add / Edit screen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonalExpenseScreen(
    categories: List<PersonalExpenseCategory>,
    existingExpense: PersonalExpense? = null,
    existingItems: List<PersonalExpenseItem> = emptyList(),
    onSave: (PersonalExpense, List<PersonalExpenseItem>) -> Unit,
    onAddCategory: (name: String, colorHex: String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditing = existingExpense != null

    var selectedDate         by remember { mutableStateOf(existingExpense?.date?.let { Date(it) } ?: Date()) }
    var showDatePicker       by remember { mutableStateOf(false) }
    var vendor               by remember { mutableStateOf(existingExpense?.vendor ?: "") }
    var selectedCategoryId   by remember { mutableStateOf(existingExpense?.categoryId) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var notes                by remember { mutableStateOf(existingExpense?.notes ?: "") }
    var receiptUri           by remember {
        mutableStateOf<Uri?>(existingExpense?.receiptImagePath?.let { Uri.parse(it) })
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var lineItems by remember {
        mutableStateOf(
            if (existingItems.isNotEmpty()) {
                existingItems.map { item ->
                    PersonalItemEntry(
                        name       = item.name,
                        categoryId = item.categoryId,
                        costText   = if (item.cost % 1 == 0.0) item.cost.toLong().toString()
                                     else item.cost.toString()
                    )
                }
            } else listOf(PersonalItemEntry())
        )
    }

    val totalCost = remember(lineItems) {
        lineItems.sumOf { it.costText.toDoubleOrNull() ?: 0.0 }
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        receiptUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "Edit Expense" else "Add Expense",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AddAccent,
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
            // ── Category ──────────────────────────────────────────────────────
            item {
                SectionCard(title = "Category") {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showCategoryDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val cat = categories.find { it.id == selectedCategoryId }
                                if (cat != null) {
                                    Box(
                                        Modifier.size(14.dp)
                                            .background(parseHexColor(cat.colorHex), CircleShape)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(cat.name, Modifier.weight(1f))
                                } else {
                                    Text("Select category (optional)", Modifier.weight(1f))
                                }
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            DropdownMenu(
                                expanded = showCategoryDropdown,
                                onDismissRequest = { showCategoryDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("None", color = OnSurfaceVariant) },
                                    onClick = { selectedCategoryId = null; showCategoryDropdown = false }
                                )
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    Modifier.size(12.dp)
                                                        .background(parseHexColor(cat.colorHex), CircleShape)
                                                )
                                                Text(cat.name)
                                            }
                                        },
                                        onClick = {
                                            selectedCategoryId = cat.id
                                            showCategoryDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                        // Add category button
                        IconButton(onClick = { showAddCategoryDialog = true }) {
                            Icon(Icons.Default.AddCircle, "Add Category", tint = AddAccent)
                        }
                    }
                }
            }

            // ── Date ──────────────────────────────────────────────────────────
            item {
                SectionCard(title = "Expense Date") {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CalendarToday, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(dateFormat.format(selectedDate), Modifier.weight(1f))
                    }
                }
            }

            // ── Vendor ────────────────────────────────────────────────────────
            item {
                SectionCard(title = "Vendor / Store") {
                    OutlinedTextField(
                        value = vendor,
                        onValueChange = { vendor = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g., Shoprite, Amazon, Restaurant Name") },
                        singleLine = true
                    )
                }
            }

            // ── Line items ────────────────────────────────────────────────────
            item {
                SectionCard(
                    title = "Expense Items",
                    trailingAction = {
                        IconButton(onClick = { lineItems = lineItems + PersonalItemEntry() }) {
                            Icon(Icons.Default.Add, "Add item", tint = AddAccent)
                        }
                    }
                ) {
                    // Table header
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(AddAccent.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("Name", Modifier.weight(2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("Category", Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("Cost", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(28.dp))
                    }
                    Spacer(Modifier.height(6.dp))

                    lineItems.forEachIndexed { index, item ->
                        PersonalItemRow(
                            item = item,
                            categories = categories,
                            onUpdate = { updated ->
                                lineItems = lineItems.toMutableList().also { it[index] = updated }
                            },
                            onDelete = if (lineItems.size > 1) {
                                { lineItems = lineItems.toMutableList().also { it.removeAt(index) } }
                            } else null
                        )
                        if (index < lineItems.lastIndex) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp), color = CardBorder)
                        }
                    }
                }
            }

            // ── Total ─────────────────────────────────────────────────────────
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AddAccent.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AddAccent.copy(alpha = 0.35f))
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "TOTAL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AddAccent
                        )
                        Text(
                            String.format("%.2f", totalCost),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = AddAccent
                        )
                    }
                }
            }

            // ── Notes ─────────────────────────────────────────────────────────
            item {
                SectionCard(title = "Notes (optional)") {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Any extra details…") },
                        minLines = 2,
                        maxLines = 4
                    )
                }
            }

            // ── Receipt ───────────────────────────────────────────────────────
            item {
                SectionCard(title = "Receipt (optional)") {
                    OutlinedButton(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (receiptUri != null) Icons.Default.CheckCircle else Icons.Default.AttachFile,
                            null,
                            tint = if (receiptUri != null) Success else AddAccent
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (receiptUri != null) "Receipt attached ✓" else "Attach Receipt Image",
                            color = if (receiptUri != null) Success else AddAccent,
                            modifier = Modifier.weight(1f)
                        )
                        if (receiptUri != null) {
                            IconButton(
                                onClick = { receiptUri = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, "Remove", Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // ── Error ─────────────────────────────────────────────────────────
            errorMessage?.let { msg ->
                item {
                    Text(
                        msg, color = Error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // ── Save button ───────────────────────────────────────────────────
            item {
                Button(
                    onClick = {
                        if (vendor.isBlank()) {
                            errorMessage = "Please enter a vendor / store name"
                            return@Button
                        }
                        val valid = lineItems.filter {
                            it.name.isNotBlank() && (it.costText.toDoubleOrNull() ?: 0.0) > 0
                        }
                        if (valid.isEmpty()) {
                            errorMessage = "Add at least one item with a name and cost"
                            return@Button
                        }
                        errorMessage = null
                        val expense = PersonalExpense(
                            id               = existingExpense?.id ?: 0L,
                            date             = selectedDate.time,
                            vendor           = vendor.trim(),
                            categoryId       = selectedCategoryId,
                            totalCost        = totalCost,
                            notes            = notes.trim().ifBlank { null },
                            receiptImagePath = receiptUri?.toString()
                        )
                        val items = valid.map { entry ->
                            PersonalExpenseItem(
                                expenseId  = existingExpense?.id ?: 0L,
                                name       = entry.name.trim(),
                                categoryId = entry.categoryId,
                                cost       = entry.costText.toDoubleOrNull() ?: 0.0
                            )
                        }
                        onSave(expense, items)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AddAccent)
                ) {
                    Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isEditing) "Update Expense" else "Save Expense",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // ── Date picker dialog ────────────────────────────────────────────────────
    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = selectedDate.time)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { selectedDate = Date(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = state) }
    }

    // ── Add category dialog ───────────────────────────────────────────────────
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onAdd = { name, colorHex ->
                onAddCategory(name, colorHex)
                showAddCategoryDialog = false
            },
            onDismiss = { showAddCategoryDialog = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Line-item row
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalItemRow(
    item: PersonalItemEntry,
    categories: List<PersonalExpenseCategory>,
    onUpdate: (PersonalItemEntry) -> Unit,
    onDelete: (() -> Unit)?
) {
    var showCatDropdown by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Name
        OutlinedTextField(
            value = item.name,
            onValueChange = { onUpdate(item.copy(name = it)) },
            modifier = Modifier.weight(2f),
            placeholder = { Text("Item", style = MaterialTheme.typography.bodySmall) },
            textStyle = MaterialTheme.typography.bodySmall,
            singleLine = true
        )

        // Category (compact dropdown)
        Box(Modifier.weight(1.5f)) {
            val cat = categories.find { it.id == item.categoryId }
            OutlinedButton(
                onClick = { showCatDropdown = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                if (cat != null) {
                    Box(Modifier.size(8.dp).background(parseHexColor(cat.colorHex), CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        cat.name,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text("Cat.", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                }
            }
            DropdownMenu(expanded = showCatDropdown, onDismissRequest = { showCatDropdown = false }) {
                DropdownMenuItem(
                    text = { Text("None", style = MaterialTheme.typography.bodySmall) },
                    onClick = { onUpdate(item.copy(categoryId = null)); showCatDropdown = false }
                )
                categories.forEach { c ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(Modifier.size(10.dp).background(parseHexColor(c.colorHex), CircleShape))
                                Text(c.name, style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        onClick = { onUpdate(item.copy(categoryId = c.id)); showCatDropdown = false }
                    )
                }
            }
        }

        // Cost
        OutlinedTextField(
            value = item.costText,
            onValueChange = { onUpdate(item.copy(costText = it)) },
            modifier = Modifier.weight(1f),
            placeholder = { Text("0", style = MaterialTheme.typography.bodySmall) },
            textStyle = MaterialTheme.typography.bodySmall,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        // Delete
        IconButton(
            onClick = { onDelete?.invoke() },
            modifier = Modifier.size(32.dp),
            enabled = onDelete != null
        ) {
            Icon(
                Icons.Default.Close,
                null,
                Modifier.size(16.dp),
                tint = if (onDelete != null) Error else Color.Transparent
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Add-category dialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AddCategoryDialog(
    onAdd: (name: String, colorHex: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name          by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(CATEGORY_PALETTE[5]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("Pick a colour", style = MaterialTheme.typography.labelLarge)
                CATEGORY_PALETTE.chunked(6).forEach { rowColors ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowColors.forEach { hex ->
                            val color = parseHexColor(hex)
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (hex == selectedColor)
                                            Modifier.border(3.dp, Color.White, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { selectedColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (hex == selectedColor) {
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onAdd(name.trim(), selectedColor) },
                enabled = name.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared card wrapper
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionCard(
    title: String,
    trailingAction: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            if (trailingAction != null) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, style = MaterialTheme.typography.labelLarge, color = AddAccent)
                    trailingAction()
                }
            } else {
                Text(title, style = MaterialTheme.typography.labelLarge, color = AddAccent)
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
