package com.example.historymotivationcoach.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.historymotivationcoach.data.entity.UserPreferences
import kotlinx.coroutines.flow.Flow

@Dao
interface PreferencesDao {
    
    /**
     * Get user preferences for one-time read.
     * Returns null if no preferences have been saved yet.
     */
    @Query("SELECT * FROM user_preferences WHERE id = 1")
    suspend fun getPreferences(): UserPreferences?
    
    /**
     * Save or update user preferences.
     * Uses REPLACE strategy to update the single row if it exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreferences(prefs: UserPreferences)
    
    /**
     * Get user preferences as a Flow for reactive updates.
     * The Flow will emit whenever preferences are updated.
     * Returns null if no preferences have been saved yet.
     */
    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun getPreferencesFlow(): Flow<UserPreferences?>
}
