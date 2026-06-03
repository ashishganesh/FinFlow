package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    // --- ACCOUNTS ---
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Int): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteTransactionsByAccount(accountId: Int)

    @Query("DELETE FROM budgets WHERE accountId = :accountId")
    suspend fun deleteBudgetsByAccount(accountId: Int)

    @Query("DELETE FROM savings_goals WHERE accountId = :accountId")
    suspend fun deleteSavingsGoalsByAccount(accountId: Int)


    // --- TRANSACTIONS ---
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY timestamp DESC")
    fun getTransactionsForAccount(accountId: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)


    // --- BUDGETS ---
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE accountId = :accountId")
    fun getBudgetsForAccount(accountId: Int): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE accountId = :accountId AND monthYear = :monthYear")
    fun getBudgetsForAccountAndMonth(accountId: Int, monthYear: String): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("DELETE FROM budgets WHERE id = :budgetId")
    suspend fun deleteBudgetById(budgetId: Int)

    // --- SAVINGS GOALS ---
    @Query("SELECT * FROM savings_goals WHERE accountId = :accountId")
    fun getSavingsGoalsForAccount(accountId: Int): Flow<List<SavingsGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoal): Long

    @Delete
    suspend fun deleteSavingsGoal(goal: SavingsGoal)

    @Query("DELETE FROM savings_goals WHERE id = :goalId")
    suspend fun deleteSavingsGoalById(goalId: Int)

    // --- DEBT ENTRIES ---
    @Query("SELECT * FROM debt_entries WHERE accountId = :accountId ORDER BY dueDate ASC")
    fun getDebtEntriesForAccount(accountId: Int): Flow<List<DebtEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtEntry(entry: DebtEntry): Long

    @Update
    suspend fun updateDebtEntry(entry: DebtEntry)

    @Delete
    suspend fun deleteDebtEntry(entry: DebtEntry)

    @Query("DELETE FROM debt_entries WHERE id = :entryId")
    suspend fun deleteDebtEntryById(entryId: Int)

    @Query("DELETE FROM debt_entries WHERE accountId = :accountId")
    suspend fun deleteDebtEntriesByAccount(accountId: Int)

    // --- SAVINGS GOALS HISTORY ---
    @Query("SELECT * FROM savings_goal_history WHERE savingsGoalId = :goalId ORDER BY timestamp DESC")
    fun getHistoryForGoal(goalId: Int): Flow<List<SavingsGoalHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoalHistory(history: SavingsGoalHistory): Long

    @Query("DELETE FROM savings_goal_history WHERE savingsGoalId = :goalId")
    suspend fun deleteHistoryByGoal(goalId: Int)

    // --- TRANSACTION ITEMS ---
    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    fun getItemsForTransaction(transactionId: Int): Flow<List<TransactionItem>>

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getItemsForTransactionDirect(transactionId: Int): List<TransactionItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItem(item: TransactionItem): Long

    @Query("DELETE FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun deleteItemsByTransaction(transactionId: Int)
}
