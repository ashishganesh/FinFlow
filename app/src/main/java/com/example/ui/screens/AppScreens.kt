package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ui.theme.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.*
import com.example.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

data class MonthlyCarryForward(
    val monthYearStr: String,
    val openingBalance: Double,
    val income: Double,
    val expense: Double,
    val closingBalance: Double
)

@Composable
fun rememberMonthlyCarryForward(
    transactions: List<Transaction>,
    enableCarryForward: Boolean
): List<MonthlyCarryForward> {
    return remember(transactions, enableCarryForward) {
        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val displayFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        val txByMonth = transactions.groupBy { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            dateFormat.format(cal.time)
        }.toSortedMap()

        val list = mutableListOf<MonthlyCarryForward>()
        var runningClosingBalance = 0.0

        txByMonth.forEach { (monthStr, monthTxs) ->
            val income = monthTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = monthTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val net = income - expense

            val opening = if (enableCarryForward) runningClosingBalance else 0.0
            val closing = opening + net

            val dateObj = dateFormat.parse(monthStr) ?: Date()
            val displayTitle = displayFormat.format(dateObj)

            list.add(
                MonthlyCarryForward(
                    monthYearStr = displayTitle,
                    openingBalance = opening,
                    income = income,
                    expense = expense,
                    closingBalance = closing
                )
            )

            runningClosingBalance = closing
        }

        list
    }
}

// Helper to resolve category specific visual icons
fun getCategoryIcon(category: String) = when (category.lowercase(Locale.ROOT)) {
    "food" -> Icons.Default.Restaurant
    "travel" -> Icons.Default.DirectionsCar
    "bills" -> Icons.Default.ReceiptLong
    "shopping" -> Icons.Default.LocalMall
    "education" -> Icons.Default.School
    "health" -> Icons.Default.MedicalServices
    "rent" -> Icons.Default.Home
    "entertainment" -> Icons.Default.LocalPlay
    "investment" -> Icons.Default.TrendingUp
    else -> Icons.Default.Category
}

// Helper to format timestamps to nice strings
fun formatTxDate(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
    return formatter.format(date)
}

// Master color list for profile dynamic creation
val ProfileColors = listOf(
    0xFF0D9488.toInt(), // Teal
    0xFF4F46E5.toInt(), // Indigo
    0xFF059669.toInt(), // Emerald Green
    0xFFE11D48.toInt(), // Crimson Rose
    0xFFD97706.toInt(), // Amber
    0xFF7C3AED.toInt(), // Violet
    0xFF2563EB.toInt(), // Royal Blue
    0xFF4B5563.toInt()  // Charcoal Slate
)

