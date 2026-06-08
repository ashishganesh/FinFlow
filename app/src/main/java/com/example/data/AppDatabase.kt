package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `debt_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `accountId` INTEGER NOT NULL, `type` TEXT NOT NULL, `personName` TEXT NOT NULL, `amount` REAL NOT NULL, `entryDate` INTEGER NOT NULL, `dueDate` INTEGER NOT NULL, `notes` TEXT NOT NULL, `phoneNumber` TEXT, `status` TEXT NOT NULL, `reminderDate` INTEGER, `reminderNotes` TEXT, `repeatReminder` INTEGER NOT NULL)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `savings_goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `accountId` INTEGER NOT NULL, `name` TEXT NOT NULL, `targetAmount` REAL NOT NULL, `currentAmount` REAL NOT NULL, `dueDate` TEXT NOT NULL)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `budgets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `accountId` INTEGER NOT NULL, `category` TEXT NOT NULL, `amount` REAL NOT NULL, `monthYear` TEXT NOT NULL)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Safe empty boundary block for v4 to v5 adjustments if any
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `savings_goal_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `savingsGoalId` INTEGER NOT NULL, `action` TEXT NOT NULL, `amount` REAL NOT NULL, `timestamp` INTEGER NOT NULL, `note` TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `transaction_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `transactionId` INTEGER NOT NULL, `name` TEXT NOT NULL, `quantity` REAL NOT NULL, `pricePerUnit` REAL NOT NULL, `unitType` TEXT NOT NULL, `note` TEXT NOT NULL)")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `accounts` ADD COLUMN `cashBalance` REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE `accounts` ADD COLUMN `bankBalance` REAL NOT NULL DEFAULT 0.0")
        db.execSQL("UPDATE `accounts` SET `cashBalance` = COALESCE((SELECT SUM(`amount`) FROM `transactions` WHERE `transactions`.`accountId` = `accounts`.`id` AND `transactions`.`type` = 'INCOME'), 0.0) - COALESCE((SELECT SUM(`amount`) FROM `transactions` WHERE `transactions`.`accountId` = `accounts`.`id` AND `transactions`.`type` = 'EXPENSE'), 0.0)")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `debt_payments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `debtEntryId` INTEGER NOT NULL, `amount` REAL NOT NULL, `timestamp` INTEGER NOT NULL, `paymentMethod` TEXT NOT NULL, `notes` TEXT NOT NULL)")
    }
}

@Database(
    entities = [Account::class, Transaction::class, Budget::class, SavingsGoal::class, DebtEntry::class, SavingsGoalHistory::class, TransactionItem::class, DebtPayment::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
