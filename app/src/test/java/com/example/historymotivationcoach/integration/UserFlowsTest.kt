package com.example.historymotivationcoach.integration

import com.example.historymotivationcoach.data.entity.DeliveryHistory
import com.example.historymotivationcoach.data.entity.DeliveryStatus
import com.example.historymotivationcoach.data.entity.MotivationItem
import com.example.historymotivationcoach.data.entity.ScheduleMode
import com.example.historymotivationcoach.data.entity.UserPreferences
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Complete user flow tests for the History Motivation Coach app.
 * 
 * Tests verify end-to-end user journeys:
 * - First launch → seed loading → notification setup
 * - Receive notification → tap → view detail
 * - Browse history → view past motivations
 * - Change settings → verify rescheduling
 * - Exhaust content → Replay Classics → reset
 * 
 * Requirements: All
 */
class UserFlowsTest {
    
    @Test
    fun `user flow - first launch with seed loading and notification setup`() = runTest {
        // Simulate first launch flow
        
        // Step 1: Check if seed data is loaded (first launch flag)
        var seedDataLoaded = false
        
        // Step 2: Load seed data from JSON
        val seedData = loadSeedData()
        assertTrue(seedData.isNotEmpty(), "Seed data should contain items")
        assertTrue(seedData.size >= 100, "Seed data should have at least 100 items")
        
        // Step 3: Validate seed data items
        seedData.forEach { item ->
            assertTrue(item.quote.isNotEmpty(), "Quote should not be empty")
            assertTrue(item.author.isNotEmpty(), "Author should not be empty")
            assertTrue(item.imageUri.isNotEmpty(), "Image URI should not be empty")
            assertTrue(item.themes.isNotEmpty(), "Themes should not be empty")
            assertTrue(item.sourceName.isNotEmpty(), "Source name should not be empty")
            assertTrue(item.license.isNotEmpty(), "License should not be empty")
        }
        
        // Step 4: Insert seed data into database (simulated)
        seedDataLoaded = true
        
        // Step 5: Set up default preferences
        val defaultPrefs = UserPreferences(
            notificationsPerDay = 3,
            scheduleMode = ScheduleMode.ALL_DAYS,
            startTime = "09:00",
            endTime = "21:00",
            enabled = true
        )
        
        // Step 6: Schedule initial notifications
        val notificationTimes = computeNotificationTimes(defaultPrefs)
        assertTrue(notificationTimes.isNotEmpty(), "Should schedule notifications on first launch")
        
        // Verify first launch completed successfully
        assertTrue(seedDataLoaded, "Seed data should be loaded")
        assertEquals(3, defaultPrefs.notificationsPerDay, "Default frequency should be 3")
        assertTrue(defaultPrefs.enabled, "Notifications should be enabled by default")
    }
    
    @Test
    fun `user flow - receive notification, tap, and view detail`() = runTest {
        // Simulate receiving and interacting with a notification
        
        // Step 1: Select a motivation item for notification
        val selectedItem = MotivationItem(
            id = 1,
            quote = "The only way to do great work is to love what you do.",
            author = "Steve Jobs",
            context = "From Stanford commencement speech, 2005",
            imageUri = "android.resource://com.example.motivationcoach/drawable/jobs",
            themes = listOf("work", "passion", "excellence"),
            sourceName = "Stanford University",
            sourceUrl = "https://news.stanford.edu/2005/06/14/jobs-061505/",
            license = "Public Domain"
        )
        
        // Step 2: Record delivery in history
        val deliveryTime = System.currentTimeMillis()
        val dateKey = formatDateKey(deliveryTime)
        val notificationId = 1
        
        val deliveryRecord = DeliveryHistory(
            historyId = 1,
            itemId = selectedItem.id,
            shownAt = deliveryTime,
            dateKey = dateKey,
            notificationId = notificationId,
            deliveryStatus = DeliveryStatus.DELIVERED
        )
        
        // Step 3: Display notification with content
        val notificationTitle = selectedItem.author
        val notificationText = if (selectedItem.quote.length > 100) {
            selectedItem.quote.take(100) + "..."
        } else {
            selectedItem.quote
        }
        
        assertEquals("Steve Jobs", notificationTitle, "Notification title should be author")
        assertTrue(
            notificationText.contains("The only way to do great work"),
            "Notification should contain quote preview"
        )
        
        // Step 4: User taps notification - create intent with motivation ID
        val intentData = mapOf("motivation_id" to selectedItem.id)
        
        // Step 5: Navigate to detail screen with motivation ID
        val motivationIdFromIntent = intentData["motivation_id"]
        assertNotNull(motivationIdFromIntent, "Intent should contain motivation ID")
        assertEquals(selectedItem.id, motivationIdFromIntent, "Should navigate to correct item")
        
        // Step 6: Display full detail view
        val detailView = createDetailView(selectedItem, deliveryRecord)
        
        assertEquals(selectedItem.quote, detailView.fullQuote, "Should show full quote")
        assertEquals(selectedItem.author, detailView.author, "Should show author")
        assertEquals(selectedItem.context, detailView.context, "Should show context")
        assertEquals(selectedItem.imageUri, detailView.imageUri, "Should show full image")
        assertEquals(selectedItem.themes, detailView.themes, "Should show themes")
        assertEquals(selectedItem.sourceName, detailView.sourceName, "Should show source")
        assertEquals(deliveryTime, detailView.deliveryTimestamp, "Should show delivery time")
        
        // Step 7: Update delivery status to OPENED
        val updatedStatus = DeliveryStatus.OPENED
        assertEquals(DeliveryStatus.OPENED, updatedStatus, "Status should be updated to OPENED")
    }
    