// --- DASHBOARD PANE ---
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    onAddTransactionClick: (type: String) -> Unit,
    onEditTransactionClick: (Transaction) -> Unit,
    onAddBudgetClick: () -> Unit,
    onAddLentClick: (() -> Unit)? = null,
    onAddBorrowedClick: (() -> Unit)? = null
) {
    val activeAcc: Account? = viewModel.activeAccount.collectAsStateWithLifecycle().value
    val accountsList: List<Account> = viewModel.accounts.collectAsStateWithLifecycle().value
    val stats: BalanceStats = viewModel.balanceStats.collectAsStateWithLifecycle().value
    val txs: List<Transaction> = viewModel.transactions.collectAsStateWithLifecycle().value
    val budgets: List<Budget> = viewModel.budgets.collectAsStateWithLifecycle().value
    val savingsGoalsList = viewModel.savingsGoals.collectAsStateWithLifecycle().value
    val debtEntriesList = viewModel.debtEntries.collectAsStateWithLifecycle().value
    val dStats = viewModel.debtStats.collectAsStateWithLifecycle().value
    val carryForwardEnabledRaw = viewModel.enableCarryForward.collectAsStateWithLifecycle().value
    val carryForwardRecords = rememberMonthlyCarryForward(txs, carryForwardEnabledRaw)
    val activeCcList = viewModel.creditCards.collectAsStateWithLifecycle().value
    val repaymentsAll = viewModel.creditCardRepayments.collectAsStateWithLifecycle().value

    val currentMonthDisplay = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    val currentRecord = carryForwardRecords.find {
        it.monthYearStr.lowercase(Locale.ROOT) == currentMonthDisplay.lowercase(Locale.ROOT)
    } ?: (
        if (carryForwardRecords.isNotEmpty()) {
            val last = carryForwardRecords.last()
            MonthlyCarryForward(
                monthYearStr = currentMonthDisplay,
                openingBalance = if (carryForwardEnabledRaw) last.closingBalance else 0.0,
                income = 0.0,
                expense = 0.0,
                closingBalance = if (carryForwardEnabledRaw) last.closingBalance else 0.0
            )
        } else {
            MonthlyCarryForward(
                monthYearStr = currentMonthDisplay,
                openingBalance = 0.0,
                income = 0.0,
                expense = 0.0,
                closingBalance = 0.0
            )
        }
    )

    var isHistoryExpanded by remember { mutableStateOf(false) }
    var targetSavingsGoal by remember { mutableStateOf(50000.0) }

    // CREDIT CARD LOCAL NOTIFICATIONS / DUE DATES ALERTS SYSTEM
    var dismissedAlerts by rememberSaveable { mutableStateOf(setOf<String>()) }
    var testAlertsMode by rememberSaveable { mutableStateOf(false) }
    var activeRepaymentCardForAlerts by remember { mutableStateOf<CreditCard?>(null) }
    var repayAlertAmountStr by remember { mutableStateOf("") }
    var repayAlertSource by remember { mutableStateOf("Bank") }
    var repayAlertNotes by remember { mutableStateOf("") }
    val dueAlerts = emptyList<Triple<CreditCard, Int, Double>>()

    val activeAndUndismissedAlerts = remember(dueAlerts, dismissedAlerts) {
        dueAlerts.filter { (card, daysLeft, _) ->
            val alertKey = "${card.id}_${daysLeft}"
            alertKey !in dismissedAlerts
        }
    }

    val overallOutstanding = activeCcList.sumOf { card ->
        val repayments = repaymentsAll.filter { it.creditCardId == card.id }
        val spentOnTransactions = txs.filter { it.type == "EXPENSE" && it.creditCardId == card.id }.sumOf { it.amount }
        val repaidOnRepayments = repayments.sumOf { it.amount }
        (spentOnTransactions - repaidOnRepayments).coerceAtLeast(0.0)
    }
    val totalLimit = activeCcList.sumOf { it.creditLimit }
    val overallUtilizationPercent = if (totalLimit > 0) ((overallOutstanding / totalLimit) * 100).toInt() else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                // PREMIUM GLASSMORPHISM BALANCE SUMMARY CARD
                val isDark = MaterialTheme.colorScheme.isDark
                val currencySymbol = activeAcc?.currency ?: "₹"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0x3B1F0D3D) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (isDark) Brush.linearGradient(listOf(Color(0x55FFFFFF), Color(0x33B57CFF)))
                        else SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // --- TOP SECTION: TOTAL NET BALANCE ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = "Total Wallet",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "TOTAL NET BALANCE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.2.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", stats.netBalance)}",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            // Premium glowing/pulsing accent indicator
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }

                        // Divider line above middle section
                        HorizontalDivider(
                            color = if (isDark) Color(0x1AFFFFFF) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )

                        // --- MIDDLE SECTION: TWO BALANCE SEGMENTS SIDE-BY-SIDE ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Left Segment: Cash Balance
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isDark) Color(0x1F2A1A4A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("💵", fontSize = 16.sp)
                                    Text(
                                        text = "Cash Balance",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", stats.cashBalance)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Right Segment: Bank / Online Balance
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isDark) Color(0x1F2A1A4A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("🏦", fontSize = 16.sp)
                                    Text(
                                        text = "Bank / Online",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", stats.bankBalance)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Divider line below middle section
                        HorizontalDivider(
                            color = if (isDark) Color(0x1AFFFFFF) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )

                        // --- CREDIT CARDS OUTSTANDING & UTILIZATION ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isDark) Color(0x1F2A1A4A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("💳", fontSize = 16.sp)
                                    Text(
                                        text = "CC Outstanding",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", overallOutstanding)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (overallOutstanding > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isDark) Color(0x1F2A1A4A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("📊", fontSize = 16.sp)
                                    Text(
                                        text = "CC Utilization",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$overallUtilizationPercent%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (overallUtilizationPercent > 30) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
                                )
                            }
                        }

                        // Divider line below credit cards section
                        HorizontalDivider(
                            color = if (isDark) Color(0x1AFFFFFF) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )

                        // --- BOTTOM SECTION: MONTH OPENING & CARRY FORWARD STATUS ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Month Opening",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", currentRecord.openingBalance)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Carry Forward Status Row (Pill with indicators)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .background(
                                        if (carryForwardEnabledRaw) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (carryForwardEnabledRaw) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outlineVariant,
                                            CircleShape
                                        )
                                )
                                Text(
                                    text = if (carryForwardEnabledRaw) "Forwarding On" else "Forwarding Off",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (carryForwardEnabledRaw) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // INCOME & EXPENSE SUMMARY (TWO EQUAL CARDS WITH ANIMATED COUNTERS)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Income card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (MaterialTheme.colorScheme.isDark) 0.dp else 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Income",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    Icons.Default.TrendingUp,
                                    contentDescription = "Income Trend",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            AnimatedContent(
                                targetState = stats.income,
                                transitionSpec = {
                                    slideInVertically { height -> height } togetherWith slideOutVertically { height -> -height }
                                },
                                label = "IncomeAnimator"
                            ) { targetValue ->
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%,.1f", targetValue)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }

                    // Expense card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (MaterialTheme.colorScheme.isDark) 0.dp else 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Expenses",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    Icons.Default.TrendingDown,
                                    contentDescription = "Expense Trend",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            AnimatedContent(
                                targetState = stats.expense,
                                transitionSpec = {
                                    slideInVertically { height -> height } togetherWith slideOutVertically { height -> -height }
                                },
                                label = "ExpenseAnimator"
                            ) { targetValue ->
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%,.1f", targetValue)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }



            // FINANCIAL OVERVIEW CARD WITH MODERN GRID LAYOUT
            item {
                val currency = activeAcc?.currency ?: "₹"
                val totalSavingsTarget = savingsGoalsList.sumOf { it.targetAmount }
                val totalSavingsCurrent = savingsGoalsList.sumOf { it.currentAmount }
                
                val totalBudgetLimit = budgets.sumOf { it.amount }
                val totalBudgetSpent = budgets.sumOf { b -> 
                    txs.filter { it.type == "EXPENSE" && it.category.equals(b.category, ignoreCase = true) }
                       .sumOf { it.amount } 
                }

                val outstandingLent = dStats.outstandingLent
                val remainingBorrowed = dStats.remainingBorrowed

                val nowMs = System.currentTimeMillis()
                val endOfWeekMs = nowMs + (7 * 24 * 60 * 60 * 1000L)
                val upcomingDueCount = debtEntriesList.count { entry ->
                    (entry.status.uppercase() != "RECOVERED" && entry.status.uppercase() != "REPAID") &&
                    (entry.dueDate <= endOfWeekMs)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section Header
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Dashboard,
                                    contentDescription = "Overview",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                "Financial Overview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Grid of overview tiles
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // 1. Savings Progress
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Savings Goal Progress", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = if (totalSavingsTarget > 0) {
                                        "$currency${String.format(Locale.getDefault(), "%,.0f", totalSavingsCurrent)} / $currency${String.format(Locale.getDefault(), "%,.0f", totalSavingsTarget)}"
                                    } else {
                                        "No Active Goals"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (totalSavingsTarget > 0) {
                                LinearProgressIndicator(
                                    progress = { (totalSavingsCurrent / totalSavingsTarget).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // 2. Budget Usage
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.PieChart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Budget Usage", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = if (totalBudgetLimit > 0) {
                                        "$currency${String.format(Locale.getDefault(), "%,.0f", totalBudgetSpent)} / $currency${String.format(Locale.getDefault(), "%,.0f", totalBudgetLimit)}"
                                    } else {
                                        "No Budgets Set"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (totalBudgetLimit > 0) {
                                LinearProgressIndicator(
                                    progress = { (totalBudgetSpent / totalBudgetLimit).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = if (totalBudgetSpent > totalBudgetLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // 2-row table for Lending / Borrowing & Reminders
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Money Lent mini-card
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                                            Text("Money Lent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("$currency${String.format(Locale.getDefault(), "%,.0f", outstandingLent)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }

                                // Money Borrowed mini-card
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                            Text("Money Borrowed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("$currency${String.format(Locale.getDefault(), "%,.0f", remainingBorrowed)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }

                            if (activeCcList.isNotEmpty()) {
                                Card(
                                    onClick = { viewModel.currentScreen.value = "credit_cards" },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CreditCard,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF59E0B),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "Credit Cards",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            
                                            Box(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "${activeCcList.size} Active",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Outstanding",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "$currency${String.format(Locale.getDefault(), "%,.1f", overallOutstanding)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (overallOutstanding > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                            )

                                            Text(
                                                text = "|",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )

                                            Text(
                                                text = "Utilization",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "$overallUtilizationPercent%",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (overallUtilizationPercent > 30) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
                                            )

                                            Spacer(modifier = Modifier.weight(1f))

                                            Icon(
                                                imageVector = Icons.AutoMirrored.Default.ArrowForward,
                                                contentDescription = "Go to Credit Cards",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(
                                                        if (overallUtilizationPercent > 30) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                                                        CircleShape
                                                    )
                                            )
                                            Text(
                                                text = if (overallUtilizationPercent > 30) "High Usage (>30%)" else "Safe Zone (<30%)",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (overallUtilizationPercent > 30) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. Upcoming Due Reminders banner
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (upcomingDueCount > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (upcomingDueCount > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = if (upcomingDueCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (upcomingDueCount > 0) {
                                            "$upcomingDueCount Due This Week"
                                        } else {
                                            "No Upcoming Due Reminders"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (upcomingDueCount > 0) FontWeight.Bold else FontWeight.Normal,
                                        color = if (upcomingDueCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(
                                    onClick = { viewModel.currentScreen.value = "more" },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("View Debts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
            
            // RECENT TRANSACTIONS HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { viewModel.currentScreen.value = "transactions" }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("See All", color = MaterialTheme.colorScheme.primary)
                            Icon(Icons.AutoMirrored.Default.ArrowForward, contentDescription = "See All", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // SKELETON LOADING OR TRANSACTION LIST WITH HOVER TAP RIPPLES
            if (txs.isEmpty()) {
                item {
                    // Beautiful custom modern design skeletons
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = "Wallet",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Text(
                                "No transaction entries tracked in this account yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { onAddTransactionClick("EXPENSE") },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Insert First Entry")
                            }
                        }
                    }
                }
            } else {
                items(txs.take(5)) { tx ->
                    // Interactive, swipable or standard beautiful cards list
                    var isHovered by remember { mutableStateOf(false) }
                    Card(
                        onClick = { onEditTransactionClick(tx) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isHovered) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val isExpense = tx.type == "EXPENSE"
                            val colorAccent = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                            val prefix = if (isExpense) "-" else "+"

                            // Category Avatar
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(colorAccent.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(tx.category),
                                    contentDescription = tx.category,
                                    tint = colorAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = tx.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "• Pay via ${tx.paymentMode}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = "$prefix₹${String.format(Locale.getDefault(), "%,.2f", tx.amount)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = colorAccent,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }

        // --- CREDIT CARD ALERTS POPUP MODAL (7, 3, 1 Days warnings) ---
        if (activeAndUndismissedAlerts.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = {
                    // Snooze/dismiss these alerts for the current session
                    val newlyDismissed = dismissedAlerts.toMutableSet()
                    activeAndUndismissedAlerts.forEach { (card, daysLeft, _) ->
                        newlyDismissed.add("${card.id}_${daysLeft}")
                    }
                    dismissedAlerts = newlyDismissed
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⏳", fontSize = 24.sp)
                        Text(
                            text = "Credit Card Bill Alert!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "You have approaching credit card deadlines. Please pay on time to avoid fees and protect your score.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        activeAndUndismissedAlerts.forEach { (card, daysLeft, outstanding) ->
                            val (alertColor, alertLabel) = when (daysLeft) {
                                7 -> Color(0xFFD97706) to "⏳ 7 Days Left! (Important)"
                                3 -> Color(0xFFEA580C) to "⚠️ 3 Days Left! (Urgent)"
                                1 -> Color(0xFFDC2626) to "🚨 1 Day Left! (CRITICAL)"
                                else -> Color.Gray to "⏳ Due Soon"
                            }
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(alertColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .border(1.dp, alertColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = card.cardIssuer + " " + card.cardName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = alertLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = alertColor
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Outstanding: ₹${String.format(Locale.getDefault(), "%,.2f", outstanding)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (card.id >= 0) { // Only allow direct pay if it is not a demo test card
                                        TextButton(
                                            onClick = {
                                                repayAlertAmountStr = outstanding.toString()
                                                repayAlertSource = "Bank"
                                                repayAlertNotes = "Paying alert bill"
                                                activeRepaymentCardForAlerts = card
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Repay Now", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Text(
                                            text = "(Test Simulation)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newlyDismissed = dismissedAlerts.toMutableSet()
                            activeAndUndismissedAlerts.forEach { (card, daysLeft, _) ->
                                newlyDismissed.add("${card.id}_${daysLeft}")
                            }
                            dismissedAlerts = newlyDismissed
                        }
                    ) {
                        Text("Got It")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            val newlyDismissed = dismissedAlerts.toMutableSet()
                            activeAndUndismissedAlerts.forEach { (card, daysLeft, _) ->
                                newlyDismissed.add("${card.id}_${daysLeft}")
                            }
                            dismissedAlerts = newlyDismissed
                        }
                    ) {
                        Text("Snooze Warning")
                    }
                }
            )
        }

        // --- DASHBOARD ALERTS DIRECT REPAYMENT DIALOG ---
        val activeRepayFromAlert = activeRepaymentCardForAlerts
        if (activeRepayFromAlert != null) {
            val repayments = repaymentsAll.filter { it.creditCardId == activeRepayFromAlert.id }
            val cardSpends = txs.filter { it.type == "EXPENSE" && it.creditCardId == activeRepayFromAlert.id }.sumOf { it.amount }
            val cardPayments = repayments.sumOf { it.amount }
            val outstanding = (cardSpends - cardPayments).coerceAtLeast(0.0)
            val context = LocalContext.current

            AlertDialog(
                onDismissRequest = { activeRepaymentCardForAlerts = null },
                title = {
                    Text(
                        text = "Repay Bill: ${activeRepayFromAlert.cardName}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Outstanding dues: ₹${String.format(Locale.getDefault(), "%,.2f", outstanding)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = repayAlertAmountStr,
                            onValueChange = { repayAlertAmountStr = it },
                            label = { Text("Repayment Amount (₹)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        // Quick select buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { repayAlertAmountStr = outstanding.toString() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Full Dues", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { repayAlertAmountStr = (outstanding * 0.5).toString() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha=0.6f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("50% Partial", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Deduct Dues From:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Cash", "Bank").forEach { src ->
                                    val isSelected = repayAlertSource == src
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { repayAlertSource = src },
                                        label = { Text(src, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = repayAlertNotes,
                            onValueChange = { repayAlertNotes = it },
                            label = { Text("Notes (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val repayAmt = repayAlertAmountStr.toDoubleOrNull() ?: 0.0
                            if (repayAmt > 0.0) {
                                viewModel.insertCreditCardRepayment(
                                    card = activeRepayFromAlert,
                                    amount = repayAmt,
                                    paymentSource = repayAlertSource,
                                    notes = repayAlertNotes.trim()
                                )
                                Toast.makeText(context, "Repayment registered and cash/bank updated!", Toast.LENGTH_SHORT).show()
                                activeRepaymentCardForAlerts = null
                                
                                // Remove from active alarms as it is settled
                                val newlyDismissed = dismissedAlerts.toMutableSet()
                                newlyDismissed.add("${activeRepayFromAlert.id}_1")
                                newlyDismissed.add("${activeRepayFromAlert.id}_3")
                                newlyDismissed.add("${activeRepayFromAlert.id}_7")
                                dismissedAlerts = newlyDismissed
                            } else {
                                Toast.makeText(context, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Pay Bill")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeRepaymentCardForAlerts = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// Quick action child button layout
@Composable
fun ExtendedActionButton(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Help items row
@Composable
fun IconButtonWithLabel(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

// Smooth Custom coordinate Running balance chart Composable
@Composable
fun RunningBalanceLineGraph(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    val sorted = remember(transactions) {
        transactions.sortedBy { it.timestamp }
    }
    val runningBalances = remember(sorted) {
        var bal = 0.0
        sorted.map { tx ->
            if (tx.type == "INCOME") bal += tx.amount else bal -= tx.amount
            bal
        }
    }

    if (runningBalances.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Establish transaction logs to trace balance lines",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val maxVal = runningBalances.maxOrNull() ?: 1.0
    val minVal = runningBalances.minOrNull() ?: 0.0
    val range = if (maxVal == minVal) 1.0 else (maxVal - minVal)

    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val points = runningBalances.mapIndexed { index, balance ->
            val x = if (runningBalances.size > 1) {
                index.toFloat() / (runningBalances.size - 1) * width
            } else {
                width / 2f
            }
            val y = height - ((balance - minVal) / range * height).toFloat()
            Offset(x, y.coerceIn(0f, height))
        }

        // Fading gradient beneath trend coordinates
        val fillPath = androidx.compose.ui.graphics.Path()
        if (points.isNotEmpty()) {
            fillPath.moveTo(0f, height)
            points.forEach { fillPath.lineTo(it.x, it.y) }
            fillPath.lineTo(width, height)
            fillPath.close()
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f),
                        Color.Transparent
                    )
                )
            )
        }

        // Draw primary line
        val strokePath = androidx.compose.ui.graphics.Path()
        if (points.isNotEmpty()) {
            strokePath.moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                strokePath.lineTo(points[i].x, points[i].y)
            }
            drawPath(
                path = strokePath,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            // Draw final neon dot
            drawCircle(
                color = primaryColor,
                radius = 5.dp.toPx(),
                center = points.last()
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = points.last()
            )
        }
    }
}


// --- TRANSACTIONS PANE (WITH HIGH-DAMP FILTER MATRIX) ---
@Composable
fun TransactionsScreen(
    viewModel: ExpenseViewModel,
    onEditTransactionClick: (Transaction) -> Unit,
    onAddTransactionClick: () -> Unit
) {
    val activeAcc: Account? = viewModel.activeAccount.collectAsStateWithLifecycle().value
    val list: List<Transaction> = viewModel.filteredTransactions.collectAsStateWithLifecycle().value
    val availableCats: List<String> = viewModel.availableCategories.collectAsStateWithLifecycle().value

    val query: String = viewModel.searchQuery.collectAsStateWithLifecycle().value
    val activeCat: String = viewModel.categoryFilter.collectAsStateWithLifecycle().value
    val activeType: String = viewModel.typeFilter.collectAsStateWithLifecycle().value
    val activeRange: String = viewModel.dateRangeFilter.collectAsStateWithLifecycle().value

    val customStart: Long? = viewModel.customStartDate.collectAsStateWithLifecycle().value
    val customEnd: Long? = viewModel.customEndDate.collectAsStateWithLifecycle().value

    val currencySymbol = activeAcc?.currency ?: "$"
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search & Add row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.searchQuery.value = it },
                label = { Text("Search transactions...") },
                placeholder = { Text("Find title or notes") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            )

            FloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(54.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }

        // Expanded filters matrix
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Text("Filter Matrix", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            // Category & Type select dropdown filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Type Filter Filter Dropdown
                var expandedType by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { expandedType = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (activeType == "All") "All Types" else if (activeType == "EXPENSE") "Expenses" else "Income",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Types") },
                            onClick = { viewModel.typeFilter.value = "All"; expandedType = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Expenses") },
                            onClick = { viewModel.typeFilter.value = "EXPENSE"; expandedType = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Income") },
                            onClick = { viewModel.typeFilter.value = "INCOME"; expandedType = false }
                        )
                    }
                }

                // Category Filter Filter Dropdown
                var expandedCat by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { expandedCat = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (activeCat == "All") "All Categories" else activeCat,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = expandedCat,
                        onDismissRequest = { expandedCat = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Categories") },
                            onClick = { viewModel.categoryFilter.value = "All"; expandedCat = false }
                        )
                        availableCats.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = { viewModel.categoryFilter.value = cat; expandedCat = false }
                            )
                        }
                    }
                }
            }

            // Date Range Filter Dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var expandedDate by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1.2f)) {
                    OutlinedButton(
                        onClick = { expandedDate = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Time: $activeRange",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = expandedDate,
                        onDismissRequest = { expandedDate = false }
                    ) {
                        listOf("All", "Today", "This Week", "This Month", "This Year", "Custom").forEach { dr ->
                            DropdownMenuItem(
                                text = { Text(dr) },
                                onClick = { viewModel.dateRangeFilter.value = dr; expandedDate = false }
                            )
                        }
                    }
                }

                // Custom dates triggers if Range == Custom
                if (activeRange == "Custom") {
                    Row(
                        modifier = Modifier.weight(2f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Start Date
                        Button(
                            onClick = {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        cal.set(y, m, d, 0, 0, 0)
                                        viewModel.customStartDate.value = cal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Text(
                                if (customStart != null) SimpleDateFormat("yy-MM-dd", Locale.getDefault()).format(Date(customStart)) else "From",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        // End Date
                        Button(
                            onClick = {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        cal.set(y, m, d, 23, 59, 59)
                                        viewModel.customEndDate.value = cal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Text(
                                if (customEnd != null) SimpleDateFormat("yy-MM-dd", Locale.getDefault()).format(Date(customEnd)) else "To",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        // Active items count feed
        Text(
            text = "Showing ${list.size} matching items",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Transactions Feed Lazy List
        if (list.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Default.List,
                        contentDescription = "Empty filter",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        "No transaction matching criteria.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(list) { tx ->
                    TransactionRowItem(
                        tx = tx,
                        currencySymbol = currencySymbol,
                        onClick = { onEditTransactionClick(tx) }
                    )
                }
            }
        }
    }
}


// --- ROW RENDER FOR TRANSACTION ITEM ---
@Composable
fun TransactionRowItem(
    tx: Transaction,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val viewModel: ExpenseViewModel = viewModel()
    var expanded by remember { mutableStateOf(false) }
    var itemsList by remember { mutableStateOf<List<TransactionItem>>(emptyList()) }

    LaunchedEffect(tx.id) {
        viewModel.getItemsForTransaction(tx.id).collect { items ->
            itemsList = items
        }
    }

    val isExpense = tx.type == "EXPENSE"
    val colorAccent = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
    val mathPrefix = if (isExpense) "-" else "+"

    Card(
        onClick = {
            if (itemsList.isNotEmpty()) {
                expanded = !expanded
            } else {
                onClick()
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(tx.category),
                    contentDescription = tx.category,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Description details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tx.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$mathPrefix$currencySymbol${String.format(Locale.getDefault(), "%,.1f", tx.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorAccent
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = tx.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (itemsList.isNotEmpty()) {
                            Text(
                                text = "• ${itemsList.size} Items",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tx.paymentMode,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (tx.imagePath != null) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "Receipt attached",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Text(
                        text = formatTxDate(tx.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (tx.remarks.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tx.remarks,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (expanded && itemsList.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    Text(
                        text = "Itemized Breakdown",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    itemsList.forEach { item ->
                        val itemTotal = item.quantity * item.pricePerUnit
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${item.quantity} ${item.unitType} × $currencySymbol${String.format("%.2f", item.pricePerUnit)}" + 
                                            if (item.note.isNotBlank()) " (${item.note})" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "$currencySymbol${String.format("%.2f", itemTotal)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = onClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit or Delete Transaction", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


// --- BUDGETS / LIMITS PANE ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: ExpenseViewModel
) {
    val context = LocalContext.current
    val activeAcc: Account? = viewModel.activeAccount.collectAsStateWithLifecycle().value
    val availableCats: List<String> = viewModel.availableCategories.collectAsStateWithLifecycle().value
    val txs: List<Transaction> = viewModel.transactions.collectAsStateWithLifecycle().value
    val budgets: List<Budget> = viewModel.budgets.collectAsStateWithLifecycle().value
    val savingsGoalsList: List<SavingsGoal> = viewModel.savingsGoals.collectAsStateWithLifecycle().value

    val currency = activeAcc?.currency ?: "₹"
    val calendar = Calendar.getInstance()
    val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)

    // Calculate spendings for current month
    val currentMonthTxs = txs.filter { tx ->
        tx.type == "EXPENSE" &&
                SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(tx.timestamp)) == currentMonthStr
    }

    val vmTabIdx by viewModel.moreSelectedTabIdx.collectAsStateWithLifecycle()
    var selectedTabIdx by remember { mutableIntStateOf(0) }
    LaunchedEffect(vmTabIdx) {
        selectedTabIdx = vmTabIdx
    }
    val tabLabels = listOf("Budget", "Savings", "Analytics", "Khata")

    var activeGoalForAddTransaction by remember { mutableStateOf<SavingsGoal?>(null) }
    var activeGoalForWithdrawTransaction by remember { mutableStateOf<SavingsGoal?>(null) }
    var activeGoalForHistory by remember { mutableStateOf<SavingsGoal?>(null) }

    var savingsActionAmount by remember { mutableStateOf("") }
    var savingsActionNote by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab Segmented Capsules
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabLabels.forEachIndexed { index, label ->
                    val isSelected = selectedTabIdx == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { 
                                selectedTabIdx = index
                                viewModel.moreSelectedTabIdx.value = index
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Tab Contents Screen Router
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTabIdx) {
                1 -> {
                    // TAB 1: SAVINGS GOALS
                    var isAddGoalDialogVisible by remember { mutableStateOf(false) }
                    var newGoalName by remember { mutableStateOf("") }
                    var newGoalTarget by remember { mutableStateOf("") }
                    var newGoalSaved by remember { mutableStateOf("") }
                    var newGoalDueDate by remember { mutableStateOf("") }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Savings Objectives",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Button(
                                    onClick = {
                                        newGoalName = ""
                                        newGoalTarget = ""
                                        newGoalSaved = ""
                                        newGoalDueDate = ""
                                        isAddGoalDialogVisible = true
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Goal")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Goal")
                                }
                            }
                        }

                        if (savingsGoalsList.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0x0FCD9BFF)),
                                    border = BorderStroke(1.dp, Color(0x20CD9BFF)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Savings,
                                            contentDescription = null,
                                            tint = Color(0xFFCD9BFF),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            "No savings objectives created yet. Anchor your future targets today!",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFFAF5FF).copy(alpha = 0.6f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(savingsGoalsList) { goal ->
                                val progressRatio = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                                val progressPercent = (progressRatio * 100).toInt()
                                val remainingAmount = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Stars, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                Text(
                                                    text = goal.name,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            IconButton(onClick = { viewModel.deleteSavingsGoal(goal) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Wipe Goal", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Saved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("$currency${String.format(Locale.getDefault(), "%,.0f", goal.currentAmount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.tertiary)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Target", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("$currency${String.format(Locale.getDefault(), "%,.0f", goal.targetAmount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                        }

                                        // Progress bar
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            LinearProgressIndicator(
                                                progress = { progressRatio },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.outlineVariant
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Completed: $progressPercent%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                Text("Target Date: ${goal.dueDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (remainingAmount > 0) {
                                                Text(
                                                    "Remaining: $currency${String.format(Locale.getDefault(), "%,.0f", remainingAmount)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            } else {
                                                Text(
                                                    "Target Mastered! 🎉",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            FilledTonalButton(
                                                onClick = {
                                                    activeGoalForAddTransaction = goal
                                                    savingsActionAmount = ""
                                                    savingsActionNote = ""
                                                },
                                                modifier = Modifier.weight(1f).height(36.dp),
                                                contentPadding = PaddingValues(0.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Add", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                            }

                                            FilledTonalButton(
                                                onClick = {
                                                    activeGoalForWithdrawTransaction = goal
                                                    savingsActionAmount = ""
                                                    savingsActionNote = ""
                                                },
                                                modifier = Modifier.weight(1f).height(36.dp),
                                                contentPadding = PaddingValues(0.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), contentColor = MaterialTheme.colorScheme.onErrorContainer)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Withdraw", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    activeGoalForHistory = goal
                                                },
                                                modifier = Modifier.weight(1f).height(36.dp),
                                                contentPadding = PaddingValues(0.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("History", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Dialog to Add Savings Goal
                    if (isAddGoalDialogVisible) {
                        AlertDialog(
                            onDismissRequest = { isAddGoalDialogVisible = false },
                            title = { Text("Add savings objective", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = newGoalName,
                                        onValueChange = { newGoalName = it },
                                        label = { Text("Goal Name") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    OutlinedTextField(
                                        value = newGoalTarget,
                                        onValueChange = { newGoalTarget = it },
                                        label = { Text("Target Amount") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    OutlinedTextField(
                                        value = newGoalSaved,
                                        onValueChange = { newGoalSaved = it },
                                        label = { Text("Current Saved Amount") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    OutlinedTextField(
                                        value = newGoalDueDate,
                                        onValueChange = { newGoalDueDate = it },
                                        label = { Text("Target Due Date (e.g. Dec 2026)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val target = newGoalTarget.toDoubleOrNull() ?: 0.0
                                        val saved = newGoalSaved.toDoubleOrNull() ?: 0.0
                                        if (newGoalName.isNotBlank() && target > 0) {
                                            viewModel.insertSavingsGoal(
                                                name = newGoalName,
                                                targetAmount = target,
                                                currentAmount = saved,
                                                dueDate = if (newGoalDueDate.isBlank()) "No Limit" else newGoalDueDate
                                            )
                                            isAddGoalDialogVisible = false
                                            Toast.makeText(context, "Savings target calibrated!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                                ) {
                                    Text("Define Target")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { isAddGoalDialogVisible = false }) {
                                    Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        )
                    }

                    // Dialogue to Add Money to Savings Goal
                    if (activeGoalForAddTransaction != null) {
                        val goal = activeGoalForAddTransaction!!
                        AlertDialog(
                            onDismissRequest = { activeGoalForAddTransaction = null },
                            title = { Text("Deposit: ${goal.name}", fontWeight = FontWeight.Bold) },
                            containerColor = MaterialTheme.colorScheme.surface,
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        "Target: $currency${String.format("%,.0f", goal.targetAmount)}  •  Saved: $currency${String.format("%,.0f", goal.currentAmount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedTextField(
                                        value = savingsActionAmount,
                                        onValueChange = { savingsActionAmount = it },
                                        label = { Text("Deposit Amount ($currency)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = savingsActionNote,
                                        onValueChange = { savingsActionNote = it },
                                        label = { Text("Optional Note") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val amt = savingsActionAmount.toDoubleOrNull()
                                        if (amt != null && amt > 0.0) {
                                            viewModel.addSavingsGoalTransaction(goal, amt, savingsActionNote.trim())
                                            Toast.makeText(context, "$currency$amt deposited into ${goal.name}!", Toast.LENGTH_SHORT).show()
                                            activeGoalForAddTransaction = null
                                        } else {
                                            Toast.makeText(context, "Please enter a valid deposit amount", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Text("Add Money")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { activeGoalForAddTransaction = null }) {
                                    Text("Cancel", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                    }

                    // Dialogue to Withdraw Money from Savings Goal
                    if (activeGoalForWithdrawTransaction != null) {
                        val goal = activeGoalForWithdrawTransaction!!
                        AlertDialog(
                            onDismissRequest = { activeGoalForWithdrawTransaction = null },
                            title = { Text("Withdraw: ${goal.name}", fontWeight = FontWeight.Bold) },
                            containerColor = MaterialTheme.colorScheme.surface,
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        "Available Balance: $currency${String.format("%,.0f", goal.currentAmount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedTextField(
                                        value = savingsActionAmount,
                                        onValueChange = { savingsActionAmount = it },
                                        label = { Text("Withdrawal Amount ($currency)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = savingsActionNote,
                                        onValueChange = { savingsActionNote = it },
                                        label = { Text("Reason / Note") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val amt = savingsActionAmount.toDoubleOrNull()
                                        if (amt != null && amt > 0.0) {
                                            if (amt > goal.currentAmount) {
                                                Toast.makeText(context, "Error: Cannot withdraw more than saved $currency${goal.currentAmount}!", Toast.LENGTH_LONG).show()
                                            } else {
                                                viewModel.withdrawSavingsGoalTransaction(goal, amt, savingsActionNote.trim())
                                                Toast.makeText(context, "$currency$amt withdrawn from ${goal.name} successfully!", Toast.LENGTH_SHORT).show()
                                                activeGoalForWithdrawTransaction = null
                                            }
                                        } else {
                                            Toast.makeText(context, "Please enter a valid withdrawal amount", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Withdraw")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { activeGoalForWithdrawTransaction = null }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    // Dialogue for Savings history logs
                    if (activeGoalForHistory != null) {
                        val goal = activeGoalForHistory!!
                        val historyList by viewModel.getSavingsGoalHistory(goal.id).collectAsStateWithLifecycle(initialValue = emptyList())
                        val dFormatter = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())

                        AlertDialog(
                            onDismissRequest = { activeGoalForHistory = null },
                            title = { Text("${goal.name} History", fontWeight = FontWeight.Bold) },
                            containerColor = MaterialTheme.colorScheme.surface,
                            text = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 400.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (historyList.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "No history entries recorded yet for this goal.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    } else {
                                        historyList.forEach { hist ->
                                            val isDeposit = hist.action == "Added"
                                            val sign = if (isDeposit) "+" else "-"
                                            val amountCol = if (isDeposit) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                            val iconVal = if (isDeposit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward
                                            val actionColor = if (isDeposit) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)

                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .background(actionColor, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = iconVal,
                                                            contentDescription = null,
                                                            tint = amountCol,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = if (isDeposit) "Deposited" else "Withdrawn",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = "$sign$currency${String.format("%.0f", hist.amount)}",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = amountCol
                                                            )
                                                        }
                                                        Text(
                                                            text = dFormatter.format(Date(hist.timestamp)),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        if (hist.note.isNotBlank()) {
                                                            Text(
                                                                text = hist.note,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.padding(top = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = { activeGoalForHistory = null }) {
                                    Text("Close")
                                }
                            }
                        )
                    }
                }
                0 -> {
                    // TAB 2: BUDGET LIMITS WITH 90% WARNING & METRICS SUMMARY
                    var selectedLimitCategory by remember { mutableStateOf("Food") }
                    var limitAmountStr by remember { mutableStateOf("") }

                    val totalBudgetAllocated = budgets.sumOf { it.amount }
                    val totalActualSpentInAllocated = budgets.sumOf { b ->
                        currentMonthTxs.filter { it.category.equals(b.category, ignoreCase = true) }.sumOf { it.amount }
                    }
                    val remainingBudgetCalc = (totalBudgetAllocated - totalActualSpentInAllocated).coerceAtLeast(0.0)

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        // Monthly Budget Summary Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Monthly Budgets Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Total Limits", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("$currency${String.format(Locale.getDefault(), "%,.0f", totalBudgetAllocated)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Column {
                                            Text("Actual Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("$currency${String.format(Locale.getDefault(), "%,.0f", totalActualSpentInAllocated)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Remaining Buffer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("$currency${String.format(Locale.getDefault(), "%,.0f", remainingBudgetCalc)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                        }
                                    }
                                }
                            }
                        }

                        // Form input to add limits
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Establish Category Budget Limit", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    
                                    // Categories Dropdown layout
                                    var isCategoryExpanded by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { isCategoryExpanded = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Category: $selectedLimitCategory", fontWeight = FontWeight.Bold)
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        }
                                        DropdownMenu(
                                            expanded = isCategoryExpanded,
                                            onDismissRequest = { isCategoryExpanded = false },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            availableCats.forEach { cat ->
                                                DropdownMenuItem(
                                                    text = { Text(cat, color = MaterialTheme.colorScheme.onSurface) },
                                                    onClick = {
                                                        selectedLimitCategory = cat
                                                        isCategoryExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = limitAmountStr,
                                        onValueChange = { limitAmountStr = it },
                                        label = { Text("Limit Amount ($currency)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    )

                                    Button(
                                        onClick = {
                                            val amt = limitAmountStr.toDoubleOrNull() ?: 0.0
                                            if (amt > 0) {
                                                viewModel.setCategoryBudget(selectedLimitCategory, amt)
                                                limitAmountStr = ""
                                                Toast.makeText(context, "Budget limit recorded!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = limitAmountStr.isNotBlank() && limitAmountStr.toDoubleOrNull() != null,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                                    ) {
                                        Text("Set Budget Limit")
                                    }
                                }
                            }
                        }

                        // List of Limits
                        if (budgets.isEmpty()) {
                            item {
                                Text("No active category budget ceilings established yet.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.padding(8.dp))
                            }
                        } else {
                            items(budgets) { b ->
                                val spent = currentMonthTxs.filter { it.category.equals(b.category, ignoreCase = true) }.sumOf { it.amount }
                                val progress = if (b.amount > 0) (spent / b.amount).toFloat().coerceIn(0f, 1f) else 0f
                                val percent = (progress * 100).toInt()

                                // Color Indicator logic: Green < 75%, Orange 75%-90%, Red >= 90%
                                val alertColor = when {
                                    progress >= 0.90f -> MaterialTheme.colorScheme.error // Red
                                    progress >= 0.75f -> Color(0xFFF59E0B) // Orange
                                    else -> MaterialTheme.colorScheme.tertiary // Green
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, alertColor.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Box(modifier = Modifier.size(8.dp).background(alertColor, CircleShape))
                                                Text(b.category, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                            IconButton(onClick = { viewModel.deleteBudget(b) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Wipe Limit", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }

                                        // Progress Bar
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = alertColor,
                                            trackColor = MaterialTheme.colorScheme.outlineVariant
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Spent: $currency${String.format(Locale.getDefault(), "%,.0f", spent)} ($percent%)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "Ceiling: $currency${b.amount.toInt()}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = alertColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Explicit Red / Orange Warning Text Labels
                                        if (progress >= 1f) {
                                            Text("⚠️ OVER BUDGET LIMIT! Cease secondary expenditures immediate.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                        } else if (progress >= 0.90f) {
                                            Text("⚠️ CRITICAL WARNING: Spent above 90% of allocated limit!", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 3: RICH ANALYTICS CHARTS & personal insights
                    AnalyticsScreen(viewModel = viewModel)
                }
                3 -> {
                    // TAB 4: DEBTS & LENDING (Khata Hub)
                    DebtModuleScreen(viewModel = viewModel)
                }
                1003 -> {
                    val netSavings = 0.0
                    val totalInc = 0.0
                    val totalExp = 0.0
                    val categoryExpenses = txs.filter { it.type == "EXPENSE" }
                        .groupBy { it.category }
                        .mapValues { entry -> entry.value.sumOf { tx -> tx.amount } }
                        .toList()
                        .sortedByDescending { it.second }

                    // count items
                    val categoryCounts = txs.filter { it.type == "EXPENSE" }
                        .groupBy { it.category }
                        .mapValues { entry -> entry.value.size }
                        .toList()
                        .sortedByDescending { it.second }

                    val topCategoryCount = categoryCounts.firstOrNull()?.first ?: "N/A"
                    val topCategoryExpenditure = categoryExpenses.firstOrNull()?.first ?: "N/A"

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        item {
                            Text("Visual Spend Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }

                        // Category Spending Pie/Donut Chart
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Category Expense Distribution", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    if (categoryExpenses.isEmpty()) {
                                        Text("Please insert transaction entries to see pie distribution.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        CategoryDonutChart(
                                            data = categoryExpenses,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            currencySymbol = currency
                                        )
                                    }
                                }
                            }
                        }

                        // Income vs Expense Bar Chart
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Flow Comparison (In vs Out)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    BarChartComparing(
                                        data = listOf(
                                            "Inflows" to totalInc,
                                            "Outflows" to totalExp
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp),
                                        barColor = MaterialTheme.colorScheme.primary,
                                        currencySymbol = currency
                                    )
                                }
                            }
                        }

                        // Savings Trend (Running Balance)
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Savings Trend Curve", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    RunningBalanceLineGraph(
                                        transactions = txs,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                    )
                                }
                            }
                        }

                        // Spending Insights Card
                        item {
                            val insightText = if (netSavings < 0) {
                                "Your spending has exceeded your income this month. Consider tracking secondary outlays or setting a monthly cap on high-value categories of $topCategoryExpenditure."
                            } else {
                                "Excellent job keeping a positive cashflow buffer! Transferring excess savings of $currency${String.format(Locale.getDefault(), "%,.0f", netSavings)} into active savings goals will accelerate your objectives."
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Text("Personal Spending Analytics Insights", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Most Used Category (Freq)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(topCategoryCount, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Highest Expense Category (Value)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(topCategoryExpenditure, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Aesthetic Flow Status", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(if (netSavings >= 0) "Healthy Reserves" else "Vulnerable Reserves", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (netSavings >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = insightText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
// --- REPORTS & GRAPH ANALYTICS PANE ---
@Composable
fun AnalyticsScreen(
    viewModel: ExpenseViewModel
) {
    val activeAcc: Account? = viewModel.activeAccount.collectAsStateWithLifecycle().value
    val list: List<Transaction> = viewModel.transactions.collectAsStateWithLifecycle().value
    val budgets: List<Budget> = viewModel.budgets.collectAsStateWithLifecycle().value
    val creditCards: List<CreditCard> = viewModel.creditCards.collectAsStateWithLifecycle().value
    val repaymentsAll = viewModel.creditCardRepayments.collectAsStateWithLifecycle().value
    val currency = activeAcc?.currency ?: "₹"
    val currentMonthStr = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) }
    val activeBudgets = budgets.filter { it.monthYear == currentMonthStr }

    var activeTimeTab by remember { mutableStateOf("All") } // "All", "Weekly", "Monthly", "Yearly"

    // Process transactions matching time filter
    val calendarInstance = Calendar.getInstance()
    val firstDayOfWeek = calendarInstance.firstDayOfWeek

    val startOfWeek = remember(list) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val startOfMonth = remember(list) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val startOfYear = remember(list) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val filteredList = when (activeTimeTab) {
        "Weekly" -> list.filter { tx -> tx.timestamp >= startOfWeek }
        "Monthly" -> list.filter { tx -> tx.timestamp >= startOfMonth }
        "Yearly" -> list.filter { tx -> tx.timestamp >= startOfYear }
        else -> list
    }

    // Calculations: category spending breakdown
    val categoryExpenses = filteredList
        .filter { tx -> tx.type == "EXPENSE" }
        .groupBy { tx -> tx.category }
        .mapValues { entry -> entry.value.sumOf { tx -> tx.amount } }
        .toList()
        .sortedByDescending { pair -> pair.second }

    // Category frequency analysis
    val categoryCounts = filteredList
        .filter { tx -> tx.type == "EXPENSE" }
        .groupBy { tx -> tx.category }
        .mapValues { entry -> entry.value.size }
        .toList()
        .sortedByDescending { pair -> pair.second }

    val topCategoryCountName = categoryCounts.firstOrNull()?.first ?: "N/A"
    val topCategoryCountVal = categoryCounts.firstOrNull()?.second ?: 0
    val topCategoryExpenditure = categoryExpenses.firstOrNull()?.first ?: "N/A"

    // Historical comparison (monthly) generic list
    val last6MonthsList = remember(list) {
        val formatter = SimpleDateFormat("MMM", Locale.getDefault())
        val map = mutableMapOf<String, Double>()
        for (i in 5 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -i)
            val monthLabel = formatter.format(cal.time)
            map[monthLabel] = 0.0
        }

        list.filter { tx -> tx.type == "EXPENSE" }.forEach { tx ->
            val label = formatter.format(Date(tx.timestamp))
            if (map.containsKey(label)) {
                map[label] = map[label]!! + tx.amount
            }
        }
        map.toList()
    }

    val totalSpent = filteredList.filter { tx -> tx.type == "EXPENSE" }.sumOf { tx -> tx.amount }
    val totalIn = filteredList.filter { tx -> tx.type == "INCOME" }.sumOf { tx -> tx.amount }
    val netSavings = totalIn - totalSpent

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)
    ) {
        // Tab Filter Row (Time Horizon Selection)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Weekly", "Monthly", "Yearly").forEach { tab ->
                    val isSel = activeTimeTab == tab
                    Button(
                        onClick = { activeTimeTab = tab },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(tab, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Summary Statistics Report Card (Monthly & Yearly Reports)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    val reportTitle = when (activeTimeTab) {
                        "Weekly" -> "Weekly Financial Summary"
                        "Monthly" -> "Monthly Statement Report"
                        "Yearly" -> "Yearly Balance Booklet"
                        else -> "All-Time Balance Brief"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(reportTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "M3 Engine Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currency${String.format(Locale.getDefault(), "%,.1f", totalIn)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column {
                            Text("Total Outgoings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currency${String.format(Locale.getDefault(), "%,.1f", totalSpent)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Net Surplus", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "$currency${String.format(Locale.getDefault(), "%,.1f", netSavings)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (netSavings >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Simple Visual progress line of Income vs Expense
                    val totalVolume = (totalIn + totalSpent).coerceAtLeast(1.0)
                    val inflowRatio = (totalIn / totalVolume).toFloat()
                    LinearProgressIndicator(
                        progress = { inflowRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Category Spending Pie-Chart Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Category Spending Breakdown ($activeTimeTab)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    CategoryDonutChart(
                        data = categoryExpenses,
                        modifier = Modifier.fillMaxWidth(),
                        currencySymbol = currency
                    )
                }
            }
        }

        // Flow Comparison (Inflows vs Outflows) Bar Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Inflow vs Outflow Comparison ($activeTimeTab)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    BarChartComparing(
                        data = listOf(
                            "Inflows" to totalIn,
                            "Outflows" to totalSpent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        barColor = MaterialTheme.colorScheme.primary,
                        currencySymbol = currency
                    )
                }
            }
        }

        // Savings Trend (Running Balance) Line Graph
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Savings Trend Curve (Cumulative Balance)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    RunningBalanceLineGraph(
                        transactions = filteredList,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            }
        }

        // Historical comparison month to month (6 Months)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Monthly Outgoings History (Last 6 Months)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    BarChartComparing(
                        data = last6MonthsList,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        barColor = Color(activeAcc?.color ?: 0xFF0D9488.toInt()),
                        currencySymbol = currency
                    )
                }
            }
        }

        // Budget Analytics & Allocation Limits Card (Monthly only)
        if (activeBudgets.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Budget limits Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        activeBudgets.forEach { b ->
                            val spentInCat = list.filter {
                                it.type == "EXPENSE" &&
                                        it.category.equals(b.category, ignoreCase = true) &&
                                        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(it.timestamp)) == currentMonthStr
                            }.sumOf { it.amount }
                            val progress = if (b.amount > 0) (spentInCat / b.amount).toFloat().coerceIn(0f, 1f) else 0f
                            val percent = (progress * 100).toInt()
                            val alertColor = when {
                                progress >= 0.90f -> MaterialTheme.colorScheme.error
                                progress >= 0.75f -> Color(0xFFF59E0B)
                                else -> MaterialTheme.colorScheme.tertiary
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(b.category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("$currency${spentInCat.toInt()} / $currency${b.amount.toInt()}", style = MaterialTheme.typography.bodySmall, color = alertColor, fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = alertColor,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                                if (progress >= 1f) {
                                    Text("🚨 EXCEEDED budget cap of $currency${b.amount.toInt()}!", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                } else if (progress >= 0.90f) {
                                    Text("⚠️ At 90%+ capacity of category budget limit!", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Credit Card Trends, utilization metrics & safety thresholds
        if (creditCards.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Credit Card Debt & Utilization Trends", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        val creditCardData = creditCards.map { card ->
                            val cardSpends = list.filter { it.type == "CARD_SPEND" && it.creditCardId == card.id }.sumOf { it.amount }
                            val cardPayments = repaymentsAll.filter { it.creditCardId == card.id }.sumOf { it.amount }
                            val oust = (cardSpends - cardPayments).coerceAtLeast(0.0)
                            Triple(card, card.creditLimit, oust)
                        }
                        val totalLimit = creditCardData.sumOf { it.second }
                        val totalOutstanding = creditCardData.sumOf { it.third }
                        val safetyLimitVal = totalLimit * 0.30
                        val utilizationPercent = if (totalLimit > 0) (totalOutstanding / totalLimit * 100).toInt() else 0

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Aggregate Credit Limit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$currency${String.format(Locale.getDefault(), "%,.0f", totalLimit)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Aggregate Outstanding Balance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$currency${String.format(Locale.getDefault(), "%,.1f", totalOutstanding)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (totalOutstanding > safetyLimitVal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Aggregate Limit Utilization ratio", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$utilizationPercent% used", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (utilizationPercent > 30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Micro cards of individual credit cards
                            creditCardData.forEach { (card, limit, outstanding) ->
                                val cardRatio = if (limit > 0) (outstanding / limit).toFloat().coerceIn(0f, 1f) else 0f
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(card.cardName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                        Text("${(cardRatio * 100).toInt()}% Used", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (cardRatio > 0.3f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
                                    }
                                    LinearProgressIndicator(
                                        progress = { cardRatio },
                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                        color = if (cardRatio > 0.3f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                }
                            }

                            // Interest & Safety insight warnings
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (utilizationPercent > 30) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = if (utilizationPercent > 30) Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (utilizationPercent > 30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Column {
                                        Text(
                                            text = if (utilizationPercent > 30) "High Credit Score Risk Alert" else "Excellent Credit Utilization!",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (utilizationPercent > 30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (utilizationPercent > 30) {
                                                "Staying below 30% utilization (recommended safe balance: $currency${String.format(Locale.getDefault(), "%,.0f", safetyLimitVal)}) protects and optimizes score indexes. Pay down soon."
                                            } else {
                                                "Aggregate ratio is safe (under 30%). Always pay total amount due on scheduled timelines to maintain zero interest metrics."
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Spending Insights & Personal Coaching Recommendations
        item {
            val insightText = if (netSavings < 0) {
                "Your outlays outpaced your inflows in this time selection. Consider reducing secondary discretionary expenditures, limiting high-cost entries of $topCategoryExpenditure, and configuring category ceiling parameters."
            } else {
                "Incredible job staying in the positive balance zone! Directing surplus savings of $currency${String.format(Locale.getDefault(), "%,.0f", netSavings)} into custom savings objectives will escalate timeline milestones."
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Personal Spending Analytics Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Most Frequent Outgoings Category", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$topCategoryCountName ($topCategoryCountVal plays)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Weightiest Expense Category (Value)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(topCategoryExpenditure, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Aesthetic Flow Status", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (netSavings >= 0) "Healthy Reserves" else "Vulnerable Reserves",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (netSavings >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = insightText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


// --- ACCOUNTS / PROFILES CONSOLE PANE ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: ExpenseViewModel
) {
    val accountsList: List<Account> = viewModel.accounts.collectAsStateWithLifecycle().value
    val activeAcc: Account? = viewModel.activeAccount.collectAsStateWithLifecycle().value

    var showCreateForm by remember { mutableStateOf(false) }

    var accountName by remember { mutableStateOf("") }
    var accountPin by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("$") }
    var selectedColor by remember { mutableStateOf(ProfileColors.first()) }
    
    var accountCashBalanceStr by remember { mutableStateOf("") }
    var accountBankBalanceStr by remember { mutableStateOf("") }
    var formErrorMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Profile Management",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = { showCreateForm = !showCreateForm }) {
                    Icon(
                        if (showCreateForm) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "New Profile"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showCreateForm) "Hide Form" else "New Profile")
                }
            }
        }

        // CREATE NEW PROFILE OVERVIEW COLLAPSIBLE CARD
        if (showCreateForm) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Forge New Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Name input
                        OutlinedTextField(
                            value = accountName,
                            onValueChange = { accountName = it },
                            label = { Text("Account Name") },
                            placeholder = { Text("e.g. Business, Personal, Family") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // PIN input
                        OutlinedTextField(
                            value = accountPin,
                            onValueChange = { input ->
                                if (input.length <= 4 && input.all { it.isDigit() }) {
                                    accountPin = input
                                }
                            },
                            label = { Text("Optional PIN Security (4 digits)") },
                            placeholder = { Text("Leave blank for direct opening") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Currency selection
                        var expandedCurrency by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedCurrency,
                            onExpandedChange = { expandedCurrency = it }
                        ) {
                            OutlinedTextField(
                                value = selectedCurrency,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Currency Base") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCurrency) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCurrency,
                                onDismissRequest = { expandedCurrency = false }
                            ) {
                                viewModel.availableCurrencies.forEach { symbol ->
                                    DropdownMenuItem(
                                        text = { Text(symbol) },
                                        onClick = {
                                            selectedCurrency = symbol.split(" ").firstOrNull() ?: "$"
                                            expandedCurrency = false
                                        }
                                    )
                                }
                            }
                        }

                        // Opening Balances
                        Text(
                            "Opening Balances",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        OutlinedTextField(
                            value = accountCashBalanceStr,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.toDoubleOrNull() != null) {
                                    accountCashBalanceStr = input
                                    formErrorMessage = null
                                }
                            },
                            label = { Text("Opening Cash Balance") },
                            placeholder = { Text("e.g. 2500") },
                            leadingIcon = { Text("💵", modifier = Modifier.padding(start = 8.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = accountBankBalanceStr,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.toDoubleOrNull() != null) {
                                    accountBankBalanceStr = input
                                    formErrorMessage = null
                                }
                            },
                            label = { Text("Opening Bank / Online Balance") },
                            placeholder = { Text("e.g. 12000") },
                            leadingIcon = { Text("🏦", modifier = Modifier.padding(start = 8.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Real-time live total preview
                        val inlineCashVal = accountCashBalanceStr.toDoubleOrNull() ?: 0.0
                        val inlineBankVal = accountBankBalanceStr.toDoubleOrNull() ?: 0.0
                        val inlineTotalVal = inlineCashVal + inlineBankVal

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Total Balance Preview",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Cash + Bank Balance",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "$selectedCurrency${String.format(Locale.getDefault(), "%,.2f", inlineTotalVal)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Theme dynamic color row selector
                        Text(
                            "Profile Design Palette Color",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ProfileColors.forEach { colorVal ->
                                val isSelected = selectedColor == colorVal
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorVal))
                                        .clickable { selectedColor = colorVal }
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Error message panel
                        formErrorMessage?.let { errMsg ->
                            Text(
                                text = errMsg,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }

                        Button(
                            onClick = {
                                val cashVal = accountCashBalanceStr.toDoubleOrNull() ?: 0.0
                                val bankVal = accountBankBalanceStr.toDoubleOrNull() ?: 0.0

                                if (cashVal < 0.0 || bankVal < 0.0) {
                                    formErrorMessage = "Opening balances cannot be negative values."
                                    return@Button
                                }

                                viewModel.addAccount(
                                    name = accountName.trim(),
                                    pin = if (accountPin.length == 4) accountPin else null,
                                    color = selectedColor,
                                    currency = selectedCurrency,
                                    openingCashBalance = cashVal,
                                    openingBankBalance = bankVal
                                )
                                accountName = ""
                                accountPin = ""
                                accountCashBalanceStr = ""
                                accountBankBalanceStr = ""
                                formErrorMessage = null
                                showCreateForm = false
                            },
                            enabled = accountName.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Create Profile Workspace")
                        }
                    }
                }
            }
        }

        // Profile lists
        items(accountsList) { acc ->
            val isActive = activeAcc?.id == acc.id
            val profileColor = Color(acc.color)

            Card(
                onClick = { viewModel.selectAccount(acc) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = if (isActive) BorderStroke(2.dp, profileColor) else null,
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(profileColor, CircleShape)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = acc.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Base Currency: ${acc.currency}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (acc.pin != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Secured",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text("PIN Locked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Delete active accounts trigger
                    if (accountsList.size > 1 && isActive) {
                        IconButton(onClick = { viewModel.deleteAccount(acc) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove Account", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}


// --- CENTRAL EXPORT & SYNC SETTINGS PANE ---
@Composable
fun SettingsScreen(
    viewModel: ExpenseViewModel
) {
    val clipManager = LocalClipboardManager.current
    val context = LocalContext.current

    val activeAccount = viewModel.activeAccount.collectAsStateWithLifecycle().value
    var backupActionPending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val lockoutSecondsRemaining by viewModel.lockoutSecondsRemaining.collectAsStateWithLifecycle()

    val triggerBackupAction = { action: () -> Unit ->
        if (activeAccount?.pin != null) {
            backupActionPending = action
        } else {
            action()
        }
    }

    if (backupActionPending != null && activeAccount != null) {
        com.example.ui.components.SecurityActionDialog(
            targetAccount = activeAccount,
            lockoutSecondsRemaining = lockoutSecondsRemaining,
            mode = com.example.ui.components.SecurityActionMode.VERIFY_EXPORT,
            onVerifySuccess = {
                backupActionPending?.invoke()
                backupActionPending = null
            },
            onVerifyFailed = {
                viewModel.verifyCurrentPin("", activeAccount.pin)
            },
            onCancel = {
                backupActionPending = null
            }
        )
    }

    // Export Config State
    var exportScope by remember { mutableStateOf("CURRENT") } // "CURRENT", "ALL"
    var exportRange by remember { mutableStateOf("CURRENT_MONTH") } // "CURRENT_MONTH", "ALL_TIME", "CUSTOM"
    var startDateText by remember { mutableStateOf("2026-05-01") }
    var endDateText by remember { mutableStateOf("2026-05-31") }

    // Backup restore state
    var restoreTextJson by remember { mutableStateOf("") }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    val dateFormatParser = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // --- SECTION HEADER ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (MaterialTheme.colorScheme.isDark) Color(0xFFCD9BFF).copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Settings & Backups",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Export statements or create state backups",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (MaterialTheme.colorScheme.isDark) Color(0xFFCD9BFF).copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Summarize, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Export Statement Builder",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        "Generate detailed ledger documentation. PDF includes spending distribution bar charts, income/outflow KPIs, and itemized lists.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 1. Selector Segment: Scope
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Export Scope Range",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val scopes = listOf("CURRENT" to "Active Profile", "ALL" to "All Profiles (Combined)")
                            scopes.forEach { (scKey, label) ->
                                val isChosen = exportScope == scKey
                                Button(
                                    onClick = { exportScope = scKey },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        contentColor = if (isChosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                  ) {
                                      Text(
                                          label,
                                          style = MaterialTheme.typography.bodySmall,
                                          fontWeight = FontWeight.Bold
                                      )
                                  }
                            }
                        }
                    }

                    // 2. Selector Segment: TimeFrame
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Statements Date Interval",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val ranges = listOf(
                                "CURRENT_MONTH" to "Month",
                                "ALL_TIME" to "All-Time",
                                "CUSTOM" to "Custom"
                            )
                            ranges.forEach { (rnKey, label) ->
                                val isChosen = exportRange == rnKey
                                Button(
                                    onClick = { exportRange = rnKey },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        contentColor = if (isChosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // 3. Custom Range Field (Animated slide visibility)
                    AnimatedVisibility(visible = exportRange == "CUSTOM") {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text(
                                "Enter Custom Bounds (YYYY-MM-DD)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                             ) {
                                OutlinedTextField(
                                    value = startDateText,
                                    onValueChange = { startDateText = it },
                                    label = { Text("Start Date") },
                                    placeholder = { Text("YYYY-MM-DD") },
                                    textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = endDateText,
                                    onValueChange = { endDateText = it },
                                    label = { Text("End Date") },
                                    placeholder = { Text("YYYY-MM-DD") },
                                    textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // 4. Trigger Buttons (Export PDF, Export CSV, Export Excel)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val triggerAction = { format: String ->
                            val sEpoch = if (exportRange == "CUSTOM") {
                                try { dateFormatParser.parse(startDateText.trim())?.time } catch (e: Exception) { null }
                            } else null
                            val eEpoch = if (exportRange == "CUSTOM") {
                                try { dateFormatParser.parse(endDateText.trim())?.time } catch (e: Exception) { null }
                            } else null

                            viewModel.exportFilteredData(
                                context = context,
                                format = format,
                                scope = exportScope,
                                dateRangeType = exportRange,
                                customStart = sEpoch,
                                customEnd = eEpoch
                            ) { isDone ->
                                if (isDone) {
                                    Toast.makeText(context, "$format Share dispatch complete", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Export formatting error. Check bounds format.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }

                        // EXPORT PDF
                        Button(
                            onClick = { triggerAction("PDF") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (MaterialTheme.colorScheme.isDark) Color(0xFF2A1242) else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (MaterialTheme.colorScheme.isDark) Color(0xFFF5EFFF) else MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export PDF Financial Report", fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // EXPORT CSV
                            Button(
                                onClick = { triggerAction("CSV") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (MaterialTheme.colorScheme.isDark) Color(0xFF10281A) else Color(0xFFE8F5E9),
                                    contentColor = if (MaterialTheme.colorScheme.isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                                ),
                                border = BorderStroke(1.dp, (if (MaterialTheme.colorScheme.isDark) Color(0xFF81C784) else Color(0xFF2E7D32)).copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export CSV")
                            }

                            // EXPORT EXCEL
                            Button(
                                onClick = { triggerAction("EXCEL") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (MaterialTheme.colorScheme.isDark) Color(0xFF0F1E36) else Color(0xFFE3F2FD),
                                    contentColor = if (MaterialTheme.colorScheme.isDark) Color(0xFF64B5F6) else Color(0xFF1565C0)
                                ),
                                border = BorderStroke(1.dp, (if (MaterialTheme.colorScheme.isDark) Color(0xFF64B5F6) else Color(0xFF1565C0)).copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.BorderAll, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export Excel")
                            }
                        }
                    }
                }
            }
        }

        // --- DATA MANAGEMENT SECTION HEADER ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Backup,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Data Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // --- FULL DATABASE BACKUP BLOCK ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, if (MaterialTheme.colorScheme.isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.SdCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Complete Multi-Profile Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        "Generate a complete offline JSON file enclosing all profiles, databases, transaction histories, and passcode parameters. Share or save this data string securely.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                triggerBackupAction {
                                    viewModel.exportBackupJson { backup ->
                                        clipManager.setText(AnnotatedString(backup))
                                        Toast.makeText(context, "Full Backup string cloned to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (MaterialTheme.colorScheme.isDark) Color(0xFF2C1E4E) else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (MaterialTheme.colorScheme.isDark) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy JSON text", style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = {
                                triggerBackupAction {
                                    viewModel.exportBackupJson { backup ->
                                        try {
                                            val dateSuffix = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                                            val file = java.io.File(context.cacheDir, "FinTrackerPro_Backup_$dateSuffix.json")
                                            val fos = java.io.FileOutputStream(file)
                                            fos.write(backup.toByteArray(Charsets.UTF_8))
                                            fos.flush()
                                            fos.close()
                                            ExportManager.shareFile(context, file, "application/json", "Master Database Backup JSON")
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Toast.makeText(context, "Failed to create JSON backup file", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (MaterialTheme.colorScheme.isDark) Color(0xFF2A1C3C) else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (MaterialTheme.colorScheme.isDark) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Backup file", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // --- JSON RESTORE HUB ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text("Restore Database Pack", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        "Paste a previously exported valid JSON string below. WARNING: Validating and loading this pack completely overwrites all current active databases, profiles, and logs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = restoreTextJson,
                        onValueChange = { restoreTextJson = it },
                        label = { Text("Paste JSON Backup String") },
                        placeholder = { Text("Ensure valid bracket syntax {...}") },
                        textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.error,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )

                    Button(
                        onClick = { showRestoreConfirm = true },
                        enabled = restoreTextJson.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Validate & Hydrate Database", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- ABOUT FINTRACKER PRO PANEL ---
        item {
            var showReleaseNotes by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About App Info Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = "FinTracker Pro",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Version 7.2",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Track. Budget. Save. Grow.",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { showReleaseNotes = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Release Notes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            viewModel.setOnboardingCompleted(false)
                            Toast.makeText(context, "Onboarding series reset. It will display on next session launch!", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset Onboarding Tour", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (showReleaseNotes) {
                AlertDialog(
                    onDismissRequest = { showReleaseNotes = false },
                    title = {
                        Text(
                            text = "Release Notes: FinTracker Pro v7.2",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "Features included in this workspace:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val releaseFeaturesList = listOf(
                                "Multi-profile accounts",
                                "PIN-protected profiles",
                                "Cash & Bank balance tracking",
                                "Income & Expense management",
                                "Savings goals",
                                "Budget monitoring",
                                "Lending & Borrowing system",
                                "Timeline history",
                                "Analytics & reports",
                                "Backup & restore",
                                "Offline-first architecture"
                            )
                            releaseFeaturesList.forEach { feat ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = feat,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Local state updates are retained fully in sandboxed Room spaces. No cloud handshakes or telemetry exports.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showReleaseNotes = false }) {
                            Text("Dismiss", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }
        }
    }

    // --- WARNING DIALOG FOR OVERWRITE ---
    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Confirm Absolute Restore?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Doing so overwrites your active profiles, budgets, and transactions catalog. " +
                            "This action cannot be undone and will restore the database to the exact state contained in the backup. " +
                            "Make sure you have copied or shared your current layout if necessary.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importBackupJson(restoreTextJson) { success ->
                            showRestoreConfirm = false
                            if (success) {
                                restoreTextJson = ""
                                Toast.makeText(context, "Full database restored successfully!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Invalid JSON formatting or syntax. Load aborted.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Overwrite & Hydrate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

// --- RECURRING SCREEN ---
@Composable
fun RecurringScreen(
    viewModel: ExpenseViewModel,
    onEditTransactionClick: (Transaction) -> Unit,
    onAddTransactionClick: () -> Unit
) {
    val txs = viewModel.transactions.collectAsStateWithLifecycle().value
    val recurringTxs = remember(txs) { txs.filter { it.isRecurring } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Autorenew,
                            contentDescription = "Subscription engine",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column {
                        Text(
                            "Autopay & Subscriptions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val monthlyEquivalent = remember(recurringTxs) {
                            recurringTxs.filter { it.type == "EXPENSE" }.sumOf {
                                when (it.recurringInterval?.lowercase(Locale.ROOT)) {
                                    "daily" -> it.amount * 30.0
                                    "weekly" -> it.amount * 4.33
                                    "monthly" -> it.amount
                                    else -> it.amount
                                }
                            }
                        }
                        Text(
                            "Monthly Commitments: ₹${String.format(Locale.getDefault(), "%,.2f", monthlyEquivalent)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        if (recurringTxs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = "Clock",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Text(
                            "No active recurring subscriptions logged yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onAddTransactionClick,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Setup Subscriptions")
                        }
                    }
                }
            }
        } else {
            items(recurringTxs) { tx ->
                Card(
                    onClick = { onEditTransactionClick(tx) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(tx.category),
                                contentDescription = tx.category,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                tx.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = tx.recurringInterval ?: "Monthly",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFCD9BFF),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Pay via ${tx.paymentMode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFAF5FF).copy(alpha = 0.6f)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val sign = if (tx.type == "EXPENSE") "-" else "+"
                            val color = if (tx.type == "EXPENSE") Color(0xFFF87171) else Color(0xFF34D399)
                            Text(
                                text = "$sign₹${String.format(Locale.getDefault(), "%,.1f", tx.amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = color
                            )
                            Text(
                                "Auto-deduct",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE8DDF4)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- MORE PORTAL HUB SCREEN ---
@Composable
fun MoreHubScreen(
    viewModel: ExpenseViewModel,
    onAddTransactionClick: () -> Unit,
    onEditTransactionClick: (Transaction) -> Unit
) {
    var activeSubScreen by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Custom sub-screen back header
        if (activeSubScreen != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { activeSubScreen = null }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (activeSubScreen) {
                        "settings" -> "Settings"
                        "notification_center" -> "Notification Center"
                        "security" -> "Security"
                        "appearance" -> "Appearance"
                        else -> ""
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeSubScreen) {
                null -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
                    ) {
                        item {
                            Text(
                                "More Options",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        item {
                            SettingsListItem(
                                icon = Icons.Default.Settings,
                                iconColor = MaterialTheme.colorScheme.primary,
                                title = "Settings",
                                subtitle = "Reports, backups & database restore",
                                onClick = { activeSubScreen = "settings" }
                            )
                        }

                        item {
                            SettingsListItem(
                                icon = Icons.Default.Notifications,
                                iconColor = MaterialTheme.colorScheme.secondary,
                                title = "Notification Center",
                                subtitle = "Manage safety & budget notices",
                                onClick = { activeSubScreen = "notification_center" }
                            )
                        }

                        item {
                            SettingsListItem(
                                icon = Icons.Default.Security,
                                iconColor = Color(0xFF10B981),
                                title = "Security",
                                subtitle = "Configure profile PIN lock passcode protection",
                                onClick = { activeSubScreen = "security" }
                            )
                        }

                        item {
                            SettingsListItem(
                                icon = Icons.Default.Palette,
                                iconColor = Color(0xFFF59E0B),
                                title = "Appearance",
                                subtitle = "Adjust daylight/dark interface theme preferences",
                                onClick = { activeSubScreen = "appearance" }
                            )
                        }
                    }
                }
                "settings" -> {
                    SettingsScreen(viewModel = viewModel)
                }
                "notification_center" -> {
                    NotificationCenterScreen(viewModel = viewModel)
                }
                "security" -> {
                    SecuritySubScreen(viewModel = viewModel)
                }
                "appearance" -> {
                    AppearanceSubScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun SettingsListItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun AppearanceSubScreen(viewModel: ExpenseViewModel) {
    val themeMode by viewModel.themePreference.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, if (MaterialTheme.colorScheme.isDark) Color(0xFFCD9BFF).copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Appearance Preferences",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        "Choose your preferred screen style layout.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val themeOptions = listOf(
                            "light" to "Day Mode",
                            "dark" to "Night Mode",
                            "system" to "Follow System"
                        )
                        themeOptions.forEach { (key, label) ->
                            val isChosen = themeMode == key
                            Button(
                                onClick = { viewModel.setThemePreference(key) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    contentColor = if (isChosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecuritySubScreen(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val activeAccount = viewModel.activeAccount.collectAsStateWithLifecycle().value
    if (activeAccount != null) {
        val lockoutSecondsRemaining by viewModel.lockoutSecondsRemaining.collectAsStateWithLifecycle()
        var showChangePinDialog by remember { mutableStateOf(false) }
        var showDisablePinDialog by remember { mutableStateOf(false) }
        var showEnablePinDialog by remember { mutableStateOf(false) }
        var newPinCode by remember { mutableStateOf("") }
        var confirmNewPinCode by remember { mutableStateOf("") }
        var securityCardError by remember { mutableStateOf<String?>(null) }

        // Dialog for Change PIN
        if (showChangePinDialog) {
            com.example.ui.components.SecurityActionDialog(
                targetAccount = activeAccount,
                lockoutSecondsRemaining = lockoutSecondsRemaining,
                mode = com.example.ui.components.SecurityActionMode.CHANGE_PIN,
                onVerifySuccess = { newPin ->
                    if (newPin != null) {
                        viewModel.updateAccountSettings(
                            account = activeAccount,
                            newName = activeAccount.name,
                            newPin = newPin,
                            newColor = activeAccount.color,
                            newCurrency = activeAccount.currency,
                            newAvatar = activeAccount.avatar,
                            newThemePreference = activeAccount.themePreference ?: "Dark Purple"
                        )
                        showChangePinDialog = false
                        Toast.makeText(context, "PIN code changed successfully!", Toast.LENGTH_SHORT).show()
                    }
                },
                onVerifyFailed = {
                    viewModel.verifyCurrentPin("", activeAccount.pin)
                },
                onCancel = { showChangePinDialog = false }
            )
        }

        // Dialog for Disabling PIN Option
        if (showDisablePinDialog) {
            com.example.ui.components.SecurityActionDialog(
                targetAccount = activeAccount,
                lockoutSecondsRemaining = lockoutSecondsRemaining,
                mode = com.example.ui.components.SecurityActionMode.VERIFY_DISABLE,
                onVerifySuccess = {
                    viewModel.updateAccountSettings(
                        account = activeAccount,
                        newName = activeAccount.name,
                        newPin = null,
                        newColor = activeAccount.color,
                        newCurrency = activeAccount.currency,
                        newAvatar = activeAccount.avatar,
                        newThemePreference = activeAccount.themePreference ?: "Dark Purple"
                    )
                    showDisablePinDialog = false
                    Toast.makeText(context, "Profile protection disabled successfully", Toast.LENGTH_SHORT).show()
                },
                onVerifyFailed = {
                    viewModel.verifyCurrentPin("", activeAccount.pin)
                },
                onCancel = { showDisablePinDialog = false }
            )
        }

        // Dialog to Enable PIN if null
        if (showEnablePinDialog) {
            AlertDialog(
                onDismissRequest = { showEnablePinDialog = false },
                title = { Text("Configure PIN Protection", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Set a 4-digit numeric code to secure your current financial workspace profile.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = newPinCode,
                            onValueChange = { if (it.length <= 4) newPinCode = it.filter { c -> c.isDigit() } },
                            label = { Text("Choose 4-Digit PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        OutlinedTextField(
                            value = confirmNewPinCode,
                            onValueChange = { if (it.length <= 4) confirmNewPinCode = it.filter { c -> c.isDigit() } },
                            label = { Text("Confirm PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        if (securityCardError != null) {
                            Text(securityCardError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (newPinCode.length != 4) {
                            securityCardError = "PIN must be exactly 4 digits."
                            return@Button
                        }
                        if (newPinCode != confirmNewPinCode) {
                            securityCardError = "PINs do not match. Check values."
                            return@Button
                        }
                        viewModel.updateAccountSettings(
                            account = activeAccount,
                            newName = activeAccount.name,
                            newPin = newPinCode,
                            newColor = activeAccount.color,
                            newCurrency = activeAccount.currency,
                            newAvatar = activeAccount.avatar,
                            newThemePreference = activeAccount.themePreference ?: "Dark Purple"
                        )
                        showEnablePinDialog = false
                        newPinCode = ""
                        confirmNewPinCode = ""
                        securityCardError = null
                        Toast.makeText(context, "Protection enabled with secure PIN code!", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Enable PIN Lock")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEnablePinDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, if (MaterialTheme.colorScheme.isDark) Color(0xFFCD9BFF).copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Profile Security Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = if (activeAccount.pin != null)
                                "Your profile is secured by a 4-digit PIN lock screen. It requests authorization when you switch profiles or when the app balances are restored."
                            else
                                "Enable PIN passcode protection to prevent unauthorized access to your records and lock screen configurations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (activeAccount.pin != null) {
                                Button(
                                    onClick = { showChangePinDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Change PIN", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { showDisablePinDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Disable PIN", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { showEnablePinDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Enable PIN Code Protection", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityBackupRestorePortal(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val clipManager = LocalClipboardManager.current
    var restoreTextJson by remember { mutableStateOf("") }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF3B82F6).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, tint = Color(0xFF3B82F6))
                        }
                        Column {
                            Text("Database Export Backups", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Safeguard your records in portable plain-text JSON format.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.exportBackupJson { backup ->
                                    clipManager.setText(AnnotatedString(backup))
                                    Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy JSON", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFEC4899).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFFEC4899))
                        }
                        Column {
                            Text("Database Restore & Import", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Paste a previous JSON backup text to restore all account states.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    OutlinedTextField(
                        value = restoreTextJson,
                        onValueChange = { restoreTextJson = it },
                        label = { Text("Paste JSON Backup Text Here") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = MaterialTheme.typography.bodySmall.fontSize)
                    )

                    Button(
                        onClick = {
                            if (restoreTextJson.isNotBlank()) {
                                showRestoreConfirm = true
                            } else {
                                Toast.makeText(context, "Please paste valid JSON text first!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Restore Database")
                    }
                }
            }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Confirm Database Restoration") },
            text = { Text("This will overwrite your existing balance books and ledger indices with the backup contents. This action cannot be reversed.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importBackupJson(restoreTextJson.trim()) { success ->
                            if (success) {
                                Toast.makeText(context, "Database state restored successfully!", Toast.LENGTH_LONG).show()
                                restoreTextJson = ""
                            } else {
                                Toast.makeText(context, "Failed to restore: Invalid JSON structure or schema mismatch.", Toast.LENGTH_LONG).show()
                            }
                        }
                        showRestoreConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProfileManagementScreenView(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    var userNameText by remember { mutableStateOf("FinTracker Pro User") }
    var selectedAvatarIdx by remember { mutableIntStateOf(0) }
    val avatarsList = listOf("👤 Classic", "💼 Professional", "⚡ Dynamic", "🚀 Premium")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when(selectedAvatarIdx) {
                                1 -> "💼"
                                2 -> "⚡"
                                3 -> "🚀"
                                else -> "👤"
                            },
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }

                    Text("User Profile Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    OutlinedTextField(
                        value = userNameText,
                        onValueChange = { userNameText = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("Select Persona Avatar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            avatarsList.forEachIndexed { idx, name ->
                                val active = selectedAvatarIdx == idx
                                Button(
                                    onClick = { selectedAvatarIdx = idx },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.weight(1.5f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(name, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            Toast.makeText(context, "Profile details updated successfully!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCenterScreen(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    var enableSound by remember { mutableStateOf(true) }
    var enableBudgetLimitAlerts by remember { mutableStateOf(true) }
    var enableKhataDueDateAlerts by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // Core Notification Panel Description Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (MaterialTheme.colorScheme.isDark) Color(0xFFCD9BFF).copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Notification Center",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Manage local financial alert dispatches",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Alert rules switches
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (MaterialTheme.colorScheme.isDark) Color(0xFFCD9BFF).copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Alert Preferences",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Budget Overspending Alerts", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Send notification when a category reaches 90% or 100% capacity", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = enableBudgetLimitAlerts, onCheckedChange = { enableBudgetLimitAlerts = it })
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Khata Repayment Reminders", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Remind tomorrow about upcoming lending or borrowers due dates", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = enableKhataDueDateAlerts, onCheckedChange = { enableKhataDueDateAlerts = it })
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("System Alert Sound", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Play standard audible alerts on system dispatches", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = enableSound, onCheckedChange = { enableSound = it })
                    }
                }
            }
        }

        // Simulation sandboxes
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (MaterialTheme.colorScheme.isDark) Color(0xFFCD9BFF).copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Test Financial Reminders Sandbox",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Simulate active physical tray financial reminders instantly to inspect target redirects.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. Budget Alert
                        Button(
                            onClick = {
                                NotificationHelper.showNotification(
                                    context = context,
                                    id = 1101,
                                    title = "Budget Warning Limit Reached",
                                    message = "Your active Monthly Food budget has reached 90% allocation.",
                                    targetScreen = "budgets",
                                    highPriority = false
                                )
                                Toast.makeText(context, "Budget warning posted to system tray!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("Simulate Budget Limit Trigger", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text("Normal Priority • Redirects to Budgets page", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                }
                                Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }

                        // 2. Credit Card Reminder
                        Button(
                            onClick = {
                                NotificationHelper.showNotification(
                                    context = context,
                                    id = 1102,
                                    title = "Credit Card Statement Due Soon",
                                    message = "SBI Credit Card account outstanding balance payment is due in 3 days.",
                                    targetScreen = "credit_cards",
                                    highPriority = true
                                )
                                Toast.makeText(context, "Credit card reminder posted to system tray!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("Simulate Card Payment Due", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text("High Priority • Redirects to Credit Cards page", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                }
                                Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }

                        // 3. Khata Book Reminder
                        Button(
                            onClick = {
                                NotificationHelper.showNotification(
                                    context = context,
                                    id = 1103,
                                    title = "Khata Book Payment Notice",
                                    message = "Lent balance repayment from Rahul is expected tomorrow.",
                                    targetScreen = "debts",
                                    highPriority = true
                                )
                                Toast.makeText(context, "Khata book payment reminder posted!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("Simulate Khata Outstanding Notice", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text("High Priority • Redirects to Debts page", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                }
                                Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Simulated notifications history logs list
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "History & Dispatch Logs",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Circle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(10.dp))
                            Column {
                                Text("System Channel Created", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text("Standard channels successfully bounded on local workspace startup.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CarryForwardScreen(viewModel: ExpenseViewModel) {
    val txs by viewModel.transactions.collectAsStateWithLifecycle(emptyList())
    val carryForwardEnabledRaw by viewModel.enableCarryForward.collectAsStateWithLifecycle()
    val carryForwardRecords = rememberMonthlyCarryForward(txs, carryForwardEnabledRaw)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // Explanatory header card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (MaterialTheme.colorScheme.isDark) Color(0xFFCD9BFF).copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Autorenew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Monthly Carry Forward",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Configure roll-over balance compounding across periods",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Config Toggle switch card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (MaterialTheme.colorScheme.isDark) Color(0xFFCD9BFF).copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Compounding Balance",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Carries monthly remaining surplus or deficit forward as next month's starting asset value.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = carryForwardEnabledRaw,
                            onCheckedChange = { viewModel.setCarryForwardEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "How it Works:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "• ON: Net financial savings from May becomes the starting balance of June. If you saved ₹5,000, your starting balance rises by ₹5,000.\\n• OFF: Every calendar month sandbox resets opening values strictly to initial opening balances, disregarding preceding spendings history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Live list of monthly roll-over stats
        item {
            Text(
                text = "Calculated Historical Cascades",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        if (carryForwardRecords.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No historical transaction cascades retrieved yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(carryForwardRecords) { record ->
                val netSaved = record.income - record.expense
                val isSurplus = netSaved >= 0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = record.monthYearStr,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isSurplus) "Surplus" else "Deficit",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSurplus) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                modifier = Modifier
                                    .background(
                                        (if (isSurplus) Color(0xFF4CAF50) else Color(0xFFFF5252)).copy(alpha = 0.12f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Opening Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = String.format("%.2f", record.openingBalance),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Monthly Net", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = (if (isSurplus) "+" else "") + String.format("%.2f", netSaved),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSurplus) Color(0xFF4CAF50) else Color(0xFFFF5252)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Closing Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = String.format("%.2f", record.closingBalance),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutFinTrackerProPortal() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFFA78BFA),
                                        Color(0xFF6366F1)
                                    )
                                ),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.TrendingUp, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }

                    Text("FinTracker Pro", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Version 2.4.0 (Aesthetics Edition)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = "FinTracker Pro is an engineering-grade local financial organizer built entirely in modern Kotlin, Jetpack Compose, and SQLite Room database structure. It has been redesigned with Material 3 dynamic color mechanics, extreme database integrity levels, custom micro-interactions, and visual data insights.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Offline-First", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("100% Secure", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            VerticalDivider(modifier = Modifier.height(30.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("M3 Engine", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("Responsive Grid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class DebtTimelineEvent(
    val title: String,
    val amount: Double,
    val paymentMethod: String,
    val timestamp: Long,
    val remaining: Double,
    val isInitial: Boolean,
    val notes: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtModuleScreen(viewModel: ExpenseViewModel) {
    val activeAcc: Account? = viewModel.activeAccount.collectAsStateWithLifecycle().value
    val debtEntries: List<DebtEntry> = viewModel.debtEntries.collectAsStateWithLifecycle().value
    val dStats: DebtStats = viewModel.debtStats.collectAsStateWithLifecycle().value
    val allPayments = viewModel.allDebtPayments.collectAsStateWithLifecycle().value
    val currency = activeAcc?.currency ?: "₹"

    var selectedDebtType by remember { mutableStateOf("LENT") } // "LENT" or "BORROWED"
    var searchText by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") } // "ALL", "LENT", "RECOVERED", "BORROWED", "REPAID", "ACTIVE", "CLEARED"

    var showAddDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<DebtEntry?>(null) }
    var activePaymentEntry by remember { mutableStateOf<DebtEntry?>(null) }
    var expandedEntryIds by remember { mutableStateOf(setOf<Int>()) }

    // Prompt variables for existing ledger detection
    var showExistingPrompt by remember { mutableStateOf(false) }
    var pendingExistingEntry by remember { mutableStateOf<DebtEntry?>(null) }
    var pendingAddName by remember { mutableStateOf("") }
    var pendingAddAmount by remember { mutableStateOf(0.0) }
    var pendingAddNotes by remember { mutableStateOf("") }
    var pendingAddPhone by remember { mutableStateOf<String?>(null) }
    var pendingAddEntryDate by remember { mutableStateOf(0L) }
    var pendingAddDueDate by remember { mutableStateOf(0L) }
    var pendingAddMethod by remember { mutableStateOf("Cash") }

    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    // Filter debt entries dynamically based on selections
    val filteredEntries = remember(debtEntries, selectedDebtType, searchText, selectedStatusFilter, allPayments) {
        val typeFiltered = when (selectedStatusFilter) {
            "LENT" -> debtEntries.filter { it.type == "LENT" }
            "BORROWED" -> debtEntries.filter { it.type == "BORROWED" }
            "RECOVERED" -> debtEntries.filter { it.type == "LENT" && (it.status.uppercase() == "RECOVERED" || (it.amount + (allPayments.filter { p -> p.debtEntryId == it.id && p.isAddition }.sumOf { p -> p.amount }) - (allPayments.filter { p -> p.debtEntryId == it.id && !p.isAddition }.sumOf { p -> p.amount })) <= 0.0) }
            "REPAID" -> debtEntries.filter { it.type == "BORROWED" && (it.status.uppercase() == "REPAID" || (it.amount + (allPayments.filter { p -> p.debtEntryId == it.id && p.isAddition }.sumOf { p -> p.amount }) - (allPayments.filter { p -> p.debtEntryId == it.id && !p.isAddition }.sumOf { p -> p.amount })) <= 0.0) }
            else -> debtEntries.filter { it.type == selectedDebtType }
        }

        val searchFiltered = if (searchText.isBlank()) {
            typeFiltered
        } else {
            typeFiltered.filter { 
                it.personName.contains(searchText, ignoreCase = true) || 
                it.notes.contains(searchText, ignoreCase = true) || 
                (it.phoneNumber?.contains(searchText) == true)
            }
        }

        val statusFiltered = when (selectedStatusFilter) {
            "ACTIVE" -> {
                searchFiltered.filter {
                    val entryPayments = allPayments.filter { p -> p.debtEntryId == it.id }
                    val totalLentOrBorrowed = it.amount + entryPayments.filter { p -> p.isAddition }.sumOf { p -> p.amount }
                    val totalPaid = entryPayments.filter { p -> !p.isAddition }.sumOf { p -> p.amount }
                    (totalLentOrBorrowed - totalPaid) > 0.0
                }
            }
            "CLEARED" -> {
                searchFiltered.filter {
                    val entryPayments = allPayments.filter { p -> p.debtEntryId == it.id }
                    val totalLentOrBorrowed = it.amount + entryPayments.filter { p -> p.isAddition }.sumOf { p -> p.amount }
                    val totalPaid = entryPayments.filter { p -> !p.isAddition }.sumOf { p -> p.amount }
                    (totalLentOrBorrowed - totalPaid) <= 0.0
                }
            }
            "OVERDUE" -> {
                val now = System.currentTimeMillis()
                searchFiltered.filter { 
                    val entryPayments = allPayments.filter { p -> p.debtEntryId == it.id }
                    val totalLentOrBorrowed = it.amount + entryPayments.filter { p -> p.isAddition }.sumOf { p -> p.amount }
                    val totalPaid = entryPayments.filter { p -> !p.isAddition }.sumOf { p -> p.amount }
                    val isPending = (totalLentOrBorrowed - totalPaid) > 0.0
                    it.status.uppercase() == "OVERDUE" || (isPending && it.dueDate < now)
                }
            }
            else -> searchFiltered
        }

        statusFiltered
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Double Toggle Tab: Money Lent / Money Borrowed and ADD Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Segmented Button / custom Row toggle
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedDebtType == "LENT") MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { 
                            selectedDebtType = "LENT" 
                            selectedStatusFilter = "ALL"
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Money Lent",
                        fontWeight = FontWeight.Bold,
                        color = if (selectedDebtType == "LENT") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedDebtType == "BORROWED") MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { 
                            selectedDebtType = "BORROWED" 
                            selectedStatusFilter = "ALL"
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Money Borrowed",
                        fontWeight = FontWeight.Bold,
                        color = if (selectedDebtType == "BORROWED") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // ADD Button
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Debt Entry", modifier = Modifier.size(22.dp))
            }
        }

        // 2. Stats Summary Card showing Breakdown totals (requested features)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1: Total Lent / Borrowed
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (selectedDebtType == "LENT") "Total Lent" else "Total Borrowed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val amount = if (selectedDebtType == "LENT") dStats.totalLent else dStats.totalBorrowed
                    Text(
                        text = "$currency${String.format(Locale.getDefault(), "%,.0f", amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                )

                // Column 2: Total Recovered / Repaid
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (selectedDebtType == "LENT") "Total Recovered" else "Total Repaid",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val amount = if (selectedDebtType == "LENT") dStats.totalRecovered else dStats.totalRepaid
                    Text(
                        text = "$currency${String.format(Locale.getDefault(), "%,.0f", amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                )

                // Column 3: Outstanding / Remaining Debt
                Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (selectedDebtType == "LENT") "Outstanding" else "Remaining Debt",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedDebtType == "LENT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val amount = if (selectedDebtType == "LENT") dStats.outstandingLent else dStats.remainingBorrowed
                    Text(
                        text = "$currency${String.format(Locale.getDefault(), "%,.0f", amount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = if (selectedDebtType == "LENT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // 3. Search Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Search by name, phone...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
        }

        // Scrollable Quick Filters: Lent, Recovered, Borrowed, Repaid, Active, Cleared (matching exact requirement list)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            val chips = listOf(
                "ALL" to "All",
                "LENT" to "Lent",
                "RECOVERED" to "Recovered",
                "BORROWED" to "Borrowed",
                "REPAID" to "Repaid",
                "ACTIVE" to "Active",
                "CLEARED" to "Cleared"
            )

            items(chips) { (optionVal, label) ->
                val isSelected = selectedStatusFilter == optionVal
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { 
                            selectedStatusFilter = optionVal 
                            if (optionVal == "LENT" || optionVal == "RECOVERED") {
                                selectedDebtType = "LENT"
                            } else if (optionVal == "BORROWED" || optionVal == "REPAID") {
                                selectedDebtType = "BORROWED"
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        label,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // 4. List of Debt Entries
        if (filteredEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Inbox,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "No records found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredEntries) { entry ->
                    val now = System.currentTimeMillis()
                    
                    val paymentsForThisEntry = remember(allPayments, entry) {
                        allPayments.filter { it.debtEntryId == entry.id }
                    }

                    val totalLentOrBorrowed = remember(paymentsForThisEntry, entry) {
                        entry.amount + paymentsForThisEntry.filter { it.isAddition }.sumOf { it.amount }
                    }

                    val totalPaid = remember(paymentsForThisEntry, entry) {
                        if (paymentsForThisEntry.isEmpty()) {
                            if (entry.status.uppercase() == "RECOVERED" || entry.status.uppercase() == "REPAID") {
                                entry.amount
                            } else {
                                0.0
                            }
                        } else {
                            paymentsForThisEntry.filter { !it.isAddition }.sumOf { it.amount }
                        }
                    }

                    val remainingBalance = remember(totalLentOrBorrowed, totalPaid) {
                        (totalLentOrBorrowed - totalPaid).coerceAtLeast(0.0)
                    }

                    val actualStatus = remember(entry, remainingBalance) {
                        val isOverdue = entry.status.uppercase() == "OVERDUE" || 
                            (entry.status.uppercase() != "RECOVERED" && entry.status.uppercase() != "REPAID" && remainingBalance > 0.0 && entry.dueDate < now)
                        
                        if (isOverdue) "OVERDUE" else if (remainingBalance <= 0.0) (if (entry.type == "LENT") "RECOVERED" else "REPAID") else entry.status.uppercase()
                    }

                    val colorIndicator = when (actualStatus) {
                        "RECOVERED", "REPAID" -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        "OVERDUE" -> MaterialTheme.colorScheme.error // Red
                        "ACTIVE" -> MaterialTheme.colorScheme.tertiary // Green
                        else -> MaterialTheme.colorScheme.tertiary
                    }

                    val isExpanded = expandedEntryIds.contains(entry.id)
                    val toggleExpand = {
                        expandedEntryIds = if (isExpanded) {
                            expandedEntryIds - entry.id
                        } else {
                            expandedEntryIds + entry.id
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { toggleExpand() }
                            .animateContentSize(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, colorIndicator.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header: Name, Status Cap, Amount
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        entry.personName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!entry.phoneNumber.isNullOrBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                entry.phoneNumber,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "$currency${String.format(Locale.getDefault(), "%,.1f", remainingBalance)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = if (entry.type == "LENT") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                    )

                                    Box(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .background(colorIndicator.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            actualStatus,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colorIndicator,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (!entry.notes.isBlank()) {
                                Text(
                                    entry.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }

                            // Progress Indicator visual bar
                            if (totalLentOrBorrowed > 0.0) {
                                val progressFraction = (totalPaid / totalLentOrBorrowed).toFloat().coerceIn(0f, 1f)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    LinearProgressIndicator(
                                        progress = { progressFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = if (entry.type == "LENT") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Paid: $currency${String.format(Locale.getDefault(), "%,.0f", totalPaid)} (${(progressFraction * 100).toInt()}%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Total: $currency${String.format(Locale.getDefault(), "%,.0f", totalLentOrBorrowed)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Dates metadata
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Lent on: ${sdf.format(Date(entry.entryDate))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Due: ${sdf.format(Date(entry.dueDate))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (actualStatus == "OVERDUE") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (actualStatus == "OVERDUE") FontWeight.Bold else FontWeight.Normal
                                    )
                                }

                                // Interactive Actions inside card
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (remainingBalance > 0.0) {
                                        IconButton(
                                            onClick = { activePaymentEntry = entry },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Payments,
                                                contentDescription = "Log Payment",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { editingEntry = entry },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Details",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.deleteDebtEntry(entry)
                                            Toast.makeText(context, "Entry removed.", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1F), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Entry",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // History timeline drawer
                            if (isExpanded) {
                                val timelineEvents = remember(entry, paymentsForThisEntry) {
                                    val list = mutableListOf<DebtTimelineEvent>()
                                    list.add(
                                        DebtTimelineEvent(
                                            title = if (entry.type == "LENT") "Money Lent" else "Money Borrowed",
                                            amount = entry.amount,
                                            paymentMethod = "Initial Setup",
                                            timestamp = entry.entryDate,
                                            remaining = entry.amount,
                                            isInitial = true
                                        )
                                    )

                                    var runningRemaining = entry.amount
                                    val sortedPayments = paymentsForThisEntry.sortedBy { it.timestamp }
                                    sortedPayments.forEach { pay ->
                                        if (pay.isAddition) {
                                            runningRemaining += pay.amount
                                            list.add(
                                                DebtTimelineEvent(
                                                    title = if (entry.type == "LENT") "Lent More Money" else "Borrowed More Money",
                                                    amount = pay.amount,
                                                    paymentMethod = pay.paymentMethod,
                                                    timestamp = pay.timestamp,
                                                    remaining = runningRemaining,
                                                    isInitial = false,
                                                    notes = pay.notes
                                                )
                                            )
                                        } else {
                                            runningRemaining = (runningRemaining - pay.amount).coerceAtLeast(0.0)
                                            list.add(
                                                DebtTimelineEvent(
                                                    title = if (entry.type == "LENT") {
                                                        if (runningRemaining <= 0.0) "Fully Recovered" else "Received Partial Payment"
                                                    } else {
                                                        if (runningRemaining <= 0.0) "Fully Repaid" else "Repaid Partial Debt"
                                                    },
                                                    amount = pay.amount,
                                                    paymentMethod = pay.paymentMethod,
                                                    timestamp = pay.timestamp,
                                                    remaining = runningRemaining,
                                                    isInitial = false,
                                                    notes = pay.notes
                                                )
                                            )
                                        }
                                    }

                                    if (sortedPayments.isEmpty() && (entry.status.uppercase() == "RECOVERED" || entry.status.uppercase() == "REPAID")) {
                                        list.add(
                                            DebtTimelineEvent(
                                                title = if (entry.type == "LENT") "Fully Recovered" else "Fully Repaid",
                                                amount = entry.amount,
                                                paymentMethod = "Direct Settle",
                                                timestamp = entry.dueDate,
                                                remaining = 0.0,
                                                isInitial = false
                                            )
                                        )
                                    }
                                    list
                                }

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(top = 10.dp)
                                ) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                        thickness = 1.dp
                                    )

                                    Text(
                                        text = "History Timeline",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    timelineEvents.forEachIndexed { idx, ev ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(
                                                            if (ev.isInitial) MaterialTheme.colorScheme.outline else colorIndicator,
                                                            CircleShape
                                                        )
                                                )
                                                if (idx < timelineEvents.size - 1) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(1.5.dp)
                                                            .height(38.dp)
                                                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                                                    )
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = ev.title,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "$currency${String.format(Locale.getDefault(), "%,.1f", ev.amount)}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (entry.type == "LENT") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                                    )
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    val pathDesc = if (ev.isInitial) "Initial Setup" else "via ${ev.paymentMethod}"
                                                    Text(
                                                        text = "$pathDesc • ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(ev.timestamp))}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "Remaining: $currency${String.format(Locale.getDefault(), "%,.1f", ev.remaining)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                if (!ev.notes.isBlank()) {
                                                    Text(
                                                        text = "\"${ev.notes}\"",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet Dialogs for Add/Edit
    if (showAddDialog) {
        DebtEntryDialog(
            type = selectedDebtType,
            entry = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, amt, notes, pNo, entryD, due, pMethod ->
                val existing = debtEntries.find { it.personName.equals(name, ignoreCase = true) && it.type == selectedDebtType }
                if (existing != null) {
                    pendingAddName = name
                    pendingAddAmount = amt
                    pendingAddNotes = notes
                    pendingAddPhone = pNo
                    pendingAddEntryDate = entryD
                    pendingAddDueDate = due
                    pendingAddMethod = pMethod
                    pendingExistingEntry = existing
                    showExistingPrompt = true
                } else {
                    viewModel.insertDebtEntry(
                        type = selectedDebtType,
                        personName = name,
                        amount = amt,
                        entryDate = entryD,
                        dueDate = due,
                        notes = notes,
                        phoneNumber = pNo,
                        paymentMethod = pMethod
                    )
                    showAddDialog = false
                }
            }
        )
    }

    if (showExistingPrompt && pendingExistingEntry != null) {
        val ext = pendingExistingEntry!!
        AlertDialog(
            onDismissRequest = { showExistingPrompt = false },
            title = { Text("Existing Ledger Found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = { Text("An existing ledger of the same type is already present for \"${ext.personName}\". Add this transaction to the existing ledger?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.insertDebtAddition(
                            entry = ext,
                            amount = pendingAddAmount,
                            paymentMethod = pendingAddMethod,
                            entryDate = pendingAddEntryDate,
                            notes = pendingAddNotes
                        )
                        showExistingPrompt = false
                        showAddDialog = false
                        Toast.makeText(context, "Added to existing ledger of ${ext.personName}.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Add to Existing Ledger", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.insertDebtEntry(
                            type = selectedDebtType,
                            personName = pendingAddName,
                            amount = pendingAddAmount,
                            entryDate = pendingAddEntryDate,
                            dueDate = pendingAddDueDate,
                            notes = pendingAddNotes,
                            phoneNumber = pendingAddPhone,
                            paymentMethod = pendingAddMethod
                        )
                        showExistingPrompt = false
                        showAddDialog = false
                        Toast.makeText(context, "New ledger created for ${pendingAddName}.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Create New Ledger")
                }
            }
        )
    }

    if (editingEntry != null) {
        DebtEntryDialog(
            type = editingEntry!!.type,
            entry = editingEntry,
            onDismiss = { editingEntry = null },
            onSave = { name, amt, notes, pNo, entryD, due, pMethod ->
                viewModel.updateDebtEntry(
                    editingEntry!!.copy(
                        personName = name,
                        amount = amt,
                        notes = notes,
                        phoneNumber = pNo,
                        entryDate = entryD,
                        dueDate = due
                    )
                )
                editingEntry = null
            }
        )
    }

    activePaymentEntry?.let { entry ->
        val paymentsForThisEntry = allPayments.filter { it.debtEntryId == entry.id }
        val totalPaid = if (paymentsForThisEntry.isEmpty()) {
            if (entry.status.uppercase() == "RECOVERED" || entry.status.uppercase() == "REPAID") entry.amount else 0.0
        } else {
            paymentsForThisEntry.sumOf { it.amount }
        }
        val remainingBalance = (entry.amount - totalPaid).coerceAtLeast(0.0)

        RecordPaymentDialog(
            entry = entry,
            remainingBalance = remainingBalance,
            currency = currency,
            onDismiss = { activePaymentEntry = null },
            onSubmit = { amt, pMethod, notes ->
                viewModel.recordDebtPayment(entry, amt, pMethod, notes)
                activePaymentEntry = null
                Toast.makeText(context, "Payment recorded successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentDialog(
    entry: DebtEntry,
    remainingBalance: Double,
    currency: String,
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, paymentMethod: String, notes: String) -> Unit
) {
    var amountStr by remember { mutableStateOf(String.format(Locale.US, "%.1f", remainingBalance)) }
    var selectedMethod by remember { mutableStateOf("Cash") } // "Cash", "Bank Account", "UPI", "Card"
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val row1 = listOf("Cash", "Bank Account")
    val row2 = listOf("UPI", "Card")
    val paymentEmojis = mapOf("Cash" to "💵", "Bank Account" to "🏦", "UPI" to "⚡", "Card" to "💳")

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (entry.type == "LENT") "Receive Payment" else "Make Repayment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Recording transaction for ${entry.personName}. Remaining balance to settle is $currency${String.format(Locale.getDefault(), "%,.1f", remainingBalance)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.toDoubleOrNull() != null) {
                            amountStr = input
                            errorMessage = null
                        }
                    },
                    label = { Text("Amount") },
                    placeholder = { Text("e.g. 500") },
                    leadingIcon = { Text(currency, modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(row1, row2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { method ->
                                val isSelected = selectedMethod == method
                                val emoji = paymentEmojis[method] ?: ""
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedMethod = method }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(emoji, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = method,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("e.g. UPI payment") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                errorMessage?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            if (amt <= 0.0) {
                                errorMessage = "Please enter a valid positive amount."
                                return@Button
                            }
                            if (amt > remainingBalance + 0.01) {
                                errorMessage = "Amount cannot exceed the remaining balance ($currency${String.format(Locale.getDefault(), "%,.1f", remainingBalance)})."
                                return@Button
                            }
                            onSubmit(amt, selectedMethod, notes)
                        }
                    ) {
                        Text("Record")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtEntryDialog(
    type: String,
    entry: DebtEntry?,
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Double, notes: String, phoneNumber: String?, entryDate: Long, dueDate: Long, paymentMethod: String) -> Unit
) {
    val context = LocalContext.current
    var personName by remember { mutableStateOf(entry?.personName ?: "") }
    var amountStr by remember { mutableStateOf(entry?.amount?.toString() ?: "") }
    var notes by remember { mutableStateOf(entry?.notes ?: "") }
    var phoneNumber by remember { mutableStateOf(entry?.phoneNumber ?: "") }
    var selectedPaymentMethod by remember { mutableStateOf("Cash") } // "Cash" or "Bank / Online"
    
    var entryDateMs by remember { mutableStateOf(entry?.entryDate ?: System.currentTimeMillis()) }
    var dueDateMs by remember { mutableStateOf(entry?.dueDate ?: (System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L))) }

    val entryCalendar = remember(entryDateMs) {
        Calendar.getInstance().apply { timeInMillis = entryDateMs }
    }
    val dueCalendar = remember(dueDateMs) {
        Calendar.getInstance().apply { timeInMillis = dueDateMs }
    }

    val activeAcc = remember { mutableStateOf<Account?>(null) }
    val currency = "₹"

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, if (MaterialTheme.colorScheme.isDark) Color(0xFFCD9BFF).copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                     text = if (entry == null) "New Debt Entry" else "Edit Debt Entry",
                     style = MaterialTheme.typography.titleLarge,
                     fontWeight = FontWeight.Bold,
                     color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text("Person Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (entry == null) {
                    val promptText = if (type == "LENT") "Where is this money being lent from?" else "Where should this borrowed money be added?"
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = promptText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val options = listOf("Cash" to "💵 Cash", "Bank / Online" to "🏦 Bank / Online")
                            options.forEach { (optionVal, labelText) ->
                                val isSelected = selectedPaymentMethod == optionVal
                                Card(
                                    onClick = { selectedPaymentMethod = optionVal },
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = labelText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Recording Date and Time selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Recording Date & Time",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Entry Date Button
                        Card(
                            onClick = {
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        entryCalendar.set(Calendar.YEAR, year)
                                        entryCalendar.set(Calendar.MONTH, month)
                                        entryCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        entryDateMs = entryCalendar.timeInMillis
                                    },
                                    entryCalendar.get(Calendar.YEAR),
                                    entryCalendar.get(Calendar.MONTH),
                                    entryCalendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                              ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Date", modifier = Modifier.size(16.dp))
                                Column {
                                    Text("Lent/Borrowed Date", style = MaterialTheme.typography.labelSmall)
                                    Text(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(entryDateMs)), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Entry Time Button
                        Card(
                            onClick = {
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        entryCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        entryCalendar.set(Calendar.MINUTE, minute)
                                        entryDateMs = entryCalendar.timeInMillis
                                    },
                                    entryCalendar.get(Calendar.HOUR_OF_DAY),
                                    entryCalendar.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = "Time", modifier = Modifier.size(16.dp))
                                Column {
                                    Text("Time", style = MaterialTheme.typography.labelSmall)
                                    Text(SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entryDateMs)), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Due Date and Time selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "End Due Date & Time",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Due Date Button
                        Card(
                            onClick = {
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        dueCalendar.set(Calendar.YEAR, year)
                                        dueCalendar.set(Calendar.MONTH, month)
                                        dueCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        dueDateMs = dueCalendar.timeInMillis
                                    },
                                    dueCalendar.get(Calendar.YEAR),
                                    dueCalendar.get(Calendar.MONTH),
                                    dueCalendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Due Date", modifier = Modifier.size(16.dp))
                                Column {
                                    Text("Due Date", style = MaterialTheme.typography.labelSmall)
                                    Text(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dueDateMs)), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Due Time Button
                        Card(
                            onClick = {
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        dueCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        dueCalendar.set(Calendar.MINUTE, minute)
                                        dueDateMs = dueCalendar.timeInMillis
                                    },
                                    dueCalendar.get(Calendar.HOUR_OF_DAY),
                                    dueCalendar.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = "Time", modifier = Modifier.size(16.dp))
                                Column {
                                    Text("Time", style = MaterialTheme.typography.labelSmall)
                                    Text(SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(dueDateMs)), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Quick offset options mapping relative to selected entryDateMs
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            7 to "1 Wk",
                            14 to "2 Wk",
                            30 to "1 Mo",
                            90 to "3 Mo"
                        ).forEach { (days, label) ->
                            Button(
                                onClick = { 
                                    dueDateMs = entryDateMs + (days * 24L * 60 * 60 * 1000L) 
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Descriptions") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            if (personName.isNotBlank() && amt > 0.0) {
                                onSave(personName, amt, notes, if (phoneNumber.isNullOrBlank()) null else phoneNumber, entryDateMs, dueDateMs, selectedPaymentMethod)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 💳 CREDIT CARDS MANAGEMENT MODULE
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardsScreen(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val cards = viewModel.creditCards.collectAsStateWithLifecycle().value
    val repaymentsAll = viewModel.creditCardRepayments.collectAsStateWithLifecycle().value
    val txs = viewModel.transactions.collectAsStateWithLifecycle().value

    var showAddDialog by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<CreditCard?>(null) }
    var repaymentCard by remember { mutableStateOf<CreditCard?>(null) }
    
    // Form fields
    var cardName by remember { mutableStateOf("") }
    var cardIssuer by remember { mutableStateOf("SBI Credit Card") }
    var creditLimitStr by remember { mutableStateOf("") }
    var billingCycleDate by remember { mutableStateOf(15) }
    var paymentDueDate by remember { mutableStateOf(3) }
    var interestRateStr by remember { mutableStateOf("") }
    var selectedCardColor by remember { mutableStateOf("#FF1A237E") } // Blue Accent default

    // Repayment fields
    var repayAmountStr by remember { mutableStateOf("") }
    var repaySource by remember { mutableStateOf("Bank") }
    var repayNotes by remember { mutableStateOf("") }

    val issuers = listOf(
        "SBI Credit Card",
        "HDFC Credit Card",
        "ICICI Credit Card",
        "Axis Bank Credit Card",
        "Kotak Credit Card",
        "Other"
    )

    val cardThemeColors = listOf(
        "#FF1A237E" to "Indigo Night",
        "#FF37474F" to "Obsidian Grey",
        "#00695C" to "Deep Emerald",
        "#880E4F" to "Crimson Maroon",
        "#4A148C" to "Royal Velvet"
    )

    // Expanded states
    var expandedCardId by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Module Title and description
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Credit Cards",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Manage spends, check optimal usage, and log bill payments.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Button(
                onClick = {
                    cardName = ""
                    cardIssuer = "SBI Credit Card"
                    creditLimitStr = ""
                    billingCycleDate = 15
                    paymentDueDate = 3
                    interestRateStr = ""
                    selectedCardColor = "#FF1A237E"
                    editingCard = null
                    showAddDialog = true
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Card", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Card", style = MaterialTheme.typography.labelSmall)
            }
        }

        if (cards.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("💳", fontSize = 48.sp)
                    Text(
                        "No Active Credit Cards",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Keep your wallet in perfect balance by tracking all secondary credit lines.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cards.size) { index ->
                    val card = cards[index]
                    val repayments = repaymentsAll.filter { it.creditCardId == card.id }
                    val cardSpends = txs.filter { it.type == "EXPENSE" && it.creditCardId == card.id }.sumOf { it.amount }
                    val cardPayments = repayments.sumOf { it.amount }
                    val outstanding = (cardSpends - cardPayments).coerceAtLeast(0.0)
                    val utilizationRatio = if (card.creditLimit > 0) (outstanding / card.creditLimit).toFloat() else 0f
                    val utilizationPercent = (utilizationRatio * 100).toInt()
                    
                    val calendar = Calendar.getInstance()
                    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
                    val dueDay = card.paymentDueDate
                    val daysLeft = if (dueDay >= currentDay) {
                        dueDay - currentDay
                    } else {
                        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                        (maxDays - currentDay) + dueDay
                    }

                    val color = try {
                        Color(android.graphics.Color.parseColor(card.colorHex))
                     } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                     }

                    val isExpanded = expandedCardId == card.id

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
                        onClick = { expandedCardId = if (isExpanded) null else card.id }
                    ) {
                        Column {
                            // Visually Polished Card Front representation
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(color, color.copy(alpha = 0.85f))
                                        )
                                    )
                                    .padding(20.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            card.cardIssuer,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                        Text(
                                            "CREDIT",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Light,
                                            color = Color.White.copy(alpha = 0.7f),
                                            letterSpacing = 2.sp
                                        )
                                    }

                                    // Microchip graphic
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp, 18.dp)
                                                .background(Color(0xFFFFD54F), RoundedCornerShape(2.dp))
                                        )
                                        Text("░░", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Column {
                                            Text(
                                                card.cardName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                "•••• •••• •••• " + (1000 + card.id),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                "OUTSTANDING",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                "₹${String.format(Locale.getDefault(), "%,.1f", outstanding)}",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            // Card Summary Stats strip (visible on main card always)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("CREDIT UTILIZATION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$utilizationPercent% ($utilizationPercent/100)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("PAYMENT DUE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        if (daysLeft == 0) "Due Today!" else "In $daysLeft Days (${card.paymentDueDate}th)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (daysLeft <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Expandable Details and Actions
                            if (isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    // Educational recommendations and details
                                    IssuerDetailsView(
                                        issuer = card.cardIssuer,
                                        limit = card.creditLimit,
                                        interestRate = card.interestRate,
                                        outstanding = outstanding
                                    )

                                    // Progress indicators
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Statement Gen Date:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${card.billingCycleDate}th of each month", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    }

                                    // Action bar
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                repayAmountStr = ""
                                                repaySource = "Bank"
                                                repayNotes = ""
                                                repaymentCard = card
                                            },
                                            modifier = Modifier.weight(1.2f),
                                            colors = ButtonDefaults.buttonColors(containerColor = color),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Record Repay", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                // Prepopulate editing form
                                                cardName = card.cardName
                                                cardIssuer = card.cardIssuer
                                                creditLimitStr = card.creditLimit.toString()
                                                billingCycleDate = card.billingCycleDate
                                                paymentDueDate = card.paymentDueDate
                                                interestRateStr = card.interestRate?.toString() ?: ""
                                                selectedCardColor = card.colorHex
                                                editingCard = card
                                                showAddDialog = true
                                            },
                                            modifier = Modifier.weight(0.9f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Edit Card", style = MaterialTheme.typography.labelMedium)
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.deleteCreditCard(card)
                                                Toast.makeText(context, "Card deleted successfully", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // List of payments & expenditures on this card
                                    Text("Recent Operations on Card", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    
                                    val cardTxList = txs.filter { it.type == "EXPENSE" && it.creditCardId == card.id }
                                    if (cardTxList.isEmpty() && repayments.isEmpty()) {
                                        Text("No transactions logged for this credit card.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            // Transactions spends
                                            cardTxList.take(3).forEach { tx ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                        .padding(8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column {
                                                        Text(tx.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                                        Text(SimpleDateFormat("dd MMMM", Locale.getDefault()).format(Date(tx.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    Text("-₹${tx.amount}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                            // Payments repays
                                            repayments.take(3).forEach { r ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                                                        .padding(8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column {
                                                        Text("Repayment logged (${r.paymentSource})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                                        Text(SimpleDateFormat("dd MMMM", Locale.getDefault()).format(Date(r.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    Text("+₹${r.amount}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // dialog ADD / EDIT CARD
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = if (editingCard == null) "Add Credit Card" else "Edit Card Settings",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Popular Issuers Selection Custom Scroll row or Column
                    Column {
                        Text("Select Card Issuer", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            issuers.forEach { issuer ->
                                val isSelected = cardIssuer == issuer
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { cardIssuer = issuer },
                                    label = { Text(issuer, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = cardName,
                        onValueChange = { cardName = it },
                        label = { Text("Card Name (e.g., SimplyCLICK RuPay, Millennia)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = creditLimitStr,
                        onValueChange = { creditLimitStr = it },
                        label = { Text("Credit Card Limit (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = billingCycleDate.toString(),
                            onValueChange = { billingCycleDate = it.toIntOrNull()?.coerceIn(1, 28) ?: 15 },
                            label = { Text("Billing Date (1-28)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = paymentDueDate.toString(),
                            onValueChange = { paymentDueDate = it.toIntOrNull()?.coerceIn(1, 28) ?: 3 },
                            label = { Text("Due Date (1-28)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = interestRateStr,
                        onValueChange = { interestRateStr = it },
                        label = { Text("Interest Rate % Per Annum (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Color theme picker
                    Column {
                        Text("Choose Card Skin", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            cardThemeColors.forEach { (hex, name) ->
                                val isSelected = selectedCardColor == hex
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCardColor = hex },
                                    label = { Text(name, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                    
                    // Display Issuer-specific Recommendation insights IMMEDIATELY inside Form Dialog!
                    val testLimit = creditLimitStr.toDoubleOrNull() ?: 10000.0
                    val testRate = interestRateStr.toDoubleOrNull()
                    Spacer(modifier = Modifier.height(6.dp))
                    IssuerFormHelperWidget(cardIssuer, testLimit, testRate)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = creditLimitStr.toDoubleOrNull() ?: 0.0
                        if (cardName.isNotBlank() && limit > 0) {
                            val interest = interestRateStr.toDoubleOrNull()
                            val original = editingCard
                            if (original == null) {
                                viewModel.insertCreditCard(
                                    cardName = cardName.trim(),
                                    cardIssuer = cardIssuer,
                                    creditLimit = limit,
                                    billingCycleDate = billingCycleDate,
                                    paymentDueDate = paymentDueDate,
                                    interestRate = interest,
                                    colorHex = selectedCardColor
                                )
                                Toast.makeText(context, "Credit card created successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.updateCreditCard(
                                    original.copy(
                                        cardName = cardName.trim(),
                                        cardIssuer = cardIssuer,
                                        creditLimit = limit,
                                        billingCycleDate = billingCycleDate,
                                        paymentDueDate = paymentDueDate,
                                        interestRate = interest,
                                        colorHex = selectedCardColor
                                    )
                                )
                                Toast.makeText(context, "Card configurations updated!", Toast.LENGTH_SHORT).show()
                            }
                            showAddDialog = false
                        } else {
                            Toast.makeText(context, "Please enter valid credit card details.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(if (editingCard == null) "Create Card" else "Save Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // dialog RECORD CREDIT CARD REPAYMENT
    val activeRepay = repaymentCard
    if (activeRepay != null) {
        val repayments = repaymentsAll.filter { it.creditCardId == activeRepay.id }
        val cardSpends = txs.filter { it.type == "EXPENSE" && it.creditCardId == activeRepay.id }.sumOf { it.amount }
        val cardPayments = repayments.sumOf { it.amount }
        val outstanding = (cardSpends - cardPayments).coerceAtLeast(0.0)

        AlertDialog(
            onDismissRequest = { repaymentCard = null },
            title = {
                Text(
                    text = "Log Repayment for ${activeRepay.cardName}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Outstanding spent on card: ₹${String.format(Locale.getDefault(), "%,.1f", outstanding)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = repayAmountStr,
                        onValueChange = { repayAmountStr = it },
                        label = { Text("Repayment Amount (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick total select buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { repayAmountStr = outstanding.toString() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Full Dues", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = { repayAmountStr = (outstanding * 0.5).toString() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha=0.6f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("50% Partial", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Column {
                        Text("Deduct Dues From:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Cash", "Bank").forEach { src ->
                                val isSelected = repaySource == src
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { repaySource = src },
                                    label = { Text(src, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = repayNotes,
                        onValueChange = { repayNotes = it },
                        label = { Text("Reciept Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val repayAmt = repayAmountStr.toDoubleOrNull() ?: 0.0
                        if (repayAmt > 0.0) {
                            viewModel.insertCreditCardRepayment(
                                card = activeRepay,
                                amount = repayAmt,
                                paymentSource = repaySource,
                                notes = repayNotes.trim()
                            )
                            Toast.makeText(context, "Repayment registered and cash/bank updated!", Toast.LENGTH_SHORT).show()
                            repaymentCard = null
                        } else {
                            Toast.makeText(context, "Please enter a valid repayment amount.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Pay Bill")
                }
            },
            dismissButton = {
                TextButton(onClick = { repaymentCard = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun IssuerFormHelperWidget(issuer: String, limit: Double, interestRate: Double?) {
    val suggestedRate = interestRate ?: 36.0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("📐 Recommended safe usage: Below ₹${String.format(Locale.getDefault(), "%,.0f", limit * 0.3)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("📊 Statement Generation: Monthly cycle generation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("⚠️ Finance Dues: Accrues $suggestedRate% annual interest if not fully repaid by due date.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun IssuerDetailsView(issuer: String, limit: Double, interestRate: Double?, outstanding: Double) {
    val rateDisplay = interestRate?.toString()?.let { "$it%" } ?: "36% - 42% (Standard)"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("🔍 Optimal Insights", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(issuer, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Text(
            "Recommended Safe limit (below 30%): ₹${String.format(Locale.getDefault(), "%,.0f", limit * 0.3)}. Staying below 30% protects and significantly boosts your credit score.",
            style = MaterialTheme.typography.bodySmall,
            color = if (outstanding > limit * 0.3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Interest Information: If outstanding balance is not fully paid by the payment due date, finance charges of $rateDisplay per annum will accrue on all transactions from date of spending. Always pay total amount due of ₹${String.format(Locale.getDefault(), "%,.1f", outstanding)} on or before the due date to avoid charges.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
