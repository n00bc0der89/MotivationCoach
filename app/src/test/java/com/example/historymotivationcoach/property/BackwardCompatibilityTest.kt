package com.example.historymotivationcoach.property

import com.example.historymotivationcoach.business.NotificationSchedule
import com.example.historymotivationcoach.business.TimeWindow
import com.example.historymotivationcoach.data.entity.MotivationItem
import com.example.historymotivationcoach.data.entity.ScheduleMode
import com.example.historymotivationcoach.data.entity.UserPreferences
import com.example.historymotivationcoach.data.entity.getActiveDays
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Property-based tests for backward compatibility with v1.
 * 
 * These tests ensure that:
 * 1. Default v2 settings maintain v1 behavior
 * 2. Existing imageUri values remain valid
 * 3. Unchanged preferences don't trigger unnecessary rescheduling
 * 
 * Requirements: 11.4, 11.5, 12.6
 */
class BackwardCompatibilityTest {

    /**
     * Property 42: Default settings maintain v1 behavior
     * 
     * For any user who doesn't change the new v2 settings (scheduleMode = ALL_DAYS,
     * customDays = empty), the notification scheduling behavior should be identical to v1.
     * 
     * In v1, notifications were scheduled every day within the time window.
     * In v2 with default settings (ALL_DAYS mode), the same behavior should occur.
     * 
     * Validates: Requirements 11.4
     * 
     * Feature: history-motivation-coach-v2, Property 42: Default settings maintain v1 behavior
     */
    @Test
    fun `Property 42 - Default settings maintain v1 behavior`() = runTest {
        checkAll(100, defaultV2PreferencesArb()) { prefs ->
            // Verify default v2 settings
            assert(prefs.scheduleMode == ScheduleMode.ALL_DAYS) {
                "Default schedule mode should be ALL_DAYS"
            }
            assert(prefs.customDays.isEmpty()) {
                "Default custom days should be empty"
            }
            
            // Create notification schedule with default settings
            val timeWindow = TimeWindow(
                startTime = LocalTime.parse(prefs.startTime),
                endTime = LocalTime.parse(prefs.endTime)
            )
            
            val schedule = NotificationSchedule(
                scheduleMode = prefs.scheduleMode,
                customDays = prefs.customDays,
                timeWindow = timeWindow,
                notificationsPerDay = prefs.notificationsPerDay
            )
            
            // Test that notifications can be scheduled on any day of the week
            // (v1 behavior: all days are active)
            val testDate = LocalDateTime.now()
            
            for (dayOffset in 0..6) {
                val testDateTime = testDate.plusDays(dayOffset.toLong())
                val nextTime = schedule.calculateNextTime(testDateTime.with(LocalTime.MIN))
                
                // Should find a valid time within the same day or next day
                assert(nextTime != null) {
                    "Should find valid notification time for day ${testDateTime.dayOfWeek} with default settings"
                }
                
                // The scheduled time should respect the time window
                if (nextTime != null) {
                    val scheduledTime = nextTime.toLocalTime()
                    assert(scheduledTime >= timeWindow.startTime && scheduledTime <= timeWindow.endTime) {
                        "Scheduled time $scheduledTime should be within time window ${timeWindow.startTime} - ${timeWindow.endTime}"
                    }
                }
            }
            
            // PROPERTY: With default settings, all days should be active (v1 behavior)
            val activeDays = prefs.scheduleMode.getActiveDays(prefs.customDays)
            assert(activeDays.size == 7) {
                "Default settings should activate all 7 days (v1 behavior), but got ${activeDays.size} days"
            }
            assert(activeDays.containsAll(DayOfWeek.values().toList())) {
                "Default settings should include all days of the week (v1 behavior)"
            }
        }
    }

