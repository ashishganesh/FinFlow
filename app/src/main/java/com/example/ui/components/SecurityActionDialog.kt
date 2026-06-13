package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Account

enum class SecurityActionMode {
    VERIFY_DELETE,
    VERIFY_DISABLE,
    VERIFY_EXPORT,
    CHANGE_PIN
}

enum class ChangePinStep {
    VERIFY_CURRENT,
    ENTER_NEW,
    CONFIRM_NEW
}

@Composable
fun SecurityActionDialog(
    targetAccount: Account,
    lockoutSecondsRemaining: Int,
    mode: SecurityActionMode,
    onVerifySuccess: (String?) -> Unit, // Callback when authentication passes. For CHANGE_PIN, returns the new verified PIN.
    onVerifyFailed: () -> Unit, // Callback on failure (triggers failed attempts)
    onCancel: () -> Unit
) {
    var currentStep by remember { mutableStateOf(if (mode == SecurityActionMode.CHANGE_PIN) ChangePinStep.VERIFY_CURRENT else null) }
    var pinText by remember { mutableStateOf("") }
    var firstNewPinText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val accountColor = try {
        Color(targetAccount.color)
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    // Lockout countdown handling
    LaunchedEffect(lockoutSecondsRemaining) {
        if (lockoutSecondsRemaining > 0) {
            errorMessage = "Too many incorrect PIN attempts. Please try again in $lockoutSecondsRemaining seconds."
            pinText = ""
        } else if (errorMessage?.startsWith("Too many incorrect PIN attempts") == true) {
            errorMessage = null
        }
    }

    val titleText = when (mode) {
        SecurityActionMode.VERIFY_DELETE -> "Verify Current PIN for Deletion"
        SecurityActionMode.VERIFY_DISABLE -> "Verify PIN to Disable Protection"
        SecurityActionMode.VERIFY_EXPORT -> "Verify PIN for Backup Export"
        SecurityActionMode.CHANGE_PIN -> {
            when (currentStep) {
                ChangePinStep.VERIFY_CURRENT -> "Change PIN: Verify Current"
                ChangePinStep.ENTER_NEW -> "Change PIN: Enter New PIN"
                ChangePinStep.CONFIRM_NEW -> "Change PIN: Confirm New PIN"
                null -> "Security Authentication"
            }
        }
    }

    val subtitleText = when (mode) {
        SecurityActionMode.VERIFY_DELETE -> "Please verify your PIN before removing \"${targetAccount.name}\"."
        SecurityActionMode.VERIFY_DISABLE -> "Enter the PIN of \"${targetAccount.name}\" before disabling lock screen."
        SecurityActionMode.VERIFY_EXPORT -> "Enter PIN to authorize backup statement cloning/sharing."
        SecurityActionMode.CHANGE_PIN -> {
            when (currentStep) {
                ChangePinStep.VERIFY_CURRENT -> "Enter current PIN for \"${targetAccount.name}\""
                ChangePinStep.ENTER_NEW -> "Enter your new 4-digit secure code"
                ChangePinStep.CONFIRM_NEW -> "Re-type your new 4-digit code to confirm"
                null -> ""
            }
        }
    }

    Dialog(
        onDismissRequest = { onCancel() },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon Header
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(accountColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (mode == SecurityActionMode.VERIFY_DELETE) Icons.Default.Lock else Icons.Default.Security,
                        contentDescription = "Shield Guard",
                        tint = accountColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                // PIN dots indicators
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { idx ->
                        val active = idx < pinText.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    color = if (active) accountColor else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Error State Display
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                }

                // Keypad Layout
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val rowKeys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("Cancel", "0", "Backspace")
                    )

                    rowKeys.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (key == "Cancel" || key == "Backspace") Color.Transparent
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                        .clickable(enabled = lockoutSecondsRemaining <= 0 || key == "Cancel") {
                                            if (lockoutSecondsRemaining <= 0) {
                                                errorMessage = null
                                            }
                                            when (key) {
                                                "Cancel" -> onCancel()
                                                "Backspace" -> {
                                                    if (pinText.isNotEmpty()) {
                                                        pinText = pinText.dropLast(1)
                                                    }
                                                }
                                                else -> {
                                                    if (pinText.length < 4) {
                                                        pinText += key
                                                        
                                                        // Complete code typed
                                                        if (pinText.length == 4) {
                                                            val currentPinInput = pinText
                                                            
                                                            if (mode != SecurityActionMode.CHANGE_PIN) {
                                                                // Simple Verification Action
                                                                if (currentPinInput == targetAccount.pin) {
                                                                    onVerifySuccess(null)
                                                                } else {
                                                                    pinText = ""
                                                                    errorMessage = "Incorrect PIN code. Authorization denied."
                                                                    onVerifyFailed()
                                                                }
                                                            } else {
                                                                // Change PIN flow state machine
                                                                when (currentStep) {
                                                                    ChangePinStep.VERIFY_CURRENT -> {
                                                                        if (currentPinInput == targetAccount.pin) {
                                                                            currentStep = ChangePinStep.ENTER_NEW
                                                                            pinText = ""
                                                                            errorMessage = null
                                                                        } else {
                                                                            pinText = ""
                                                                            errorMessage = "Incorrect current PIN. Authorization denied."
                                                                            onVerifyFailed()
                                                                        }
                                                                    }
                                                                    ChangePinStep.ENTER_NEW -> {
                                                                        firstNewPinText = currentPinInput
                                                                        currentStep = ChangePinStep.CONFIRM_NEW
                                                                        pinText = ""
                                                                        errorMessage = null
                                                                    }
                                                                    ChangePinStep.CONFIRM_NEW -> {
                                                                        if (currentPinInput == firstNewPinText) {
                                                                            // Match! Save!
                                                                            onVerifySuccess(currentPinInput)
                                                                        } else {
                                                                            // Mis-match! Clear back to Enter New state
                                                                            currentStep = ChangePinStep.ENTER_NEW
                                                                            pinText = ""
                                                                            firstNewPinText = ""
                                                                            errorMessage = "Passcodes do not match. Please enter new PIN again."
                                                                        }
                                                                    }
                                                                    null -> {}
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (key) {
                                        "Backspace" -> Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        "Cancel" -> Text(
                                            text = "Cancel",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        else -> Text(
                                            text = key,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
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
