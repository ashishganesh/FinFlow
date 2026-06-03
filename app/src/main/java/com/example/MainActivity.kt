package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.draw.clip
import com.example.data.*
import com.example.ui.*
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Local SQLite Room instance bindings
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ExpenseRepository(database.expenseDao())

        setContent {
            val app = this@MainActivity.application
            val model: ExpenseViewModel = viewModel(
                factory = ExpenseViewModelFactory(app, repository)
            )

            val themePref by model.themePreference.collectAsStateWithLifecycle()
            val systemIsDark = isSystemInDarkTheme()
            val darkTheme = when (themePref) {
                "light" -> false
                "dark" -> true
                else -> systemIsDark
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigationShell(
                        viewModel = model,
                        isDark = darkTheme,
                        onThemeToggle = {
                            val newPref = if (darkTheme) "light" else "dark"
                            model.setThemePreference(newPref)
                        }
                    )
                }
            }
        }
    }
}

data class ScreenItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationShell(
    viewModel: ExpenseViewModel,
    isDark: Boolean,
    onThemeToggle: () -> Unit
) {
    val context = LocalContext.current
    val currentRoute = viewModel.currentScreen.collectAsStateWithLifecycle().value
    val activeAcc = viewModel.activeAccount.collectAsStateWithLifecycle().value
    val accountsList = viewModel.accounts.collectAsStateWithLifecycle().value
    val authTargetAcc = viewModel.authTargetAccount.collectAsStateWithLifecycle().value
    val lockoutSecondsRemaining = viewModel.lockoutSecondsRemaining.collectAsStateWithLifecycle().value
    val availableCats = viewModel.availableCategories.collectAsStateWithLifecycle().value

    // Dialog form triggers
    var showAddDialog by remember { mutableStateOf(false) }
    var initialAddType by remember { mutableStateOf("EXPENSE") }
    var selectedTxForEdit by remember { mutableStateOf<Transaction?>(null) }
    var selectedTxItems by remember { mutableStateOf<List<TransactionItem>>(emptyList()) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTxForEdit) {
        val tx = selectedTxForEdit
        if (tx != null) {
            viewModel.getItemsForTransaction(tx.id).collect { items ->
                selectedTxItems = items
            }
        } else {
            selectedTxItems = emptyList()
        }
    }

    // Account selector comprehensive sheet state
    var showProfileManager by remember { mutableStateOf(false) }

    // Listen to budget overspending alert triggers
    LaunchedEffect(viewModel.alertMessage) {
        viewModel.alertMessage.collectLatest { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    if (showProfileManager) {
        ProfileManagerBottomSheet(
            viewModel = viewModel,
            accountsList = accountsList,
            activeAccount = activeAcc,
            onDismiss = { showProfileManager = false }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        // Premium gradient avatar with active account emblem
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp, end = 8.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(activeAcc?.color ?: 0xFFCD9BFF.toInt()),
                                            Color(activeAcc?.color ?: 0xFFCD9BFF.toInt()).copy(alpha = 0.7f)
                                        )
                                    )
                                )
                                .clickable { showProfileManager = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (activeAcc != null) {
                                val avatarIcon = when (activeAcc.avatar.lowercase()) {
                                    "personal" -> Icons.Default.Person
                                    "business" -> Icons.Default.Business
                                    "family" -> Icons.Default.People
                                    "savings" -> Icons.Default.AccountBalance
                                    "travel" -> Icons.Default.Explore
                                    else -> null
                                }
                                if (avatarIcon != null) {
                                    Icon(
                                        imageVector = avatarIcon,
                                        contentDescription = activeAcc.avatar,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        text = activeAcc.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            } else {
                                Text(
                                    text = "P",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    },
                    title = {
                        // Dynamic Switchable Workspace Selector
                        Box {
                            Column(
                                modifier = Modifier
                                    .clickable { showProfileManager = true }
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = activeAcc?.name ?: "Personal",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown Switcher",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "Switch Space",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                    actions = {
                        // High-fidelity Theme Toggler
                        IconButton(onClick = onThemeToggle) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Switch Theme",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            }
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                // Customized bottom navigation as requested (Dashboard, Transactions, Budgets, Recurring, More)
                val screens = listOf(
                    ScreenItem("Dashboard", "dashboard", Icons.Default.Dashboard),
                    ScreenItem("Ledger", "transactions", Icons.Default.ReceiptLong),
                    ScreenItem("Budgets", "budgets", Icons.Default.TrendingUp),
                    ScreenItem("Recurring", "recurring", Icons.Default.Autorenew),
                    ScreenItem("More", "more", Icons.Default.MoreHoriz)
                )

                screens.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.currentScreen.value = screen.route },
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen router
            Crossfade(
                targetState = currentRoute,
                label = "ScreenSwitch"
            ) { route ->
                when (route) {
                    "dashboard" -> DashboardScreen(
                        viewModel = viewModel,
                        onAddTransactionClick = { type ->
                            initialAddType = type
                            showAddDialog = true
                        },
                        onEditTransactionClick = { tx -> selectedTxForEdit = tx },
                        onAddBudgetClick = { showAddBudgetDialog = true }
                    )
                    "transactions" -> TransactionsScreen(
                        viewModel = viewModel,
                        onEditTransactionClick = { tx -> selectedTxForEdit = tx },
                        onAddTransactionClick = {
                            initialAddType = "EXPENSE"
                            showAddDialog = true
                        }
                    )
                    "budgets" -> BudgetsScreen(viewModel = viewModel)
                    "recurring" -> RecurringScreen(
                        viewModel = viewModel,
                        onEditTransactionClick = { tx -> selectedTxForEdit = tx },
                        onAddTransactionClick = {
                            initialAddType = "EXPENSE"
                            showAddDialog = true
                        }
                    )
                    "more" -> MoreHubScreen(viewModel = viewModel)
                }
            }

            // Secure PIN padlock screen blocker
            if (authTargetAcc != null) {
                PinPadDialog(
                    targetAccount = authTargetAcc!!,
                    lockoutSecondsRemaining = lockoutSecondsRemaining,
                    onVerify = { pin ->
                        viewModel.authenticateTargetAccount(pin)
                    },
                    onCancel = {
                        viewModel.cancelAuthentication()
                    }
                )
            }

            // Overlay form dialogue for ADDING transaction entries
            if (showAddDialog) {
                AddEditTransactionDialog(
                    initialType = initialAddType,
                    categories = availableCats,
                    currencySymbol = activeAcc?.currency ?: "$",
                    accounts = viewModel.accounts.collectAsStateWithLifecycle().value,
                    activeAccountId = viewModel.selectedAccountId.collectAsStateWithLifecycle().value,
                    onDismiss = { showAddDialog = false },
                    onSubmit = { amount, title, category, type, remarks, timestamp, isRec, recInterval, payMode, imgPath, destId, items ->
                        if (type == "TRANSFER" && destId != null) {
                            viewModel.transferBalance(destId, amount, title, remarks, timestamp, payMode)
                        } else {
                            if (items.isNotEmpty()) {
                                viewModel.insertTransactionWithItems(
                                    amount, title, category, type, remarks, timestamp, isRec, recInterval, payMode, imgPath, items
                                )
                            } else {
                                viewModel.insertTransaction(
                                    amount, title, category, type, remarks, timestamp, isRec, recInterval, payMode, imgPath
                                )
                            }
                        }
                        showAddDialog = false
                        Toast.makeText(context, "Transaction successfully logged!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Overlay dialog for ADDING budget limits directly from FAB
            if (showAddBudgetDialog) {
                var selectedBudgetCategory by remember { mutableStateOf(availableCats.firstOrNull() ?: "Food") }
                var budgetAmountStr by remember { mutableStateOf("") }
                var expandedCatMenu by remember { mutableStateOf(false) }

                Dialog(onDismissRequest = { showAddBudgetDialog = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Add New Budget Target",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { expandedCatMenu = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Category: $selectedBudgetCategory")
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = expandedCatMenu,
                                    onDismissRequest = { expandedCatMenu = false }
                                ) {
                                    availableCats.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat) },
                                            onClick = {
                                                selectedBudgetCategory = cat
                                                expandedCatMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = budgetAmountStr,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.toDoubleOrNull() != null) {
                                        budgetAmountStr = input
                                    }
                                },
                                label = { Text("Monthly Cap (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showAddBudgetDialog = false },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        val limit = budgetAmountStr.toDoubleOrNull() ?: 100.0
                                        viewModel.setCategoryBudget(selectedBudgetCategory, limit)
                                        showAddBudgetDialog = false
                                        Toast.makeText(context, "Budget created successfully!", Toast.LENGTH_SHORT).show()
                                    },
                                    enabled = budgetAmountStr.isNotBlank() && budgetAmountStr.toDoubleOrNull() != null,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Save")
                                }
                            }
                        }
                    }
                }
            }

            // Overlay form dialogue for EDITING/DELETING existing records
            if (selectedTxForEdit != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AddEditTransactionDialog(
                        transaction = selectedTxForEdit,
                        initialItems = selectedTxItems,
                        categories = availableCats,
                        currencySymbol = activeAcc?.currency ?: "$",
                        accounts = viewModel.accounts.collectAsStateWithLifecycle().value,
                        activeAccountId = viewModel.selectedAccountId.collectAsStateWithLifecycle().value,
                        onDismiss = { selectedTxForEdit = null },
                        onSubmit = { amount, title, category, type, remarks, timestamp, isRec, recInterval, payMode, imgPath, destId, items ->
                            val original = selectedTxForEdit!!
                            val updatedTx = original.copy(
                                amount = amount,
                                title = title,
                                category = category,
                                type = type,
                                remarks = remarks,
                                timestamp = timestamp,
                                isRecurring = isRec,
                                recurringInterval = recInterval,
                                paymentMode = payMode,
                                imagePath = imgPath
                            )
                            if (items.isNotEmpty() || selectedTxItems.isNotEmpty()) {
                                viewModel.updateTransactionWithItems(updatedTx, items)
                            } else {
                                viewModel.updateTransaction(updatedTx)
                            }
                            selectedTxForEdit = null
                            Toast.makeText(context, "Record updated successfully!", Toast.LENGTH_SHORT).show()
                        }
                    )

                    // Floating quick Delete action next to modifying card
                    Card(
                        onClick = {
                            val original = selectedTxForEdit!!
                            viewModel.deleteTransaction(original)
                            selectedTxForEdit = null
                            Toast.makeText(context, "Transaction record deleted.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .height(48.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Trash", tint = Color.White)
                            Text("DELETE TRANSACTION", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
