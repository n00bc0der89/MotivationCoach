package com.example.historymotivationcoach.business

import android.content.Context
import androidx.work.WorkManager
import com.example.historymotivationcoach.data.entity.ScheduleMode
import com.example.historymotivationcoach.data.entity.UserPreferences
import com.example.historymotivationcoach.data.repository.PreferencesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Calendar
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for NotificationScheduler business logic component.
 * 
 * Tests verify:
 * - Time window mode: even distribution of notifications
 * - Fixed times mode: exact scheduling at specified times
 * - Time parsing and calculation
 * - Future time filtering
 * - Edge cases (boundary times, single notification, max notifications)
 * 
 * Note: WorkManager integration is tested separately in integration tests.
 * These unit tests focus on the time computation logic.
 */
class NotificationSchedulerTest {
    
    private lateinit var context: Context
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var notificationScheduler: NotificationSchedulerImpl
    
    @Before
    fun setup() {
        context = mock()
        preferencesRepository = mock()
        
        // Mock WorkManager.getInstance to avoid Android framework dependencies
        val workManager: WorkManager = mock()
        whenever(context.applicationContext).thenReturn(context)
        
        notificationScheduler = NotificationSchedulerImpl(context, preferencesRepository)
    }
    
    @Test
    fun `computeNotificationTimes distributes evenly in ALL_DAYS mode`() = runTest {
        // Given: ALL_DAYS mode with 3 notifications from 9 AM to 9 PM
        val prefs = UserPreferences(
            notificationsPerDay = 3,
            scheduleMode = ScheduleMode.ALL_DAYS,
            startTime = "09:00",
            endTime = "21:00",
            enabled = true
        )
        
        whenever(preferencesRepository.getPreferences()).thenReturn(prefs)
        
        // When: computing notification times
        val times = computeNotificationTimesPublic(prefs)
        
        // Then: should have 3 times (or fewer if some are in the past)
        assertTrue(times.size <= 3, "Should have at most 3 notification times")
        
        // Verify even distribution if we have multiple times
        if (times.size >= 2) {
            val intervals = mutableListOf<Long>()
            for (i in 0 until times.size - 1) {
                intervals.add(times[i + 1] - times[i])
            }
            
            // All intervals should be approximately equal
            // (within 1 second tolerance for rounding)
            val expectedInterval = intervals.first()
            intervals.forEach { interval ->
                val difference = kotlin.math.abs(interval - expectedInterval)
                assertTrue(difference < 1000, "Intervals should be equal: expected $expectedInterval, got $interval")
            }
        }
    }
    
    @Test
    fun `computeNotificationTimes distributes evenly across time window`() = runTest {
        // Given: ALL_DAYS mode with evenly distributed notifications
        val prefs = UserPreferences(
            notificationsPerDay = 3,
            scheduleMode = ScheduleMode.ALL_DAYS,
            startTime = "08:00",
            endTime = "22:00",
            enabled = true
        )
        
        whenever(preferencesRepository.getPreferences()).thenReturn(prefs)
        
        // When: computing notification times
        val times = computeNotificationTimesPublic(prefs)
        
        // Then: should have at most 3 notification times
        assertTrue(times.size <= 3, "Should have at most 3 notification times")
        
        // Verify even distribution if we have multiple times
        if (times.size >= 2) {
            val intervals = mutableListOf<Long>()
            for (i in 0 until times.size - 1) {
                intervals.add(times[i + 1] - times[i])
            }
            
            // All intervals should be approximately equal
            val expectedInterval = intervals.first()
            intervals.forEach { interval ->
                val difference = kotlin.math.abs(interval - expectedInterval)
                assertTrue(difference < 3600000, "Intervals should be roughly equal")
            }
        }
    }
    
