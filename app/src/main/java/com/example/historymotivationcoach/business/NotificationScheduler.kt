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
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Interface for scheduling notifications.
 * 
 * Handles the computation of notification times and scheduling of WorkManager tasks
 * to deliver motivational content at the configured times.
 * 
 * Requirements:
 * - 3.2: Manual notification triggering
 * - 4.7: Schedule notifications respecting schedule mode
 * - 4.8: Reschedule on preference changes
 * - 5.8: Reschedule on time window changes
 * - 7.1: WorkManager integration
 * - 7.3: OneTimeWorkRequests with exact timing
 * - 7.4: Auto-schedule next notification after delivery
 */
interface NotificationScheduler {
    /**
     * Schedules the next notification based on current preferences.
     * Applies schedule mode and time window constraints.
     * 
     * Requirements: 4.7, 7.1, 7.3
     */
    suspend fun scheduleNextNotification()
    
    /**
     * Reschedules all pending notifications.
     * Called when preferences change.
     * 
     * Requirements: 4.8, 5.8
     */
    suspend fun rescheduleAllNotifications()
    
    /**
     * Cancels all pending notifications.
     * 
     * Requirements: 7.7
     */
    suspend fun cancelAllNotifications()
    
    /**
     * Triggers a manual notification immediately.
     * Returns the delivered motivation ID or null if failed.
     * 
     * Requirements: 3.2
     */
    suspend fun triggerManualNotification(): Long?
    
    /**
     * Calculates the next valid notification time
     * based on schedule mode and time window.
     * 
     * Requirements: 6.1, 6.2, 6.3, 7.2
     */
    fun calculateNextNotificationTime(
        preferences: UserPreferences,
        fromTime: LocalDateTime = LocalDateTime.now()
    ): LocalDateTime?
}

/**
 * Implementation of NotificationScheduler using WorkManager.
 * 
 * Handles the computation of notification times and scheduling of WorkManager tasks
 * to deliver motivational content at the configured times.
 * 
 * Requirements:
 * - 1.3: Reschedule notifications when frequency changes
 * - 2.2: Even distribution in TIME_WINDOW mode
 * - 2.4: Cancel and reschedule when mode changes
 * - 4.4: Stop scheduling when content is exhausted
 * - 15.2: Compute delivery times at midnight each day
 * - 15.3: Use deterministic work names to prevent duplicates
 */
class NotificationSchedulerImpl(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository
) : NotificationScheduler {
    
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
     * For v2, this uses the ALL_DAYS schedule mode with time window distribution.
     * Advanced schedule mode logic will be implemented in task 2.
     * 
     * @param prefs User preferences containing schedule configuration
     * @return List of Unix timestamps (milliseconds) for notification times, filtered to future times only
     */
    private fun computeNotificationTimes(prefs: UserPreferences): List<Long> {
        val today = Calendar.getInstance()
        val times = mutableListOf<Long>()
        
        // For now, use time window distribution (v2 default behavior)
        // Parse start and end times for today
        val start = parseTime(prefs.startTime, today)
        val end = parseTime(prefs.endTime, today)
        
        // Calculate interval for even distribution
        val interval = (end - start) / prefs.notificationsPerDay
        
        // Generate evenly spaced times
        for (i in 0 until prefs.notificationsPerDay) {
            times.add(start + (interval * i))
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
    override suspend fun cancelAllNotifications() {
        WorkManager.getInstance(context).cancelAllWorkByTag(NOTIFICATION_TAG)
    }
    
    /**
     * Schedules the next notification based on current preferences.
     * Applies schedule mode and time window constraints.
     * 
     * Requirements: 4.7, 7.1, 7.3
     */
    override suspend fun scheduleNextNotification() {
        val prefs = preferencesRepository.getPreferences()
        
        // If notifications are disabled, cancel all work and return
        if (!prefs.enabled) {
            cancelAllNotifications()
            return
        }
        
        // Calculate next notification time using new schedule logic
        val nextTime = calculateNextNotificationTime(prefs)
        
        if (nextTime != null) {
            // Convert LocalDateTime to milliseconds
            val nextTimeMillis = nextTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val delay = nextTimeMillis - System.currentTimeMillis()
            
            // Only schedule if the time is in the future
            if (delay > 0) {
                val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .addTag(NOTIFICATION_TAG)
                    .setInputData(workDataOf(NotificationWorker.KEY_NOTIFICATION_ID to System.currentTimeMillis().toInt()))
                    .build()
                
                WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        "next_notification",
                        ExistingWorkPolicy.REPLACE,
                        workRequest
                    )
            }
        }
    }
    
    /**
     * Reschedules all pending notifications.
     * Called when preferences change.
     * 
     * Requirements: 4.8, 5.8
     */
    override suspend fun rescheduleAllNotifications() {
        // Cancel all existing notifications
        cancelAllNotifications()
        
        // Schedule the next notification with new preferences
        scheduleNextNotification()
    }
    
    /**
     * Triggers a manual notification immediately.
     * Returns the delivered motivation ID or null if failed.
     * 
     * Requirements: 3.2
     */
    override suspend fun triggerManualNotification(): Long? {
        return try {
            // Get dependencies
            val database = com.example.historymotivationcoach.data.AppDatabase.getInstance(context)
            val motivationRepo = com.example.historymotivationcoach.data.repository.MotivationRepository(
                database.motivationDao(),
                database.historyDao()
            )
            val prefsRepo = preferencesRepository
            val contentSelector = ContentSelector(motivationRepo, prefsRepo)
            
            // Check for content exhaustion
            if (contentSelector.isContentExhausted()) {
                return null
            }
            
            // Select next motivation item
            val motivation = contentSelector.selectNextMotivation() ?: return null
            
            // Record delivery in history with manual notification ID
            val notificationId = System.currentTimeMillis().toInt()
            motivationRepo.recordDelivery(motivation.id, notificationId)
            
            // Show the notification directly
            showManualNotification(motivation, notificationId)
            
            motivation.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Show a manual notification for the given motivation.
     */
    private fun showManualNotification(item: com.example.historymotivationcoach.data.entity.MotivationItem, notificationId: Int) {
        val intent = android.content.Intent(context, com.example.historymotivationcoach.MainActivity::class.java).apply {
            putExtra(NotificationWorker.EXTRA_MOTIVATION_ID, item.id)
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = androidx.core.app.NotificationCompat.Builder(context, NotificationWorker.CHANNEL_ID)
            .setContentTitle(item.author)
            .setContentText(if (item.quote.length > 100) item.quote.take(100) + "..." else item.quote)
            .setSmallIcon(com.example.historymotivationcoach.R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * Calculates the next valid notification time
     * based on schedule mode and time window.
     * 
     * Requirements: 6.1, 6.2, 6.3, 7.2
     */
    override fun calculateNextNotificationTime(
        preferences: UserPreferences,
        fromTime: LocalDateTime
    ): LocalDateTime? {
        val timeWindow = TimeWindow(
            startTime = java.time.LocalTime.parse(preferences.startTime),
            endTime = java.time.LocalTime.parse(preferences.endTime)
        )
        
        val schedule = NotificationSchedule(
            scheduleMode = preferences.scheduleMode,
            customDays = preferences.customDays,
            timeWindow = timeWindow,
            notificationsPerDay = preferences.notificationsPerDay
        )
        
        return schedule.calculateNextTime(fromTime)
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
