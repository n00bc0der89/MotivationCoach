package com.example.historymotivationcoach.data.repository

import android.util.Log
import com.example.historymotivationcoach.data.dao.PreferencesDao
import com.example.historymotivationcoach.data.entity.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing user preferences.
 * Provides a clean API for reading and updating user settings.
 * 
 * All database operations are wrapped in try-catch blocks for error handling.
 * Requirements: 22.3
 */
open class PreferencesRepository(private val preferencesDao: PreferencesDao) {
    
    companion object {
        private const val TAG = "PreferencesRepository"
    }
    
    /**
     * Get user preferences for one-time read.
     * Returns default preferences if none have been saved yet.
     * 
     * @return UserPreferences with either saved values or defaults
     * @throws DatabaseException if database operation fails
     */
    open suspend fun getPreferences(): UserPreferences {
        return try {
            preferencesDao.getPreferences() ?: UserPreferences()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting preferences, returning defaults", e)
            // Return defaults on error to allow app to continue functioning
            UserPreferences()
        }
    }
    
    /**
     * Get user preferences as a Flow for reactive updates.
     * The Flow will emit whenever preferences are updated.
     * Returns default preferences if none have been saved yet.
     * 
     * @return Flow of UserPreferences
     */
    fun getPreferencesFlow(): Flow<UserPreferences> {
        return try {
            preferencesDao.getPreferencesFlow()
                .map { it ?: UserPreferences() }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting preferences flow", e)
            throw DatabaseException("Failed to load preferences", e)
        }
    }
    
    /**
     * Update user preferences.
     * Saves the preferences to the database, replacing any existing values.
     * 
     * @param prefs The UserPreferences to save
     * @throws DatabaseException if database operation fails
     */
    suspend fun updatePreferences(prefs: UserPreferences) {
        try {
            preferencesDao.savePreferences(prefs)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating preferences", e)
            throw DatabaseException("Failed to save preferences", e)
        }
    }
}
