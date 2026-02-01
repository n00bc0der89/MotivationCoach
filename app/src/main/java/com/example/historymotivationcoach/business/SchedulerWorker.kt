package com.example.historymotivationcoach.business

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.historymotivationcoach.data.AppDatabase
import com.example.historymotivationcoach.data.repository.PreferencesRepository

/**
 * WorkManager worker for daily notification rescheduling.
 * 
 * This worker runs at midnight each day to reschedule notifications for the new day.
 * It ensures that notifications are computed fresh each day with deterministic work names
 * that include the current date.
 * 
 * The worker:
 * 1. Gets the database and repository dependencies
 * 2. Creates a NotificationScheduler instance
 * 3. Calls scheduleNotifications() to compute and schedule today's notifications
 * 4. Handles errors gracefully with retry logic
 * 
 * Requirements:
 * - 15.2: Compute delivery times at midnight each day
 */
class SchedulerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            // 1. Get dependencies (database, repositories)
            val database = AppDatabase.getInstance(applicationContext)
            val prefsRepo = PreferencesRepository(database.preferencesDao())
            
            // 2. Create NotificationScheduler instance
            val scheduler = NotificationScheduler(applicationContext, prefsRepo)
            
            // 3. Call scheduleNotifications() to reschedule for the new day
            scheduler.scheduleNotifications()
            
            // Return success - notifications have been rescheduled
            Result.success()
        } catch (e: Exception) {
            // Handle errors gracefully
            // Log the error for debugging
            e.printStackTrace()
            
            // Retry on failure - WorkManager will automatically retry with backoff
            // This handles transient failures like database locks or temporary issues
            Result.retry()
        }
    }
}
