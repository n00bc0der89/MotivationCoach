package com.example.historymotivationcoach.business

import com.example.historymotivationcoach.data.entity.ScheduleMode
import com.example.historymotivationcoach.data.entity.getActiveDays
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Represents a notification schedule configuration.
 * 
 * This class encapsulates all the scheduling parameters and provides
 * logic to calculate the next valid notification time based on:
 * - Schedule mode (which days are active)
 * - Time window (what times are allowed)
 * - Number of notifications per day
 * 
 * Requirements:
 * - 6.1: Apply both schedule mode and time window constraints
 * - 6.2: Exclude days not in schedule mode
 * - 6.3: Include days in schedule mode within time window
 * - 7.2: Respect day-of-week and time-of-day constraints
 */
data class NotificationSchedule(
    val scheduleMode: ScheduleMode,
    val customDays: Set<DayOfWeek>,
    val timeWindow: TimeWindow,
    val notificationsPerDay: Int
) {
    /**
     * Calculates the next notification time from a given starting point.
     * 
     * The algorithm:
     * 1. Gets the set of active days based on schedule mode
     * 2. Calculates the notification times for each day within the time window
     * 3. Starting from the given time, looks for the next valid notification time
     * 4. Checks up to 14 days ahead to find a valid time
     * 5. Returns null if no valid time is found
     * 
     * A valid notification time must satisfy:
     * - The day of week is active according to the schedule mode
     * - The time of day falls within the configured time window
     * - The time is in the future (after the 'from' parameter)
     * 
     * @param from Starting point for calculating next notification time
     * @return Next valid notification time, or null if none found within 14 days
     */
    fun calculateNextTime(from: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        val activeDays = scheduleMode.getActiveDays(customDays)
        val notificationTimes = timeWindow.calculateNotificationTimes(notificationsPerDay)
        
        var candidate = from
        val maxDaysToCheck = 14 // Look ahead up to 2 weeks
        
        repeat(maxDaysToCheck) {
            // Check if current day is active
            if (candidate.dayOfWeek in activeDays) {
                // Find next notification time on this day
                val nextTimeToday = notificationTimes.firstOrNull { time ->
                    candidate.toLocalTime() < time
                }
                
                if (nextTimeToday != null) {
                    return candidate.with(nextTimeToday)
                }
            }
            
            // Move to next day at start of time window
            candidate = candidate.plusDays(1)
                .with(timeWindow.startTime)
        }
        
        return null // No valid time found
    }
}
