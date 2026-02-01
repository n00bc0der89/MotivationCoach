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
    version = 2,
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
                .addMigrations(MIGRATION_1_2)
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
    }
}
