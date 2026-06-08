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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    onAddBudgetClick: () -> Unit
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
    var isFabExpanded by remember { mutableStateOf(false) }

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

        // EXPANDABLE FLOATING ACTION BUTTON WITH PURPLE NEON GRADIENT
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Expanded Mini Action Buttons Floating Up
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
                        // Expense Item Button
                        ExtendedActionButton(
                            text = "Add Expense",
                            icon = Icons.Default.AddShoppingCart,
                            color = Color(0xFFF87171),
                            onClick = {
                                onAddTransactionClick("EXPENSE")
                                isFabExpanded = false
                            }
                        )
                        // Income Item Button
                        ExtendedActionButton(
                            text = "Add Income",
                            icon = Icons.Default.Payments,
                            color = Color(0xFF34D399),
                            onClick = {
                                onAddTransactionClick("INCOME")
                                isFabExpanded = false
                            }
                        )
                        // Transfer Item Button
                        ExtendedActionButton(
                            text = "Transfer Funds",
                            icon = Icons.Default.SwapHoriz,
                            color = Color(0xFFCD9BFF),
                            onClick = {
                                onAddTransactionClick("TRANSFER")
                                isFabExpanded = false
                            }
                        )
                        // Budget Item Button
                        ExtendedActionButton(
                            text = "Set Budget",
                            icon = Icons.Default.BarChart,
                            color = Color(0xFFF59E0B),
                            onClick = {
                                onAddBudgetClick()
                                isFabExpanded = false
                            }
                        )
                    }
                }

                // Core Floating Action button Trigger
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
                        .scale(1f)
                ) {
                    Icon(
                        imageVector = if (isFabExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Expand controls",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
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

    var selectedTabIdx by remember { mutableIntStateOf(0) }
    val tabLabels = listOf("Savings", "Budget", "Analytics")

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
                            .clickable { selectedTabIdx = index }
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
                0 -> {
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
                1 -> {
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
                    val netSavings = txs.filter { it.type == "INCOME" }.sumOf { it.amount } - txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                    val totalInc = txs.filter { it.type == "INCOME" }.sumOf { it.amount }
                    val totalExp = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }

                    // group items
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
    val currency = activeAcc?.currency ?: "$"

    var activeTimeTab by remember { mutableStateOf("All") } // "All", "Weekly", "Monthly"

    // Process transactions matching time filter
    val calendar = Calendar.getInstance()
    val startOfWeek = calendar.apply { set(Calendar.DAY_OF_WEEK, firstDayOfWeek) }.timeInMillis
    val startOfMonth = calendar.apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis

    val filteredList = when (activeTimeTab) {
        "Weekly" -> list.filter { tx -> tx.timestamp >= startOfWeek }
        "Monthly" -> list.filter { tx -> tx.timestamp >= startOfMonth }
        else -> list
    }

    // Calculations: category spending breakdown
    val categoryExpenses = filteredList
        .filter { tx -> tx.type == "EXPENSE" }
        .groupBy { tx -> tx.category }
        .mapValues { entry -> entry.value.sumOf { tx -> tx.amount } }
        .toList()
        .sortedByDescending { pair -> pair.second }

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Tab selector
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Weekly", "Monthly").forEach { tab ->
                    val isSel = activeTimeTab == tab
                    Button(
                        onClick = { activeTimeTab = tab },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(tab)
                    }
                }
            }
        }

        // Summary Statistics Card
        item {
            val totalSpent = filteredList.filter { tx -> tx.type == "EXPENSE" }.sumOf { tx -> tx.amount }
            val totalIn = filteredList.filter { tx -> tx.type == "INCOME" }.sumOf { tx -> tx.amount }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Total Outgoings",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$currency${String.format(Locale.getDefault(), "%,.1f", totalSpent)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(50.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Total Income",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$currency${String.format(Locale.getDefault(), "%,.1f", totalIn)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Category Spending Pie-Chart Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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

        // Historical bar graph compared month to month
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Monthly Outgoings History",
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

        // --- APPEARANCE THEME SELECTOR CARD ---
        item {
            val themeMode by viewModel.themePreference.collectAsStateWithLifecycle()
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

        // --- EXPORT BUILDER CONTROLLER TOOL ---
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
                                viewModel.exportBackupJson { backup ->
                                    clipManager.setText(AnnotatedString(backup))
                                    Toast.makeText(context, "Full Backup string cloned to clipboard!", Toast.LENGTH_SHORT).show()
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
                                viewModel.exportBackupJson { backup ->
                                    try {
                                        val file = java.io.File(context.cacheDir, "ExpenseTracker_Master_Backup.json")
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
fun MoreHubScreen(viewModel: ExpenseViewModel) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Insights", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) },
                icon = { Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Debts/Lending", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) },
                icon = { Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Backups", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) },
                icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> AnalyticsScreen(viewModel = viewModel)
                1 -> DebtModuleScreen(viewModel = viewModel)
                2 -> SettingsScreen(viewModel = viewModel)
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
    var selectedStatusFilter by remember { mutableStateOf("ALL") } // "ALL", "ACTIVE", "OVERDUE", "RECOVERED", "REPAID"

    var showAddDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<DebtEntry?>(null) }
    var activePaymentEntry by remember { mutableStateOf<DebtEntry?>(null) }
    var expandedEntryIds by remember { mutableStateOf(setOf<Int>()) }

    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    // Filter debt entries
    val filteredEntries = remember(debtEntries, selectedDebtType, searchText, selectedStatusFilter) {
        val typeFiltered = debtEntries.filter { it.type == selectedDebtType }
        val searchFiltered = if (searchText.isBlank()) {
            typeFiltered
        } else {
            typeFiltered.filter { it.personName.contains(searchText, ignoreCase = true) || it.notes.contains(searchText, ignoreCase = true) }
        }
        val statusFiltered = if (selectedStatusFilter == "ALL") {
            searchFiltered
        } else if (selectedStatusFilter == "OVERDUE") {
            val now = System.currentTimeMillis()
            searchFiltered.filter { 
                it.status.uppercase() == "OVERDUE" || (it.status.uppercase() != "RECOVERED" && it.status.uppercase() != "REPAID" && it.dueDate < now)
            }
        } else {
            searchFiltered.filter { it.status.uppercase() == selectedStatusFilter }
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

        // 2. Stats Summary Row for Active Tab
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (selectedDebtType == "LENT") "Total Outstanding Lent" else "Total Remaining Debt",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val value = if (selectedDebtType == "LENT") dStats.outstandingLent else dStats.remainingBorrowed
                    Text(
                        text = "$currency${String.format(Locale.getDefault(), "%,.1f", value)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (selectedDebtType == "LENT") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                }

                Box(
                    modifier = Modifier
                        .background(if (selectedDebtType == "LENT") MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f), CircleShape)
                        .padding(10.dp)
                ) {
                    Icon(
                        imageVector = if (selectedDebtType == "LENT") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (selectedDebtType == "LENT") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // 3. Search and Quick Filter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Search by name...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
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

        // Quick Status Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val statusOptions = listOf(
                "ALL" to "All",
                "ACTIVE" to "Active",
                "OVERDUE" to "Overdue",
                (if (selectedDebtType == "LENT") "RECOVERED" else "REPAID") to (if (selectedDebtType == "LENT") "Recovered" else "Repaid")
            )

            statusOptions.forEach { (optionVal, label) ->
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
                        .clickable { selectedStatusFilter = optionVal }
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

                    val totalPaid = remember(paymentsForThisEntry, entry) {
                        if (paymentsForThisEntry.isEmpty()) {
                            if (entry.status.uppercase() == "RECOVERED" || entry.status.uppercase() == "REPAID") {
                                entry.amount
                            } else {
                                0.0
                            }
                        } else {
                            paymentsForThisEntry.sumOf { it.amount }
                        }
                    }

                    val remainingBalance = remember(entry, totalPaid) {
                        (entry.amount - totalPaid).coerceAtLeast(0.0)
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
                            if (entry.amount > 0.0) {
                                val progressFraction = (totalPaid / entry.amount).toFloat().coerceIn(0f, 1f)
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
                                            text = "Total: $currency${String.format(Locale.getDefault(), "%,.0f", entry.amount)}",
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
                                        runningRemaining = (runningRemaining - pay.amount).coerceAtLeast(0.0)
                                        list.add(
                                            DebtTimelineEvent(
                                                title = if (entry.type == "LENT") "Received Partial Payment" else "Repaid Partial Debt",
                                                amount = pay.amount,
                                                paymentMethod = pay.paymentMethod,
                                                timestamp = pay.timestamp,
                                                remaining = runningRemaining,
                                                isInitial = false,
                                                notes = pay.notes
                                            )
                                        )
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
            onSave = { name, amt, notes, pNo, entryD, due ->
                viewModel.insertDebtEntry(
                    type = selectedDebtType,
                    personName = name,
                    amount = amt,
                    entryDate = entryD,
                    dueDate = due,
                    notes = notes,
                    phoneNumber = pNo
                )
                showAddDialog = false
            }
        )
    }

    if (editingEntry != null) {
        DebtEntryDialog(
            type = editingEntry!!.type,
            entry = editingEntry,
            onDismiss = { editingEntry = null },
            onSave = { name, amt, notes, pNo, entryD, due ->
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
    onSave: (name: String, amount: Double, notes: String, phoneNumber: String?, entryDate: Long, dueDate: Long) -> Unit
) {
    val context = LocalContext.current
    var personName by remember { mutableStateOf(entry?.personName ?: "") }
    var amountStr by remember { mutableStateOf(entry?.amount?.toString() ?: "") }
    var notes by remember { mutableStateOf(entry?.notes ?: "") }
    var phoneNumber by remember { mutableStateOf(entry?.phoneNumber ?: "") }
    
    var entryDateMs by remember { mutableStateOf(entry?.entryDate ?: System.currentTimeMillis()) }
    var dueDateMs by remember { mutableStateOf(entry?.dueDate ?: (System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L))) }

    val entryCalendar = remember(entryDateMs) {
        Calendar.getInstance().apply { timeInMillis = entryDateMs }
    }
    val dueCalendar = remember(dueDateMs) {
        Calendar.getInstance().apply { timeInMillis = dueDateMs }
    }

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
                                onSave(personName, amt, notes, if (phoneNumber.isNullOrBlank()) null else phoneNumber, entryDateMs, dueDateMs)
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
