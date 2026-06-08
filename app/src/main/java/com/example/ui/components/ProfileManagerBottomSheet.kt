package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.theme.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.Account
import com.example.ui.ExpenseViewModel
import java.util.Locale

@Composable
fun getAvatarIcon(avatar: String): ImageVector {
    return when (avatar.lowercase()) {
        "personal" -> Icons.Default.Person
        "business" -> Icons.Default.Business
        "family" -> Icons.Default.People
        "savings" -> Icons.Default.AccountBalance
        "travel" -> Icons.Default.Explore
        else -> Icons.Default.AccountBalanceWallet
    }
}

val ProfileColorList = listOf(
    0xFF0D9488.toInt(), // Modern Teal
    0xFFCD9BFF.toInt(), // Pastel Purple
    0xFFE11D48.toInt(), // Rose Pink
    0xFFF59E0B.toInt(), // Amber Orange
    0xFF6366F1.toInt(), // Indigo Blue
    0xFF06B6D4.toInt(), // Cyan
    0xFF84CC16.toInt()  // Lime Green
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileManagerBottomSheet(
    viewModel: ExpenseViewModel,
    accountsList: List<Account>,
    activeAccount: Account?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Dialog layout
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Bottom Sheet Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                                imageVector = Icons.Default.ManageAccounts,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Profile Spaces",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // 1. Current Active Profile Info Card
                if (activeAccount != null) {
                    Text(
                        text = "Active Space",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.5.dp, Color(activeAccount.color))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(Color(activeAccount.color), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getAvatarIcon(activeAccount.avatar),
                                        contentDescription = activeAccount.avatar,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = activeAccount.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Currency: ${activeAccount.currency}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f), CircleShape)
                                        )
                                        Text(
                                            text = activeAccount.themePreference,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }

                            // PIN Lock indicator flag
                            if (activeAccount.pin != null) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "PIN Locked",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = "PIN Unlocked",
                                    tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Select / Switch Profile Section
                Text(
                    text = "Switch Space Profile",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (accountsList.size <= 1) {
                    Text(
                        text = "No secondary spaces configured.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(accountsList) { acc ->
                            val isCurrentSelected = activeAccount?.id == acc.id
                            Card(
                                modifier = Modifier
                                    .width(135.dp)
                                    .clickable {
                                        if (!isCurrentSelected) {
                                            viewModel.selectAccount(acc)
                                            onDismiss() // Automatically close on switch
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrentSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    if (isCurrentSelected) 2.dp else 1.dp,
                                    if (isCurrentSelected) Color(acc.color) else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color(acc.color), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getAvatarIcon(acc.avatar),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Text(
                                        text = acc.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrentSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // 3. Centralised Action Operations Row
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ADD NEW PROFILE BUTTON
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.AddHome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add New Profile", fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // EDIT PROFILE BUTTON
                        Button(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Profile")
                        }

                        // DELETE PROFILE BUTTON (except default/only remaining profile)
                        if (accountsList.size > 1 && activeAccount != null) {
                            Button(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG: DELETE CONFIRMER ---
    if (showDeleteConfirm && activeAccount != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Workspace Profile?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you absolutely sure you want to delete \"${activeAccount.name}\"? " +
                            "This action will permanently delete all associated ledger transactions, budgets, " +
                            "savings goals, and cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount(activeAccount)
                        showDeleteConfirm = false
                        Toast.makeText(context, "Workspace successfully removed", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // --- DIALOG: ADD NEW PROFILE ---
    if (showAddDialog) {
        ProfileConfigDialog(
            titleLabel = "Instantiate Profile Space",
            onDismiss = { showAddDialog = false },
            onSubmit = { name, currency, pin, colorVal, avatar, theme, cash, bank ->
                viewModel.addAccount(
                    name = name,
                    pin = pin,
                    color = colorVal,
                    currency = currency,
                    avatar = avatar,
                    themePreference = theme,
                    openingCashBalance = cash,
                    openingBankBalance = bank
                )
                showAddDialog = false
                Toast.makeText(context, "Welcome to $name Workspace!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // --- DIALOG: EDIT ACTIVE PROFILE ---
    if (showEditDialog && activeAccount != null) {
        ProfileConfigDialog(
            titleLabel = "Configure Workspace Settings",
            existingAccount = activeAccount,
            onDismiss = { showEditDialog = false },
            onSubmit = { name, currency, pin, colorVal, avatar, theme, _, _ ->
                viewModel.updateAccountSettings(
                    account = activeAccount,
                    newName = name,
                    newPin = pin,
                    newColor = colorVal,
                    newCurrency = currency,
                    newAvatar = avatar,
                    newThemePreference = theme
                )
                showEditDialog = false
                Toast.makeText(context, "Workspace updated successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * A beautiful, integrated configuration modal supporting setup, edit, avatars, PIN settings, themed settings, and opening balances.
 */
@Composable
fun ProfileConfigDialog(
    titleLabel: String,
    existingAccount: Account? = null,
    onDismiss: () -> Unit,
    onSubmit: (name: String, currency: String, pin: String?, colorVal: Int, avatar: String, theme: String, cashBalance: Double, bankBalance: Double) -> Unit
) {
    var name by remember { mutableStateOf(existingAccount?.name ?: "") }
    var currency by remember { mutableStateOf(existingAccount?.currency ?: "$") }
    var avatar by remember { mutableStateOf(existingAccount?.avatar ?: "Personal") }
    var selectedColor by remember { mutableStateOf(existingAccount?.color ?: ProfileColorList.first()) }
    var themePreference by remember { mutableStateOf(existingAccount?.themePreference ?: "Dark Purple") }
    
    // Dual Opening Balances
    var openingCashStr by remember { mutableStateOf("") }
    var openingBankStr by remember { mutableStateOf("") }

    // PIN configurations
    var pinEnabled by remember { mutableStateOf(existingAccount?.pin != null) }
    var pinCode by remember { mutableStateOf(existingAccount?.pin ?: "") }
    var confirmPinCode by remember { mutableStateOf(existingAccount?.pin ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val avatars = listOf("Personal", "Business", "Family", "Savings", "Travel")
    val currencies = listOf("$", "€", "£", "₹", "¥")
    val themes = listOf("Dark Purple", "Cozy Teal", "Solar Light")

    // Dynamic balance calculations
    val liveCash = openingCashStr.toDoubleOrNull() ?: 0.0
    val liveBank = openingBankStr.toDoubleOrNull() ?: 0.0
    val liveTotal = liveCash + liveBank

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = titleLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Profile Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name") },
                    placeholder = { Text("e.g. Personal Account") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true
                )

                // Opening Balance Fields (Only shown during profile creation)
                if (existingAccount == null) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Opening Balances",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        OutlinedTextField(
                            value = openingCashStr,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.toDoubleOrNull() != null) {
                                    openingCashStr = input
                                }
                            },
                            label = { Text("Opening Cash Balance") },
                            placeholder = { Text("e.g. 5000") },
                            leadingIcon = { Text("💵", modifier = Modifier.padding(start = 8.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = openingBankStr,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.toDoubleOrNull() != null) {
                                    openingBankStr = input
                                }
                            },
                            label = { Text("Opening Bank / Online Balance") },
                            placeholder = { Text("e.g. 12000") },
                            leadingIcon = { Text("🏦", modifier = Modifier.padding(start = 8.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true
                        )

                        // Real-time live total calculation card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Calculated Total Balance",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Cash + Bank Balance",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "$currency${String.format(Locale.getDefault(), "%,.2f", liveTotal)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Currency selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Currency", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(currencies) { curr ->
                            val isSelected = currency == curr
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { currency = curr },
                                contentAlignment = Alignment.Center
                              ) {
                                Text(
                                    text = curr,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Avatar / Icon selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Profile Avatar/Icon", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(avatars) { avOption ->
                            val isSelected = avatar.lowercase() == avOption.lowercase()
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { avatar = avOption }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getAvatarIcon(avOption),
                                        contentDescription = avOption,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = avOption,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Color palette
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Palette Accenting", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ProfileColorList) { colVal ->
                            val isSelected = selectedColor == colVal
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(colVal))
                                    .clickable { selectedColor = colVal }
                                    .border(
                                        if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) 
                                        else BorderStroke(0.dp, Color.Transparent),
                                        CircleShape
                                    )
                            )
                        }
                    }
                }

                // Theme selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Theme Preference", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        themes.forEach { tPref ->
                            val isSelected = themePreference == tPref
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { themePreference = tPref },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tPref,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // PIN Protection Settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("PIN Code Protection", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text("Require lock pincode to access", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = pinEnabled,
                        onCheckedChange = {
                            pinEnabled = it
                            if (!it) {
                                pinCode = ""
                                confirmPinCode = ""
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }

                AnimatedVisibility(visible = pinEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = pinCode,
                                onValueChange = { if (it.length <= 4) pinCode = it.filter { digit -> digit.isDigit() } },
                                label = { Text("Set 4-Digit PIN") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = confirmPinCode,
                                onValueChange = { if (it.length <= 4) confirmPinCode = it.filter { digit -> digit.isDigit() } },
                                label = { Text("Confirm PIN") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true
                            )
                        }
                    }
                }

                // Verification / validation warning panel
                AnimatedVisibility(visible = errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                // Submit / Confirm Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Please supply a valid workspace nickname."
                                return@Button
                            }
                            if (pinEnabled) {
                                if (pinCode.length != 4) {
                                    errorMessage = "PIN code must be exactly 4 digits."
                                    return@Button
                                }
                                if (pinCode != confirmPinCode) {
                                    errorMessage = "PIN verification does not match. Re-check entry."
                                    return@Button
                                }
                            }
                            
                            val liveCashVal = openingCashStr.toDoubleOrNull() ?: 0.0
                            val liveBankVal = openingBankStr.toDoubleOrNull() ?: 0.0
                            
                            if (liveCashVal < 0.0 || liveBankVal < 0.0) {
                                errorMessage = "Opening balances cannot be negative values."
                                return@Button
                            }

                            onSubmit(
                                name,
                                currency,
                                if (pinEnabled) pinCode else null,
                                selectedColor,
                                avatar,
                                themePreference,
                                liveCashVal,
                                liveBankVal
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirm", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
