package com.example.historymotivationcoach.business

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.historymotivationcoach.MainActivity
import com.example.historymotivationcoach.R
import com.example.historymotivationcoach.data.AppDatabase
import com.example.historymotivationcoach.data.entity.MotivationItem
import com.example.historymotivationcoach.data.repository.MotivationRepository
import com.example.historymotivationcoach.data.repository.PreferencesRepository

/**
 * WorkManager worker for delivering motivational notifications.
 * 
 * This worker is scheduled by NotificationScheduler and executes at the configured times
 * to deliver motivational content to the user. It implements the core notification delivery
 * logic including content selection, history recording, and notification display.
 * 
 * Requirements:
 * - 3.1: Display notification with content (quote, author, image, themes)
 * - 3.2: Handle notification tap navigation to detail screen
 * - 4.1: Select from unseen pool (non-repeating content)
 * - 4.2: Record delivery in history immediately
 * - 4.4: Handle content exhaustion gracefully
 */
class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            // 1. Get dependencies (database, repositories, selector)
            val database = AppDatabase.getInstance(applicationContext)
            val motivationRepo = MotivationRepository(
                database.motivationDao(),
                database.historyDao()
            )
            val prefsRepo = PreferencesRepository(database.preferencesDao())
            val contentSelector = ContentSelector(motivationRepo, prefsRepo)
            val notificationScheduler = NotificationSchedulerImpl(applicationContext, prefsRepo)
            
            // Get notification ID from input data
            val notificationId = inputData.getInt(KEY_NOTIFICATION_ID, 0)
            
            // 2. Check for content exhaustion
            if (contentSelector.isContentExhausted()) {
                // Content is exhausted - stop scheduling notifications
                // The app will display an exhaustion message and offer "Replay Classics"
                return Result.success()
            }
            
            // 3. Select next motivation item
            val motivation = contentSelector.selectNextMotivation()
                ?: return Result.failure() // Should not happen if exhaustion check passed
            
            // 4. Record delivery in history
            motivationRepo.recordDelivery(motivation.id, notificationId)
            
            // 5. Build and show notification with PendingIntent
            showNotification(motivation, notificationId)
            
            // 6. Schedule the next notification (Requirement 7.4)
            notificationScheduler.scheduleNextNotification()
            
            Result.success()
        } catch (e: Exception) {
            // Handle errors gracefully - log and return failure for retry
            e.printStackTrace()
            Result.failure()
        }
    }
    
    /**
     * Build and display a notification for the given motivation item.
     * 
     * The notification includes:
     * - Title: Author name
     * - Text: Quote preview (truncated to 100 characters if needed)
     * - Small icon: App notification icon
     * - Content intent: Opens MainActivity with motivation ID for detail view
     * 
     * @param item The MotivationItem to display in the notification
     * @param notificationId Unique ID for this notification
     */
    private fun showNotification(item: MotivationItem, notificationId: Int) {
        // Create intent to open MainActivity with motivation ID
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra(EXTRA_MOTIVATION_ID, item.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        // Create PendingIntent for notification tap
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Build notification with content
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(item.author)
            .setContentText(truncateQuote(item.quote))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        // Show notification
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * Truncate quote text to fit in notification preview.
     * Adds ellipsis if text is longer than maximum length.
     * 
     * @param quote The full quote text
     * @return Truncated quote with ellipsis if needed
     */
    private fun truncateQuote(quote: String): String {
        return if (quote.length > MAX_QUOTE_LENGTH) {
            quote.take(MAX_QUOTE_LENGTH) + "..."
        } else {
            quote
        }
    }
    
    companion object {
        /**
         * Notification channel ID - must match the channel created in Application class
         */
        const val CHANNEL_ID = "motivation_channel"
        
        /**
         * Intent extra key for passing motivation item ID
         */
        const val EXTRA_MOTIVATION_ID = "motivation_id"
        
        /**
         * Input data key for notification ID
         */
        const val KEY_NOTIFICATION_ID = "notification_id"
        
        /**
         * Maximum length for quote preview in notification
         */
        private const val MAX_QUOTE_LENGTH = 100
    }
}
