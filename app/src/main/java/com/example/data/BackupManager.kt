package com.example.data

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    /**
     * Converts a list of transactions into a standard CSV format string.
     */
    fun exportTransactionsToCsv(
        transactions: List<Transaction>,
        accountMap: Map<Int, String>
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val csv = StringBuilder()
        // Header
        csv.append("Transaction ID,Account Name,Amount,Type,Title,Category,Date Time,Remarks,Recurring,Interval\n")

        for (tx in transactions) {
            val accountName = accountMap[tx.accountId] ?: "Unknown Account"
            val formattedDate = dateFormat.format(Date(tx.timestamp))
            // Escape values containing commas or quotes
            val titleEscaped = tx.title.replace("\"", "\"\"")
            val remarksEscaped = tx.remarks.replace("\"", "\"\"")
            val categoryEscaped = tx.category.replace("\"", "\"\"")

            csv.append("${tx.id},")
                .append("\"$accountName\",")
                .append("${tx.amount},")
                .append("${tx.type},")
                .append("\"$titleEscaped\",")
                .append("\"$categoryEscaped\",")
                .append("\"$formattedDate\",")
                .append("\"$remarksEscaped\",")
                .append("${tx.isRecurring},")
                .append("${tx.recurringInterval ?: "N/A"}\n")
        }
        return csv.toString()
    }

    /**
     * Exports the entire database state (Accounts, Transactions, Budgets) to a single JSON string.
     */
    fun createFullBackupJson(
        accounts: List<Account>,
        transactions: List<Transaction>,
        budgets: List<Budget>
    ): String {
        val root = JSONObject()

        // Accounts Array
        val accountsArray = JSONArray()
        for (acc in accounts) {
            val accObj = JSONObject().apply {
                put("id", acc.id)
                put("name", acc.name)
                put("pin", acc.pin ?: JSONObject.NULL)
                put("color", acc.color)
                put("currency", acc.currency)
                put("avatar", acc.avatar)
                put("themePreference", acc.themePreference)
                put("cashBalance", acc.cashBalance)
                put("bankBalance", acc.bankBalance)
            }
            accountsArray.put(accObj)
        }
        root.put("accounts", accountsArray)

        // Transactions Array
        val txsArray = JSONArray()
        for (tx in transactions) {
            val txObj = JSONObject().apply {
                put("id", tx.id)
                put("accountId", tx.accountId)
                put("amount", tx.amount)
                put("title", tx.title)
                put("timestamp", tx.timestamp)
                put("remarks", tx.remarks)
                put("type", tx.type)
                put("category", tx.category)
                put("isRecurring", tx.isRecurring)
                put("recurringInterval", tx.recurringInterval ?: JSONObject.NULL)
            }
            txsArray.put(txObj)
        }
        root.put("transactions", txsArray)

        // Budgets Array
        val budgetsArray = JSONArray()
        for (b in budgets) {
            val bObj = JSONObject().apply {
                put("id", b.id)
                put("accountId", b.accountId)
                put("category", b.category)
                put("amount", b.amount)
                put("monthYear", b.monthYear)
            }
            budgetsArray.put(bObj)
        }
        root.put("budgets", budgetsArray)

        return root.toString(4) // Beautifully indented JSON
    }

    /**
     * Parse a full restore JSON and return the parsed components.
     * Returns null if parsing fails or structure is invalid.
     */
    fun parseFullBackupJson(jsonString: String): FullBackupContent? {
        return try {
            val root = JSONObject(jsonString)

            val accounts = mutableListOf<Account>()
            val transactions = mutableListOf<Transaction>()
            val budgets = mutableListOf<Budget>()

            // Parse Accounts
            if (root.has("accounts")) {
                val array = root.getJSONArray("accounts")
                for (i in 0 until array.length()) {
                    val accObj = array.getJSONObject(i)
                    accounts.add(
                        Account(
                            id = accObj.optInt("id", 0),
                            name = accObj.getString("name"),
                            pin = if (accObj.isNull("pin")) null else accObj.getString("pin"),
                            color = accObj.optInt("color", 0xFF6200EE.toInt()),
                            currency = accObj.optString("currency", "USD"),
                            avatar = accObj.optString("avatar", "Personal"),
                            themePreference = accObj.optString("themePreference", "Dark Purple"),
                            cashBalance = accObj.optDouble("cashBalance", 0.0),
                            bankBalance = accObj.optDouble("bankBalance", 0.0)
                        )
                    )
                }
            }

            // Parse Transactions
            if (root.has("transactions")) {
                val array = root.getJSONArray("transactions")
                for (i in 0 until array.length()) {
                    val txObj = array.getJSONObject(i)
                    transactions.add(
                        Transaction(
                            id = txObj.optInt("id", 0),
                            accountId = txObj.getInt("accountId"),
                            amount = txObj.getDouble("amount"),
                            title = txObj.getString("title"),
                            timestamp = txObj.getLong("timestamp"),
                            remarks = txObj.optString("remarks", ""),
                            type = txObj.getString("type"),
                            category = txObj.getString("category"),
                            isRecurring = txObj.optBoolean("isRecurring", false),
                            recurringInterval = if (txObj.isNull("recurringInterval")) null else txObj.getString("recurringInterval")
                        )
                    )
                }
            }

            // Parse Budgets
            if (root.has("budgets")) {
                val array = root.getJSONArray("budgets")
                for (i in 0 until array.length()) {
                    val bObj = array.getJSONObject(i)
                    budgets.add(
                        Budget(
                            id = bObj.optInt("id", 0),
                            accountId = bObj.getInt("accountId"),
                            category = bObj.getString("category"),
                            amount = bObj.getDouble("amount"),
                            monthYear = bObj.getString("monthYear")
                        )
                    )
                }
            }

            FullBackupContent(accounts, transactions, budgets)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    data class FullBackupContent(
        val accounts: List<Account>,
        val transactions: List<Transaction>,
        val budgets: List<Budget>
    )
}
