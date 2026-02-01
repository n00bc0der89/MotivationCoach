package com.example.historymotivationcoach.data.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.historymotivationcoach.data.entity.DeliveryHistory
import com.example.historymotivationcoach.data.entity.DeliveryStatus
import com.example.historymotivationcoach.data.entity.MotivationItem

/**
 * Data class that combines a MotivationItem with its delivery history information.
 * Used for displaying history entries with full motivation details.
 */
data class MotivationWithHistory(
    @Embedded val item: MotivationItem,
    @ColumnInfo(name = "shown_at") val shownAt: Long,
    @ColumnInfo(name = "date_key") val dateKey: String,
    @ColumnInfo(name = "delivery_status") val deliveryStatus: DeliveryStatus
)

@Dao
interface HistoryDao {
    
    /**
     * Insert a new delivery history record.
     * Returns the ID of the inserted record.
     */
    @Insert
    suspend fun insert(history: DeliveryHistory): Long
    
    /**
     * Get all delivery history records for a specific date.
     * Results are ordered by delivery time (most recent first).
     */
    @Query("SELECT * FROM delivery_history WHERE date_key = :dateKey ORDER BY shown_at DESC")
    suspend fun getHistoryByDate(dateKey: String): List<DeliveryHistory>
    
    /**
     * Get all unique date keys from delivery history.
     * Results are ordered chronologically (most recent first).
     */
    @Query("SELECT DISTINCT date_key FROM delivery_history ORDER BY date_key DESC")
    suspend fun getAllDateKeys(): List<String>
    
    /**
     * Get motivation items with their delivery history for a specific date.
     * This performs a JOIN between motivation_items and delivery_history tables.
     * Results are ordered by delivery time (most recent first).
     */
    @Transaction
    @Query("""
        SELECT m.*, h.shown_at, h.date_key, h.delivery_status 
        FROM motivation_items m 
        INNER JOIN delivery_history h ON m.id = h.item_id 
        WHERE h.date_key = :dateKey 
        ORDER BY h.shown_at DESC
    """)
    suspend fun getMotivationsWithHistoryByDate(dateKey: String): List<MotivationWithHistory>
    
    /**
     * Clear all delivery history records.
     * Used for the "Replay Classics" feature to reset the seen pool.
     */
    @Query("DELETE FROM delivery_history")
    suspend fun clearAll()
    
    /**
     * Update the delivery status of a specific history entry.
     * Used to track whether a notification was opened or dismissed.
     */
    @Query("UPDATE delivery_history SET delivery_status = :status WHERE historyId = :historyId")
    suspend fun updateDeliveryStatus(historyId: Long, status: DeliveryStatus)
}
