package com.example.historymotivationcoach.business

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.historymotivationcoach.data.entity.ScheduleMode
import com.example.historymotivationcoach.data.entity.UserPreferences
import com.example.historymotivationcoach.data.repository.PreferencesRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Business logic component for scheduling notifications.
 * 
 * Handles the computation of notification times and scheduling of WorkManager tasks
 * to deliver motivational content at the configured times.
 * 
 * Requirements:
 * - 1.3: Reschedule notifications when frequency changes
 * - 2.2: Even distribution in TIME_WINDOW mode
 * - 2.3: Exact times in FIXED_TIMES mode
 * - 2.4: Cancel and reschedule when mode changes
 * - 4.4: Stop scheduling when content is exhausted
 * - 15.2: Compute delivery times at midnight each day
 * - 15.3: Use deterministic work names to prevent duplicates
 */
class NotificationScheduler(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository
) {
    
    /**
     * Schedule notifications based on current user preferences.
     * 
     * This is the main entry point for notification scheduling. It:
     * 1. Retrieves current user preferences
     * 2. Cancels all existing scheduled notifications
     * 3. Computes notification times based on schedule mode
     * 4. Schedules WorkManager tasks for each notification time
     * 5. Schedules a daily rescheduler to run at midnight
     * 
     * If notifications are disabled in preferences, all scheduled work is cancelled.
     */
    suspend fun scheduleNotifications() {
        val prefs = preferencesRepository.getPreferences()
        
        // If notifications are disabled, cancel all work and return
        if (!prefs.enabled) {
            cancelAllNotifications()
            return
        }
        
        // Compute notification times based on current preferences
        val times = computeNotificationTimes(prefs)
        
        // Cancel existing notification work to avoid duplicates
        WorkManager.getInstance(context).cancelAllWorkByTag(NOTIFICATION_TAG)
        
        // Schedule new work for each computed time
        times.forEachIndexed { index, time ->
            // Use deterministic work name: motivation_YYYYMMDD_index
            val workName = "motivation_${getCurrentDateKey()}_$index"
            val delay = time - System.currentTimeMillis()
            
            // Only schedule if the time is in the future
            if (delay > 0) {
                val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .addTag(NOTIFICATION_TAG)
                    .setInputData(workDataOf("notification_id" to index))
                    .build()
                
                WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        workName,
                        ExistingWorkPolicy.REPLACE,
                        workRequest
                    )
            }
        }
        
        // Schedule daily rescheduler to run at midnight
        scheduleDailyRescheduler()
    }
    
    /**
     * Compute notification times based on user preferences.
     * 
     * Supports two scheduling modes:
     * 
     * TIME_WINDOW mode:
     * - Distributes notifications evenly between start and end times
     * - Calculates equal intervals: (end - start) / notificationsPerDay
     * - Example: 9 AM to 9 PM with 3 notifications = 9 AM, 3 PM, 9 PM
     * 
     * FIXED_TIMES mode:
     * - Uses exact times specified by the user
     * - Takes up to notificationsPerDay times from the fixedTimes list
     * - Example: User specifies [08:00, 12:00, 18:00, 22:00] with 3 per day = 08:00, 12:00, 18:00
     * 
     * @param prefs User preferences containing schedule configuration
     * @return List of Unix timestamps (milliseconds) for notification times, filtered to future times only
     */
    private fun computeNotificationTimes(prefs: UserPreferences): List<Long> {
        val today = Calendar.getInstance()
        val times = mutableListOf<Long>()
        
        when (prefs.scheduleMode) {
            ScheduleMode.TIME_WINDOW -> {
                // Parse start and end times for today
                val start = parseTime(prefs.startTime, today)
                val end = parseTime(prefs.endTime, today)
                
                // Calculate interval for even distribution
                val interval = (end - start) / prefs.notificationsPerDay
                
                // Generate evenly spaced times
                for (i in 0 until prefs.notificationsPerDay) {
                    times.add(start + (interval * i))
                }
            }
            ScheduleMode.FIXED_TIMES -> {
                // Use user-specified fixed times, up to notificationsPerDay limit
                prefs.fixedTimes.take(prefs.notificationsPerDay).forEach { timeStr ->
                    times.add(parseTime(timeStr, today))
                }
            }
        }
        
        // Filter to only include times in the future
        return times.filter { it > System.currentTimeMillis() }
    }
    
    /**
     * Parse a time string (HH:mm format) into a Unix timestamp for today.
     * 
     * Takes a time string like "09:00" or "14:30" and converts it to a timestamp
     * for that time on the current date.
     * 
     * @param timeStr Time string in HH:mm format (e.g., "09:00", "14:30")
     * @param baseDate Calendar instance representing the target date (typically today)
     * @return Unix timestamp in milliseconds for the specified time on the base date
     */
    private fun parseTime(timeStr: String, baseDate: Calendar): Long {
        val parts = timeStr.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        
        // Clone the base date and set the time components
        @Suppress("UNCHECKED_CAST")
        val calendar = baseDate.clone() as Calendar
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        return calendar.timeInMillis
    }
    
    /**
     * Schedule a worker to run at midnight to reschedule notifications for the next day.
     * 
     * This ensures that:
     * - Notifications are computed fresh each day
     * - Work names remain deterministic (include date in name)
     * - Past notifications don't accumulate
     * 
     * The SchedulerWorker will call scheduleNotifications() again at midnight.
     */
    private fun scheduleDailyRescheduler() {
        val midnight = getNextMidnight()
        val delay = midnight - System.currentTimeMillis()
        
        val workRequest = OneTimeWorkRequestBuilder<SchedulerWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(SCHEDULER_TAG)
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "daily_scheduler",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
    }
    
    /**
     * Calculate the timestamp for the next midnight (00:00:00).
     * 
     * @return Unix timestamp in milliseconds for the next midnight
     */
    private fun getNextMidnight(): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
    /**
     * Cancel all scheduled notifications.
     * 
     * This is called when:
     * - Notifications are disabled in settings
     * - Schedule preferences change (before rescheduling)
     * - User explicitly cancels notifications
     */
    fun cancelAllNotifications() {
        WorkManager.getInstance(context).cancelAllWorkByTag(NOTIFICATION_TAG)
    }
    
    /**
     * Get the current date as a key string in YYYYMMDD format.
     * 
     * This format is used in deterministic work names to ensure uniqueness
     * per day and prevent duplicate scheduling.
     * 
     * @return Date string in YYYYMMDD format (e.g., "20240115")
     */
    private fun getCurrentDateKey(): String {
        val format = SimpleDateFormat("yyyyMMdd", Locale.US)
        return format.format(Date())
    }
    
    companion object {
        /**
         * Tag for notification work requests.
         * Used to identify and cancel notification-related work.
         */
        const val NOTIFICATION_TAG = "motivation_notification"
        
        /**
         * Tag for scheduler work requests.
         * Used to identify the daily rescheduler work.
         */
        const val SCHEDULER_TAG = "daily_scheduler"
    }
}
