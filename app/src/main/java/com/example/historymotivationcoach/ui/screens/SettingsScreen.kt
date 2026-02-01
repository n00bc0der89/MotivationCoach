package com.example.historymotivationcoach.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.historymotivationcoach.data.entity.ScheduleMode
import com.example.historymotivationcoach.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

/**
 * Settings screen for configuring notification preferences.
 * 
 * Requirements: 1.1, 2.1, 3.4, 13.1, 13.2, 13.3
 * 
 * @param viewModel The SettingsViewModel that manages the settings state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val preferences by viewModel.preferences.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    
    // Refresh notification status when screen is displayed
    LaunchedEffect(Unit) {
        viewModel.refreshNotificationStatus()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        // System notifications warning (if disabled)
        AnimatedVisibility(
            visible = !notificationsEnabled,
            enter = expandVertically(
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300)),
            exit = shrinkVertically(
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "⚠️ Notifications Disabled",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "System notifications are disabled for this app. " +
                              "You won't receive motivational notifications until you enable them in system settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Open System Settings")
                    }
                }
            }
        }
        
        // Notifications enabled toggle
        SettingCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Notifications",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Receive daily motivational notifications",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = preferences.enabled,
                    onCheckedChange = { viewModel.toggleNotifications(it) },
                    modifier = Modifier.semantics {
                        contentDescription = if (preferences.enabled) {
                            "Notifications enabled. Toggle to disable."
                        } else {
                            "Notifications disabled. Toggle to enable."
                        }
                    }
                )
            }
        }
        
        // Notifications per day slider
        SettingCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Notifications per day",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = preferences.notificationsPerDay.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = preferences.notificationsPerDay.toFloat(),
                    onValueChange = { viewModel.updateNotificationsPerDay(it.roundToInt()) },
                    valueRange = 1f..10f,
                    steps = 8,
                    enabled = preferences.enabled,
                    modifier = Modifier.semantics {
                        contentDescription = "Notifications per day: ${preferences.notificationsPerDay}. Slide to adjust between 1 and 10."
                    }
                )
                Text(
                    text = "Choose between 1 and 10 notifications per day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Schedule mode selector
        SettingCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Schedule Mode",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = preferences.scheduleMode == ScheduleMode.TIME_WINDOW,
                        onClick = { viewModel.updateScheduleMode(ScheduleMode.TIME_WINDOW) },
                        label = { Text("Time Window") },
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = if (preferences.scheduleMode == ScheduleMode.TIME_WINDOW) {
                                    "Time Window mode selected"
                                } else {
                                    "Time Window mode. Tap to select."
                                }
                            },
                        enabled = preferences.enabled
                    )
                    FilterChip(
                        selected = preferences.scheduleMode == ScheduleMode.FIXED_TIMES,
                        onClick = { viewModel.updateScheduleMode(ScheduleMode.FIXED_TIMES) },
                        label = { Text("Fixed Times") },
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = if (preferences.scheduleMode == ScheduleMode.FIXED_TIMES) {
                                    "Fixed Times mode selected"
                                } else {
                                    "Fixed Times mode. Tap to select."
                                }
                            },
                        enabled = preferences.enabled
                    )
                }
            }
        }
        
        // Time window settings (only shown in TIME_WINDOW mode)
        AnimatedVisibility(
            visible = preferences.scheduleMode == ScheduleMode.TIME_WINDOW,
            enter = expandVertically(
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300)),
            exit = shrinkVertically(
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        ) {
            SettingCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Time Window",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Start time
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Start Time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = { /* TODO: Show time picker */ },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = preferences.enabled
                            ) {
                                Text(preferences.startTime)
                            }
                        }
                        
                        // End time
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "End Time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = { /* TODO: Show time picker */ },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = preferences.enabled
                            ) {
                                Text(preferences.endTime)
                            }
                        }
                    }
                    
                    Text(
                        text = "Notifications will be evenly distributed between these times",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Fixed times settings (only shown in FIXED_TIMES mode)
        AnimatedVisibility(
            visible = preferences.scheduleMode == ScheduleMode.FIXED_TIMES,
            enter = expandVertically(
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300)),
            exit = shrinkVertically(
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        ) {
            SettingCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Fixed Times",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    if (preferences.fixedTimes.isEmpty()) {
                        Text(
                            text = "No fixed times configured. Add times below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        preferences.fixedTimes.take(preferences.notificationsPerDay).forEach { time ->
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = time,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    TextButton(
                                        onClick = { /* TODO: Remove time */ },
                                        enabled = preferences.enabled
                                    ) {
                                        Text("Remove")
                                    }
                                }
                            }
                        }
                    }
                    
                    OutlinedButton(
                        onClick = { /* TODO: Add time */ },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = preferences.enabled
                    ) {
                        Text("Add Time")
                    }
                }
            }
        }
        
        Divider()
        
        // Clear history button
        SettingCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Clear History",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Remove all delivery history. This will allow you to see all motivations again (Replay Classics).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { showClearHistoryDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear History")
                }
            }
        }
    }
    
    // Clear history confirmation dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear History?") },
            text = {
                Text("This will remove all delivery history and allow you to see all motivations again. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearHistoryDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}
