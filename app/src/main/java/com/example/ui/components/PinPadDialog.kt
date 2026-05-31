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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Account

@Composable
fun PinPadDialog(
    targetAccount: Account,
    lockoutSecondsRemaining: Int,
    onVerify: (String) -> Boolean,
    onCancel: () -> Unit
) {
    var pinText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lockoutSecondsRemaining) {
        if (lockoutSecondsRemaining > 0) {
            errorMessage = "Too many failed attempts. Locked for $lockoutSecondsRemaining seconds."
        } else if (errorMessage?.startsWith("Too many failed attempts") == true) {
            errorMessage = null
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
                // Header Lock icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            Color(targetAccount.color).copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secured Account",
                        tint = Color(targetAccount.color),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Profile Secured",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Please enter the PIN for ${targetAccount.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
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
                                    color = if (active) Color(targetAccount.color) else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Status message
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Dynamic Pin Board (Grid UI of Keys)
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
                                                        // Automatically verify once 4 digits entered
                                                        if (pinText.length == 4) {
                                                            val success = onVerify(pinText)
                                                            if (!success) {
                                                                // Clear and prompt
                                                                pinText = ""
                                                                errorMessage = "Incorrect PIN code. Authorization denied."
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