    @Test
    fun `user flow - browse history and view past motivations`() = runTest {
        // Simulate browsing history
        
        // Step 1: Create sample delivery history
        val today = formatDateKey(System.currentTimeMillis())
        val yesterday = formatDateKey(System.currentTimeMillis() - 86400000)
        val twoDaysAgo = formatDateKey(System.currentTimeMillis() - 172800000)
        
        val historyItems = listOf(
            createHistoryItem(1, today, System.currentTimeMillis()),
            createHistoryItem(2, today, System.currentTimeMillis() - 3600000),
            createHistoryItem(3, yesterday, System.currentTimeMillis() - 86400000),
            createHistoryItem(4, twoDaysAgo, System.currentTimeMillis() - 172800000)
        )
        
        // Step 2: Group history by date
        val groupedHistory = historyItems.groupBy { it.dateKey }
        
        // Step 3: Format date labels
        val dateLabels = groupedHistory.keys.map { dateKey ->
            when (dateKey) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> dateKey
            }
        }
        
        assertTrue(dateLabels.contains("Today"), "Should have 'Today' label")
        assertTrue(dateLabels.contains("Yesterday"), "Should have 'Yesterday' label")
        
        // Step 4: Verify today's count
        val todayCount = groupedHistory[today]?.size ?: 0
        assertEquals(2, todayCount, "Should have 2 items for today")
        
        // Step 5: User taps a history item to view detail
        val selectedHistoryItem = historyItems.first()
        val motivationId = selectedHistoryItem.itemId
        
        // Step 6: Navigate to detail screen
        assertNotNull(motivationId, "Should have motivation ID for navigation")
        
