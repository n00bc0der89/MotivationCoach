package com.example.historymotivationcoach.business

import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Represents a time window for notification scheduling.
 * 
 * A time window defines the start and end times during which notifications
 * can be sent. It provides methods to calculate evenly distributed notification
 * times and validate if the window is wide enough for the requested number
 * of notifications.
 * 
 * Requirements:
 * - 5.6: Distribute notifications evenly within the time window
 * - 5.7: Validate time window is wide enough for requested notifications
 */
data class TimeWindow(
    val startTime: LocalTime,
    val endTime: LocalTime
) {
    init {
        require(startTime < endTime) { "Start time must be before end time" }
    }
    
    /**
     * Calculates evenly distributed notification times within the time window.
     * 
     * The algorithm distributes notifications evenly by:
     * 1. Calculating the total duration of the time window
     * 2. Dividing the duration by the number of notifications to get the interval
     * 3. Placing each notification at the midpoint of its interval
     * 
     * For example, with a 12-hour window (09:00-21:00) and 3 notifications:
     * - Interval = 12 hours / 3 = 4 hours
     * - Notification 1: 09:00 + 2 hours = 11:00
     * - Notification 2: 09:00 + 6 hours = 15:00
     * - Notification 3: 09:00 + 10 hours = 19:00
     * 
     * @param count Number of notifications to distribute
     * @return List of LocalTime values evenly distributed within the window
     * @throws IllegalArgumentException if count is not positive
     */
    fun calculateNotificationTimes(count: Int): List<LocalTime> {
        require(count > 0) { "Count must be positive" }
        
        val totalMinutes = ChronoUnit.MINUTES.between(startTime, endTime)
        val interval = totalMinutes / count
        
        return (0 until count).map { index ->
            startTime.plusMinutes(interval * index + interval / 2)
        }
    }
    
    /**
     * Checks if the time window is wide enough for the requested number of notifications.
     * 
     * A time window is considered wide enough if it can accommodate all notifications
     * with at least the minimum interval between them.
     * 
     * @param count Number of notifications to schedule
     * @param minIntervalMinutes Minimum interval between notifications in minutes (default: 30)
     * @return true if the window is wide enough, false otherwise
     */
    fun isWideEnough(count: Int, minIntervalMinutes: Int = 30): Boolean {
        val totalMinutes = ChronoUnit.MINUTES.between(startTime, endTime)
        return totalMinutes >= (count * minIntervalMinutes)
    }
}
