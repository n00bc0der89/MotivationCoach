package com.example.historymotivationcoach.viewmodel

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.historymotivationcoach.business.NotificationScheduler
import com.example.historymotivationcoach.data.entity.ScheduleMode
import com.example.historymotivationcoach.data.entity.UserPreferences
import com.example.historymotivationcoach.data.repository.MotivationRepository
import com.example.historymotivationcoach.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * ViewModel for the Settings screen.
 * Manages user preferences and triggers notification rescheduling when settings change.
 * 
 * Requirements: 1.1, 1.3, 2.1, 2.4, 3.4, 5.3, 13.2, 13.4
 */
class SettingsViewModel(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val motivationRepository: MotivationRepository,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {
    
    /**
     * Expose user preferences as a StateFlow.
     * The Flow automatically updates when preferences change in the database.
     * 
     * Requirements: 13.4
     */
    val preferences: StateFlow<UserPreferences> = preferencesRepository
        .getPreferencesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )
    
    /**
     * State flow for notification permission status.
     * 
     * Requirements: 3.4
     */
    private val _notificationsEnabled = MutableStateFlow(areNotificationsEnabled())
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()
    
    /**
     * State flow for validation errors.
     * 
     * Requirements: 8.6
     */
    private val _validationState = MutableStateFlow<ValidationState>(ValidationState.Valid)
    val validationState: StateFlow<ValidationState> = _validationState.asStateFlow()
    
    /**
     * Check if system notifications are enabled for the app.
     * 
     * Requirements: 3.4
     */
    fun areNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.areNotificationsEnabled()
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
    
    /**
     * Refresh the notification permission status.
     * Should be called when returning from system settings.
     * 
     * Requirements: 3.4
     */
    fun refreshNotificationStatus() {
        _notificationsEnabled.value = areNotificationsEnabled()
    }
    
    /**
     * Update the number of notifications per day.
     * Validates that the count is within the valid range (1-10).
     * Triggers notification rescheduling after updating.
     * 
     * Requirements: 1.1, 1.3
     * 
     * @param count The desired number of notifications per day
     */
    fun updateNotificationsPerDay(count: Int) {
        viewModelScope.launch {
            try {
                val current = preferences.value
                val validatedCount = count.coerceIn(1, 10)
                val updated = current.copy(notificationsPerDay = validatedCount)
                preferencesRepository.updatePreferences(updated)
                notificationScheduler.rescheduleAllNotifications()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating notifications per day", e)
                // In production, this would update an error state to show to user
            }
        }
    }
    
    /**
     * Update the schedule mode (Time Window or Fixed Times).
     * Triggers notification rescheduling after updating.
     * 
     * Requirements: 2.1, 2.4, 4.2
     * 
     * @param mode The new schedule mode
     */
    fun updateScheduleMode(mode: ScheduleMode) {
        viewModelScope.launch {
            try {
                val current = preferences.value
                val updated = current.copy(scheduleMode = mode)
                preferencesRepository.updatePreferences(updated)
                notificationScheduler.rescheduleAllNotifications()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating schedule mode", e)
            }
        }
    }
    
    /**
     * Update custom days selection.
     * Only applicable when schedule mode is CUSTOM_DAYS.
     * Triggers notification rescheduling after updating.
     * 
     * Requirements: 4.2
     * 
     * @param days Set of selected days of the week
     */
    fun updateCustomDays(days: Set<DayOfWeek>) {
        viewModelScope.launch {
            try {
                val current = preferences.value
                val updated = current.copy(customDays = days)
                preferencesRepository.updatePreferences(updated)
                notificationScheduler.rescheduleAllNotifications()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating custom days", e)
            }
        }
    }
    
    /**
     * Update the time window for notifications.
     * Validates the time window before updating.
     * Triggers notification rescheduling after updating.
     * 
     * Requirements: 2.1, 2.4, 5.2, 5.3, 5.4, 8.6
     * 
     * @param startTime The start time in HH:mm format
     * @param endTime The end time in HH:mm format
     */
    fun updateTimeWindow(startTime: String, endTime: String) {
        viewModelScope.launch {
            try {
                val current = preferences.value
                
                // Validate time window
                val validationResult = validateTimeWindow(
                    startTime,
                    endTime,
                    current.notificationsPerDay
                )
                
                if (!validationResult.isValid) {
                    _validationState.value = ValidationState.Invalid(validationResult.errors)
                    return@launch
                }
                
                _validationState.value = ValidationState.Valid
                val updated = current.copy(startTime = startTime, endTime = endTime)
                preferencesRepository.updatePreferences(updated)
                notificationScheduler.rescheduleAllNotifications()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating time window", e)
                _validationState.value = ValidationState.Invalid(
                    listOf("Failed to update time window: ${e.message}")
                )
            }
        }
    }
    
    /**
     * Validates time window configuration.
     * Checks that start time is before end time and that the window
     * is wide enough for the requested number of notifications.
     * 
     * Requirements: 5.2, 5.3, 8.6
     * 
     * @param startTime The start time in HH:mm format
     * @param endTime The end time in HH:mm format
     * @param notificationsPerDay The number of notifications per day
     * @return ValidationResult with isValid flag and error messages
     */
    fun validateTimeWindow(
        startTime: String,
        endTime: String,
        notificationsPerDay: Int
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        try {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val start = LocalTime.parse(startTime, formatter)
            val end = LocalTime.parse(endTime, formatter)
            
            // Check that start time is before end time
            if (start >= end) {
                errors.add("Start time must be before end time")
            }
            
            // Check if time window is wide enough
            val totalMinutes = ChronoUnit.MINUTES.between(start, end)
            val minIntervalMinutes = 30
            val requiredMinutes = notificationsPerDay * minIntervalMinutes
            
            if (totalMinutes < requiredMinutes) {
                errors.add(
                    "Time window is too narrow for $notificationsPerDay notifications. " +
                    "Need at least ${requiredMinutes / 60} hours and ${requiredMinutes % 60} minutes."
                )
            }
        } catch (e: Exception) {
            errors.add("Invalid time format. Use HH:mm format (e.g., 09:00)")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
    
    /**
     * Toggle notifications on or off.
     * When disabled, cancels all scheduled notifications.
     * When enabled, schedules notifications according to current preferences.
     * 
     * Requirements: 13.2
     * 
     * @param enabled Whether notifications should be enabled
     */
    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val current = preferences.value
                val updated = current.copy(enabled = enabled)
                preferencesRepository.updatePreferences(updated)
                
                if (enabled) {
                    notificationScheduler.scheduleNextNotification()
                } else {
                    notificationScheduler.cancelAllNotifications()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling notifications", e)
            }
        }
    }
    
    /**
     * Update preferred themes for content selection.
     * Themes are used to bias content selection toward user preferences.
     * 
     * Requirements: 13.4
     * 
     * @param themes List of preferred theme names
     */
    fun updatePreferredThemes(themes: List<String>) {
        viewModelScope.launch {
            try {
                val current = preferences.value
                val updated = current.copy(preferredThemes = themes)
                preferencesRepository.updatePreferences(updated)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating preferred themes", e)
            }
        }
    }
    
    /**
     * Clear all delivery history.
     * This implements the "Replay Classics" feature, allowing users to see
     * all content again from the beginning.
     * 
     * Requirements: 5.3
     */
    fun clearHistory() {
        viewModelScope.launch {
            try {
                motivationRepository.clearHistory()
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing history", e)
            }
        }
    }
    
    companion object {
        private const val TAG = "SettingsViewModel"
    }
}

/**
 * Represents the validation state of settings inputs.
 * 
 * Requirements: 8.6
 */
sealed class ValidationState {
    object Valid : ValidationState()
    data class Invalid(val errors: List<String>) : ValidationState()
}

/**
 * Result of time window validation.
 * 
 * Requirements: 5.2, 5.3, 8.6
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
)
