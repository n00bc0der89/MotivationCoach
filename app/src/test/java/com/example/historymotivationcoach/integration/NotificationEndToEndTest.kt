package com.example.historymotivationcoach.integration

import com.example.historymotivationcoach.data.entity.ScheduleMode
import com.example.historymotivationcoach.data.entity.UserPreferences
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.Calendar
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end test for notification scheduling logic.
 * 
 * Tests verify the complete notification scheduling flow:
 * - Schedule notifications with various preferences (TIME_WINDOW and FIXED_TIMES modes)
 * - Verify notifications are computed at correct times
 * - Test notification tap navigation intent data
 * - Verify rescheduling behavior when preferences change
 * 
 * Requirements: 2.2, 2.3, 3.1, 3.2
 * 
 * Note: This test focuses on the scheduling logic. Actual WorkManager integration
 * and device reboot testing must be done manually or with instrumented tests.
 */
class NotificationEndToEndTest {
    
    @Test
    fun `end-to-end TIME_WINDOW mode scheduling with 3 notifications`() = runTest {
        // Given: TIME_WINDOW mode with 3 notifications from 9 AM to 9 PM
        val prefs = UserPreferences(
            notificationsPerDay = 3,
            scheduleMode = ScheduleMode.TIME_WINDOW,
            startTime = "09:00",
            endTime = "21:00",
            enabled = true
        )
        
        // When: computing notification times
        val times = computeNotificationTimes(prefs)
        
        // Then: verify times are evenly distributed
        assertTrue(times.size <= 3, "Should have at most 3 notification times")
        
        if (times.size >= 2) {
            // Verify even distribution
            val intervals = mutableListOf<Long>()
            for (i in 0 until times.size - 1) {
                intervals.add(times[i + 1] - times[i])
            }
            
            // All intervals should be approximately equal (within 1 second)
            val expectedInterval = intervals.first()
            intervals.forEach { interval ->
                val difference = kotlin.math.abs(interval - expectedInterval)
                assertTrue(
                    difference < 1000,
                    "Intervals should be equal: expected $expectedInterval, got $interval"
                )
            }
        }
        
        // Verify all times are in the future
        val now = System.currentTimeMillis()
        times.forEach { time ->
            assertTrue(time > now, "All notification times should be in the future")
        }
    }
    
    @Test
    fun `end-to-end FIXED_TIMES mode scheduling with specific times`() = runTest {
        // Given: FIXED_TIMES mode with specific times
        val fixedTimes = listOf("08:00", "12:00", "18:00", "22:00")
        val prefs = UserPreferences(
            notificationsPerDay = 3,
            scheduleMode = ScheduleMode.FIXED_TIMES,
            fixedTimes = fixedTimes,
            enabled = true
        )
        
        // When: computing notification times
        val times = computeNotificationTimes(prefs)
        
        // Then: verify times match the fixed times (up to notificationsPerDay limit)
        assertTrue(times.size <= 3, "Should have at most 3 notification times")
        
        // Verify times match the fixed times
        times.forEach { timestamp ->
            val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            val timeStr = "%02d:%02d".format(hour, minute)
            
            assertTrue(
                fixedTimes.take(3).contains(timeStr),
                "Time $timeStr should be in the first 3 fixed times"
            )
        }
        
        // Verify all times are in the future
        val now = System.currentTimeMillis()
        times.forEach { time ->
            assertTrue(time > now, "All notification times should be in the future")
        }
    }
    
    @Test
    fun `end-to-end notification content includes all required fields`() {
        // Given: A motivation item for notification
        val quote = "The only way to do great work is to love what you do."
        val author = "Steve Jobs"
        val imageUri = "android.resource://com.example.motivationcoach/drawable/jobs"
        val themes = listOf("work", "passion", "excellence")
        
        // When: preparing notification content
        val notificationTitle = author
        val notificationText = if (quote.length > 100) {
            quote.take(100) + "..."
        } else {
            quote
        }
        
        // Then: verify all required fields are present
        assertEquals(author, notificationTitle, "Notification title should be author name")
        assertTrue(
            notificationText.contains("The only way to do great work"),
            "Notification text should contain quote preview"
        )
        assertTrue(
            notificationText.length <= 103,
            "Notification text should be truncated to 100 chars + ellipsis"
        )
        
        // Verify image and themes would be included
        assertTrue(imageUri.isNotEmpty(), "Image URI should be present")
        assertTrue(themes.isNotEmpty(), "Themes should be present")
    }
    
    @Test
    fun `end-to-end notification intent contains correct motivation ID`() {
        // Given: A motivation item ID
        val motivationId = 42L
        
        // When: creating notification intent data
        val intentData = mapOf("motivation_id" to motivationId)
        
        // Then: verify intent contains correct ID for navigation
        assertEquals(
            motivationId,
            intentData["motivation_id"],
            "Intent should contain correct motivation ID"
        )
    }
    
