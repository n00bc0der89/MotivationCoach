package com.example.historymotivationcoach.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.historymotivationcoach.data.dao.MotivationWithHistory
import com.example.historymotivationcoach.data.repository.MotivationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * ViewModel for the History screen.
 * Manages the state of the delivery history grouped by date.
 * 
 * Requirements: 7.1, 7.2, 7.3, 8.1, 8.2
 */
class HistoryViewModel(
    private val motivationRepository: MotivationRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    init {
        loadHistory()
    }
    
    /**
     * Load the complete delivery history grouped by date.
     * Updates the UI state based on the results.
     * 
     * Requirements: 7.1, 7.2, 7.3, 8.1, 8.2
     */
    fun loadHistory() {
        viewModelScope.launch {
            try {
                _uiState.value = HistoryUiState.Loading
                
                val dateKeys = motivationRepository.getAllDateKeys()
                
                if (dateKeys.isEmpty()) {
                    _uiState.value = HistoryUiState.Empty
                    return@launch
                }
                
                val groupedHistory = dateKeys.map { dateKey ->
                    val label = formatDateLabel(dateKey)
                    val items = motivationRepository.getHistoryByDate(dateKey)
                    HistoryGroup(label, dateKey, items)
                }
                
                _uiState.value = HistoryUiState.Success(groupedHistory)
            } catch (e: Exception) {
                // Provide user-friendly error message
                val message = when (e) {
                    is com.example.historymotivationcoach.data.repository.DatabaseException -> 
                        "Unable to load history. Please try again."
                    else -> "An unexpected error occurred. Please restart the app."
                }
                _uiState.value = HistoryUiState.Error(message)
            }
        }
    }
    
    /**
     * Format a date key into a human-readable label.
     * Returns "Today" for today's date, "Yesterday" for yesterday's date,
     * and a formatted date string for older dates.
     * 
     * Requirements: 8.2
     * 
     * @param dateKey The date key in YYYY-MM-DD format
     * @return A formatted label string
     */
    private fun formatDateLabel(dateKey: String): String {
        val today = getCurrentDateKey()
        val yesterday = getYesterdayDateKey()
        
        return when (dateKey) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> {
                // Format as "Month Day, Year" (e.g., "January 15, 2024")
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val outputFormat = SimpleDateFormat("MMMM d, yyyy", Locale.US)
                    val date = inputFormat.parse(dateKey)
                    date?.let { outputFormat.format(it) } ?: dateKey
                } catch (e: Exception) {
                    dateKey // Fallback to raw date key if parsing fails
                }
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
    
    /**
     * Get yesterday's date key in YYYY-MM-DD format.
     */
    private fun getYesterdayDateKey(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return format.format(calendar.time)
    }
}

/**
 * UI state for the History screen.
 * Represents the different states the history screen can be in.
 */
sealed class HistoryUiState {
    /**
     * Loading state - data is being fetched.
     */
    object Loading : HistoryUiState()
    
    /**
     * Success state - data loaded successfully.
     * 
     * @param groups The list of history groups, each containing a date label and items
     */
    data class Success(val groups: List<HistoryGroup>) : HistoryUiState()
    
    /**
     * Empty state - no history records exist.
     */
    object Empty : HistoryUiState()
    
    /**
     * Error state - an error occurred while loading data.
     * 
     * @param message The error message to display
     */
    data class Error(val message: String) : HistoryUiState()
}

/**
 * Represents a group of history items for a specific date.
 * 
 * @param label The human-readable date label (e.g., "Today", "Yesterday", "January 15, 2024")
 * @param dateKey The date key in YYYY-MM-DD format
 * @param items The list of motivation items delivered on this date
 */
data class HistoryGroup(
    val label: String,
    val dateKey: String,
    val items: List<MotivationWithHistory>
)