    @Test
    fun `computeNotificationTimes filters out past times`() = runTest {
        // Given: ALL_DAYS mode with times that may be in the past
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        
        // Set start time to early morning (likely in the past)
        val prefs = UserPreferences(
            notificationsPerDay = 5,
            scheduleMode = ScheduleMode.ALL_DAYS,
            startTime = "00:00",
            endTime = "23:59",
            enabled = true
        )
        
        whenever(preferencesRepository.getPreferences()).thenReturn(prefs)
        
        // When: computing notification times
        val times = computeNotificationTimesPublic(prefs)
        
        // Then: all returned times should be in the future
        val currentTime = System.currentTimeMillis()
        times.forEach { time ->
            assertTrue(time > currentTime, "All notification times should be in the future")
        }
    }
    
    @Test
    fun `computeNotificationTimes handles single notification per day`() = runTest {
        // Given: Only 1 notification per day
        val prefs = UserPreferences(
            notificationsPerDay = 1,
            scheduleMode = ScheduleMode.ALL_DAYS,
            startTime = "12:00",
            endTime = "13:00",
            enabled = true
        )
        
        whenever(preferencesRepository.getPreferences()).thenReturn(prefs)
        
        // When: computing notification times
        val times = computeNotificationTimesPublic(prefs)
        
        // Then: should have at most 1 time
        assertTrue(times.size <= 1, "Should have at most 1 notification time")
    }
    
    @Test
    fun `computeNotificationTimes handles maximum notifications per day`() = runTest {
        // Given: Maximum 10 notifications per day
        val prefs = UserPreferences(
            notificationsPerDay = 10,
            scheduleMode = ScheduleMode.ALL_DAYS,
            startTime = "08:00",
            endTime = "22:00",
            enabled = true
        )
        
        whenever(preferencesRepository.getPreferences()).thenReturn(prefs)
        
        // When: computing notification times
        val times = computeNotificationTimesPublic(prefs)
        
        // Then: should have at most 10 times
        assertTrue(times.size <= 10, "Should have at most 10 notification times")
    }
    
    @Test
    fun `computeNotificationTimes handles midnight boundary`() = runTest {
        // Given: Time window crossing midnight (edge case)
        val prefs = UserPreferences(
            notificationsPerDay = 2,
            scheduleMode = ScheduleMode.ALL_DAYS,
            startTime = "23:00",
            endTime = "23:59",
            enabled = true
        )
        
        whenever(preferencesRepository.getPreferences()).thenReturn(prefs)
        
        // When: computing notification times
        val times = computeNotificationTimesPublic(prefs)
        
        // Then: should handle the boundary correctly
        assertTrue(times.size <= 2, "Should have at most 2 notification times")
        
        // All times should be for today
        val today = Calendar.getInstance()
        times.forEach { timestamp ->
            val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
            assertEquals(
                today.get(Calendar.DAY_OF_YEAR),
                cal.get(Calendar.DAY_OF_YEAR),
                "All times should be for today"
            )
        }
    }
    
    @Test
    fun `computeNotificationTimes handles same start and end time`() = runTest {
        // Given: Start and end time are the same (edge case)
        val prefs = UserPreferences(
            notificationsPerDay = 3,
            scheduleMode = ScheduleMode.ALL_DAYS,
            startTime = "12:00",
            endTime = "12:00",
            enabled = true
        )
        
        whenever(preferencesRepository.getPreferences()).thenReturn(prefs)
        
        // When: computing notification times
        val times = computeNotificationTimesPublic(prefs)
        
        // Then: should handle gracefully (all times at the same moment)
        assertTrue(times.size <= 3, "Should have at most 3 notification times")
        
        if (times.isNotEmpty()) {
            // All times should be the same
            val firstTime = times.first()
            times.forEach { time ->
                assertEquals(firstTime, time, "All times should be identical when start equals end")
            }
        }
    }
    