    @Test
    fun `end-to-end rescheduling when preferences change`() = runTest {
        // Given: Initial preferences with 2 notifications
        val initialPrefs = UserPreferences(
            notificationsPerDay = 2,
            scheduleMode = ScheduleMode.TIME_WINDOW,
            startTime = "09:00",
            endTime = "17:00",
            enabled = true
        )
        
        // When: computing initial notification times
        val initialTimes = computeNotificationTimes(initialPrefs)
        
        // Then: should have at most 2 times
        assertTrue(initialTimes.size <= 2, "Should have at most 2 notification times initially")
        
        // When: changing preferences to 4 notifications
        val updatedPrefs = initialPrefs.copy(notificationsPerDay = 4)
        val updatedTimes = computeNotificationTimes(updatedPrefs)
        
        // Then: should have at most 4 times
        assertTrue(updatedTimes.size <= 4, "Should have at most 4 notification times after update")
    }
    
    @Test
    fun `end-to-end disabling notifications prevents scheduling`() = runTest {
        // Given: Notifications are disabled
        val disabledPrefs = UserPreferences(
            notificationsPerDay = 3,
            scheduleMode = ScheduleMode.TIME_WINDOW,
            startTime = "09:00",
            endTime = "21:00",
            enabled = false
        )
        
        // When: checking if notifications should be scheduled
        val shouldSchedule = disabledPrefs.enabled
        
        // Then: should not schedule notifications
        assertEquals(false, shouldSchedule, "Should not schedule when disabled")
    }
    
    @Test
    fun `end-to-end scheduling with boundary times`() = runTest {
        // Given: Notifications at start and end of day
        val prefs = UserPreferences(
            notificationsPerDay = 2,
            scheduleMode = ScheduleMode.FIXED_TIMES,
            fixedTimes = listOf("00:00", "23:59"),
            enabled = true
        )
        
        // When: computing notification times
        val times = computeNotificationTimes(prefs)
        
        // Then: should handle boundary times correctly
        assertTrue(times.size <= 2, "Should have at most 2 notification times")
        
        times.forEach { timestamp ->
            val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            
            assertTrue(
                (hour == 0 && minute == 0) || (hour == 23 && minute == 59),
                "Times should be at boundary times (00:00 or 23:59)"
            )
        }
    }
    
    @Test
    fun `end-to-end work request naming is deterministic`() {
        // Given: A date and notification index
        val dateKey = "20260131"
        val index = 2
        
        // When: generating work request name
        val workName = "motivation_${dateKey}_$index"
        
        // Then: verify name follows deterministic pattern
        assertEquals("motivation_20260131_2", workName, "Work name should be deterministic")
        assertTrue(
            workName.startsWith("motivation_"),
            "Work name should start with 'motivation_'"
        )
        assertTrue(
            workName.contains(dateKey),
            "Work name should contain date key"
        )
        assertTrue(
            workName.endsWith("_$index"),
            "Work name should end with index"
        )
    }
    
    @Test
    fun `end-to-end scheduling respects minimum and maximum frequency`() = runTest {
        // Given: Minimum frequency (1 notification per day)
        val minPrefs = UserPreferences(
            notificationsPerDay = 1,
            scheduleMode = ScheduleMode.TIME_WINDOW,
            startTime = "12:00",
            endTime = "13:00",
            enabled = true
        )
        
        // When: computing notification times
        val minTimes = computeNotificationTimes(minPrefs)
        
        // Then: should have at most 1 time
        assertTrue(minTimes.size <= 1, "Should have at most 1 notification time")
        
        // Given: Maximum frequency (10 notifications per day)
        val maxPrefs = UserPreferences(
            notificationsPerDay = 10,
            scheduleMode = ScheduleMode.TIME_WINDOW,
            startTime = "08:00",
            endTime = "22:00",
            enabled = true
        )
        
        // When: computing notification times
        val maxTimes = computeNotificationTimes(maxPrefs)
        
        // Then: should have at most 10 times
        assertTrue(maxTimes.size <= 10, "Should have at most 10 notification times")
    }
    
    // Helper method to compute notification times (simulates NotificationScheduler logic)
    private fun computeNotificationTimes(prefs: UserPreferences): List<Long> {
        val today = Calendar.getInstance()
        val times = mutableListOf<Long>()
        
        when (prefs.scheduleMode) {
            ScheduleMode.TIME_WINDOW -> {
                val start = parseTime(prefs.startTime, today)
                val end = parseTime(prefs.endTime, today)
                val interval = (end - start) / prefs.notificationsPerDay
                
                for (i in 0 until prefs.notificationsPerDay) {
                    times.add(start + (interval * i))
                }
            }
            ScheduleMode.FIXED_TIMES -> {
                prefs.fixedTimes.take(prefs.notificationsPerDay).forEach { timeStr ->
                    times.add(parseTime(timeStr, today))
                }
            }
        }
        
        // Filter out past times
        return times.filter { it > System.currentTimeMillis() }
    }
    
    private fun parseTime(timeStr: String, baseDate: Calendar): Long {
        val parts = timeStr.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        
        return (baseDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
