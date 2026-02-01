package com.example.historymotivationcoach.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.historymotivationcoach.data.entity.MotivationItem

@Dao
interface MotivationDao {
    
    /**
     * Get all motivation items that have not been delivered yet.
     * Returns items NOT IN the delivery_history table.
     */
    @Query("SELECT * FROM motivation_items WHERE id NOT IN (SELECT item_id FROM delivery_history)")
    suspend fun getUnseenItems(): List<MotivationItem>
    
    /**
     * Get a single motivation item by its ID.
     */
    @Query("SELECT * FROM motivation_items WHERE id = :id")
    suspend fun getItemById(id: Long): MotivationItem?
    
    /**
     * Insert multiple motivation items at once.
     * Replaces items if there's a conflict (same ID).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MotivationItem>)
    
    /**
     * Get the count of unseen motivation items.
     * Returns the number of items that have not been delivered yet.
     */
    @Query("SELECT COUNT(*) FROM motivation_items WHERE id NOT IN (SELECT item_id FROM delivery_history)")
    suspend fun getUnseenCount(): Int
}