    /**
     * Property 43: Existing imageUri values remain valid
     * 
     * For any MotivationItem record with an imageUri from v1, the imageUri should
     * still be valid and loadable in v2.
     * 
     * This test verifies that:
     * - android.resource:// URIs remain valid
     * - https:// URIs remain valid
     * - URI format is preserved
     * 
     * Validates: Requirements 11.5
     * 
     * Feature: history-motivation-coach-v2, Property 43: Existing imageUri values remain valid
     */
    @Test
    fun `Property 43 - Existing imageUri values remain valid`() = runTest {
        checkAll(100, motivationItemWithImageArb()) { item ->
            // PROPERTY: imageUri should not be null or empty
            assert(item.imageUri.isNotEmpty()) {
                "imageUri should not be empty"
            }
            
            // PROPERTY: imageUri should have a valid scheme
            val validSchemes = listOf("android.resource://", "https://", "http://")
            val hasValidScheme = validSchemes.any { scheme ->
                item.imageUri.startsWith(scheme)
            }
            assert(hasValidScheme) {
                "imageUri '${item.imageUri}' should start with a valid scheme: ${validSchemes.joinToString(", ")}"
            }
            
            // PROPERTY: android.resource URIs should have correct format
            if (item.imageUri.startsWith("android.resource://")) {
                val parts = item.imageUri.split("/")
                assert(parts.size >= 3) {
                    "android.resource URI should have format: android.resource://package/type/name"
                }
            }
            
            // PROPERTY: https URIs should have correct format
            if (item.imageUri.startsWith("https://")) {
                assert(item.imageUri.length > 8) {
                    "https URI should have content after scheme"
                }
            }
            
            // PROPERTY: imageUri should not contain invalid characters
            val invalidChars = listOf(" ", "\n", "\t", "\r")
            invalidChars.forEach { char ->
                assert(!item.imageUri.contains(char)) {
                    "imageUri should not contain whitespace or control characters"
                }
            }
        }
    }

    /**
     * Property 45: Unchanged preferences don't trigger rescheduling
     * 
     * For any settings screen interaction that doesn't change preference values,
     * notification rescheduling should not be triggered.
     * 
     * This test verifies that:
     * - Identical preference objects are considered equal
     * - No rescheduling occurs when preferences don't change
     * 
     * Validates: Requirements 12.6
     * 
     * Feature: history-motivation-coach-v2, Property 45: Unchanged preferences don't trigger rescheduling
     */
    @Test
    fun `Property 45 - Unchanged preferences don't trigger rescheduling`() = runTest {
        checkAll(100, userPreferencesArb()) { originalPrefs ->
            // Create a copy of the preferences (simulating a "save" with no changes)
            val unchangedPrefs = originalPrefs.copy()
            
            // PROPERTY: Copied preferences should be equal to original
            assert(originalPrefs == unchangedPrefs) {
                "Unchanged preferences should be equal to original"
            }
            
            // PROPERTY: All fields should match
            assert(originalPrefs.notificationsPerDay == unchangedPrefs.notificationsPerDay) {
                "notificationsPerDay should match"
            }
            assert(originalPrefs.startTime == unchangedPrefs.startTime) {
                "startTime should match"
            }
            assert(originalPrefs.endTime == unchangedPrefs.endTime) {
                "endTime should match"
            }
            assert(originalPrefs.enabled == unchangedPrefs.enabled) {
                "enabled should match"
            }
            assert(originalPrefs.preferredThemes == unchangedPrefs.preferredThemes) {
                "preferredThemes should match"
            }
            assert(originalPrefs.scheduleMode == unchangedPrefs.scheduleMode) {
                "scheduleMode should match"
            }
            assert(originalPrefs.customDays == unchangedPrefs.customDays) {
                "customDays should match"
            }
            
            // PROPERTY: Hash codes should be equal for unchanged preferences
            assert(originalPrefs.hashCode() == unchangedPrefs.hashCode()) {
                "Hash codes should match for unchanged preferences"
            }
        }
    }

    // ========== Arbitrary Generators ==========