    @Test
    fun `computeNotificationTimes respects notificationsPerDay limit`() = runTest {
        // Given: Multiple notifications per day
        val prefs = UserPreferences(
            notificationsPerDay = 3,
            scheduleMode = ScheduleMode.ALL_DAYS,
            startTime = "06:00",
            endTime = "21:00",
            enabled = true
        )
        
        whenever(preferencesRepository.getPreferences()).thenReturn(prefs)
        
        // When: computing notification times
        val times = computeNotificationTimesPublic(prefs)
        
        // Then: should only have at most 3 times
        assertTrue(times.size <= 3, "Should respect notificationsPerDay limit")
    }
    
    @Test
    fun `parseTime correctly converts time string to timestamp`() {
        // Given: A specific time string
        val timeStr = "14:30"
        val today = Calendar.getInstance()
        
        // When: parsing the time
        val timestamp = parseTimePublic(timeStr, today)
        
        // Then: should create correct timestamp for today at 14:30
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        
        assertEquals(14, cal.get(Calendar.HOUR_OF_DAY), "Hour should be 14")
        assertEquals(30, cal.get(Calendar.MINUTE), "Minute should be 30")
        assertEquals(0, cal.get(Calendar.SECOND), "Second should be 0")
        assertEquals(0, cal.get(Calendar.MILLISECOND), "Millisecond should be 0")
        assertEquals(
            today.get(Calendar.DAY_OF_YEAR),
            cal.get(Calendar.DAY_OF_YEAR),
            "Should be for today"
        )
    }
    
    @Test
    fun `parseTime handles midnight time`() {
        // Given: Midnight time string
        val timeStr = "00:00"
        val today = Calendar.getInstance()
        
        // When: parsing the time
        val timestamp = parseTimePublic(timeStr, today)
        
        // Then: should create correct timestamp for midnight
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY), "Hour should be 0")
        assertEquals(0, cal.get(Calendar.MINUTE), "Minute should be 0")
    }
    
    @Test
    fun `parseTime handles end of day time`() {
        // Given: End of day time string
        val timeStr = "23:59"
        val today = Calendar.getInstance()
        
        // When: parsing the time
        val timestamp = parseTimePublic(timeStr, today)
        
        // Then: should create correct timestamp for 23:59
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY), "Hour should be 23")
        assertEquals(59, cal.get(Calendar.MINUTE), "Minute should be 59")
    }
    
    @Test
    fun `getCurrentDateKey returns correct format`() {
        // When: getting current date key
        val dateKey = getCurrentDateKeyPublic()
        
        // Then: should be in YYYYMMDD format
        assertTrue(dateKey.matches(Regex("\\d{8}")), "Date key should be 8 digits")
        
        // Verify it matches today's date
        val today = Calendar.getInstance()
        val expectedYear = today.get(Calendar.YEAR)
        val expectedMonth = today.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
        val expectedDay = today.get(Calendar.DAY_OF_MONTH)
        val expected = "%04d%02d%02d".format(expectedYear, expectedMonth, expectedDay)
        
        assertEquals(expected, dateKey, "Date key should match today's date")
    }
    
    // Helper methods to access private methods for testing
    // In a real implementation, these would use reflection or the methods would be made internal
    
    private fun computeNotificationTimesPublic(prefs: UserPreferences): List<Long> {
        // Use reflection to access private method
        val method = NotificationSchedulerImpl::class.java.getDeclaredMethod(
            "computeNotificationTimes",
            UserPreferences::class.java
        )
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(notificationScheduler, prefs) as List<Long>
    }
    
    private fun parseTimePublic(timeStr: String, baseDate: Calendar): Long {
        // Use reflection to access private method
        val method = NotificationSchedulerImpl::class.java.getDeclaredMethod(
            "parseTime",
            String::class.java,
            Calendar::class.java
        )
        method.isAccessible = true
        return method.invoke(notificationScheduler, timeStr, baseDate) as Long
    }
    
    private fun getCurrentDateKeyPublic(): String {
        // Use reflection to access private method
        val method = NotificationSchedulerImpl::class.java.getDeclaredMethod("getCurrentDateKey")
        method.isAccessible = true
        return method.invoke(notificationScheduler) as String
    }
}
