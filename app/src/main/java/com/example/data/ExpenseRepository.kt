package com.example.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    val allAccounts: Flow<List<Account>> = expenseDao.getAllAccounts()

    suspend fun getAccountById(id: Int): Account? {
        return expenseDao.getAccountById(id)
    }

    suspend fun insertAccount(account: Account): Long {
        return expenseDao.insertAccount(account)
    }

    suspend fun updateAccount(account: Account) {
        expenseDao.updateAccount(account)
    }

    /**
     * Delete an account and strictly Cascade-Delete all its associated transactions and budgets.
     */
    suspend fun deleteAccount(account: Account) {
        expenseDao.deleteTransactionsByAccount(account.id)
        expenseDao.deleteBudgetsByAccount(account.id)
        expenseDao.deleteSavingsGoalsByAccount(account.id)
        expenseDao.deleteDebtEntriesByAccount(account.id)
        expenseDao.deleteAccount(account)
    }


    // --- TRANSACTIONS ---
    fun getTransactionsForAccount(accountId: Int): Flow<List<Transaction>> {
        return expenseDao.getTransactionsForAccount(accountId)
    }

    fun getAllTransactions(): Flow<List<Transaction>> {
        return expenseDao.getAllTransactions()
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        return expenseDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        expenseDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        expenseDao.deleteTransaction(transaction)
    }


    // --- BUDGETS ---
    fun getAllBudgets(): Flow<List<Budget>> {
        return expenseDao.getAllBudgets()
    }

    fun getBudgetsForAccount(accountId: Int): Flow<List<Budget>> {
        return expenseDao.getBudgetsForAccount(accountId)
    }

    fun getBudgetsForAccountAndMonth(accountId: Int, monthYear: String): Flow<List<Budget>> {
        return expenseDao.getBudgetsForAccountAndMonth(accountId, monthYear)
    }

    suspend fun insertBudget(budget: Budget): Long {
        return expenseDao.insertBudget(budget)
    }

    suspend fun deleteBudget(budget: Budget) {
        expenseDao.deleteBudget(budget)
    }

    suspend fun deleteBudgetById(budgetId: Int) {
        expenseDao.deleteBudgetById(budgetId)
    }

    // --- SAVINGS GOALS ---
    fun getSavingsGoalsForAccount(accountId: Int): Flow<List<SavingsGoal>> {
        return expenseDao.getSavingsGoalsForAccount(accountId)
    }

    suspend fun insertSavingsGoal(goal: SavingsGoal): Long {
        return expenseDao.insertSavingsGoal(goal)
    }

    suspend fun deleteSavingsGoal(goal: SavingsGoal) {
        expenseDao.deleteSavingsGoal(goal)
    }

    suspend fun deleteSavingsGoalById(goalId: Int) {
        expenseDao.deleteSavingsGoalById(goalId)
    }

    // --- DEBT ENTRIES ---
    fun getDebtEntriesForAccount(accountId: Int): Flow<List<DebtEntry>> {
        return expenseDao.getDebtEntriesForAccount(accountId)
    }

    suspend fun insertDebtEntry(entry: DebtEntry): Long {
        return expenseDao.insertDebtEntry(entry)
    }

    suspend fun updateDebtEntry(entry: DebtEntry) {
        expenseDao.updateDebtEntry(entry)
    }

    suspend fun deleteDebtEntry(entry: DebtEntry) {
        expenseDao.deleteDebtEntry(entry)
    }

    suspend fun deleteDebtEntryById(entryId: Int) {
        expenseDao.deleteDebtEntryById(entryId)
    }

    // --- SAVINGS GOALS HISTORY ---
    fun getHistoryForGoal(goalId: Int): Flow<List<SavingsGoalHistory>> {
        return expenseDao.getHistoryForGoal(goalId)
    }

    suspend fun insertSavingsGoalHistory(history: SavingsGoalHistory): Long {
        return expenseDao.insertSavingsGoalHistory(history)
    }

    suspend fun deleteHistoryByGoal(goalId: Int) {
        expenseDao.deleteHistoryByGoal(goalId)
    }

    // --- TRANSACTION ITEMS ---
    fun getItemsForTransaction(transactionId: Int): Flow<List<TransactionItem>> {
        return expenseDao.getItemsForTransaction(transactionId)
    }

    suspend fun getItemsForTransactionDirect(transactionId: Int): List<TransactionItem> {
        return expenseDao.getItemsForTransactionDirect(transactionId)
    }

    suspend fun insertTransactionItem(item: TransactionItem): Long {
        return expenseDao.insertTransactionItem(item)
    }

    suspend fun deleteItemsByTransaction(transactionId: Int) {
        expenseDao.deleteItemsByTransaction(transactionId)
    }

    // --- DEBT PAYMENTS ---
    fun getPaymentsForDebt(debtEntryId: Int): Flow<List<DebtPayment>> {
        return expenseDao.getPaymentsForDebt(debtEntryId)
    }

    suspend fun getPaymentsForDebtDirect(debtEntryId: Int): List<DebtPayment> {
        return expenseDao.getPaymentsForDebtDirect(debtEntryId)
    }

    suspend fun insertDebtPayment(payment: DebtPayment): Long {
        return expenseDao.insertDebtPayment(payment)
    }

    suspend fun deleteDebtPayment(payment: DebtPayment) {
        expenseDao.deleteDebtPayment(payment)
    }

    suspend fun deletePaymentsByDebtId(debtEntryId: Int) {
        expenseDao.deletePaymentsByDebtId(debtEntryId)
    }

    fun getAllPayments(): Flow<List<DebtPayment>> {
        return expenseDao.getAllPayments()
    }

    // --- CREDIT CARDS ---
    fun getAllCreditCards(): Flow<List<CreditCard>> {
        return expenseDao.getAllCreditCards()
    }

    fun getCreditCardsForAccount(accountId: Int): Flow<List<CreditCard>> {
        return expenseDao.getCreditCardsForAccount(accountId)
    }

    suspend fun getCreditCardById(id: Int): CreditCard? {
        return expenseDao.getCreditCardById(id)
    }

    suspend fun insertCreditCard(card: CreditCard): Long {
        return expenseDao.insertCreditCard(card)
    }

    suspend fun updateCreditCard(card: CreditCard) {
        expenseDao.updateCreditCard(card)
    }

    suspend fun deleteCreditCard(card: CreditCard) {
        expenseDao.deleteCreditCard(card)
    }

    suspend fun deleteCreditCardsByAccount(accountId: Int) {
        expenseDao.deleteCreditCardsByAccount(accountId)
    }

    // --- CREDIT CARD REPAYMENTS ---
    fun getRepaymentsForCard(creditCardId: Int): Flow<List<CreditCardRepayment>> {
        return expenseDao.getRepaymentsForCard(creditCardId)
    }

    suspend fun getRepaymentsForCardDirect(creditCardId: Int): List<CreditCardRepayment> {
        return expenseDao.getRepaymentsForCardDirect(creditCardId)
    }

    fun getAllCreditCardRepayments(): Flow<List<CreditCardRepayment>> {
        return expenseDao.getAllCreditCardRepayments()
    }

    suspend fun insertCreditCardRepayment(repayment: CreditCardRepayment): Long {
        return expenseDao.insertCreditCardRepayment(repayment)
    }

    suspend fun deleteCreditCardRepayment(repayment: CreditCardRepayment) {
        expenseDao.deleteCreditCardRepayment(repayment)
    }

    suspend fun deleteRepaymentsByCardId(creditCardId: Int) {
        expenseDao.deleteRepaymentsByCardId(creditCardId)
    }
}