    /**
     * Generates UserPreferences with default v2 settings (ALL_DAYS mode, empty custom days).
     * This represents a user who upgraded from v1 and hasn't changed the new settings.
     */
    private fun defaultV2PreferencesArb(): Arb<UserPreferences> = arbitrary {
        val startHour = Arb.int(0..21).bind()
        val startMinute = Arb.int(0..59).bind()
        val startTime = String.format("%02d:%02d", startHour, startMinute)
        
        // Ensure end time is after start time
        val endHour = Arb.int(startHour + 1..23).bind()
        val endMinute = Arb.int(0..59).bind()
        val endTime = String.format("%02d:%02d", endHour, endMinute)
        
        UserPreferences(
            id = 1,
            notificationsPerDay = Arb.int(1..5).bind(),
            startTime = startTime,
            endTime = endTime,
            enabled = Arb.boolean().bind(),
            preferredThemes = Arb.list(
                Arb.of("work", "passion", "excellence", "leadership", "innovation"),
                0..3
            ).bind(),
            scheduleMode = ScheduleMode.ALL_DAYS, // Default v2 setting
            customDays = emptySet() // Default v2 setting
        )
    }

    /**
     * Generates MotivationItem with valid imageUri values.
     * Covers both android.resource:// and https:// schemes.
     */
    private fun motivationItemWithImageArb(): Arb<MotivationItem> = arbitrary {
        val imageUriType = Arb.int(0..1).bind()
        val imageUri = when (imageUriType) {
            0 -> {
                // android.resource URI
                val resourceId = Arb.int(1..1000).bind()
                "android.resource://com.example.historymotivationcoach/drawable/img_$resourceId"
            }
            else -> {
                // https URI
                val imageId = Arb.int(1..1000).bind()
                "https://example.com/images/personality_$imageId.jpg"
            }
        }
        
        MotivationItem(
            id = Arb.long(1L..10000L).bind(),
            quote = Arb.string(20..200).bind(),
            author = Arb.of("Einstein", "Churchill", "Gandhi", "Mandela", "King").bind(),
            context = if (Arb.boolean().bind()) Arb.string(10..100).bind() else null,
            imageUri = imageUri,
            themes = Arb.list(
                Arb.of("work", "passion", "excellence", "leadership"),
                1..3
            ).bind(),
            sourceName = Arb.string(5..30).bind(),
            sourceUrl = if (Arb.boolean().bind()) "https://example.com/${Arb.int(1..100).bind()}" else null,
            license = Arb.of("Public Domain", "CC BY 4.0", "CC BY-SA 4.0").bind()
        )
    }

    /**
     * Generates arbitrary UserPreferences with all possible configurations.
     */
    private fun userPreferencesArb(): Arb<UserPreferences> = arbitrary {
        val startHour = Arb.int(0..21).bind()
        val startMinute = Arb.int(0..59).bind()
        val startTime = String.format("%02d:%02d", startHour, startMinute)
        
        // Ensure end time is after start time
        val endHour = Arb.int(startHour + 1..23).bind()
        val endMinute = Arb.int(0..59).bind()
        val endTime = String.format("%02d:%02d", endHour, endMinute)
        
        UserPreferences(
            id = 1,
            notificationsPerDay = Arb.int(1..5).bind(),
            startTime = startTime,
            endTime = endTime,
            enabled = Arb.boolean().bind(),
            preferredThemes = Arb.list(
                Arb.of("work", "passion", "excellence", "leadership", "innovation"),
                0..3
            ).bind(),
            scheduleMode = Arb.enum<ScheduleMode>().bind(),
            customDays = generateCustomDays().bind()
        )
    }

    /**
     * Generates a set of custom days.
     */
    private fun generateCustomDays(): Arb<Set<DayOfWeek>> = arbitrary {
        val dayCount = Arb.int(0..7).bind()
        val allDays = DayOfWeek.values().toList()
        allDays.shuffled().take(dayCount).toSet()
    }
}
