package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val pin: String? = null, // Optional PIN lock. Null means unlocked.
    val color: Int = 0xFF6200EE.toInt(), // RGB Color representation
    val currency: String = "USD", // "USD", "EUR", "GBP", "INR", "JPY", etc.
    val avatar: String = "Personal", // "Personal", "Business", "Family", "Savings", "Travel"
    val themePreference: String = "Dark Purple",
    val cashBalance: Double = 0.0,
    val bankBalance: Double = 0.0
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val accountId: Int,
    val amount: Double,
    val title: String,
    val timestamp: Long, // milliseconds
    val remarks: String = "",
    val type: String, // "EXPENSE" or "INCOME"
    val category: String, // Predefined or custom category
    val isRecurring: Boolean = false,
    val recurringInterval: String? = null, // "Daily", "Weekly", "Monthly", or null
    val paymentMode: String = "Cash", // "Cash", "UPI", "Bank", "Card"
    val imagePath: String? = null // Optional receipt image reference
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val accountId: Int,
    val category: String,
    val amount: Double,
    val monthYear: String // "YYYY-MM" format, e.g. "2026-05"
)

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val accountId: Int,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val dueDate: String
)

@Entity(tableName = "savings_goal_history")
data class SavingsGoalHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val savingsGoalId: Int,
    val action: String, // "Added" or "Withdrawn"
    val amount: Double,
    val timestamp: Long,
    val note: String = ""
)

@Entity(tableName = "transaction_items")
data class TransactionItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val transactionId: Int,
    val name: String,
    val quantity: Double,
    val pricePerUnit: Double,
    val unitType: String = "pcs", // "pcs", "kg", "litre", "packet", "box", etc.
    val note: String = ""
)

@Entity(tableName = "debt_entries")
data class DebtEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val accountId: Int,
    val type: String, // "LENT" or "BORROWED"
    val personName: String,
    val amount: Double,
    val entryDate: Long, // timestamp
    val dueDate: Long, // timestamp
    val notes: String = "",
    val phoneNumber: String? = null,
    val status: String, // "ACTIVE", "DUE_SOON", "OVERDUE", "RECOVERED", "REPAID"
    val reminderDate: Long? = null,
    val reminderNotes: String? = null,
    val repeatReminder: Boolean = false
)

@Entity(tableName = "debt_payments")
data class DebtPayment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val debtEntryId: Int,
    val amount: Double,
    val timestamp: Long,
    val paymentMethod: String, // "Cash", "Bank Account", "UPI", "Card"
    val notes: String = ""
)

