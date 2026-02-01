package com.example.historymotivationcoach.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.historymotivationcoach.data.dao.MotivationDao
import com.example.historymotivationcoach.data.entity.MotivationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/**
 * SeedDataLoader is responsible for loading initial motivation content from a JSON file
 * into the Room database on first app launch.
 * 
 * Requirements:
 * - 22.1: Load seed dataset from bundled JSON file on first launch
 * - 22.2: Validate each item before insertion
 * - 22.4: Ensure idempotent loading (only once)
 */
class SeedDataLoader(
    private val context: Context,
    private val motivationDao: MotivationDao
) {
    
    companion object {
        private const val TAG = "SeedDataLoader"
        private const val PREFS_NAME = "seed_data_prefs"
        private const val KEY_SEED_LOADED = "seed_data_loaded"
        private const val SEED_FILE_NAME = "motivations.json"
    }
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Load seed data from JSON file into the database.
     * This method is idempotent - it will only load data once.
     * 
     * @return Result indicating success or failure with error message
     */
    suspend fun loadSeedData(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Check if seed data has already been loaded (Requirement 22.4)
            if (isSeedDataLoaded()) {
                Log.d(TAG, "Seed data already loaded, skipping")
                return@withContext Result.success(0)
            }
            
            Log.d(TAG, "Loading seed data from $SEED_FILE_NAME")
            
            // Read JSON file from assets (Requirement 22.1)
            val jsonString = readJsonFromAssets()
            
            // Parse JSON and validate items (Requirement 22.2)
            val motivationItems = parseAndValidateJson(jsonString)
            
            if (motivationItems.isEmpty()) {
                val errorMsg = "No valid motivation items found in seed data"
                Log.e(TAG, errorMsg)
                return@withContext Result.failure(Exception(errorMsg))
            }
            
            // Insert items into database
            motivationDao.insertAll(motivationItems)
            
            // Mark seed data as loaded (Requirement 22.4)
            markSeedDataLoaded()
            
            Log.d(TAG, "Successfully loaded ${motivationItems.size} motivation items")
            Result.success(motivationItems.size)
            
        } catch (e: IOException) {
            val errorMsg = "Failed to read seed data file: ${e.message}"
            Log.e(TAG, errorMsg, e)
            Result.failure(Exception(errorMsg, e))
        } catch (e: JSONException) {
            val errorMsg = "Failed to parse seed data JSON: ${e.message}"
            Log.e(TAG, errorMsg, e)
            Result.failure(Exception(errorMsg, e))
        } catch (e: Exception) {
            val errorMsg = "Unexpected error loading seed data: ${e.message}"
            Log.e(TAG, errorMsg, e)
            Result.failure(Exception(errorMsg, e))
        }
    }
    
    /**
     * Check if seed data has already been loaded.
     */
    private fun isSeedDataLoaded(): Boolean {
        return prefs.getBoolean(KEY_SEED_LOADED, false)
    }
    
    /**
     * Mark seed data as loaded in SharedPreferences.
     */
    private fun markSeedDataLoaded() {
        prefs.edit().putBoolean(KEY_SEED_LOADED, true).apply()
    }
    
    /**
     * Read JSON file from assets directory.
     * 
     * @throws IOException if file cannot be read
     */
    private fun readJsonFromAssets(): String {
        return context.assets.open(SEED_FILE_NAME).bufferedReader().use { it.readText() }
    }
    
    /**
     * Parse JSON string and validate each motivation item.
     * Invalid items are logged and skipped.
     * 
     * @param jsonString The JSON string to parse
     * @return List of valid MotivationItem objects
     * @throws JSONException if JSON structure is invalid
     */
    private fun parseAndValidateJson(jsonString: String): List<MotivationItem> {
        val validItems = mutableListOf<MotivationItem>()
        val jsonObject = JSONObject(jsonString)
        val motivationsArray = jsonObject.getJSONArray("motivations")
        
        for (i in 0 until motivationsArray.length()) {
            try {
                val itemJson = motivationsArray.getJSONObject(i)
                val item = parseMotivationItem(itemJson)
                
                // Validate item (Requirement 22.2)
                if (validateMotivationItem(item)) {
                    validItems.add(item)
                } else {
                    Log.w(TAG, "Skipping invalid item at index $i: missing required fields")
                }
            } catch (e: JSONException) {
                Log.w(TAG, "Failed to parse item at index $i: ${e.message}", e)
                // Continue with next item (graceful error handling)
            }
        }
        
        return validItems
    }
    
    /**
     * Parse a single motivation item from JSON.
     * 
     * @param json The JSON object representing a motivation item
     * @return MotivationItem object
     * @throws JSONException if required fields are missing
     */
    private fun parseMotivationItem(json: JSONObject): MotivationItem {
        // Parse themes array
        val themesArray = json.getJSONArray("themes")
        val themes = mutableListOf<String>()
        for (i in 0 until themesArray.length()) {
            themes.add(themesArray.getString(i))
        }
        
        return MotivationItem(
            id = 0, // Auto-generated by Room
            quote = json.getString("quote"),
            author = json.getString("author"),
            context = json.optString("context").takeIf { it.isNotEmpty() },
            imageUri = json.getString("imageUri"),
            themes = themes,
            sourceName = json.getString("sourceName"),
            sourceUrl = json.optString("sourceUrl").takeIf { it.isNotEmpty() },
            license = json.getString("license")
        )
    }
    
    /**
     * Validate that a motivation item has all required fields.
     * 
     * Required fields (Requirement 22.2):
     * - quote (non-empty)
     * - author (non-empty)
     * - imageUri (non-empty)
     * - themes (non-empty list)
     * - sourceName (non-empty)
     * - license (non-empty)
     * 
     * @param item The motivation item to validate
     * @return true if valid, false otherwise
     */
    private fun validateMotivationItem(item: MotivationItem): Boolean {
        return item.quote.isNotBlank() &&
               item.author.isNotBlank() &&
               item.imageUri.isNotBlank() &&
               item.themes.isNotEmpty() &&
               item.sourceName.isNotBlank() &&
               item.license.isNotBlank()
    }
    
    /**
     * Reset the seed data loaded flag.
     * This is useful for testing or if the user wants to reload seed data.
     */
    fun resetSeedDataFlag() {
        prefs.edit().putBoolean(KEY_SEED_LOADED, false).apply()
        Log.d(TAG, "Seed data flag reset")
    }
}
