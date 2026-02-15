package com.example.historymotivationcoach.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.historymotivationcoach.business.ContentSelector
import com.example.historymotivationcoach.data.dao.MotivationWithHistory
import com.example.historymotivationcoach.data.repository.MotivationRepository
import com.example.historymotivationcoach.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel for the Home screen.
 * Manages the state of the latest motivation display and today's statistics.
 * 
 * Requirements: 12.1, 12.2, 12.3, 12.4, 3.2, 3.4, 3.5
 */
class HomeViewModel(
    private val motivationRepository: MotivationRepository,
    private val preferencesRepository: PreferencesRepository,
    private val notificationScheduler: com.example.historymotivationcoach.business.NotificationScheduler
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val _manualTriggerState = MutableStateFlow<ManualTriggerState>(ManualTriggerState.Idle)
    val manualTriggerState: StateFlow<ManualTriggerState> = _manualTriggerState.asStateFlow()
    
    private val contentSelector = ContentSelector(motivationRepository, preferencesRepository)
    
    init {
        loadLatestMotivation()
    }
    
    /**
     * Load the latest motivation and today's statistics.
     * Updates the UI state based on the results.
     * 
     * Requirements: 12.1, 12.2, 12.3
     */
    fun loadLatestMotivation() {
        viewModelScope.launch {
            try {
                _uiState.value = HomeUiState.Loading
                
                val today = getCurrentDateKey()
                val todayHistory = motivationRepository.getHistoryByDate(today)
                val unseenCount = motivationRepository.getUnseenCount()
                
                // Check for content exhaustion
                if (unseenCount == 0 && todayHistory.isEmpty()) {
                    _uiState.value = HomeUiState.ContentExhausted
                } else if (todayHistory.isEmpty()) {
                    _uiState.value = HomeUiState.Empty(unseenCount)
                } else {
                    _uiState.value = HomeUiState.Success(
                        latestMotivation = todayHistory.first(),
                        todayCount = todayHistory.size,
                        unseenCount = unseenCount
                    )
                }
            } catch (e: Exception) {
                // Provide user-friendly error message
                val message = when (e) {
                    is com.example.historymotivationcoach.data.repository.DatabaseException -> 
                        "Unable to load motivations. Please try again."
                    else -> "An unexpected error occurred. Please restart the app."
                }
                _uiState.value = HomeUiState.Error(message)
            }
        }
    }
    
    /**
     * Trigger a manual notification immediately.
     * This allows users to request a motivation on-demand.
     * 
     * This method:
     * 1. Checks if content is exhausted
     * 2. Triggers the notification via NotificationScheduler
     * 3. Updates the manual trigger state
     * 4. Refreshes the UI to show the new motivation
     * 
     * Requirements: 3.2, 3.4, 3.5
     */
    fun triggerManualNotification() {
        viewModelScope.launch {
            try {
                // Show loading state
                _manualTriggerState.value = ManualTriggerState.Loading
                
                // Trigger manual notification via scheduler
                val motivationId = notificationScheduler.triggerManualNotification()
                
                if (motivationId != null) {
                    // Success - update state and refresh
                    _manualTriggerState.value = ManualTriggerState.Success(motivationId)
                    refreshLatestMotivation()
                } else {
                    // Failed - likely content exhausted
                    _manualTriggerState.value = ManualTriggerState.Error("No unseen motivations available")
                }
            } catch (e: Exception) {
                // Provide user-friendly error message
                val message = when (e) {
                    is com.example.historymotivationcoach.data.repository.DatabaseException -> 
                        "Unable to deliver motivation. Please try again."
                    else -> "An unexpected error occurred. Please try again."
                }
                _manualTriggerState.value = ManualTriggerState.Error(message)
            } finally {
                // Reset to idle after a short delay
                viewModelScope.launch {
                    kotlinx.coroutines.delay(2000)
                    _manualTriggerState.value = ManualTriggerState.Idle
                }
            }
        }
    }
    
    /**
     * Refresh the latest motivation display.
     * Called after manual notification to update the UI.
     * 
     * Requirements: 3.5
     */
    fun refreshLatestMotivation() {
        loadLatestMotivation()
    }
    
    /**
     * Get the current date key in YYYY-MM-DD format.
     */
    private fun getCurrentDateKey(): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return format.format(Date())
    }
}

/**
 * UI state for the Home screen.
 * Represents the different states the home screen can be in.
 */
sealed class HomeUiState {
    /**
     * Loading state - data is being fetched.
     */
    object Loading : HomeUiState()
    
    /**
     * Success state - data loaded successfully.
     * 
     * @param latestMotivation The most recent motivation delivered today
     * @param todayCount The number of motivations received today
     * @param unseenCount The number of unseen motivations remaining
     */
    data class Success(
        val latestMotivation: MotivationWithHistory,
        val todayCount: Int,
        val unseenCount: Int
    ) : HomeUiState()
    
    /**
     * Empty state - no motivations received today.
     * 
     * @param unseenCount The number of unseen motivations remaining
     */
    data class Empty(val unseenCount: Int) : HomeUiState()
    
    /**
     * Content exhausted state - all content has been seen.
     * User should be offered the "Replay Classics" option.
     * 
     * Requirements: 4.4, 5.1, 5.2
     */
    object ContentExhausted : HomeUiState()
    
    /**
     * Error state - an error occurred while loading data.
     * 
     * @param message The error message to display
     */
    data class Error(val message: String) : HomeUiState()
}

/**
 * State for manual notification triggering.
 * Tracks the progress and result of manual notification requests.
 * 
 * Requirements: 3.2, 3.4, 3.5
 */
sealed class ManualTriggerState {
    /**
     * Idle state - no manual trigger in progress.
     */
    object Idle : ManualTriggerState()
    
    /**
     * Loading state - manual notification is being triggered.
     */
    object Loading : ManualTriggerState()
    
    /**
     * Success state - manual notification was delivered successfully.
     * 
     * @param motivationId The ID of the delivered motivation
     */
    data class Success(val motivationId: Long) : ManualTriggerState()
    
    /**
     * Error state - manual notification failed.
     * 
     * @param message The error message to display
     */
    data class Error(val message: String) : ManualTriggerState()
}
