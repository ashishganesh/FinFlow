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
import androidx.compose.ui.text.style.TextAlign
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
    private var expenseViewModel: ExpenseViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Local SQLite Room instance bindings
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ExpenseRepository(database.expenseDao())

        // Initialize standard notifications channel
        NotificationHelper.init(applicationContext)

        // Request standard notification permission on Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 101)
            }
        }

        setContent {
            val app = this@MainActivity.application
            val model: ExpenseViewModel = viewModel(
                factory = ExpenseViewModelFactory(app, repository)
            )
            expenseViewModel = model

            // Route dynamically if app opened from standard system notification tap
            LaunchedEffect(Unit) {
                intent?.getStringExtra("target_screen")?.let { targetScreen ->
                    if (targetScreen.isNotEmpty()) {
                        when (targetScreen) {
                            "budgets" -> {
                                model.currentScreen.value = "budgets"
                                model.moreSelectedTabIdx.value = 0
                            }
                            "savings" -> {
                                model.currentScreen.value = "budgets"
                                model.moreSelectedTabIdx.value = 1
                            }
                            "debts" -> {
                                model.currentScreen.value = "budgets"
                                model.moreSelectedTabIdx.value = 3
                            }
                            else -> {
                                model.currentScreen.value = targetScreen
                            }
                        }
                    }
                }
            }

            val authTargetAcc by model.authTargetAccount.collectAsStateWithLifecycle()
            LaunchedEffect(authTargetAcc) {
                if (authTargetAcc != null && authTargetAcc?.pin != null) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

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

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("target_screen")?.let { targetScreen ->
            if (targetScreen.isNotEmpty()) {
                val model = expenseViewModel ?: return@let
                when (targetScreen) {
                    "budgets" -> {
                        model.currentScreen.value = "budgets"
                        model.moreSelectedTabIdx.value = 0
                    }
                    "savings" -> {
                        model.currentScreen.value = "budgets"
                        model.moreSelectedTabIdx.value = 1
                    }
                    "debts" -> {
                        model.currentScreen.value = "budgets"
                        model.moreSelectedTabIdx.value = 3
                    }
                    else -> {
                        model.currentScreen.value = targetScreen
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        expenseViewModel?.lockAccountManually()
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
    LaunchedEffect(activeAcc) {
        if (activeAcc != null) {
            viewModel.checkAndTriggerDueReminders(context)
        }
    }
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
    var showDebtTypeForAdd by remember { mutableStateOf<String?>(null) }
    var showAddSavingsDepositDialog by remember { mutableStateOf(false) }
    var initialPaymentMode by remember { mutableStateOf("") }

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

    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
    var showFirstProfileSetup by remember { mutableStateOf(false) }

    if (showFirstProfileSetup) {
        ProfileConfigDialog(
            titleLabel = "Instantiate Profile Space",
            onDismiss = { showFirstProfileSetup = false },
            onSubmit = { name, currency, pin, colorVal, avatar, theme, cash, bank ->
                viewModel.addAccount(
                    name = name,
                    pin = pin,
                    color = colorVal,
                    currency = currency,
                    avatar = avatar,
                    themePreference = theme,
                    openingCashBalance = cash,
                    openingBankBalance = bank
                )
                showFirstProfileSetup = false
                viewModel.setOnboardingCompleted(true)
                Toast.makeText(context, "Welcome to $name Workspace!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (!isOnboardingCompleted) {
        var activeSlide by remember { mutableStateOf(0) }
        val slideCount = 4

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 450.dp)
                    .padding(vertical = 16.dp)
            ) {
                // Header / Branding info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "FinTracker Pro",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Dynamic Animated slide content
                AnimatedContent(
                    targetState = activeSlide,
                    transitionSpec = {
                        slideInHorizontally { width -> if (targetState > initialState) width else -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> if (targetState > initialState) -width else width } + fadeOut()
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    label = "slide_transition"
                ) { slide ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                    ) {
                        val icon: ImageVector
                        val title: String
                        val desc: String
                        val tint: Color

                        when (slide) {
                            0 -> {
                                icon = Icons.Default.VerifiedUser
                                title = "100% Offline & Private"
                                desc = "Your financial logs, itemized receipts, and PIN passcodes are stored safely and solely on your physical device.\nNo servers, no tracking, complete digital privacy."
                                tint = MaterialTheme.colorScheme.primary
                            }
                            1 -> {
                                icon = Icons.Default.TrendingUp
                                title = "Proactive Budget Caps"
                                desc = "Set monthly spending limits per category. Visual 90% and 100% notification alert indicators with live system tray integration keep you accountable."
                                tint = MaterialTheme.colorScheme.secondary
                            }
                            2 -> {
                                icon = Icons.Default.People
                                title = "Khata Digital Ledger"
                                desc = "Keep immaculate records of money lent to or borrowed from colleagues. Automated proximity system notifications remind you of due dates."
                                tint = MaterialTheme.colorScheme.tertiary
                            }
                            else -> {
                                icon = Icons.Default.Security
                                title = "Secure Profile Spaces"
                                desc = "Instantiate different workspaces for business, household, or personal tracking. Secure individual environments with a 4-digit PIN lock screen."
                                tint = MaterialTheme.colorScheme.primary
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(tint.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = tint,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }

                // Indicators and bottom operations panel
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Bullets indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(slideCount) { idx ->
                            Box(
                                modifier = Modifier
                                    .size(if (activeSlide == idx) 12.dp else 8.dp)
                                    .background(
                                        if (activeSlide == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buttons Layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (activeSlide > 0) {
                            OutlinedButton(
                                onClick = { activeSlide-- },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Back")
                            }
                        }

                        Button(
                            onClick = {
                                if (activeSlide < slideCount - 1) {
                                    activeSlide++
                                } else {
                                    showFirstProfileSetup = true
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeSlide == slideCount - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (activeSlide == slideCount - 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            if (activeSlide == slideCount - 1) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Instantiate Workspace", fontWeight = FontWeight.Bold)
                            } else {
                                Text("Next Feature", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        return
    }

    // Dynamic initial loading check to prevent screen flashes
    var hasExpiredLoadingGracePeriod by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(180)
        hasExpiredLoadingGracePeriod = true
    }

    if (accountsList.isEmpty()) {
        if (!hasExpiredLoadingGracePeriod) {
            // Elegant M3 loading container on startup
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
            }
            return
        } else {
            // Fallback workspace screen if no accounts exist after loading grace period
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.widthIn(max = 400.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = "Wallet Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Text(
                        text = "Initialize Workspace",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "To start tracking, please configure a financial workspace profile. All profiles run as distinct and private local sandboxes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showFirstProfileSetup = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Instantiate Profile Space",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            return
        }
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
                // Customized bottom navigation as requested (Dashboard, Ledger, Finance, Cards, More)
                val screens = listOf(
                    ScreenItem("Dashboard", "dashboard", Icons.Default.Dashboard),
                    ScreenItem("Ledger", "transactions", Icons.Default.ReceiptLong),
                    ScreenItem("Finance", "budgets", Icons.Default.TrendingUp),
                    ScreenItem("Cards", "credit_cards", Icons.Default.Wallet),
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
                        onAddBudgetClick = { showAddBudgetDialog = true },
                        onAddLentClick = { showDebtTypeForAdd = "LENT" },
                        onAddBorrowedClick = { showDebtTypeForAdd = "BORROWED" }
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
                    "credit_cards" -> CreditCardsScreen(viewModel = viewModel)
                    "more" -> MoreHubScreen(
                        viewModel = viewModel,
                        onAddTransactionClick = {
                            initialAddType = "EXPENSE"
                            initialPaymentMode = ""
                            showAddDialog = true
                        },
                        onEditTransactionClick = { tx -> selectedTxForEdit = tx }
                    )
                }
            }

            var isFabExpanded by remember { mutableStateOf(false) }

            // Global Floating Action Button Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Expanded actions list
                    AnimatedVisibility(
                        visible = isFabExpanded,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            // 1. Add Expense
                            ExtendedActionButton(
                                text = "Add Expense",
                                icon = Icons.Default.AddShoppingCart,
                                color = Color(0xFFF87171),
                                onClick = {
                                    initialAddType = "EXPENSE"
                                    initialPaymentMode = ""
                                    showAddDialog = true
                                    isFabExpanded = false
                                }
                            )

                            // 2. Add Income
                            ExtendedActionButton(
                                text = "Add Income",
                                icon = Icons.Default.Payments,
                                color = Color(0xFF34D399),
                                onClick = {
                                    initialAddType = "INCOME"
                                    initialPaymentMode = ""
                                    showAddDialog = true
                                    isFabExpanded = false
                                }
                            )

                            // 3. Add Lent
                            ExtendedActionButton(
                                text = "Add Lent",
                                icon = Icons.Default.ArrowOutward,
                                color = Color(0xFF60A5FA),
                                onClick = {
                                    showDebtTypeForAdd = "LENT"
                                    isFabExpanded = false
                                }
                            )

                            // 4. Add Borrowed
                            ExtendedActionButton(
                                text = "Add Borrowed",
                                icon = Icons.Default.ArrowDownward,
                                color = Color(0xFFF472B6),
                                onClick = {
                                    showDebtTypeForAdd = "BORROWED"
                                    isFabExpanded = false
                                }
                            )

                            // 5. Add Savings Deposit
                            ExtendedActionButton(
                                text = "Add Savings Deposit",
                                icon = Icons.Default.Savings,
                                color = Color(0xFFFBBF24),
                                onClick = {
                                    showAddSavingsDepositDialog = true
                                    isFabExpanded = false
                                }
                            )

                            // 6. Add Credit Card Expense
                            ExtendedActionButton(
                                text = "Add Credit Card Expense",
                                icon = Icons.Default.CreditCard,
                                color = Color(0xFFA78BFA),
                                onClick = {
                                    initialAddType = "EXPENSE"
                                    initialPaymentMode = "Credit Card"
                                    showAddDialog = true
                                    isFabExpanded = false
                                }
                            )
                        }
                    }

                    // Main (+) button
                    LargeFloatingActionButton(
                        onClick = { isFabExpanded = !isFabExpanded },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                ),
                                RoundedCornerShape(20.dp)
                            )
                    ) {
                        Icon(
                            imageVector = if (isFabExpanded) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Expand action menu",
                            modifier = Modifier.size(32.dp)
                        )
                    }
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
                    initialPaymentMode = initialPaymentMode,
                    categories = availableCats,
                    currencySymbol = activeAcc?.currency ?: "$",
                    accounts = viewModel.accounts.collectAsStateWithLifecycle().value,
                    activeAccountId = viewModel.selectedAccountId.collectAsStateWithLifecycle().value,
                    creditCards = viewModel.creditCards.collectAsStateWithLifecycle().value,
                    onDismiss = { 
                        showAddDialog = false 
                        initialPaymentMode = ""
                    },
                    onSubmit = { amount, title, category, type, remarks, timestamp, isRec, recInterval, payMode, imgPath, destId, items, ccId ->
                        if (type == "TRANSFER" && destId != null) {
                            viewModel.transferBalance(destId, amount, title, remarks, timestamp, payMode)
                        } else {
                            if (items.isNotEmpty()) {
                                viewModel.insertTransactionWithItems(
                                    amount, title, category, type, remarks, timestamp, isRec, recInterval, payMode, imgPath, items, ccId
                                )
                            } else {
                                viewModel.insertTransaction(
                                    amount, title, category, type, remarks, timestamp, isRec, recInterval, payMode, imgPath, ccId
                                )
                            }
                        }
                        showAddDialog = false
                        initialPaymentMode = ""
                        Toast.makeText(context, "Transaction successfully logged!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Overlay form dialogue for ADDING/EDITING debt entries from FAB/Dashboard
            if (showDebtTypeForAdd != null) {
                DebtEntryDialog(
                    type = showDebtTypeForAdd!!,
                    entry = null,
                    onDismiss = { showDebtTypeForAdd = null },
                    onSave = { name, amount, notes, phone, entryDate, dueDate, paymentMethod ->
                        viewModel.insertDebtEntry(
                            type = showDebtTypeForAdd!!,
                            personName = name,
                            amount = amount,
                            entryDate = entryDate,
                            dueDate = dueDate,
                            notes = notes,
                            phoneNumber = phone,
                            paymentMethod = paymentMethod
                        )
                        showDebtTypeForAdd = null
                        Toast.makeText(context, "Debt logged successfully!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Overlay form dialogue for ADDING savings deposit from FAB
            if (showAddSavingsDepositDialog) {
                val savingsGoals = viewModel.savingsGoals.collectAsStateWithLifecycle().value
                if (savingsGoals.isEmpty()) {
                    AlertDialog(
                        onDismissRequest = { showAddSavingsDepositDialog = false },
                        title = { Text("No Savings Goals") },
                        text = { Text("You must establish a savings goal first in the Budgets section under the Savings tab!") },
                        confirmButton = {
                            Button(onClick = { showAddSavingsDepositDialog = false }) {
                                Text("OK")
                            }
                        }
                    )
                } else {
                    var selectedGoalIdx by remember { mutableStateOf(0) }
                    var depositAmountStr by remember { mutableStateOf("") }
                    var depositNote by remember { mutableStateOf("FAB Deposit") }
                    var expandGoalDropdown by remember { mutableStateOf(false) }

                    AlertDialog(
                        onDismissRequest = { showAddSavingsDepositDialog = false },
                        title = { Text("Add Savings Deposit") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { expandGoalDropdown = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Goal: " + (savingsGoals.getOrNull(selectedGoalIdx)?.name ?: "Select"))
                                            Icon(Icons.Default.ArrowDropDown, null)
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = expandGoalDropdown,
                                        onDismissRequest = { expandGoalDropdown = false }
                                    ) {
                                        savingsGoals.forEachIndexed { idx, goal ->
                                            DropdownMenuItem(
                                                text = { Text(goal.name) },
                                                onClick = {
                                                    selectedGoalIdx = idx
                                                    expandGoalDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = depositAmountStr,
                                    onValueChange = { depositAmountStr = it },
                                    label = { Text("Deposit Amount (₹)") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = depositNote,
                                    onValueChange = { depositNote = it },
                                    label = { Text("Note") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val amt = depositAmountStr.toDoubleOrNull() ?: 0.0
                                    val goal = savingsGoals.getOrNull(selectedGoalIdx)
                                    if (amt > 0 && goal != null) {
                                        viewModel.addSavingsGoalTransaction(goal, amt, depositNote)
                                        showAddSavingsDepositDialog = false
                                        Toast.makeText(context, "Deposited ₹${amt} successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = depositAmountStr.toDoubleOrNull() != null && (depositAmountStr.toDoubleOrNull() ?: 0.0) > 0
                            ) {
                                Text("Deposit")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddSavingsDepositDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
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
                        creditCards = viewModel.creditCards.collectAsStateWithLifecycle().value,
                        onDismiss = { selectedTxForEdit = null },
                        onSubmit = { amount, title, category, type, remarks, timestamp, isRec, recInterval, payMode, imgPath, destId, items, ccId ->
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
                                imagePath = imgPath,
                                creditCardId = ccId
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
