package com.simi.refillme.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.simi.refillme.RefillMeApplication
import com.simi.refillme.data.entity.PersonalExpense
import com.simi.refillme.data.entity.PersonalExpenseCategory
import com.simi.refillme.data.entity.PersonalExpenseItem
import com.simi.refillme.data.entity.Expense
import com.simi.refillme.data.entity.Refill
import com.simi.refillme.data.entity.Service
import com.simi.refillme.data.entity.UserPreferences
import com.simi.refillme.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── Accent colour shared across this feature ─────────────────────────────────
private val PersonalAccent      = Color(0xFF6C3483)
private val PersonalAccentLight = Color(0xFF9B59B6)

// ─────────────────────────────────────────────────────────────────────────────
// Main screen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalExpensesScreen(
    expenses: List<PersonalExpense>,
    allItems: List<PersonalExpenseItem>,
    categories: List<PersonalExpenseCategory>,
    allRefills: List<Refill>,
    allServices: List<Service> = emptyList(),
    allVehicleExpenses: List<Expense> = emptyList(),
    preferences: UserPreferences,
    onAddExpense: () -> Unit,
    onEditExpense: (Long) -> Unit,
    onDeleteExpense: (PersonalExpense) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Analytics")
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val itemsByExpenseId = remember(allItems) { allItems.groupBy { it.expenseId } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Personal Expenses",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onAddExpense) {
                        Icon(Icons.Default.Add, contentDescription = "Add Expense", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PersonalAccent,
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
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Surface,
                contentColor = PersonalAccent
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> PersonalOverviewTab(
                    expenses = expenses,
                    itemsByExpenseId = itemsByExpenseId,
                    categories = categories,
                    allRefills = allRefills,
                    preferences = preferences,
                    dateFormat = dateFormat,
                    onAddExpense = onAddExpense,
                    onEditExpense = onEditExpense,
                    onDeleteExpense = onDeleteExpense
                )
                1 -> PersonalAnalyticsTab(
                    expenses = expenses,
                    itemsByExpenseId = itemsByExpenseId,
                    categories = categories,
                    allRefills = allRefills,
                    allServices = allServices,
                    allVehicleExpenses = allVehicleExpenses,
                    preferences = preferences
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Overview tab
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PersonalOverviewTab(
    expenses: List<PersonalExpense>,
    itemsByExpenseId: Map<Long, List<PersonalExpenseItem>>,
    categories: List<PersonalExpenseCategory>,
    allRefills: List<Refill>,
    preferences: UserPreferences,
    dateFormat: SimpleDateFormat,
    onAddExpense: () -> Unit,
    onEditExpense: (Long) -> Unit,
    onDeleteExpense: (PersonalExpense) -> Unit
) {
    var showAll by remember { mutableStateOf(false) }

    val cal = Calendar.getInstance()
    val thisMonth = cal.get(Calendar.MONTH)
    val thisYear  = cal.get(Calendar.YEAR)

    val thisMonthTotal = remember(expenses) {
        expenses.filter {
            val c = Calendar.getInstance(); c.timeInMillis = it.date
            c.get(Calendar.MONTH) == thisMonth && c.get(Calendar.YEAR) == thisYear
        }.sumOf { it.totalCost }
    }
    val cutoff30 = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L
    val expenses30d = remember(expenses) {
        expenses.filter { it.date >= cutoff30 }.sumOf { it.totalCost }
    }
    val allTimeTotal = remember(expenses) { expenses.sumOf { it.totalCost } }
    // Sorted newest-first; recent shows top 5, show-all shows everything
    val sortedExpenses = remember(expenses) { expenses.sortedByDescending { it.date } }
    val recentExpenses = sortedExpenses.take(5)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Stat circles ─────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCircle(
                    value = String.format(preferences.priceFormat, thisMonthTotal),
                    label = "This\nMonth",
                    gradient = Brush.linearGradient(listOf(PersonalAccent, PersonalAccentLight))
                )
                StatCircle(
                    value = String.format(preferences.priceFormat, expenses30d),
                    label = "Expenses\n(30 days)",
                    gradient = Brush.linearGradient(listOf(Color(0xFFE67E22), Color(0xFFF39C12)))
                )
                StatCircle(
                    value = String.format(preferences.priceFormat, allTimeTotal),
                    label = "All\nTime",
                    gradient = Brush.linearGradient(listOf(Color(0xFF16A085), Color(0xFF1ABC9C)))
                )
            }
        }

        // ── Section header ────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Expenses",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (expenses.isNotEmpty()) {
                    TextButton(onClick = { showAll = !showAll }) {
                        Text(if (showAll) "Show Less" else "Show All")
                    }
                }
            }
        }

        // ── Expense cards or empty state ──────────────────────────────────────
        if (expenses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.AccountBalance, null, Modifier.size(52.dp), tint = OnSurfaceVariant)
                        Text(
                            "No personal expenses yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = OnSurfaceVariant
                        )
                        Button(
                            onClick = onAddExpense,
                            colors = ButtonDefaults.buttonColors(containerColor = PersonalAccent)
                        ) {
                            Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Record Expense")
                        }
                    }
                }
            }
        } else if (!showAll) {
            // ── Recent: top 5 ────────────────────────────────────────────────
            items(recentExpenses, key = { it.id }) { expense ->
                PersonalExpenseCard(
                    expense = expense,
                    items = itemsByExpenseId[expense.id] ?: emptyList(),
                    categories = categories,
                    preferences = preferences,
                    dateFormat = dateFormat,
                    onEdit = { onEditExpense(expense.id) },
                    onDelete = { onDeleteExpense(expense) }
                )
            }
        } else {
            // ── All Expenses sorted by date ───────────────────────────────────
            item {
                Text(
                    "All Expenses",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(sortedExpenses, key = { it.id }) { expense ->
                PersonalExpenseCard(
                    expense = expense,
                    items = itemsByExpenseId[expense.id] ?: emptyList(),
                    categories = categories,
                    preferences = preferences,
                    dateFormat = dateFormat,
                    onEdit = { onEditExpense(expense.id) },
                    onDelete = { onDeleteExpense(expense) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Analytics tab
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalAnalyticsTab(
    expenses: List<PersonalExpense>,
    itemsByExpenseId: Map<Long, List<PersonalExpenseItem>>,
    categories: List<PersonalExpenseCategory>,
    allRefills: List<Refill>,
    allServices: List<Service>,
    allVehicleExpenses: List<Expense>,
    preferences: UserPreferences
) {
    var selectedRange by remember { mutableStateOf("This Month") }
    var showRangeMenu by remember { mutableStateOf(false) }
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val dateRanges = listOf("This Month", "Last Month", "Last 7 Days", "This Year", "All Time", "Custom Dates")
    val displayFmt = remember { SimpleDateFormat("d MMM yy", Locale.getDefault()) }

    val filteredExpenses = remember(expenses, selectedRange, customStartDate, customEndDate) {
        personalExpensesInRange(expenses, selectedRange, customStartDate, customEndDate)
    }
    val filteredRefills = remember(allRefills, selectedRange, customStartDate, customEndDate) {
        refillsInRange(allRefills, selectedRange, customStartDate, customEndDate)
    }
    val filteredServices = remember(allServices, selectedRange, customStartDate, customEndDate) {
        servicesInRange(allServices, selectedRange, customStartDate, customEndDate)
    }
    val filteredVehicleExpenses = remember(allVehicleExpenses, selectedRange, customStartDate, customEndDate) {
        vehicleExpensesInRange(allVehicleExpenses, selectedRange, customStartDate, customEndDate)
    }

    val personalTotal      = filteredExpenses.sumOf { it.totalCost }
    val fuelTotal          = filteredRefills.sumOf { it.totalPrice }
    val servicesTotal      = filteredServices.sumOf { it.totalCost }
    val vehicleOtherTotal  = filteredVehicleExpenses.sumOf { it.totalCost }
    val vehicleTotal       = fuelTotal + servicesTotal + vehicleOtherTotal
    val grandTotal         = personalTotal + vehicleTotal

    // Build category breakdown: item category → amount
    val categoryBreakdown: List<Pair<Long?, Double>> = remember(filteredExpenses, itemsByExpenseId) {
        val breakdown = mutableMapOf<Long?, Double>()
        filteredExpenses.forEach { expense ->
            val expenseItems = itemsByExpenseId[expense.id] ?: emptyList()
            if (expenseItems.isEmpty()) {
                // whole expense goes to its top-level category
                breakdown[expense.categoryId] = (breakdown[expense.categoryId] ?: 0.0) + expense.totalCost
            } else {
                expenseItems.forEach { item ->
                    val catId = item.categoryId ?: expense.categoryId
                    breakdown[catId] = (breakdown[catId] ?: 0.0) + item.cost
                }
            }
        }
        breakdown.entries.sortedByDescending { it.value }.map { it.key to it.value }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Date range selector ───────────────────────────────────────────────
        item {
            Box {
                OutlinedButton(
                    onClick = { showRangeMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PersonalAccent),
                    border = BorderStroke(1.dp, PersonalAccent)
                ) {
                    Icon(Icons.Default.DateRange, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (selectedRange == "Custom Dates"
                            && customStartDate != null && customEndDate != null
                        ) {
                            "${displayFmt.format(Date(customStartDate!!))} – ${displayFmt.format(Date(customEndDate!!))}"
                        } else selectedRange,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(expanded = showRangeMenu, onDismissRequest = { showRangeMenu = false }) {
                    dateRanges.forEach { range ->
                        DropdownMenuItem(
                            text = { Text(range) },
                            onClick = {
                                selectedRange = range
                                showRangeMenu = false
                                if (range == "Custom Dates") showStartDatePicker = true
                            }
                        )
                    }
                }
            }
        }

        // ── Summary circles ───────────────────────────────────────────────────
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatCircle(
                    value = String.format(preferences.priceFormat, personalTotal),
                    label = "Personal\nExpenses",
                    gradient = Brush.linearGradient(listOf(PersonalAccent, PersonalAccentLight))
                )
                StatCircle(
                    value = String.format(preferences.priceFormat, vehicleTotal),
                    label = "Vehicle\nExpenses",
                    gradient = Brush.linearGradient(listOf(Color(0xFFE67E22), Color(0xFFF39C12)))
                )
                StatCircle(
                    value = String.format(preferences.priceFormat, grandTotal),
                    label = "Grand\nTotal",
                    gradient = Brush.linearGradient(listOf(Color(0xFF16A085), Color(0xFF1ABC9C)))
                )
            }
        }

        // ── Personal Expenses breakdown ───────────────────────────────────────
        if (categoryBreakdown.isNotEmpty()) {
            item {
                AnalyticsSectionHeader(
                    title = "Personal Expenses",
                    icon = Icons.Default.AccountBalance,
                    color = PersonalAccent
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        categoryBreakdown.forEach { (catId, amount) ->
                            val cat = categories.find { it.id == catId }
                            PersonalCategoryBar(
                                label = cat?.name ?: "Uncategorized",
                                amount = amount,
                                total = if (personalTotal > 0) personalTotal else 1.0,
                                color = cat?.colorHex?.let { parseHexColor(it) } ?: OnSurfaceVariant,
                                priceFormat = preferences.priceFormat
                            )
                        }
                    }
                }
            }
        }

        // ── Vehicle(s) breakdown ──────────────────────────────────────────────
        if (vehicleTotal > 0) {
            item {
                AnalyticsSectionHeader(
                    title = "Vehicle(s)",
                    icon = Icons.Default.DirectionsCar,
                    color = Color(0xFFE67E22)
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        if (fuelTotal > 0) {
                            PersonalCategoryBar(
                                label = "⛽ Fuel",
                                amount = fuelTotal,
                                total = vehicleTotal,
                                color = Color(0xFFE67E22),
                                priceFormat = preferences.priceFormat
                            )
                        }
                        if (servicesTotal > 0) {
                            PersonalCategoryBar(
                                label = "🔧 Services",
                                amount = servicesTotal,
                                total = vehicleTotal,
                                color = Color(0xFF2980B9),
                                priceFormat = preferences.priceFormat
                            )
                        }
                        if (vehicleOtherTotal > 0) {
                            PersonalCategoryBar(
                                label = "💳 Other Expenses",
                                amount = vehicleOtherTotal,
                                total = vehicleTotal,
                                color = Color(0xFF8E44AD),
                                priceFormat = preferences.priceFormat
                            )
                        }
                    }
                }
            }
        }

        // ── Empty state ───────────────────────────────────────────────────────
        if (categoryBreakdown.isEmpty() && vehicleTotal == 0.0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.BarChart, null, Modifier.size(48.dp), tint = OnSurfaceVariant)
                        Text(
                            "No data for the selected period",
                            style = MaterialTheme.typography.bodyLarge,
                            color = OnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Custom date pickers
    if (showStartDatePicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false; selectedRange = "All Time" },
            confirmButton = {
                TextButton(onClick = {
                    customStartDate = state.selectedDateMillis
                    showStartDatePicker = false
                    showEndDatePicker = true
                }) { Text("Next →") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false; selectedRange = "All Time" }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state, title = { Text("Start Date", Modifier.padding(16.dp)) })
        }
    }
    if (showEndDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    customEndDate = state.selectedDateMillis
                    showEndDatePicker = false
                }) { Text("Done") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state, title = { Text("End Date", Modifier.padding(16.dp)) })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Expense card
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PersonalExpenseCard(
    expense: PersonalExpense,
    items: List<PersonalExpenseItem>,
    categories: List<PersonalExpenseCategory>,
    preferences: UserPreferences,
    dateFormat: SimpleDateFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as RefillMeApplication
    val scope = rememberCoroutineScope()

    val category      = categories.find { it.id == expense.categoryId }
    val categoryColor = category?.colorHex?.let { parseHexColor(it) } ?: PersonalAccent
    var isExpanded        by remember { mutableStateOf(false) }
    var showActionMenu    by remember { mutableStateOf(false) }   // long-press sheet
    var showPasswordDialog by remember { mutableStateOf(false) }  // password-gate
    var password          by remember { mutableStateOf("") }
    var passwordError     by remember { mutableStateOf(false) }
    var isVerifying       by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { isExpanded = !isExpanded },
                onLongClick = { showActionMenu = true }
            ),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Left accent strip
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        categoryColor,
                        RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                    )
            )
            Column(
                Modifier.weight(1f).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            expense.vendor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            dateFormat.format(Date(expense.date)),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                        if (category != null) {
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier
                                    .background(categoryColor.copy(alpha = 0.15f), RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    category.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = categoryColor
                                )
                            }
                        }
                    }
                    // Cost + 3-dot menu
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${preferences.currencySymbol} ${String.format(preferences.priceFormat, expense.totalCost)}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = PersonalAccent
                        )
                        Box {
                            IconButton(onClick = { showActionMenu = true }, Modifier.size(32.dp)) {
                                Icon(Icons.Default.MoreVert, null, Modifier.size(18.dp), tint = OnSurfaceVariant)
                            }
                        }
                    }
                }

                // Items expand/collapse
                if (items.isNotEmpty()) {
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null, Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${items.size} line item${if (items.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (isExpanded) {
                        Divider(color = CardBorder)
                        items.forEach { item ->
                            val itemCat = categories.find { it.id == item.categoryId }
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (itemCat != null) {
                                        Box(
                                            Modifier.size(8.dp)
                                                .background(parseHexColor(itemCat.colorHex), CircleShape)
                                        )
                                    }
                                    Text(item.name, style = MaterialTheme.typography.bodySmall)
                                    if (itemCat != null) {
                                        Text(
                                            "· ${itemCat.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = OnSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    String.format(preferences.priceFormat, item.cost),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                if (expense.notes != null) {
                    Text(
                        expense.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }

    // ── Long-press / 3-dot action menu ───────────────────────────────────────
    if (showActionMenu) {
        AlertDialog(
            onDismissRequest = { showActionMenu = false },
            title = { Text(expense.vendor, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Edit") },
                        leadingContent = { Icon(Icons.Default.Edit, null, tint = Primary) },
                        modifier = Modifier.clickable {
                            showActionMenu = false
                            onEdit()
                        }
                    )
                    Divider()
                    ListItem(
                        headlineContent = { Text("Delete", color = Error) },
                        leadingContent = { Icon(Icons.Default.Delete, null, tint = Error) },
                        modifier = Modifier.clickable {
                            showActionMenu = false
                            password = ""
                            passwordError = false
                            showPasswordDialog = true
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showActionMenu = false }) { Text("Cancel") }
            }
        )
    }

    // ── Password-protected delete ────────────────────────────────────────────
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
                    Text("Enter your password to delete \"${expense.vendor}\"")
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; passwordError = false },
                        label = { Text("Password") },
                        isError = passwordError,
                        enabled = !isVerifying,
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        )
                    )
                    if (passwordError) {
                        Text("Incorrect password", color = Error, style = MaterialTheme.typography.bodySmall)
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
                                    val token = app.authManager.getToken()
                                    if (token != null) {
                                        val result = com.simi.refillme.data.auth.AuthApiService.getInstance().verifyPassword(token, password)
                                        result.onSuccess { response ->
                                            if (response.success && response.valid) {
                                                showPasswordDialog = false
                                                password = ""
                                                passwordError = false
                                                isVerifying = false
                                                onDelete()
                                            } else {
                                                passwordError = true
                                                isVerifying = false
                                            }
                                        }.onFailure {
                                            passwordError = true
                                            isVerifying = false
                                        }
                                    }
                                } catch (_: Exception) {
                                    passwordError = true
                                    isVerifying = false
                                }
                            }
                        }
                    },
                    enabled = !isVerifying && password.isNotBlank()
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Delete", color = Error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordDialog = false
                    password = ""
                    passwordError = false
                }) { Text("Cancel") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Analytics section header
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AnalyticsSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Category progress bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PersonalCategoryBar(
    label: String,
    amount: Double,
    total: Double,
    color: Color,
    priceFormat: String
) {
    val fraction = if (total > 0) (amount / total).toFloat().coerceIn(0f, 1f) else 0f
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.size(10.dp).background(color, CircleShape))
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            Text(
                String.format(priceFormat, amount),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        LinearProgressIndicator(
            progress = fraction,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
        Text(
            "${(fraction * 100).toInt()}% of total",
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

/** Parse a hex colour string to a Compose Color; falls back to grey on error. */
fun parseHexColor(hex: String): Color =
    try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color(0xFF9E9E9E) }

private fun personalExpensesInRange(
    expenses: List<PersonalExpense>,
    range: String,
    customStart: Long?,
    customEnd: Long?
): List<PersonalExpense> {
    val cal = Calendar.getInstance()
    return when (range) {
        "This Month" -> {
            val m = cal.get(Calendar.MONTH); val y = cal.get(Calendar.YEAR)
            expenses.filter {
                cal.timeInMillis = it.date
                cal.get(Calendar.MONTH) == m && cal.get(Calendar.YEAR) == y
            }
        }
        "Last Month" -> {
            cal.add(Calendar.MONTH, -1)
            val m = cal.get(Calendar.MONTH); val y = cal.get(Calendar.YEAR)
            expenses.filter {
                cal.timeInMillis = it.date
                cal.get(Calendar.MONTH) == m && cal.get(Calendar.YEAR) == y
            }
        }
        "Last 7 Days" -> {
            val cutoff = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            expenses.filter { it.date >= cutoff }
        }
        "This Year" -> {
            val y = cal.get(Calendar.YEAR)
            expenses.filter { cal.timeInMillis = it.date; cal.get(Calendar.YEAR) == y }
        }
        "Custom Dates" -> {
            if (customStart != null && customEnd != null)
                expenses.filter { it.date >= customStart && it.date <= customEnd + 86_400_000L }
            else expenses
        }
        else -> expenses
    }
}

private fun refillsInRange(
    refills: List<Refill>,
    range: String,
    customStart: Long?,
    customEnd: Long?
): List<Refill> {
    val cal = Calendar.getInstance()
    return when (range) {
        "This Month" -> {
            val m = cal.get(Calendar.MONTH); val y = cal.get(Calendar.YEAR)
            refills.filter {
                cal.timeInMillis = it.date
                cal.get(Calendar.MONTH) == m && cal.get(Calendar.YEAR) == y
            }
        }
        "Last Month" -> {
            cal.add(Calendar.MONTH, -1)
            val m = cal.get(Calendar.MONTH); val y = cal.get(Calendar.YEAR)
            refills.filter {
                cal.timeInMillis = it.date
                cal.get(Calendar.MONTH) == m && cal.get(Calendar.YEAR) == y
            }
        }
        "Last 7 Days" -> {
            val cutoff = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            refills.filter { it.date >= cutoff }
        }
        "This Year" -> {
            val y = cal.get(Calendar.YEAR)
            refills.filter { cal.timeInMillis = it.date; cal.get(Calendar.YEAR) == y }
        }
        "Custom Dates" -> {
            if (customStart != null && customEnd != null)
                refills.filter { it.date >= customStart && it.date <= customEnd + 86_400_000L }
            else refills
        }
        else -> refills
    }
}

private fun servicesInRange(
    services: List<Service>,
    range: String,
    customStart: Long?,
    customEnd: Long?
): List<Service> {
    val cal = Calendar.getInstance()
    return when (range) {
        "This Month" -> {
            val m = cal.get(Calendar.MONTH); val y = cal.get(Calendar.YEAR)
            services.filter {
                cal.timeInMillis = it.date
                cal.get(Calendar.MONTH) == m && cal.get(Calendar.YEAR) == y
            }
        }
        "Last Month" -> {
            cal.add(Calendar.MONTH, -1)
            val m = cal.get(Calendar.MONTH); val y = cal.get(Calendar.YEAR)
            services.filter {
                cal.timeInMillis = it.date
                cal.get(Calendar.MONTH) == m && cal.get(Calendar.YEAR) == y
            }
        }
        "Last 7 Days" -> {
            val cutoff = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            services.filter { it.date >= cutoff }
        }
        "This Year" -> {
            val y = cal.get(Calendar.YEAR)
            services.filter { cal.timeInMillis = it.date; cal.get(Calendar.YEAR) == y }
        }
        "Custom Dates" -> {
            if (customStart != null && customEnd != null)
                services.filter { it.date >= customStart && it.date <= customEnd + 86_400_000L }
            else services
        }
        else -> services
    }
}

private fun vehicleExpensesInRange(
    expenses: List<Expense>,
    range: String,
    customStart: Long?,
    customEnd: Long?
): List<Expense> {
    val cal = Calendar.getInstance()
    return when (range) {
        "This Month" -> {
            val m = cal.get(Calendar.MONTH); val y = cal.get(Calendar.YEAR)
            expenses.filter {
                cal.timeInMillis = it.date
                cal.get(Calendar.MONTH) == m && cal.get(Calendar.YEAR) == y
            }
        }
        "Last Month" -> {
            cal.add(Calendar.MONTH, -1)
            val m = cal.get(Calendar.MONTH); val y = cal.get(Calendar.YEAR)
            expenses.filter {
                cal.timeInMillis = it.date
                cal.get(Calendar.MONTH) == m && cal.get(Calendar.YEAR) == y
            }
        }
        "Last 7 Days" -> {
            val cutoff = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            expenses.filter { it.date >= cutoff }
        }
        "This Year" -> {
            val y = cal.get(Calendar.YEAR)
            expenses.filter { cal.timeInMillis = it.date; cal.get(Calendar.YEAR) == y }
        }
        "Custom Dates" -> {
            if (customStart != null && customEnd != null)
                expenses.filter { it.date >= customStart && it.date <= customEnd + 86_400_000L }
            else expenses
        }
        else -> expenses
    }
}
