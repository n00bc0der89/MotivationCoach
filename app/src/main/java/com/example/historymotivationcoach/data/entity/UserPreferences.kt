package com.example.historymotivationcoach.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey
    val id: Int = 1, // Single row table
    
    @ColumnInfo(name = "notifications_per_day")
    val notificationsPerDay: Int = 3,
    
    @ColumnInfo(name = "start_time")
    val startTime: String = "09:00", // HH:mm format
    
    @ColumnInfo(name = "end_time")
    val endTime: String = "21:00", // HH:mm format
    
    @ColumnInfo(name = "enabled")
    val enabled: Boolean = true,
    
    @ColumnInfo(name = "preferred_themes")
    val preferredThemes: List<String> = emptyList(),
    
    // New fields for v2
    @ColumnInfo(name = "schedule_mode")
    val scheduleMode: ScheduleMode = ScheduleMode.ALL_DAYS,
    
    @ColumnInfo(name = "custom_days")
    val customDays: Set<DayOfWeek> = emptySet()
)

enum class ScheduleMode {
    ALL_DAYS,
    WEEKDAYS_ONLY,
    WEEKENDS_ONLY,
    CUSTOM_DAYS
}

/**
 * Extension function to get active days for a schedule mode.
 * 
 * @param customDays Set of custom days when mode is CUSTOM_DAYS
 * @return Set of DayOfWeek values that are active for this schedule mode
 */
fun ScheduleMode.getActiveDays(customDays: Set<DayOfWeek> = emptySet()): Set<DayOfWeek> {
    return when (this) {
        ScheduleMode.ALL_DAYS -> DayOfWeek.values().toSet()
        ScheduleMode.WEEKDAYS_ONLY -> setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )
        ScheduleMode.WEEKENDS_ONLY -> setOf(
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )
        ScheduleMode.CUSTOM_DAYS -> customDays
    }
}
