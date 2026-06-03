package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ExpenseViewModel(
    application: Application,
    private val repository: ExpenseRepository
) : AndroidViewModel(application) {

    // Preset standard categories
    val defaultCategories = listOf(
        "Food", "Travel", "Bills", "Shopping", "Education",
        "Health", "Rent", "Entertainment", "Investment", "Other"
    )

    // --- ACCREDITED ACCOUNTS ---
    val accounts = repository.allAccounts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedAccountId = MutableStateFlow<Int>(-1)
    val selectedAccountId: StateFlow<Int> = _selectedAccountId.asStateFlow()

    // Keep track of which PIN-restricted account IDs have been unlocked in this session
    private val _authenticatedAccountIds = MutableStateFlow<Set<Int>>(emptySet())
    val authenticatedAccountIds: StateFlow<Set<Int>> = _authenticatedAccountIds.asStateFlow()

    // PIN lock workflow state
    private val _authTargetAccount = MutableStateFlow<Account?>(null)
    val authTargetAccount: StateFlow<Account?> = _authTargetAccount.asStateFlow()

    // Active Account state
    val activeAccount: StateFlow<Account?> = combine(
        accounts,
        _selectedAccountId
    ) { accountList, activeId ->
        accountList.find { it.id == activeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- PERSISTED SETTINGS ---
    private val prefs = application.getSharedPreferences("expense_tracker_prefs", android.content.Context.MODE_PRIVATE)
    val enableCarryForward = MutableStateFlow(prefs.getBoolean("enable_carry_forward", true))

    fun setCarryForwardEnabled(enabled: Boolean) {
        enableCarryForward.value = enabled
        prefs.edit().putBoolean("enable_carry_forward", enabled).apply()
    }

    val themePreference = MutableStateFlow(prefs.getString("theme_preference", "system") ?: "system")

    fun setThemePreference(pref: String) {
        themePreference.value = pref
        prefs.edit().putString("theme_preference", pref).apply()
    }

    // --- FILTERS ---
    val searchQuery = MutableStateFlow("")
    val categoryFilter = MutableStateFlow("All")
    val typeFilter = MutableStateFlow("All") // "All", "EXPENSE", "INCOME"
    val dateRangeFilter = MutableStateFlow("All") // "All", "Today", "This Week", "This Month", "This Year", "Custom"
    val customStartDate = MutableStateFlow<Long?>(null)
    val customEndDate = MutableStateFlow<Long?>(null)

    // Active screen route
    val currentScreen = MutableStateFlow("dashboard") // "dashboard", "transactions", "analytics", "budgets", "settings", "accounts"

    // Custom currencies array
    val availableCurrencies = listOf("USD ($)", "EUR (€)", "GBP (£)", "INR (₹)", "JPY (¥)", "CAD ($)", "AUD ($)", "CNY (¥)")

    // Dynamic error/alert messages
    private val _alertMessage = MutableSharedFlow<String>()
    val alertMessage = _alertMessage.asSharedFlow()

    // --- TRANSACTIONS STREAM ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<Transaction>> = _selectedAccountId
        .flatMapLatest { accountId ->
            if (accountId != -1) {
                repository.getTransactionsForAccount(accountId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- BUDGETS STREAM ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val budgets: StateFlow<List<Budget>> = _selectedAccountId
        .flatMapLatest { accountId ->
            if (accountId != -1) {
                repository.getBudgetsForAccount(accountId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- SAVINGS GOALS STREAM ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val savingsGoals: StateFlow<List<SavingsGoal>> = _selectedAccountId
        .flatMapLatest { accountId ->
            if (accountId != -1) {
                repository.getSavingsGoalsForAccount(accountId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- COOPERATIVE DEBT AND LENDING STREAMS ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val debtEntries: StateFlow<List<DebtEntry>> = _selectedAccountId
        .flatMapLatest { accountId ->
            if (accountId != -1) {
                repository.getDebtEntriesForAccount(accountId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debtStats: StateFlow<DebtStats> = debtEntries.map { list ->
        val currentTime = System.currentTimeMillis()
        var lentTotal = 0.0
        var recoveredTotal = 0.0
        var outstandingLentTotal = 0.0
        var overdueLentTotal = 0.0

        var borrowedTotal = 0.0
        var repaidTotal = 0.0
        var remainingBorrowedTotal = 0.0
        var overdueBorrowedTotal = 0.0

        list.forEach { entry ->
            val isOverdue = entry.status.uppercase() == "OVERDUE" || 
                (entry.status.uppercase() != "RECOVERED" && entry.status.uppercase() != "REPAID" && entry.dueDate < currentTime)
            
            if (entry.type == "LENT") {
                lentTotal += entry.amount
                if (entry.status.uppercase() == "RECOVERED") {
                    recoveredTotal += entry.amount
                } else {
                    outstandingLentTotal += entry.amount
                    if (isOverdue) {
                        overdueLentTotal += entry.amount
                    }
                }
            } else if (entry.type == "BORROWED") {
                borrowedTotal += entry.amount
                if (entry.status.uppercase() == "REPAID") {
                    repaidTotal += entry.amount
                } else {
                    remainingBorrowedTotal += entry.amount
                    if (isOverdue) {
                        overdueBorrowedTotal += entry.amount
                    }
                }
            }
        }

        DebtStats(
            totalLent = lentTotal,
            totalRecovered = recoveredTotal,
            outstandingLent = outstandingLentTotal,
            overdueLent = overdueLentTotal,
            totalBorrowed = borrowedTotal,
            totalRepaid = repaidTotal,
            remainingBorrowed = remainingBorrowedTotal,
            overdueBorrowed = overdueBorrowedTotal
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DebtStats())

    // --- EXTRACTED DYNAMIC CATEGORIES ---
    // Includes standard defaults + any custom categories parsed from transactions
    val availableCategories: StateFlow<List<String>> = transactions.map { txList ->
        val custom = txList.map { it.category }.distinct()
        val all = (defaultCategories + custom).distinct()
        all
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultCategories)


    // --- COMPILED UI STATISTICS ---
    val balanceStats = transactions.map { txList ->
        val totalIncome = txList.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = txList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        BalanceStats(
            income = totalIncome,
            expense = totalExpense,
            netBalance = totalIncome - totalExpense
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BalanceStats())


    // --- CHROME/FILTERED TRANSACTIONS ---
    @Suppress("UNCHECKED_CAST")
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        transactions,
        searchQuery,
        categoryFilter,
        typeFilter,
        dateRangeFilter,
        customStartDate,
        customEndDate
    ) { array: Array<Any?> ->
        val txList = array[0] as List<Transaction>
        val query = array[1] as String
        val cat = array[2] as String
        val type = array[3] as String
        val dateRange = array[4] as String
        val start = array[5] as Long?
        val end = array[6] as Long?

        var result = txList

        // 1. Search Query filter (by Title or Remarks)
        if (query.isNotBlank()) {
            result = result.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.remarks.contains(query, ignoreCase = true)
            }
        }

        // 2. Category filter
        if (cat != "All") {
            result = result.filter { it.category == cat }
        }

        // 3. Type filter
        if (type != "All") {
            result = result.filter { it.type == type }
        }

        // 4. Date Range filter
        val now = Calendar.getInstance()
        val startOfToday = now.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfWeek = now.apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        }.timeInMillis

        val startOfMonth = now.apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis

        val startOfYear = now.apply {
            set(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        result = when (dateRange) {
            "Today" -> result.filter { it.timestamp >= startOfToday }
            "This Week" -> result.filter { it.timestamp >= startOfWeek }
            "This Month" -> result.filter { it.timestamp >= startOfMonth }
            "This Year" -> result.filter { it.timestamp >= startOfYear }
            "Custom" -> {
                if (start != null && end != null) {
                    result.filter { it.timestamp in start..end }
                } else if (start != null) {
                    result.filter { it.timestamp >= start }
                } else if (end != null) {
                    result.filter { it.timestamp <= end }
                } else {
                    result
                }
            }
            else -> result
        }

        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- ACCOUNTS & PIN SECURITY LOGIC ---

    // Failed attempts and countdown timer state
    val pinFailedAttempts = MutableStateFlow(0)
    val lockoutSecondsRemaining = MutableStateFlow(0)

    private fun startLockoutTimer() {
        lockoutSecondsRemaining.value = 30
        viewModelScope.launch {
            while (lockoutSecondsRemaining.value > 0) {
                kotlinx.coroutines.delay(1000)
                lockoutSecondsRemaining.value -= 1
            }
        }
    }

    init {
        // Create a default "Personal" account on initial run if database empty, and manage reactive default selection
        viewModelScope.launch {
            repository.allAccounts.collect { accountList ->
                if (accountList.isEmpty()) {
                    val defaultId = repository.insertAccount(
                        Account(
                            name = "Personal Account",
                            pin = null,
                            color = 0xFF0D9488.toInt(), // Modern Teal accent
                            currency = "₹"
                        )
                    )
                    _selectedAccountId.value = defaultId.toInt()
                    prefs.edit().putInt("selected_account_id", defaultId.toInt()).apply()
                    
                    // Prepopulate beautiful sample savings goals
                    repository.insertSavingsGoal(
                        SavingsGoal(
                            accountId = defaultId.toInt(),
                            name = "Emergency Fund",
                            targetAmount = 50000.0,
                            currentAmount = 25000.0,
                            dueDate = "Dec 31, 2026"
                        )
                    )
                    repository.insertSavingsGoal(
                        SavingsGoal(
                            accountId = defaultId.toInt(),
                            name = "New Laptop",
                            targetAmount = 60000.0,
                            currentAmount = 32000.0,
                            dueDate = "Aug 15, 2026"
                        )
                    )
                } else {
                    val currentId = _selectedAccountId.value
                    if (currentId == -1) {
                        // Startup initialization
                        val savedId = prefs.getInt("selected_account_id", -1)
                        val lastSelected = accountList.find { it.id == savedId }
                        if (lastSelected != null) {
                            if (lastSelected.pin != null) {
                                _authTargetAccount.value = lastSelected
                            } else {
                                _selectedAccountId.value = lastSelected.id
                            }
                        } else {
                            // Select first account
                            val first = accountList.firstOrNull()
                            if (first != null) {
                                if (first.pin != null) {
                                    _authTargetAccount.value = first
                                } else {
                                    _selectedAccountId.value = first.id
                                    prefs.edit().putInt("selected_account_id", first.id).apply()
                                }
                            }
                        }
                    } else {
                        // Ensure active account still exists
                        val exists = accountList.any { it.id == currentId }
                        if (!exists) {
                            val first = accountList.firstOrNull()
                            if (first != null) {
                                if (first.pin != null) {
                                    _authTargetAccount.value = first
                                } else {
                                    _selectedAccountId.value = first.id
                                    prefs.edit().putInt("selected_account_id", first.id).apply()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun selectAccount(account: Account) {
        if (lockoutSecondsRemaining.value > 0) return
        if (account.pin == null) {
            _selectedAccountId.value = account.id
            prefs.edit().putInt("selected_account_id", account.id).apply()
            _authTargetAccount.value = null
        } else {
            // Needs auth (Require PIN when switching to another protected profile)
            _authTargetAccount.value = account
        }
    }

    fun authenticateTargetAccount(enteredPin: String): Boolean {
        if (lockoutSecondsRemaining.value > 0) return false
        val target = _authTargetAccount.value ?: return false
        if (target.pin == enteredPin) {
            // Success
            _selectedAccountId.value = target.id
            prefs.edit().putInt("selected_account_id", target.id).apply()
            _authTargetAccount.value = null
            pinFailedAttempts.value = 0
            return true
        } else {
            pinFailedAttempts.value += 1
            if (pinFailedAttempts.value >= 5) {
                startLockoutTimer()
            }
            return false
        }
    }

    fun cancelAuthentication() {
        if (_selectedAccountId.value != -1) {
            _authTargetAccount.value = null
        } else {
            // Keep app locked since user hasn't authenticated any account yet on startup!
        }
    }

    fun lockAccountManually() {
        val active = activeAccount.value ?: return
        if (active.pin != null) {
            _selectedAccountId.value = -1
            _authTargetAccount.value = active
        }
    }

    // --- DATABASE MUTATIONS ---

    fun addAccount(
        name: String,
        pin: String?,
        color: Int,
        currency: String,
        avatar: String = "Personal",
        themePreference: String = "Dark Purple",
        openingBalance: Double = 0.0
    ) {
        viewModelScope.launch {
            val trimmedPin = if (pin.isNullOrBlank()) null else pin.trim()
            val newAcc = Account(
                name = name,
                pin = trimmedPin,
                color = color,
                currency = currency,
                avatar = avatar,
                themePreference = themePreference
            )
            val newId = repository.insertAccount(newAcc)
            
            // Log opening balance as an initial INCOME transaction
            if (openingBalance > 0.0) {
                val initTx = Transaction(
                    accountId = newId.toInt(),
                    amount = openingBalance,
                    title = "Opening Balance",
                    timestamp = System.currentTimeMillis(),
                    remarks = "Initial opening balance",
                    type = "INCOME",
                    category = "Other",
                    isRecurring = false,
                    paymentMode = "Cash"
                )
                repository.insertTransaction(initTx)
            }
            
            _selectedAccountId.value = newId.toInt()
            prefs.edit().putInt("selected_account_id", newId.toInt()).apply()
        }
    }

    fun updateAccountSettings(
        account: Account,
        newName: String,
        newPin: String?,
        newColor: Int,
        newCurrency: String,
        newAvatar: String = "Personal",
        newThemePreference: String = "Dark Purple"
    ) {
        viewModelScope.launch {
            val trimmedPin = if (newPin.isNullOrBlank()) null else newPin.trim()
            val updated = account.copy(
                name = newName,
                pin = trimmedPin,
                color = newColor,
                currency = newCurrency,
                avatar = newAvatar,
                themePreference = newThemePreference
            )
            repository.updateAccount(updated)
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            repository.deleteAccount(account)
            _selectedAccountId.value = -1

            // Select another generic account if available
            val list = repository.allAccounts.first()
            val first = list.firstOrNull { it.pin == null } ?: list.firstOrNull()
            if (first != null) {
                selectAccount(first)
            }
        }
    }

    fun insertTransaction(
        amount: Double,
        title: String,
        category: String,
        type: String,
        remarks: String,
        timestamp: Long,
        isRecurring: Boolean,
        recurringInterval: String?,
        paymentMode: String = "Cash",
        imagePath: String? = null
    ) {
        viewModelScope.launch {
            val activeId = _selectedAccountId.value
            if (activeId == -1) return@launch

            val tx = Transaction(
                accountId = activeId,
                amount = amount,
                title = title,
                timestamp = timestamp,
                remarks = remarks,
                type = type,
                category = category,
                isRecurring = isRecurring,
                recurringInterval = recurringInterval,
                paymentMode = paymentMode,
                imagePath = imagePath
            )
            repository.insertTransaction(tx)

            // Budget Threshold warning verification
            checkBudgetExceededWarning(category, amount, type)
        }
    }

    fun transferBalance(
        destinationAccountId: Int,
        amount: Double,
        title: String,
        remarks: String,
        timestamp: Long,
        paymentMode: String
    ) {
        viewModelScope.launch {
            val sourceId = _selectedAccountId.value
            if (sourceId == -1 || sourceId == destinationAccountId) return@launch

            val sourceAccount = repository.getAccountById(sourceId)
            val destAccount = repository.getAccountById(destinationAccountId)
            val sourceName = sourceAccount?.name ?: "Source"
            val destName = destAccount?.name ?: "Destination"

            // Insert EXPENSE on source side
            val sourceTx = Transaction(
                accountId = sourceId,
                amount = amount,
                title = "[Transfer Goal] $title",
                timestamp = timestamp,
                remarks = "Transfer out to $destName. $remarks",
                type = "EXPENSE",
                category = "Transfer",
                paymentMode = paymentMode
            )
            repository.insertTransaction(sourceTx)

            // Insert INCOME on destination side
            val destTx = Transaction(
                accountId = destinationAccountId,
                amount = amount,
                title = "[Transfer Recv] $title",
                timestamp = timestamp,
                remarks = "Transfer in from $sourceName. $remarks",
                type = "INCOME",
                category = "Transfer",
                paymentMode = paymentMode
            )
            repository.insertTransaction(destTx)
        }
    }

    private suspend fun checkBudgetExceededWarning(category: String, addedAmount: Double, type: String) {
        if (type != "EXPENSE") return
        val activeId = _selectedAccountId.value
        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val currentMonth = dateFormat.format(Date())

        val dbBudgets = repository.getBudgetsForAccountAndMonth(activeId, currentMonth).first()
        val budget = dbBudgets.find { it.category.equals(category, ignoreCase = true) } ?: return

        val txs = repository.getTransactionsForAccount(activeId).first()
        val calendar = Calendar.getInstance()
        val startOfMonth = calendar.apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Exclude the current added tx in this query to get prior spending matching the category
        val currentSpending = txs.filter {
            it.type == "EXPENSE" &&
                    it.category.equals(category, ignoreCase = true) &&
                    it.timestamp >= startOfMonth
        }.sumOf { it.amount }

        if (currentSpending > budget.amount) {
            _alertMessage.emit("⚠️ Budget warning! Total spending on $category is now ${currentSpending}, which exceeds your monthly limit of ${budget.amount}!")
        } else if (currentSpending + addedAmount >= budget.amount * 0.9) {
            _alertMessage.emit("⚠️ Budget notice! You have used over 90% of your $category budget monthly cap (${currentSpending} / ${budget.amount})!")
        }
    }

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
        }
    }

    fun updateTransaction(tx: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(tx)
        }
    }

    // --- BUDG-GETS ACTIONS ---
    fun setCategoryBudget(category: String, amount: Double) {
        viewModelScope.launch {
            val activeId = _selectedAccountId.value
            if (activeId == -1) return@launch
            val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val currentMonth = dateFormat.format(Date())

            val currentBudgets = repository.getBudgetsForAccountAndMonth(activeId, currentMonth).first()
            val existing = currentBudgets.find { it.category.equals(category, ignoreCase = true) }

            val budget = existing?.copy(amount = amount) ?: Budget(
                accountId = activeId,
                category = category,
                amount = amount,
                monthYear = currentMonth
            )
            repository.insertBudget(budget)
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }


    // --- BACKUP & RESTORE TRIGGERS ---
    fun exportBackupJson(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val currAccounts = accounts.value
            val totalTxs = repository.getAllTransactions().first()
            val totalBudgets = repository.getAllBudgets().first()
            val backup = BackupManager.createFullBackupJson(currAccounts, totalTxs, totalBudgets)
            onComplete(backup)
        }
    }

    /**
     * Perform a complete JSON backup parse and replace the local tables.
     */
    fun importBackupJson(backupText: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val content = BackupManager.parseFullBackupJson(backupText.trim())
            if (content == null) {
                onComplete(false)
                return@launch
            }

            try {
                // Clear active tables and load backup properties
                // 1. Restore accounts
                for (acc in content.accounts) {
                    repository.insertAccount(acc)
                }
                // 2. Restore transactions
                for (tx in content.transactions) {
                    repository.insertTransaction(tx)
                }
                // 3. Restore budgets
                for (b in content.budgets) {
                    repository.insertBudget(b)
                }

                // Pick first parsed account if loaded
                val firstAcc = content.accounts.firstOrNull()
                if (firstAcc != null) {
                    _selectedAccountId.value = firstAcc.id
                }

                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun exportTransactionsCsvText(): String {
        val txList = transactions.value
        val accMap = accounts.value.associate { it.id to it.name }
        return BackupManager.exportTransactionsToCsv(txList, accMap)
    }

    // --- ADVANCED DYNAMIC EXPORTS ---
    fun exportFilteredData(
        context: Context,
        format: String, // "PDF", "EXCEL", "CSV"
        scope: String, // "CURRENT", "ALL"
        dateRangeType: String, // "CURRENT_MONTH", "ALL_TIME", "CUSTOM"
        customStart: Long?,
        customEnd: Long?,
        onFinished: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val allAccs = repository.allAccounts.first()
                val targetAccId = _selectedAccountId.value
                val activeAcc = allAccs.find { it.id == targetAccId } ?: allAccs.firstOrNull()
                if (activeAcc == null) {
                    onFinished(false)
                    return@launch
                }

                // 1. Fetch transactions based on scope
                var txList = if (scope == "CURRENT") {
                    repository.getTransactionsForAccount(activeAcc.id).first()
                } else {
                    repository.getAllTransactions().first()
                }

                // 2. Filter by date range
                val now = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val currentMonthStr = dateFormat.format(Date(now))

                val rangeLabel: String
                txList = when (dateRangeType) {
                    "CURRENT_MONTH" -> {
                        rangeLabel = "Current Month (${currentMonthStr})"
                        txList.filter {
                            val txM = dateFormat.format(Date(it.timestamp))
                            txM == currentMonthStr
                        }
                    }
                    "CUSTOM" -> {
                        val sDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val sStr = if (customStart != null) sDate.format(Date(customStart)) else "Beginning"
                        val eStr = if (customEnd != null) sDate.format(Date(customEnd)) else "End"
                        rangeLabel = "Custom ($sStr to $eStr)"
                        txList.filter {
                            val afterStart = customStart == null || it.timestamp >= customStart
                            val beforeEnd = customEnd == null || it.timestamp <= customEnd
                            afterStart && beforeEnd
                        }
                    }
                    else -> {
                        rangeLabel = "All Time"
                        txList
                    }
                }

                // Map of account ID to name
                val accountMap = allAccs.associate { it.id to it.name }

                // 3. Perform specific export
                when (format) {
                    "PDF" -> {
                        // Generate extra metrics for report
                        val totalIncome = txList.filter { it.type == "INCOME" }.sumOf { it.amount }
                        val totalExpenses = txList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                        val netSaved = totalIncome - totalExpenses
                        
                        // Category aggregation for chart (only expense types)
                        val catSummary = txList.filter { it.type == "EXPENSE" }
                            .groupBy { it.category }
                            .mapValues { entry -> entry.value.sumOf { it.amount } }

                        // Save report using active account's name (or "All Accounts")
                        val repName = if (scope == "CURRENT") activeAcc.name else "All Accounts Combined"
                        val currencySym = if (scope == "CURRENT") activeAcc.currency else "$"

                        val file = ExportManager.generatePdfReport(
                            context = context,
                            accountName = repName,
                            dateRangeStr = rangeLabel,
                            openingBalance = 0.0, // Calculated dynamically
                            closingBalance = netSaved,
                            totalIncome = totalIncome,
                            totalExpenses = totalExpenses,
                            remainingBalance = netSaved,
                            carryForward = netSaved,
                            transactions = txList,
                            categorySummary = catSummary,
                            currencySymbol = currencySym
                        )

                        if (file != null) {
                            ExportManager.shareFile(context, file, "application/pdf", "Financial Report PDF")
                            onFinished(true)
                        } else {
                            onFinished(false)
                        }
                    }
                    "EXCEL" -> {
                        val repName = if (scope == "CURRENT") activeAcc.name else "All Accounts Combined"
                        val file = ExportManager.generateExcelReport(
                            context = context,
                            accountName = repName,
                            dateRangeStr = rangeLabel,
                            transactions = txList,
                            accountMap = accountMap
                        )
                        if (file != null) {
                            ExportManager.shareFile(context, file, "application/vnd.ms-excel", "Spreadsheet Report XLS")
                            onFinished(true)
                        } else {
                            onFinished(false)
                        }
                    }
                    "CSV" -> {
                        val csvContent = BackupManager.exportTransactionsToCsv(txList, accountMap)
                        val repName = if (scope == "CURRENT") activeAcc.name else "All Accounts Combined"
                        val file = java.io.File(context.cacheDir, "Financial_Report_${repName.replace(" ", "_")}.csv")
                        val fos = java.io.FileOutputStream(file)
                        fos.write(csvContent.toByteArray(Charsets.UTF_8))
                        fos.flush()
                        fos.close()

                        ExportManager.shareFile(context, file, "text/csv", "Spreadsheet Report CSV")
                        onFinished(true)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onFinished(false)
            }
        }
    }

    // --- SAVINGS GOALS MUTATIONS ---
    fun insertSavingsGoal(name: String, targetAmount: Double, currentAmount: Double, dueDate: String) {
        viewModelScope.launch {
            val activeId = _selectedAccountId.value
            if (activeId == -1) return@launch
            val goal = SavingsGoal(
                accountId = activeId,
                name = name,
                targetAmount = targetAmount,
                currentAmount = currentAmount,
                dueDate = dueDate
            )
            repository.insertSavingsGoal(goal)
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
        }
    }

    // --- DEBT MUTATIONS ---
    fun insertDebtEntry(
        type: String,
        personName: String,
        amount: Double,
        entryDate: Long,
        dueDate: Long,
        notes: String = "",
        phoneNumber: String? = null,
        status: String = "ACTIVE",
        reminderDate: Long? = null,
        reminderNotes: String? = null,
        repeatReminder: Boolean = false
    ) {
        viewModelScope.launch {
            val activeId = _selectedAccountId.value
            if (activeId != -1) {
                val newEntry = DebtEntry(
                    accountId = activeId,
                    type = type,
                    personName = personName,
                    amount = amount,
                    entryDate = entryDate,
                    dueDate = dueDate,
                    notes = notes,
                    phoneNumber = phoneNumber,
                    status = status,
                    reminderDate = reminderDate,
                    reminderNotes = reminderNotes,
                    repeatReminder = repeatReminder
                )
                repository.insertDebtEntry(newEntry)
            }
        }
    }

    fun updateDebtEntry(entry: DebtEntry) {
        viewModelScope.launch {
            repository.updateDebtEntry(entry)
        }
    }

    fun deleteDebtEntry(entry: DebtEntry) {
        viewModelScope.launch {
            repository.deleteDebtEntry(entry)
        }
    }

    fun deleteDebtEntryById(entryId: Int) {
        viewModelScope.launch {
            repository.deleteDebtEntryById(entryId)
        }
    }
}

// Stats Holder
data class BalanceStats(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val netBalance: Double = 0.0
)

data class DebtStats(
    val totalLent: Double = 0.0,
    val totalRecovered: Double = 0.0,
    val outstandingLent: Double = 0.0,
    val overdueLent: Double = 0.0,
    val totalBorrowed: Double = 0.0,
    val totalRepaid: Double = 0.0,
    val remainingBorrowed: Double = 0.0,
    val overdueBorrowed: Double = 0.0
)

class ExpenseViewModelFactory(
    private val application: Application,
    private val repository: ExpenseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            return ExpenseViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
