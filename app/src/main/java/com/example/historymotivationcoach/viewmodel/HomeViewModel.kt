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
import kotlin.random.Random

/**
 * ViewModel for the Home screen.
 * Manages the state of the latest motivation display and today's statistics.
 * 
 * Requirements: 12.1, 12.2, 12.3, 12.4
 */
class HomeViewModel(
    private val motivationRepository: MotivationRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
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
     * 2. Selects a random unseen motivation
     * 3. Records it in delivery history
     * 4. Reloads the UI to show the new motivation
     * 
     * Requirements: 12.4
     */
    fun triggerManualNotification() {
        viewModelScope.launch {
            try {
                // Show loading state
                _uiState.value = HomeUiState.Loading
                
                // Check if content is exhausted
                if (contentSelector.isContentExhausted()) {
                    _uiState.value = HomeUiState.ContentExhausted
                    return@launch
                }
                
                // Select next motivation
                val motivation = contentSelector.selectNextMotivation()
                if (motivation == null) {
                    _uiState.value = HomeUiState.ContentExhausted
                    return@launch
                }
                
                // Record delivery with a random notification ID for manual triggers
                val notificationId = Random.nextInt(10000, 99999)
                motivationRepository.recordDelivery(motivation.id, notificationId)
                
                // Reload to show the new motivation
                loadLatestMotivation()
            } catch (e: Exception) {
                // Provide user-friendly error message
                val message = when (e) {
                    is com.example.historymotivationcoach.data.repository.DatabaseException -> 
                        "Unable to deliver motivation. Please try again."
                    else -> "An unexpected error occurred. Please try again."
                }
                _uiState.value = HomeUiState.Error(message)
            }
        }
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
