package com.example.historymotivationcoach.data.repository

import android.util.Log
import com.example.historymotivationcoach.data.dao.HistoryDao
import com.example.historymotivationcoach.data.dao.MotivationDao
import com.example.historymotivationcoach.data.dao.MotivationWithHistory
import com.example.historymotivationcoach.data.entity.DeliveryHistory
import com.example.historymotivationcoach.data.entity.DeliveryStatus
import com.example.historymotivationcoach.data.entity.MotivationItem
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository for managing motivation items and delivery history.
 * Provides a clean API for content selection, delivery recording, and history queries.
 * 
 * All database operations are wrapped in try-catch blocks for error handling.
 * Requirements: 22.3
 */
open class MotivationRepository(
    private val motivationDao: MotivationDao,
    private val historyDao: HistoryDao
) {
    // Mutex to ensure atomic select-and-deliver operations
    private val selectionLock = kotlinx.coroutines.sync.Mutex()
    
    companion object {
        private const val TAG = "MotivationRepository"
    }
    
    /**
     * Select a random unseen motivation item.
     * If preferred themes are provided, attempts to select from themed items first.
     * Falls back to all unseen items if no themed items are available.
     * 
     * @param preferredThemes List of theme strings to prefer in selection
     * @return A random unseen MotivationItem, or null if all content is exhausted
     * @throws DatabaseException if database operation fails
     */
    open suspend fun selectRandomUnseen(preferredThemes: List<String>): MotivationItem? {
        return try {
            val unseenItems = motivationDao.getUnseenItems()
            
            if (preferredThemes.isEmpty() || unseenItems.isEmpty()) {
                return unseenItems.randomOrNull()
            }
            
            // Filter items that match any of the preferred themes
            val themed = unseenItems.filter { item ->
                item.themes.any { theme -> preferredThemes.contains(theme) }
            }
            
            // Return themed item if available, otherwise any unseen item
            if (themed.isNotEmpty()) themed.random() else unseenItems.randomOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Error selecting random unseen item", e)
            throw DatabaseException("Failed to select motivation item", e)
        }
    }
    
    /**
     * Atomically select and deliver a motivation item.
     * This method ensures that selection and delivery happen atomically,
     * preventing race conditions in concurrent scenarios.
     * 
     * @param preferredThemes List of theme strings to prefer in selection
     * @param notificationId The notification ID for this delivery
     * @return Pair of (MotivationItem, historyId) if successful, null if content exhausted
     * @throws DatabaseException if database operation fails
     */
    suspend fun selectAndDeliver(
        preferredThemes: List<String>,
        notificationId: Int
    ): Pair<MotivationItem, Long>? {
        return try {
            selectionLock.withLock {
                val item = selectRandomUnseen(preferredThemes) ?: return null
                val historyId = recordDelivery(item.id, notificationId)
                Pair(item, historyId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in selectAndDeliver", e)
            throw DatabaseException("Failed to select and deliver motivation", e)
        }
    }
    
    /**
     * Record the delivery of a motivation item.
     * Creates a delivery history entry with current timestamp and date key.
     * 
     * @param itemId The ID of the motivation item that was delivered
     * @param notificationId The notification ID used for this delivery
     * @return The ID of the inserted history record
     * @throws DatabaseException if database operation fails
     */
    suspend fun recordDelivery(itemId: Long, notificationId: Int): Long {
        return try {
            val now = System.currentTimeMillis()
            val dateKey = formatDateKey(now)
            
            val history = DeliveryHistory(
                itemId = itemId,
                shownAt = now,
                dateKey = dateKey,
                notificationId = notificationId,
                deliveryStatus = DeliveryStatus.DELIVERED
            )
            
            historyDao.insert(history)
        } catch (e: Exception) {
            Log.e(TAG, "Error recording delivery for item $itemId", e)
            throw DatabaseException("Failed to record delivery", e)
        }
    }
    
    /**
     * Get the count of unseen motivation items.
     * 
     * @return The number of items that have not been delivered yet
     * @throws DatabaseException if database operation fails
     */
    open suspend fun getUnseenCount(): Int {
        return try {
            motivationDao.getUnseenCount()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unseen count", e)
            throw DatabaseException("Failed to get unseen count", e)
        }
    }
    
    /**
     * Get all motivations delivered on a specific date with their history information.
     * 
     * @param dateKey The date in YYYY-MM-DD format
     * @return List of MotivationWithHistory for the specified date
     * @throws DatabaseException if database operation fails
     */
    suspend fun getHistoryByDate(dateKey: String): List<MotivationWithHistory> {
        return try {
            historyDao.getMotivationsWithHistoryByDate(dateKey)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting history for date $dateKey", e)
            throw DatabaseException("Failed to load history", e)
        }
    }
    
    /**
     * Get all unique date keys from delivery history.
     * Results are ordered chronologically (most recent first).
     * 
     * @return List of date keys in YYYY-MM-DD format
     * @throws DatabaseException if database operation fails
     */
    suspend fun getAllDateKeys(): List<String> {
        return try {
            historyDao.getAllDateKeys()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all date keys", e)
            throw DatabaseException("Failed to load history dates", e)
        }
    }
    
    /**
     * Clear all delivery history.
     * Used for the "Replay Classics" feature to reset the seen pool.
     * 
     * @throws DatabaseException if database operation fails
     */
    suspend fun clearHistory() {
        try {
            historyDao.clearAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing history", e)
            throw DatabaseException("Failed to clear history", e)
        }
    }
    
    /**
     * Format a timestamp into a date key string.
     * 
     * @param timestamp Unix timestamp in milliseconds
     * @return Date string in YYYY-MM-DD format
     */
    private fun formatDateKey(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return format.format(date)
    }
}

/**
 * Exception thrown when database operations fail.
 * Provides user-friendly error messages.
 */
class DatabaseException(message: String, cause: Throwable? = null) : Exception(message, cause)
