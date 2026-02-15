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
import com.example.historymotivationcoach.viewmodel.ValidationState
import java.time.DayOfWeek
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
    val validationState by viewModel.validationState.collectAsState()
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Schedule Mode",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Choose when to receive notifications",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // All Days option
                FilterChip(
                    selected = preferences.scheduleMode == ScheduleMode.ALL_DAYS,
                    onClick = { viewModel.updateScheduleMode(ScheduleMode.ALL_DAYS) },
                    label = { Text("All Days") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = if (preferences.scheduleMode == ScheduleMode.ALL_DAYS) {
                                "All Days mode selected"
                            } else {
                                "All Days mode. Tap to select."
                            }
                        },
                    enabled = preferences.enabled
                )
                
                // Weekdays Only option
                FilterChip(
                    selected = preferences.scheduleMode == ScheduleMode.WEEKDAYS_ONLY,
                    onClick = { viewModel.updateScheduleMode(ScheduleMode.WEEKDAYS_ONLY) },
                    label = { Text("Weekdays Only (Mon-Fri)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = if (preferences.scheduleMode == ScheduleMode.WEEKDAYS_ONLY) {
                                "Weekdays Only mode selected"
                            } else {
                                "Weekdays Only mode. Tap to select."
                            }
                        },
                    enabled = preferences.enabled
                )
                
                // Weekends Only option
                FilterChip(
                    selected = preferences.scheduleMode == ScheduleMode.WEEKENDS_ONLY,
                    onClick = { viewModel.updateScheduleMode(ScheduleMode.WEEKENDS_ONLY) },
                    label = { Text("Weekends Only (Sat-Sun)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = if (preferences.scheduleMode == ScheduleMode.WEEKENDS_ONLY) {
                                "Weekends Only mode selected"
                            } else {
                                "Weekends Only mode. Tap to select."
                            }
                        },
                    enabled = preferences.enabled
                )
                
                // Custom Days option
                FilterChip(
                    selected = preferences.scheduleMode == ScheduleMode.CUSTOM_DAYS,
                    onClick = { viewModel.updateScheduleMode(ScheduleMode.CUSTOM_DAYS) },
                    label = { Text("Custom Days") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = if (preferences.scheduleMode == ScheduleMode.CUSTOM_DAYS) {
                                "Custom Days mode selected"
                            } else {
                                "Custom Days mode. Tap to select."
                            }
                        },
                    enabled = preferences.enabled
                )
                
                // Custom days selection (shown only when CUSTOM_DAYS is selected)
                AnimatedVisibility(
                    visible = preferences.scheduleMode == ScheduleMode.CUSTOM_DAYS,
                    enter = expandVertically(
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300)),
                    exit = shrinkVertically(
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                ) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Select Days",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Day checkboxes
                        DayOfWeek.values().forEach { day ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = day.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Checkbox(
                                    checked = preferences.customDays.contains(day),
                                    onCheckedChange = { checked ->
                                        val newDays = if (checked) {
                                            preferences.customDays + day
                                        } else {
                                            preferences.customDays - day
                                        }
                                        viewModel.updateCustomDays(newDays)
                                    },
                                    enabled = preferences.enabled,
                                    modifier = Modifier.semantics {
                                        contentDescription = if (preferences.customDays.contains(day)) {
                                            "${day.name} selected"
                                        } else {
                                            "${day.name} not selected"
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Time window settings (shown for all modes in v2)
        AnimatedVisibility(
            visible = true,
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
                    
                    Text(
                        text = "Set the time range for notifications",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                onClick = { showStartTimePicker = true },
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
                                onClick = { showEndTimePicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = preferences.enabled
                            ) {
                                Text(preferences.endTime)
                            }
                        }
                    }
                    
                    // Validation error messages
                    when (val state = validationState) {
                        is ValidationState.Invalid -> {
                            state.errors.forEach { error ->
                                Text(
                                    text = "⚠️ $error",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                        ValidationState.Valid -> {
                            Text(
                                text = "Notifications will be evenly distributed between these times",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
    
    // Start time picker dialog
    if (showStartTimePicker) {
        TimePickerDialog(
            currentTime = preferences.startTime,
            onTimeSelected = { time ->
                viewModel.updateTimeWindow(time, preferences.endTime)
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false }
        )
    }
    
    // End time picker dialog
    if (showEndTimePicker) {
        TimePickerDialog(
            currentTime = preferences.endTime,
            onTimeSelected = { time ->
                viewModel.updateTimeWindow(preferences.startTime, time)
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
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

/**
 * Time picker dialog for selecting notification times.
 * Uses Material3 TimePicker component.
 * 
 * Requirements: 5.1, 8.3
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    currentTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Parse current time
    val timeParts = currentTime.split(":")
    val initialHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 9
    val initialMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
    
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val hour = timePickerState.hour.toString().padStart(2, '0')
                    val minute = timePickerState.minute.toString().padStart(2, '0')
                    onTimeSelected("$hour:$minute")
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            TimePicker(
                state = timePickerState,
                modifier = Modifier.padding(16.dp)
            )
        }
    )
}