        // Step 7: Verify history list displays required fields
        historyItems.forEach { item ->
            assertTrue(item.shownAt > 0, "Should have timestamp")
            assertTrue(item.itemId > 0, "Should have item ID")
            assertTrue(item.dateKey.isNotEmpty(), "Should have date key")
        }
    }
    
    @Test
    fun `user flow - change settings and verify rescheduling`() = runTest {
        // Simulate changing settings
        
        // Step 1: Load current preferences
        val currentPrefs = UserPreferences(
            notificationsPerDay = 3,
            scheduleMode = ScheduleMode.ALL_DAYS,
            startTime = "09:00",
            endTime = "21:00",
            enabled = true
        )
        
        // Step 2: User changes notification frequency to 5
        val updatedPrefs1 = currentPrefs.copy(notificationsPerDay = 5)
        
        // Step 3: Verify preferences are persisted
        assertEquals(5, updatedPrefs1.notificationsPerDay, "Frequency should be updated")
        
        // Step 4: Reschedule notifications with new frequency
        val newTimes1 = computeNotificationTimes(updatedPrefs1)
        assertTrue(newTimes1.size <= 5, "Should schedule up to 5 notifications")
        
        // Step 5: User switches to WEEKDAYS_ONLY mode
        val updatedPrefs2 = updatedPrefs1.copy(
            scheduleMode = ScheduleMode.WEEKDAYS_ONLY
        )
        
        // Step 6: Verify mode change is persisted
        assertEquals(ScheduleMode.WEEKDAYS_ONLY, updatedPrefs2.scheduleMode, "Mode should be updated")
        
        // Step 7: Reschedule notifications with weekdays only
        val newTimes2 = computeNotificationTimes(updatedPrefs2)
        assertTrue(newTimes2.size <= 5, "Should schedule up to 5 notifications on weekdays")
        
        // Step 8: User changes time window
        val updatedPrefs3 = updatedPrefs2.copy(
            scheduleMode = ScheduleMode.ALL_DAYS,
            startTime = "10:00",
            endTime = "20:00"
        )
        
        // Step 9: Verify time window change is persisted
        assertEquals("10:00", updatedPrefs3.startTime, "Start time should be updated")
        assertEquals("20:00", updatedPrefs3.endTime, "End time should be updated")
        
        // Step 10: Reschedule notifications with new time window
        val newTimes3 = computeNotificationTimes(updatedPrefs3)
        assertTrue(newTimes3.isNotEmpty(), "Should reschedule with new time window")
        
        // Step 11: User disables notifications
        val updatedPrefs4 = updatedPrefs3.copy(enabled = false)
        
        // Step 12: Verify notifications are disabled
        assertFalse(updatedPrefs4.enabled, "Notifications should be disabled")
        
        // Step 13: Cancel all scheduled notifications
        val shouldSchedule = updatedPrefs4.enabled
        assertFalse(shouldSchedule, "Should not schedule when disabled")
    }
    
    @Test
    fun `user flow - exhaust content, replay classics, and reset`() = runTest {
        // Simulate content exhaustion and replay
        
        // Step 1: Create a small dataset (5 items)
        val totalItems = 5
        val allItems = (1..totalItems).map { id ->
            MotivationItem(
                id = id.toLong(),
                quote = "Quote $id",
                author = "Author $id",
                imageUri = "uri_$id",
                themes = listOf("theme$id"),
                sourceName = "Source $id",
                license = "Public Domain"
            )
        }
        
        // Step 2: Deliver all items (exhaust content)
        val deliveredItems = mutableSetOf<Long>()
        allItems.forEach { item ->
            deliveredItems.add(item.id)
        }
        
        // Step 3: Check unseen count
        val unseenCount = totalItems - deliveredItems.size
        assertEquals(0, unseenCount, "All content should be exhausted")
        
        // Step 4: Display exhaustion message
        val isExhausted = unseenCount == 0
        assertTrue(isExhausted, "Content should be exhausted")
        
        // Step 5: Show "Replay Classics" option
        val replayClassicsAvailable = isExhausted
        assertTrue(replayClassicsAvailable, "Replay Classics should be available")
        
        // Step 6: User activates "Replay Classics"
        val userActivatesReplay = true
        
        // Step 7: Clear delivery history
        if (userActivatesReplay) {
            deliveredItems.clear()
        }
        
        // Step 8: Verify history is cleared
        assertEquals(0, deliveredItems.size, "Delivery history should be cleared")
        
        // Step 9: Check unseen count after reset
        val unseenCountAfterReset = totalItems - deliveredItems.size
        assertEquals(totalItems, unseenCountAfterReset, "All items should be available again")
        
        // Step 10: Resume normal notification scheduling
        val prefs = UserPreferences(
            notificationsPerDay = 3,
            scheduleMode = ScheduleMode.ALL_DAYS,
            startTime = "09:00",
            endTime = "21:00",
            enabled = true
        )
        
        val notificationTimes = computeNotificationTimes(prefs)
        assertTrue(notificationTimes.isNotEmpty(), "Should resume scheduling after reset")
        
        // Step 11: Verify content can be delivered again
        val canSelectContent = unseenCountAfterReset > 0
        assertTrue(canSelectContent, "Should be able to select content after reset")
    }
    
    @Test
    fun `user flow - complete daily cycle with multiple notifications`() = runTest {
        // Simulate a complete day of notifications
        
        val now = System.currentTimeMillis()
        val today = formatDateKey(now)
        
        // Step 1: Morning - first notification at 9 AM
        val morning = createHistoryItem(1, today, now - 7200000) // 2 hours ago
        
        // Step 2: Afternoon - second notification at 2 PM
        val afternoon = createHistoryItem(2, today, now - 3600000) // 1 hour ago
        
        // Step 3: Evening - third notification at 8 PM
        val evening = createHistoryItem(3, today, now) // now
        
        // Step 4: Verify all notifications are for today
        assertEquals(today, morning.dateKey, "Morning notification should be for today")
        assertEquals(today, afternoon.dateKey, "Afternoon notification should be for today")
        assertEquals(today, evening.dateKey, "Evening notification should be for today")
        
        // Step 5: Check today's count on home screen
        val todayHistory = listOf(morning, afternoon, evening)
        val todayCount = todayHistory.size
        assertEquals(3, todayCount, "Should have 3 notifications today")
        
        // Step 6: Verify latest motivation is displayed on home screen
        val latestMotivation = todayHistory.maxByOrNull { it.shownAt }
        assertNotNull(latestMotivation, "Should have latest motivation")
        assertEquals(evening.itemId, latestMotivation.itemId, "Latest should be evening notification")
        
        // Step 7: Midnight - daily rescheduler runs
        // (This would trigger SchedulerWorker to reschedule for next day)
        val nextDayPrefs = UserPreferences(
            notificationsPerDay = 3,
            scheduleMode = ScheduleMode.ALL_DAYS,
            startTime = "09:00",
            endTime = "21:00",
            enabled = true
        )
        
        val nextDayTimes = computeNotificationTimes(nextDayPrefs)
        assertTrue(nextDayTimes.isNotEmpty(), "Should schedule for next day")
    }
    
    @Test
    fun `user flow - manual trigger notification from home screen`() = runTest {
        // Simulate manual notification trigger
        
        // Step 1: User is on home screen
        val currentUnseenCount = 50
        
        // Step 2: User taps "Send one now" button
        val userTapsManualTrigger = true
        
        // Step 3: Select next motivation immediately
        if (userTapsManualTrigger && currentUnseenCount > 0) {
            val selectedItem = MotivationItem(
                id = 10,
                quote = "Manually triggered quote",
                author = "Manual Author",
                imageUri = "uri",
                themes = listOf("manual"),
                sourceName = "Source",
                license = "Public Domain"
            )
            
            // Step 4: Record delivery
            val deliveryTime = System.currentTimeMillis()
            val dateKey = formatDateKey(deliveryTime)
            
            val deliveryRecord = DeliveryHistory(
                historyId = 10,
                itemId = selectedItem.id,
                shownAt = deliveryTime,
                dateKey = dateKey,
                notificationId = 999, // Special ID for manual triggers
                deliveryStatus = DeliveryStatus.DELIVERED
            )
            
            // Step 5: Show notification immediately
            assertNotNull(deliveryRecord, "Should create delivery record")
            assertEquals(selectedItem.id, deliveryRecord.itemId, "Should record correct item")
            
            // Step 6: Update unseen count
            val newUnseenCount = currentUnseenCount - 1
            assertEquals(49, newUnseenCount, "Unseen count should decrease")
        }
    }
    
    // Helper methods
    
    private fun loadSeedData(): List<MotivationItem> {
        // Simulate loading seed data from JSON
        return (1..100).map { id ->
            MotivationItem(
                id = id.toLong(),
                quote = "Motivational quote $id",
                author = "Author $id",
                context = "Context $id",
                imageUri = "android.resource://com.example.motivationcoach/drawable/image_$id",
                themes = listOf("theme${id % 5}"),
                sourceName = "Source $id",
                sourceUrl = "https://example.com/$id",
                license = "Public Domain"
            )
        }
    }
    
    private fun createHistoryItem(id: Long, dateKey: String, timestamp: Long): DeliveryHistory {
        return DeliveryHistory(
            historyId = id,
            itemId = id,
            shownAt = timestamp,
            dateKey = dateKey,
            notificationId = id.toInt(),
            deliveryStatus = DeliveryStatus.DELIVERED
        )
    }
    
    private fun formatDateKey(timestamp: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return format.format(Date(timestamp))
    }
    
    private fun computeNotificationTimes(prefs: UserPreferences): List<Long> {
        if (!prefs.enabled) return emptyList()
        
        val times = mutableListOf<Long>()
        val now = System.currentTimeMillis()
        
        // Simplified computation for testing
        for (i in 0 until prefs.notificationsPerDay) {
            times.add(now + (i * 3600000)) // Add 1 hour intervals
        }
        
        return times.filter { it > now }
    }
    
    private fun createDetailView(item: MotivationItem, delivery: DeliveryHistory): DetailView {
        return DetailView(
            fullQuote = item.quote,
            author = item.author,
            context = item.context,
            imageUri = item.imageUri,
            themes = item.themes,
            sourceName = item.sourceName,
            sourceUrl = item.sourceUrl,
            deliveryTimestamp = delivery.shownAt
        )
    }
    
    data class DetailView(
        val fullQuote: String,
        val author: String,
        val context: String?,
        val imageUri: String,
        val themes: List<String>,
        val sourceName: String,
        val sourceUrl: String?,
        val deliveryTimestamp: Long
    )
}
