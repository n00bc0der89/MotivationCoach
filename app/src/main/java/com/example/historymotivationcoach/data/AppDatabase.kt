package com.example.historymotivationcoach.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.historymotivationcoach.data.entity.DeliveryHistory
import com.example.historymotivationcoach.data.entity.MotivationItem
import com.example.historymotivationcoach.data.entity.UserPreferences

@Database(
    entities = [
        MotivationItem::class,
        DeliveryHistory::class,
        UserPreferences::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    // Abstract DAO accessor methods - implemented by Room
    abstract fun motivationDao(): com.example.historymotivationcoach.data.dao.MotivationDao
    abstract fun historyDao(): com.example.historymotivationcoach.data.dao.HistoryDao
    abstract fun preferencesDao(): com.example.historymotivationcoach.data.dao.PreferencesDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "motivation_coach_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        // Migration from version 1 to 2: Add composite index on delivery_history
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Create composite index on date_key and shown_at for better query performance
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_date_time` ON `delivery_history` (`date_key`, `shown_at`)"
                )
            }
        }
        
        // Migration from version 2 to 3: Update UserPreferences schema for v2 features
        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Create new table with updated schema
                database.execSQL(
                    """
                    CREATE TABLE user_preferences_new (
                        id INTEGER PRIMARY KEY NOT NULL,
                        notifications_per_day INTEGER NOT NULL,
                        start_time TEXT NOT NULL,
                        end_time TEXT NOT NULL,
                        enabled INTEGER NOT NULL,
                        preferred_themes TEXT NOT NULL,
                        schedule_mode TEXT NOT NULL,
                        custom_days TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                
                // Copy existing data with default values for new fields
                database.execSQL(
                    """
                    INSERT INTO user_preferences_new 
                    (id, notifications_per_day, start_time, end_time, enabled, preferred_themes, schedule_mode, custom_days)
                    SELECT id, notifications_per_day, start_time, end_time, enabled, preferred_themes, 'ALL_DAYS', ''
                    FROM user_preferences
                    """.trimIndent()
                )
                
                // Drop old table
                database.execSQL("DROP TABLE user_preferences")
                
                // Rename new table to original name
                database.execSQL("ALTER TABLE user_preferences_new RENAME TO user_preferences")
            }
        }
    }
}
