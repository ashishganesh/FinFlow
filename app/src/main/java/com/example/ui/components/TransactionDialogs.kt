package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.Account
import com.example.data.Transaction
import com.example.data.TransactionItem
import androidx.compose.foundation.horizontalScroll
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionDialog(
    transaction: Transaction? = null,
    initialItems: List<TransactionItem> = emptyList(),
    initialType: String = "EXPENSE",
    categories: List<String>,
    currencySymbol: String = "$",
    accounts: List<Account> = emptyList(),
    activeAccountId: Int = -1,
    onDismiss: () -> Unit,
    onSubmit: (
        amount: Double,
        title: String,
        category: String,
        type: String, // "EXPENSE", "INCOME", "TRANSFER"
        remarks: String,
        timestamp: Long,
        isRecurring: Boolean,
        recurringInterval: String?,
        paymentMode: String,
        imagePath: String?,
        transferDestAccountId: Int?,
        items: List<TransactionItem>
    ) -> Unit
) {
    val context = LocalContext.current
    
    // Type selector state: "EXPENSE", "INCOME", "TRANSFER"
    var transactionType by remember {
        mutableStateOf(
            if (transaction != null) {
                transaction.type
            } else {
                initialType
            }
        )
    }

    var amountStr by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var title by remember { mutableStateOf(transaction?.title ?: "") }
    var remarks by remember { mutableStateOf(transaction?.remarks ?: "") }

    var selectedCategory by remember { mutableStateOf(transaction?.category ?: "Food") }
    var isCustomCategory by remember { mutableStateOf(transaction != null && !categories.contains(transaction.category)) }
    var customCategoryText by remember { mutableStateOf(if (isCustomCategory) selectedCategory else "") }

    var selectedTimestamp by remember { mutableStateOf(transaction?.timestamp ?: System.currentTimeMillis()) }

    var isRecurring by remember { mutableStateOf(transaction?.isRecurring ?: false) }
    var recurringInterval by remember { mutableStateOf(transaction?.recurringInterval ?: "Monthly") }
    
    // Payment Mode: "Cash", "UPI", "Bank", "Card"
    var paymentMode by remember { mutableStateOf(transaction?.paymentMode ?: "Cash") }
    
    // Simulated receipt image path/existence
    var imagePath by remember { mutableStateOf(transaction?.imagePath) }

    var isItemized by remember(initialItems) { mutableStateOf(initialItems.isNotEmpty()) }
    var itemizedItems by remember(initialItems) { mutableStateOf(initialItems) }

    // Item builder state
    var newItemName by remember { mutableStateOf("") }
    var newItemQuantity by remember { mutableStateOf("") }
    var newItemPrice by remember { mutableStateOf("") }
    var newItemUnitType by remember { mutableStateOf("pcs") }
    var newItemNote by remember { mutableStateOf("") }
    
    // Transfer destination account
    var selectedDestAccountId by remember {
        mutableStateOf(
            accounts.firstOrNull { it.id != activeAccountId }?.id
        )
    }

    val dateFormater = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())

    val calendar = remember(selectedTimestamp) {
        Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
    }

    // Receipt Visual Preview Mode State
    var showReceiptPreview by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (transaction == null) "New Record" else "Modify Record",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 3-way Type Toggle: Expense, Income, Transfer
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Transaction Type",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("EXPENSE" to "Expense", "INCOME" to "Income", "TRANSFER" to "Transfer").forEach { (typeKey, label) ->
                            val isSelected = transactionType == typeKey
                            val containerCol = when (typeKey) {
                                "EXPENSE" -> if (isSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant
                                "INCOME" -> if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                else -> if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                            }
                            val contentCol = when (typeKey) {
                                "EXPENSE" -> if (isSelected) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant
                                "INCOME" -> if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                else -> if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Button(
                                onClick = { 
                                    transactionType = typeKey
                                    if (typeKey == "TRANSFER") {
                                        selectedCategory = "Transfer"
                                    } else if (selectedCategory == "Transfer") {
                                        selectedCategory = "Food"
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = containerCol,
                                    contentColor = contentCol
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // If TRANSFER type, render target destination account selection
                if (transactionType == "TRANSFER") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Destination Account",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val otherAccounts = accounts.filter { it.id != activeAccountId }
                        if (otherAccounts.isEmpty()) {
                            Text(
                                "⚠️ Create of at least two accounts required to make a transfer.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        } else {
                            var expandedDestDropdown by remember { mutableStateOf(false) }
                            val activeDestAccount = otherAccounts.find { it.id == selectedDestAccountId } ?: otherAccounts.first()
                            
                            ExposedDropdownMenuBox(
                                expanded = expandedDestDropdown,
                                onExpandedChange = { expandedDestDropdown = it }
                            ) {
                                OutlinedTextField(
                                    value = activeDestAccount.name,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Transfer To Profile") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDestDropdown) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedDestDropdown,
                                    onDismissRequest = { expandedDestDropdown = false }
                                ) {
                                    otherAccounts.forEach { acc ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Box(modifier = Modifier.size(8.dp).background(Color(acc.color), CircleShape))
                                                    Text(acc.name)
                                                }
                                            },
                                            onClick = {
                                                selectedDestAccountId = acc.id
                                                expandedDestDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (transactionType == "EXPENSE") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("Itemized Expense Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("Add multiple products & services", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = isItemized,
                                onCheckedChange = { isItemized = it }
                            )
                        }
                    }
                }

                val calculatedTotal = if (isItemized && transactionType == "EXPENSE") {
                    val builderQuantity = newItemQuantity.toDoubleOrNull() ?: 0.0
                    val builderPrice = newItemPrice.toDoubleOrNull() ?: 0.0
                    val builderTotal = if (newItemName.isNotBlank()) builderQuantity * builderPrice else 0.0
                    itemizedItems.sumOf { it.quantity * it.pricePerUnit } + builderTotal
                } else {
                    0.0
                }

                if (isItemized && transactionType == "EXPENSE") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total Amount (Calculated)", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = "$currencySymbol${String.format("%.2f", calculatedTotal)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            AssistChip(
                                onClick = {},
                                label = { Text("${itemizedItems.size + if (newItemName.isNotBlank() && newItemQuantity.toDoubleOrNull() != null && newItemPrice.toDoubleOrNull() != null) 1 else 0} Items") },
                                leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                } else {
                    // Amount Text Field
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.toDoubleOrNull() != null || input.count { it == '.' } <= 1) {
                                amountStr = input
                            }
                        },
                        label = { Text("Amount ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isItemized && transactionType == "EXPENSE") {
                    if (itemizedItems.isNotEmpty()) {
                        Text(
                            "Items list",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        itemizedItems.forEachIndexed { index, item ->
                            val itemTotal = item.quantity * item.pricePerUnit
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${item.quantity} ${item.unitType} × $currencySymbol${String.format("%.2f", item.pricePerUnit)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (item.note.isNotBlank()) {
                                                Text(
                                                    "• ${item.note}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        "$currencySymbol${String.format("%.2f", itemTotal)}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { itemizedItems = itemizedItems.filterIndexed { idx, _ -> idx != index } },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Item Builder / Input Form
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Add Item details",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = newItemName,
                                onValueChange = { newItemName = it },
                                label = { Text("Product/Service Name", style = MaterialTheme.typography.bodySmall) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newItemQuantity,
                                    onValueChange = { input ->
                                        if (input.isEmpty() || input.toDoubleOrNull() != null) {
                                            newItemQuantity = input
                                        }
                                    },
                                    label = { Text("Qty", style = MaterialTheme.typography.bodySmall) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = newItemPrice,
                                    onValueChange = { input ->
                                        if (input.isEmpty() || input.toDoubleOrNull() != null) {
                                            newItemPrice = input
                                        }
                                    },
                                    label = { Text("Price Unit", style = MaterialTheme.typography.bodySmall) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1.5f)
                                )
                            }

                            // Unit Type selector (as modern Horizontal Chips scrollable row!)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Unit Type",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val unitTypes = listOf("pcs", "kg", "litre", "packet", "box", "g", "ml", "bottle")
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    unitTypes.forEach { uType ->
                                        val isSel = newItemUnitType == uType
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { newItemUnitType = uType },
                                            label = { Text(uType) }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = newItemNote,
                                onValueChange = { newItemNote = it },
                                label = { Text("Optional Item Note", style = MaterialTheme.typography.bodySmall) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Add Item Button
                            Button(
                                onClick = {
                                    if (newItemName.isNotBlank() && newItemQuantity.toDoubleOrNull() != null && newItemPrice.toDoubleOrNull() != null) {
                                        val qty = newItemQuantity.toDoubleOrNull() ?: 1.0
                                        val price = newItemPrice.toDoubleOrNull() ?: 0.0
                                        val newItem = TransactionItem(
                                            transactionId = transaction?.id ?: 0,
                                            name = newItemName.trim(),
                                            quantity = qty,
                                            pricePerUnit = price,
                                            unitType = newItemUnitType,
                                            note = newItemNote.trim()
                                        )
                                        itemizedItems = itemizedItems + newItem
                                        // Reset fields
                                        newItemName = ""
                                        newItemQuantity = ""
                                        newItemPrice = ""
                                        newItemUnitType = "pcs"
                                        newItemNote = ""
                                    } else {
                                        Toast.makeText(context, "Please complete Item Name, Qty, and Price fields", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Another Item", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                // Title Text Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (transactionType == "TRANSFER") "Transfer Description" else "Title / Purpose") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selection (only shown for non-Transfers)
                if (transactionType != "TRANSFER") {
                    Column {
                        Text(
                            "Category Selection",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { isCustomCategory = !isCustomCategory },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCustomCategory) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (isCustomCategory) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Text(if (isCustomCategory) "Standard Categories" else "Create Custom")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isCustomCategory) {
                            OutlinedTextField(
                                value = customCategoryText,
                                onValueChange = { customCategoryText = it },
                                label = { Text("Custom Category name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedCategory,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select Category") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    categories.filter { it != "Transfer" }.forEach { category ->
                                        DropdownMenuItem(
                                            text = { Text(category) },
                                            onClick = {
                                                selectedCategory = category
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Payment Mode Selection segment
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Payment Mode",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Cash", "UPI", "Bank", "Card").forEach { mode ->
                            val isSelected = paymentMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { paymentMode = mode },
                                label = { Text(mode, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Date and Time selection buttons (Tap to change)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date button
                    Card(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    calendar.set(Calendar.YEAR, year)
                                    calendar.set(Calendar.MONTH, month)
                                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    selectedTimestamp = calendar.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Date")
                            Column {
                                Text("Date", style = MaterialTheme.typography.labelSmall)
                                Text(dateFormater.format(selectedTimestamp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Time button
                    Card(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                    calendar.set(Calendar.MINUTE, minute)
                                    selectedTimestamp = calendar.timeInMillis
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                false
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = "Time")
                            Column {
                                Text("Time", style = MaterialTheme.typography.labelSmall)
                                Text(timeFormatter.format(selectedTimestamp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Remarks Text Field
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks / Notes") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional Receipt Image Attachment simulator
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (imagePath != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (imagePath != null) MaterialTheme.colorScheme.primary else Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = if (imagePath != null) Icons.Default.ReceiptLong else Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Receipt",
                                    tint = if (imagePath != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (imagePath != null) "Receipt Attachment Attached" else "No Receipt Attached",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (imagePath != null) {
                                IconButton(onClick = { imagePath = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove receipt attachment", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        if (imagePath != null) {
                            Button(
                                onClick = { showReceiptPreview = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Preview Thermal Receipt")
                            }
                        } else {
                            Button(
                                onClick = { 
                                    imagePath = "receipt_" + System.currentTimeMillis() + ".png"
                                    Toast.makeText(context, "MOCK: Receipt successfully scanned & attached!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Cam")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simulate Receipt Scan")
                            }
                        }
                    }
                }

                // Optional Recurrence segment (only for non-Transfers)
                if (transactionType != "TRANSFER") {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Set as Recurring Transaction",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = isRecurring,
                                onCheckedChange = { isRecurring = it }
                            )
                        }

                        if (isRecurring) {
                            Spacer(modifier = Modifier.height(8.dp))
                            var expandedInterval by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expandedInterval,
                                onExpandedChange = { expandedInterval = it }
                            ) {
                                OutlinedTextField(
                                    value = recurringInterval,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Recurrence Interval") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedInterval) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedInterval,
                                    onDismissRequest = { expandedInterval = false }
                                ) {
                                    listOf("Daily", "Weekly", "Monthly", "Yearly").forEach { interval ->
                                        DropdownMenuItem(
                                            text = { Text(interval) },
                                            onClick = {
                                                recurringInterval = interval
                                                expandedInterval = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions Card Segment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Cancel", color = MaterialTheme.colorScheme.error)
                    }

                    val submitEnabled = (if (isItemized && transactionType == "EXPENSE") {
                        itemizedItems.isNotEmpty() || newItemName.isNotBlank()
                    } else {
                        amountStr.toDoubleOrNull() != null
                    }) && title.isNotBlank() && (transactionType != "TRANSFER" || selectedDestAccountId != null)

                    Button(
                        onClick = {
                            val finalItems = if (isItemized && transactionType == "EXPENSE") {
                                if (newItemName.isNotBlank() && newItemQuantity.toDoubleOrNull() != null && newItemPrice.toDoubleOrNull() != null) {
                                    itemizedItems + TransactionItem(
                                        transactionId = transaction?.id ?: 0,
                                        name = newItemName.trim(),
                                        quantity = newItemQuantity.toDoubleOrNull() ?: 1.0,
                                        pricePerUnit = newItemPrice.toDoubleOrNull() ?: 0.0,
                                        unitType = newItemUnitType,
                                        note = newItemNote.trim()
                                    )
                                } else {
                                    itemizedItems
                                }
                            } else {
                                emptyList()
                            }

                            val amountParsed = if (isItemized && transactionType == "EXPENSE") {
                                finalItems.sumOf { it.quantity * it.pricePerUnit }
                            } else {
                                amountStr.toDoubleOrNull() ?: 0.0
                            }

                            val finalCategory = if (transactionType == "TRANSFER") {
                                "Transfer"
                            } else if (isCustomCategory) {
                                if (customCategoryText.isNotBlank()) customCategoryText.trim() else "Other"
                            } else {
                                selectedCategory
                            }

                            onSubmit(
                                amountParsed,
                                title.trim(),
                                finalCategory,
                                transactionType,
                                remarks.trim(),
                                selectedTimestamp,
                                isRecurring,
                                if (isRecurring) recurringInterval else null,
                                paymentMode,
                                imagePath,
                                if (transactionType == "TRANSFER") selectedDestAccountId else null,
                                finalItems
                            )
                        },
                        enabled = submitEnabled
                    ) {
                        Text(if (transaction == null) "Create" else "Save")
                    }
                }
            }
        }
    }

    // Gorgeous custom Stylized Thermal checkout receipt dialog
    if (showReceiptPreview) {
        Dialog(onDismissRequest = { showReceiptPreview = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Text(
                        "★★★★ RECEIPT ★★★★",
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "LOCAL OFFLINE ENGINE",
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "--------------------------------",
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray
                    )

                    // Title & Time Details
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("DESC:", fontFamily = FontFamily.Monospace, color = Color.Black, fontWeight = FontWeight.Bold)
                        Text(if (title.isBlank()) "Untitled Record" else title, fontFamily = FontFamily.Monospace, color = Color.Black, maxLines = 1)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("DATE:", fontFamily = FontFamily.Monospace, color = Color.Black, fontWeight = FontWeight.Bold)
                        Text(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(selectedTimestamp)), fontFamily = FontFamily.Monospace, color = Color.Black)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TYPE:", fontFamily = FontFamily.Monospace, color = Color.Black, fontWeight = FontWeight.Bold)
                        Text(transactionType, fontFamily = FontFamily.Monospace, color = if (transactionType == "EXPENSE") Color.Red else Color.Green)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("MODE:", fontFamily = FontFamily.Monospace, color = Color.Black, fontWeight = FontWeight.Bold)
                        Text(paymentMode, fontFamily = FontFamily.Monospace, color = Color.Black)
                    }

                    if (isItemized && transactionType == "EXPENSE" && itemizedItems.isNotEmpty()) {
                        Text(
                            "ITEMS LIST:",
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        itemizedItems.forEach { item ->
                            val itemTotal = item.quantity * item.pricePerUnit
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${item.name} (${String.format(Locale.getDefault(), "%.1f", item.quantity)} ${item.unitType})",
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Black,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "$currencySymbol${String.format(Locale.getDefault(), "%.2f", itemTotal)}",
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Black
                                )
                            }
                        }
                        Text(
                            "--------------------------------",
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray
                        )
                    }

                    // Financial calculations details
                    val amountParsed = if (isItemized && transactionType == "EXPENSE") {
                        itemizedItems.sumOf { it.quantity * it.pricePerUnit }
                    } else {
                        amountStr.toDoubleOrNull() ?: 0.0
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SUBTOTAL:", fontFamily = FontFamily.Monospace, color = Color.Black, style = MaterialTheme.typography.labelLarge)
                        Text("$currencySymbol${String.format(Locale.getDefault(), "%.2f", amountParsed)}", fontFamily = FontFamily.Monospace, color = Color.Black)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TAX (0%):", fontFamily = FontFamily.Monospace, color = Color.Black, style = MaterialTheme.typography.labelLarge)
                        Text("${currencySymbol}0.00", fontFamily = FontFamily.Monospace, color = Color.Black)
                    }
                    
                    Text(
                        "================================",
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TOTAL PAID:", fontFamily = FontFamily.Monospace, color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "$currencySymbol${String.format("%.2f", amountParsed)}",
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    Text(
                        "================================",
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black
                    )

                    // Footer notes
                    Text(
                        "Remarks: " + if (remarks.isBlank()) "None" else remarks,
                        fontFamily = FontFamily.Monospace,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showReceiptPreview = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CLOSE RECEIPT", fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
