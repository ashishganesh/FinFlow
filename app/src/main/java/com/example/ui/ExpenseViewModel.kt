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
    val enableCarryForward = MutableStateFlow(true)

    fun setCarryForwardEnabled(enabled: Boolean) {
        enableCarryForward.value = true
    }

    val themePreference = MutableStateFlow(prefs.getString("theme_preference", "system") ?: "system")

    fun setThemePreference(pref: String) {
        themePreference.value = pref
        prefs.edit().putString("theme_preference", pref).apply()
    }

    val isOnboardingCompleted = MutableStateFlow(prefs.getBoolean("onboarding_completed", false))

    fun setOnboardingCompleted(completed: Boolean) {
        isOnboardingCompleted.value = completed
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
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
    val moreSelectedTabIdx = MutableStateFlow(0)

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

    @OptIn(ExperimentalCoroutinesApi::class)
    val allDebtPayments: StateFlow<List<DebtPayment>> = _selectedAccountId
        .flatMapLatest { accountId ->
            if (accountId != -1) {
                repository.getAllPayments()
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val creditCards: StateFlow<List<CreditCard>> = _selectedAccountId
        .flatMapLatest { accountId ->
            if (accountId != -1) {
                repository.getCreditCardsForAccount(accountId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val creditCardRepayments: StateFlow<List<CreditCardRepayment>> = _selectedAccountId
        .flatMapLatest { accountId ->
            if (accountId != -1) {
                repository.getAllCreditCardRepayments()
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debtStats: StateFlow<DebtStats> = combine(debtEntries, allDebtPayments) { entries, payments ->
        val currentTime = System.currentTimeMillis()
        var lentTotal = 0.0
        var recoveredTotal = 0.0
        var outstandingLentTotal = 0.0
        var overdueLentTotal = 0.0

        var borrowedTotal = 0.0
        var repaidTotal = 0.0
        var remainingBorrowedTotal = 0.0
        var overdueBorrowedTotal = 0.0

        val paymentsMap = payments.groupBy { it.debtEntryId }

        entries.forEach { entry ->
            val entryPaymentsObj = paymentsMap[entry.id] ?: emptyList()
            val totalLentOrBorrowedAdded = entry.amount + entryPaymentsObj.filter { it.isAddition }.sumOf { it.amount }
            val totalPaidForThisEntry = if (entryPaymentsObj.isEmpty()) {
                if (entry.status.uppercase() == "RECOVERED" || entry.status.uppercase() == "REPAID") {
                    entry.amount
                } else {
                    0.0
                }
            } else {
                entryPaymentsObj.filter { !it.isAddition }.sumOf { it.amount }
            }

            val remainingForEntry = (totalLentOrBorrowedAdded - totalPaidForThisEntry).coerceAtLeast(0.0)
            val isOverdue = entry.status.uppercase() == "OVERDUE" || 
                (entry.status.uppercase() != "RECOVERED" && entry.status.uppercase() != "REPAID" && remainingForEntry > 0.0 && entry.dueDate < currentTime)
            
            if (entry.type == "LENT") {
                lentTotal += totalLentOrBorrowedAdded
                recoveredTotal += totalPaidForThisEntry
                outstandingLentTotal += remainingForEntry
                if (isOverdue) {
                    overdueLentTotal += remainingForEntry
                }
            } else if (entry.type == "BORROWED") {
                borrowedTotal += totalLentOrBorrowedAdded
                repaidTotal += totalPaidForThisEntry
                remainingBorrowedTotal += remainingForEntry
                if (isOverdue) {
                    overdueBorrowedTotal += remainingForEntry
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
        
        val cashIncome = txList.filter { it.type == "INCOME" && it.paymentMode.equals("Cash", ignoreCase = true) }.sumOf { it.amount }
        val cashExpense = txList.filter { it.type == "EXPENSE" && it.paymentMode.equals("Cash", ignoreCase = true) && !it.paymentMode.equals("Credit Card", ignoreCase = true) && it.creditCardId == null }.sumOf { it.amount }
        val cashBalanceValue = cashIncome - cashExpense
        
        val bankIncome = txList.filter { it.type == "INCOME" && !it.paymentMode.equals("Cash", ignoreCase = true) && !it.paymentMode.equals("Credit Card", ignoreCase = true) && it.creditCardId == null }.sumOf { it.amount }
        val bankExpense = txList.filter { it.type == "EXPENSE" && !it.paymentMode.equals("Cash", ignoreCase = true) && !it.paymentMode.equals("Credit Card", ignoreCase = true) && it.creditCardId == null }.sumOf { it.amount }
        val bankBalanceValue = bankIncome - bankExpense
        
        BalanceStats(
            income = totalIncome,
            expense = totalExpense,
            netBalance = cashBalanceValue + bankBalanceValue,
            cashBalance = cashBalanceValue,
            bankBalance = bankBalanceValue
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
        // Manage reactive workspace selection starting completely empty
        viewModelScope.launch {
            repository.allAccounts.collect { accountList ->
                if (accountList.isNotEmpty()) {
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
                } else {
                    _selectedAccountId.value = -1
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

    fun verifyCurrentPin(enteredPin: String, expectedPin: String?): Boolean {
        if (lockoutSecondsRemaining.value > 0) return false
        if (expectedPin == enteredPin) {
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

    // --- DATABASE MUTATIONS ---

    fun addAccount(
        name: String,
        pin: String?,
        color: Int,
        currency: String,
        avatar: String = "Personal",
        themePreference: String = "Dark Purple",
        openingCashBalance: Double = 0.0,
        openingBankBalance: Double = 0.0
    ) {
        viewModelScope.launch {
            val trimmedPin = if (pin.isNullOrBlank()) null else pin.trim()
            val newAcc = Account(
                name = name,
                pin = trimmedPin,
                color = color,
                currency = currency,
                avatar = avatar,
                themePreference = themePreference,
                cashBalance = openingCashBalance,
                bankBalance = openingBankBalance
            )
            val newId = repository.insertAccount(newAcc)
            
            // Log opening cash balance as an initial INCOME transaction
            if (openingCashBalance > 0.0) {
                val initCashTx = Transaction(
                    accountId = newId.toInt(),
                    amount = openingCashBalance,
                    title = "Opening Cash Balance",
                    timestamp = System.currentTimeMillis(),
                    remarks = "Initial opening cash balance",
                    type = "INCOME",
                    category = "Other",
                    isRecurring = false,
                    paymentMode = "Cash"
                )
                repository.insertTransaction(initCashTx)
            }
            
            // Log opening bank balance as an initial INCOME transaction
            if (openingBankBalance > 0.0) {
                val initBankTx = Transaction(
                    accountId = newId.toInt(),
                    amount = openingBankBalance,
                    title = "Opening Bank Balance",
                    timestamp = System.currentTimeMillis(),
                    remarks = "Initial opening bank balance",
                    type = "INCOME",
                    category = "Other",
                    isRecurring = false,
                    paymentMode = "Bank"
                )
                repository.insertTransaction(initBankTx)
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
        imagePath: String? = null,
        creditCardId: Int? = null
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
                imagePath = imagePath,
                creditCardId = creditCardId
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
            val title = "Budget Warning"
            val msg = "${category} budget has reached 100%."
            NotificationHelper.showNotification(
                context = getApplication(),
                id = 2001,
                title = title,
                message = msg,
                targetScreen = "budgets",
                highPriority = false
            )
            _alertMessage.emit("⚠️ Budget warning! Total spending on $category is now ${currentSpending}, which exceeds your monthly limit of ${budget.amount}!")
        } else if (currentSpending + addedAmount >= budget.amount * 0.9) {
            val title = "Budget Warning"
            val msg = "${category} budget has reached 90%."
            NotificationHelper.showNotification(
                context = getApplication(),
                id = 2002,
                title = title,
                message = msg,
                targetScreen = "budgets",
                highPriority = false
            )
            _alertMessage.emit("⚠️ Budget notice! You have used over 90% of your $category budget monthly cap (${currentSpending} / ${budget.amount})!")
        }
    }

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch {
            repository.deleteItemsByTransaction(tx.id)
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
                        val dateSuffix = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
                        val file = java.io.File(context.cacheDir, "FinTrackerPro_Report_$dateSuffix.csv")
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
            repository.deleteHistoryByGoal(goal.id)
            repository.deleteSavingsGoal(goal)
        }
    }

    fun getSavingsGoalHistory(goalId: Int): Flow<List<SavingsGoalHistory>> {
        return repository.getHistoryForGoal(goalId)
    }

    fun addSavingsGoalTransaction(goal: SavingsGoal, amount: Double, note: String) {
        viewModelScope.launch {
            val updated = goal.copy(currentAmount = goal.currentAmount + amount)
            repository.insertSavingsGoal(updated)
            val history = SavingsGoalHistory(
                savingsGoalId = goal.id,
                action = "Added",
                amount = amount,
                timestamp = System.currentTimeMillis(),
                note = note
            )
            repository.insertSavingsGoalHistory(history)
        }
    }

    fun withdrawSavingsGoalTransaction(goal: SavingsGoal, amount: Double, note: String) {
        viewModelScope.launch {
            if (amount > goal.currentAmount) {
                _alertMessage.emit("Cannot withdraw more than current saved amount!")
                return@launch
            }
            val updated = goal.copy(currentAmount = goal.currentAmount - amount)
            repository.insertSavingsGoal(updated)
            val history = SavingsGoalHistory(
                savingsGoalId = goal.id,
                action = "Withdrawn",
                amount = amount,
                timestamp = System.currentTimeMillis(),
                note = note
            )
            repository.insertSavingsGoalHistory(history)
        }
    }

    // --- TRANSACTION ITEMS DETAILS ---
    fun getItemsForTransaction(transactionId: Int): Flow<List<TransactionItem>> {
        return repository.getItemsForTransaction(transactionId)
    }

    fun insertTransactionWithItems(
        amount: Double,
        title: String,
        category: String,
        type: String,
        remarks: String,
        timestamp: Long,
        isRecurring: Boolean,
        recurringInterval: String?,
        paymentMode: String = "Cash",
        imagePath: String? = null,
        items: List<TransactionItem>,
        creditCardId: Int? = null
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
                imagePath = imagePath,
                creditCardId = creditCardId
            )
            val newTxId = repository.insertTransaction(tx)

            items.forEach { item ->
                repository.insertTransactionItem(item.copy(transactionId = newTxId.toInt()))
            }

            // Budget Threshold warning verification
            checkBudgetExceededWarning(category, amount, type)
        }
    }

    fun updateTransactionWithItems(tx: Transaction, items: List<TransactionItem>) {
        viewModelScope.launch {
            repository.updateTransaction(tx)
            repository.deleteItemsByTransaction(tx.id)
            items.forEach { item ->
                repository.insertTransactionItem(item.copy(transactionId = tx.id))
            }
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
        repeatReminder: Boolean = false,
        paymentMethod: String = "Cash"
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

                // Add standard transaction to deduct (expense for lending) or add (income for borrowing) balance
                val txType = if (type == "LENT") "EXPENSE" else "INCOME"
                val txTitle = if (type == "LENT") "Money Lent (to $personName)" else "Money Borrowed (from $personName)"
                val normalizedPaymentMode = when (paymentMethod) {
                    "Cash" -> "Cash"
                    "Bank Account", "Bank / Online" -> "Bank"
                    "UPI" -> "UPI"
                    "Card" -> "Card"
                    else -> "Bank"
                }
                val tx = Transaction(
                    accountId = activeId,
                    amount = amount,
                    title = txTitle,
                    timestamp = entryDate,
                    remarks = notes.ifBlank { "Initial ledger entry setup" },
                    type = txType,
                    category = "Other",
                    isRecurring = false,
                    paymentMode = normalizedPaymentMode
                )
                repository.insertTransaction(tx)
            }
        }
    }

    fun insertDebtAddition(
        entry: DebtEntry,
        amount: Double,
        paymentMethod: String,
        entryDate: Long,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val payment = DebtPayment(
                debtEntryId = entry.id,
                amount = amount,
                timestamp = entryDate,
                paymentMethod = paymentMethod,
                notes = notes,
                isAddition = true
            )
            repository.insertDebtPayment(payment)

            // Dynamic transaction to subtract wallet balance (EXPENSE for lent) or addition (INCOME for borrow)
            val txType = if (entry.type == "LENT") "EXPENSE" else "INCOME"
            val txTitle = if (entry.type == "LENT") {
                "Lent More Money (to ${entry.personName})"
            } else {
                "Borrowed More Money (from ${entry.personName})"
            }

            val normalizedPaymentMode = when (paymentMethod) {
                "Cash" -> "Cash"
                "Bank Account", "Bank / Online" -> "Bank"
                "UPI" -> "UPI"
                "Card" -> "Card"
                else -> "Bank"
            }

            val tx = Transaction(
                accountId = entry.accountId,
                amount = amount,
                title = txTitle,
                timestamp = entryDate,
                remarks = notes.ifBlank { "Added to ${entry.personName}'s ledger" },
                type = txType,
                category = "Other",
                isRecurring = false,
                paymentMode = normalizedPaymentMode
            )
            repository.insertTransaction(tx)

            // Automatically update status back to ACTIVE if there is an outstanding amount
            val allPaymentsForDebt = repository.getPaymentsForDebtDirect(entry.id)
            val totalLentOrBorrowedAdded = entry.amount + allPaymentsForDebt.filter { it.isAddition }.sumOf { it.amount }
            val totalPaidAll = allPaymentsForDebt.filter { !it.isAddition }.sumOf { it.amount }
            if (totalLentOrBorrowedAdded > totalPaidAll) {
                repository.updateDebtEntry(entry.copy(status = "ACTIVE"))
            } else {
                val finalStatus = if (entry.type == "LENT") "RECOVERED" else "REPAID"
                repository.updateDebtEntry(entry.copy(status = finalStatus))
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
            repository.deletePaymentsByDebtId(entry.id)
            repository.deleteDebtEntry(entry)
        }
    }

    fun deleteDebtEntryById(entryId: Int) {
        viewModelScope.launch {
            repository.deletePaymentsByDebtId(entryId)
            repository.deleteDebtEntryById(entryId)
        }
    }

    fun recordDebtPayment(
        entry: DebtEntry,
        amount: Double,
        paymentMethod: String,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val payment = DebtPayment(
                debtEntryId = entry.id,
                amount = amount,
                timestamp = System.currentTimeMillis(),
                paymentMethod = paymentMethod,
                notes = notes,
                isAddition = false
            )
            repository.insertDebtPayment(payment)

            val txType = if (entry.type == "LENT") "INCOME" else "EXPENSE"
            val txTitle = if (entry.type == "LENT") {
                "Lent Recovered (from ${entry.personName})"
            } else {
                "Borrowed Repaid (to ${entry.personName})"
            }

            val normalizedPaymentMode = when (paymentMethod) {
                "Cash" -> "Cash"
                "Bank Account", "Bank / Online" -> "Bank"
                "UPI" -> "UPI"
                "Card" -> "Card"
                else -> "Bank"
            }

            val tx = Transaction(
                accountId = entry.accountId,
                amount = amount,
                title = txTitle,
                timestamp = System.currentTimeMillis(),
                remarks = notes.ifBlank { "Recorded payment for ${entry.personName}'s debt" },
                type = txType,
                category = "Other",
                isRecurring = false,
                paymentMode = normalizedPaymentMode
            )
            repository.insertTransaction(tx)

            // Auto update status
            val allPaymentsForDebt = repository.getPaymentsForDebtDirect(entry.id)
            val totalLentOrBorrowedAdded = entry.amount + allPaymentsForDebt.filter { it.isAddition }.sumOf { it.amount }
            val totalPaidAll = allPaymentsForDebt.filter { !it.isAddition }.sumOf { it.amount }
            if (totalPaidAll >= totalLentOrBorrowedAdded) {
                val finalStatus = if (entry.type == "LENT") "RECOVERED" else "REPAID"
                repository.updateDebtEntry(entry.copy(status = finalStatus))
            } else {
                repository.updateDebtEntry(entry.copy(status = "ACTIVE"))
            }
        }
    }

    // --- CREDIT CARD VM OPERATIONS ---
    fun insertCreditCard(
        cardName: String,
        cardIssuer: String,
        creditLimit: Double,
        billingCycleDate: Int,
        paymentDueDate: Int,
        interestRate: Double? = null,
        colorHex: String = "#FF1A237E"
    ) {
        viewModelScope.launch {
            val activeId = _selectedAccountId.value
            if (activeId == -1) return@launch
            val card = CreditCard(
                accountId = activeId,
                cardName = cardName,
                cardIssuer = cardIssuer,
                creditLimit = creditLimit,
                billingCycleDate = billingCycleDate,
                paymentDueDate = paymentDueDate,
                interestRate = interestRate,
                colorHex = colorHex
            )
            repository.insertCreditCard(card)
        }
    }

    fun updateCreditCard(card: CreditCard) {
        viewModelScope.launch {
            repository.updateCreditCard(card)
        }
    }

    fun deleteCreditCard(card: CreditCard) {
        viewModelScope.launch {
            repository.deleteCreditCard(card)
            // also clean up any repayments associated with it
            repository.deleteRepaymentsByCardId(card.id)
        }
    }

    fun insertCreditCardRepayment(
        card: CreditCard,
        amount: Double,
        paymentSource: String, // "Cash" or "Bank"
        notes: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            // 1. Save repayment in credit_card_repayments
            val repayment = CreditCardRepayment(
                creditCardId = card.id,
                amount = amount,
                timestamp = timestamp,
                paymentSource = paymentSource,
                notes = notes
            )
            repository.insertCreditCardRepayment(repayment)

            // 2. Synchronize with Transaction list to automatically reduce wallet balances
            val systemNotes = if (notes.isNotBlank()) notes else "Repayment for ${card.cardName}"
            val tx = Transaction(
                accountId = card.accountId,
                amount = amount,
                title = "Credit Card Pay: ${card.cardName}",
                timestamp = timestamp,
                remarks = systemNotes,
                type = "EXPENSE",
                category = "Credit Card",
                paymentMode = paymentSource // "Cash" or "Bank"
            )
            repository.insertTransaction(tx)
        }
    }

    fun checkAndTriggerDueReminders(context: Context) {
        viewModelScope.launch {
            val accountId = _selectedAccountId.value
            if (accountId == -1) return@launch

            try {
                // 1. Credit Card reminders check
                val cards = repository.getCreditCardsForAccount(accountId).first()
                val txs = repository.getTransactionsForAccount(accountId).first()
                val repaymentsAll = repository.getAllCreditCardRepayments().first()

                val calendar = Calendar.getInstance()
                val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

                cards.forEach { card ->
                    val repayments = repaymentsAll.filter { it.creditCardId == card.id }
                    val cardSpends = txs.filter { it.type == "EXPENSE" && it.creditCardId == card.id }.sumOf { it.amount }
                    val cardPayments = repayments.sumOf { it.amount }
                    val outstanding = (cardSpends - cardPayments).coerceAtLeast(0.0)

                    if (outstanding > 0.0) {
                        val dueDay = card.paymentDueDate
                        val daysLeft = if (dueDay >= currentDay) {
                            dueDay - currentDay
                        } else {
                            val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                            (maxDays - currentDay) + dueDay
                        }

                        // Criteria: payment due in exactly 3 days
                        if (daysLeft == 3) {
                            NotificationHelper.showNotification(
                                context = context,
                                id = 3000 + card.id,
                                title = "Credit Card Due Soon",
                                message = "${card.cardName} payment due in 3 days.",
                                targetScreen = "credit_cards",
                                highPriority = true
                            )
                        }
                    }
                }

                // 2. Khata (DebtEntry) reminders check
                val debts = repository.getDebtEntriesForAccount(accountId).first()
                val oneDayMs = 24 * 60 * 60 * 1000L
                val nowMs = System.currentTimeMillis()

                debts.forEach { entry ->
                    if (entry.status.uppercase() != "RECOVERED" && entry.status.uppercase() != "REPAID") {
                        val timeDiff = entry.dueDate - nowMs
                        // Criteria: due tomorrow (between 12 and 36 hours remaining)
                        val isDueTomorrow = timeDiff in (12 * 60 * 60 * 1000L)..(36 * 60 * 60 * 1000L)
                        if (isDueTomorrow) {
                            NotificationHelper.showNotification(
                                context = context,
                                id = 4000 + entry.id,
                                title = "Payment Reminder",
                                message = "${entry.personName}'s repayment is due tomorrow.",
                                targetScreen = "debts",
                                highPriority = true
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

// Stats Holder
data class BalanceStats(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val netBalance: Double = 0.0,
    val cashBalance: Double = 0.0,
    val bankBalance: Double = 0.0
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
