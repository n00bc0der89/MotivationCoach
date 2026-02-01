package com.example.historymotivationcoach.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey
    val id: Int = 1, // Single row table
    
    @ColumnInfo(name = "notifications_per_day")
    val notificationsPerDay: Int = 3,
    
    @ColumnInfo(name = "schedule_mode")
    val scheduleMode: ScheduleMode = ScheduleMode.TIME_WINDOW,
    
    @ColumnInfo(name = "start_time")
    val startTime: String = "09:00", // HH:mm format
    
    @ColumnInfo(name = "end_time")
    val endTime: String = "21:00", // HH:mm format
    
    @ColumnInfo(name = "fixed_times")
    val fixedTimes: List<String> = emptyList(), // List of HH:mm strings
    
    @ColumnInfo(name = "enabled")
    val enabled: Boolean = true,
    
    @ColumnInfo(name = "preferred_themes")
    val preferredThemes: List<String> = emptyList()
)

enum class ScheduleMode {
    TIME_WINDOW,
    FIXED_TIMES
}
