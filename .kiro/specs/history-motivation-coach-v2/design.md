# Design Document: History Motivation Coach v2

## Overview

Version 2 of the History Motivation Coach enhances the user experience with three major feature sets: personality image display, home page redesign with manual notifications, and advanced scheduling capabilities. The design maintains the existing architecture while extending the data model, UI components, and scheduling logic.

The implementation leverages existing infrastructure:
- **Data Layer**: Extends UserPreferences entity with schedule mode fields; MotivationItem already has imageUri support
- **UI Layer**: Enhances HomeScreen with new design and manual trigger button; extends SettingsScreen with schedule configuration
- **Business Logic**: Extends notification scheduling to support schedule modes and time windows
- **Image Loading**: Integrates Coil library for efficient image loading and caching

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  HomeScreen  │  │HistoryScreen │  │SettingsScreen│      │
│  │  (Enhanced)  │  │  (Enhanced)  │  │  (Enhanced)  │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                  │                  │              │
│  ┌──────▼──────────────────▼──────────────────▼───────┐    │
│  │              ViewModels Layer                       │    │
│  │  HomeViewModel │ HistoryViewModel │ SettingsViewModel│   │
│  └──────┬──────────────────┬──────────────────┬───────┘    │
└─────────┼──────────────────┼──────────────────┼────────────┘
          │                  │                  │
┌─────────▼──────────────────▼──────────────────▼────────────┐
│                    Business Logic Layer                     │
│  ┌────────────────────┐  ┌──────────────────────────┐      │
│  │MotivationRepository│  │  PreferencesRepository   │      │
│  └────────┬───────────┘  └──────────┬───────────────┘      │
│           │                          │                      │
│  ┌────────▼──────────────────────────▼───────────┐         │
│  │      NotificationScheduler (Enhanced)         │         │
│  │  - Schedule Mode Logic                        │         │
│  │  - Time Window Calculation                    │         │
│  │  - WorkManager Integration                    │         │
│  └────────┬──────────────────────────────────────┘         │
└───────────┼──────────────────────────────────────────────────┘
            │
┌───────────▼──────────────────────────────────────────────────┐
│                      Data Layer                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │MotivationItem│  │DeliveryHistory│ │UserPreferences│       │
│  │  (Existing)  │  │  (Existing)   │  │  (Extended)  │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
│                                                               │
│  ┌──────────────────────────────────────────────────┐       │
│  │            Room Database (v2)                     │       │
│  │  - Migration from v1 to v2                        │       │
│  └──────────────────────────────────────────────────┘       │
└───────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────┐
│                   External Dependencies                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │    Coil      │  │ WorkManager  │  │   Material3  │        │
│  │Image Loading │  │  Scheduling  │  │  UI Components│       │
│  └──────────────┘  └──────────────┘  └──────────────┘        │
└───────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

1. **UI Layer**
   - HomeScreen: Displays personality images, provides manual notification trigger
   - HistoryScreen: Shows thumbnail personality images in history list
   - SettingsScreen: Provides schedule mode and time window configuration

2. **ViewModel Layer**
   - HomeViewModel: Manages manual notification triggering, image loading state
   - SettingsViewModel: Handles schedule mode changes, validates time windows

3. **Business Logic Layer**
   - NotificationScheduler: Calculates notification times based on schedule mode and time windows
   - MotivationRepository: Provides motivation selection logic (unchanged)
   - PreferencesRepository: Manages schedule mode and time window preferences

4. **Data Layer**
   - UserPreferences: Extended with scheduleMode, customDays fields
   - Room Database: Migrated to version 2 with schema changes

## Components and Interfaces

### 1. Enhanced UserPreferences Entity

```kotlin
@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey
    val id: Int = 1,
    
    // Existing fields
    @ColumnInfo(name = "notifications_per_day")
    val notificationsPerDay: Int = 3,
    
    @ColumnInfo(name = "start_time")
    val startTime: String = "09:00",
    
    @ColumnInfo(name = "end_time")
    val endTime: String = "21:00",
    
    @ColumnInfo(name = "enabled")
    val enabled: Boolean = true,
    
    @ColumnInfo(name = "preferred_themes")
    val preferredThemes: List<String> = emptyList(),
    
    // New fields for v2
    @ColumnInfo(name = "schedule_mode")
    val scheduleMode: ScheduleMode = ScheduleMode.ALL_DAYS,
    
    @ColumnInfo(name = "custom_days")
    val customDays: Set<DayOfWeek> = emptySet()
)

enum class ScheduleMode {
    ALL_DAYS,
    WEEKDAYS_ONLY,
    WEEKENDS_ONLY,
    CUSTOM_DAYS
}
```

**Changes from v1:**
- Removed `scheduleMode: ScheduleMode.TIME_WINDOW` (replaced with new schedule modes)
- Removed `fixedTimes: List<String>` (not needed for v2 requirements)
- Added `scheduleMode` with new enum values
- Added `customDays` for custom day selection

### 2. NotificationScheduler Interface

```kotlin
interface NotificationScheduler {
    /**
     * Schedules the next notification based on current preferences
     * Applies schedule mode and time window constraints
     */
    suspend fun scheduleNextNotification()
    
    /**
     * Reschedules all pending notifications
     * Called when preferences change
     */
    suspend fun rescheduleAllNotifications()
    
    /**
     * Cancels all pending notifications
     */
    suspend fun cancelAllNotifications()
    
    /**
     * Triggers a manual notification immediately
     * Returns the delivered motivation ID or null if failed
     */
    suspend fun triggerManualNotification(): Long?
    
    /**
     * Calculates the next valid notification time
     * based on schedule mode and time window
     */
    fun calculateNextNotificationTime(
        preferences: UserPreferences,
        fromTime: LocalDateTime = LocalDateTime.now()
    ): LocalDateTime?
}
```

### 3. Enhanced HomeViewModel

```kotlin
class HomeViewModel(
    private val motivationRepository: MotivationRepository,
    private val preferencesRepository: PreferencesRepository,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {
    
    val uiState: StateFlow<HomeUiState>
    val manualTriggerState: StateFlow<ManualTriggerState>
    
    /**
     * Triggers a manual notification
     * Updates UI state based on result
     */
    fun triggerManualNotification()
    
    /**
     * Refreshes the home screen after manual trigger
     */
    fun refreshLatestMotivation()
}

sealed class ManualTriggerState {
    object Idle : ManualTriggerState()
    object Loading : ManualTriggerState()
    data class Success(val motivationId: Long) : ManualTriggerState()
    data class Error(val message: String) : ManualTriggerState()
}
```

### 4. Enhanced SettingsViewModel

```kotlin
class SettingsViewModel(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {
    
    val preferences: StateFlow<UserPreferences>
    val validationState: StateFlow<ValidationState>
    
    /**
     * Updates the schedule mode
     * Triggers notification rescheduling
     */
    fun updateScheduleMode(mode: ScheduleMode)
    
    /**
     * Updates custom days selection
     * Only applicable when schedule mode is CUSTOM_DAYS
     */
    fun updateCustomDays(days: Set<DayOfWeek>)
    
    /**
     * Updates time window
     * Validates start < end
     */
    fun updateTimeWindow(startTime: String, endTime: String)
    
    /**
     * Validates time window configuration
     * Returns validation errors if any
     */
    fun validateTimeWindow(
        startTime: String,
        endTime: String,
        notificationsPerDay: Int
    ): ValidationResult
}

sealed class ValidationState {
    object Valid : ValidationState()
    data class Invalid(val errors: List<String>) : ValidationState()
}

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
)
```

### 5. Image Loading Component

```kotlin
/**
 * Composable for loading and displaying personality images
 * Uses Coil for efficient loading and caching
 */
@Composable
fun PersonalityImage(
    imageUri: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUri)
            .crossfade(true)
            .placeholder(R.drawable.placeholder_personality)
            .error(R.drawable.placeholder_personality)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}
```

## Data Models

### Schedule Mode Logic

The schedule mode determines which days of the week notifications should be sent:

```kotlin
fun ScheduleMode.getActiveDays(customDays: Set<DayOfWeek>): Set<DayOfWeek> {
    return when (this) {
        ScheduleMode.ALL_DAYS -> DayOfWeek.values().toSet()
        ScheduleMode.WEEKDAYS_ONLY -> setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )
        ScheduleMode.WEEKENDS_ONLY -> setOf(
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )
        ScheduleMode.CUSTOM_DAYS -> customDays
    }
}
```

### Time Window Calculation

```kotlin
data class TimeWindow(
    val startTime: LocalTime,
    val endTime: LocalTime
) {
    /**
     * Calculates evenly distributed notification times
     * within the time window
     */
    fun calculateNotificationTimes(count: Int): List<LocalTime> {
        require(count > 0) { "Count must be positive" }
        require(startTime < endTime) { "Start time must be before end time" }
        
        val totalMinutes = ChronoUnit.MINUTES.between(startTime, endTime)
        val interval = totalMinutes / count
        
        return (0 until count).map { index ->
            startTime.plusMinutes(interval * index + interval / 2)
        }
    }
    
    /**
     * Checks if the time window is wide enough
     * for the requested number of notifications
     */
    fun isWideEnough(count: Int, minIntervalMinutes: Int = 30): Boolean {
        val totalMinutes = ChronoUnit.MINUTES.between(startTime, endTime)
        return totalMinutes >= (count * minIntervalMinutes)
    }
}
```

### Notification Time Calculation

```kotlin
data class NotificationSchedule(
    val scheduleMode: ScheduleMode,
    val customDays: Set<DayOfWeek>,
    val timeWindow: TimeWindow,
    val notificationsPerDay: Int
) {
    /**
     * Calculates the next notification time from a given starting point
     * Returns null if no valid time can be found within reasonable future
     */
    fun calculateNextTime(from: LocalDateTime): LocalDateTime? {
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
```

### Database Migration

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new columns with default values
        database.execSQL(
            """
            ALTER TABLE user_preferences 
            ADD COLUMN schedule_mode TEXT NOT NULL DEFAULT 'ALL_DAYS'
            """
        )
        
        database.execSQL(
            """
            ALTER TABLE user_preferences 
            ADD COLUMN custom_days TEXT NOT NULL DEFAULT '[]'
            """
        )
        
        // Remove old columns that are no longer needed
        // Note: SQLite doesn't support DROP COLUMN directly
        // We need to create a new table and copy data
        
        database.execSQL(
            """
            CREATE TABLE user_preferences_new (
                id INTEGER PRIMARY KEY NOT NULL,
                notifications_per_day INTEGER NOT NULL,
                start_time TEXT NOT NULL,
                end_time TEXT NOT NULL,
                enabled INTEGER NOT NULL,
                preferred_themes TEXT NOT NULL,
                schedule_mode TEXT NOT NULL,
                custom_days TEXT NOT NULL
            )
            """
        )
        
        database.execSQL(
            """
            INSERT INTO user_preferences_new 
            SELECT id, notifications_per_day, start_time, end_time, 
                   enabled, preferred_themes, 'ALL_DAYS', '[]'
            FROM user_preferences
            """
        )
        
        database.execSQL("DROP TABLE user_preferences")
        database.execSQL("ALTER TABLE user_preferences_new RENAME TO user_preferences")
    }
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*


### Property Reflection

After analyzing all acceptance criteria, I identified several redundant properties that can be consolidated:

**Redundancies Identified:**
1. Properties 5.2 and 5.3 (start/end time validation) can be combined into one comprehensive time window validation property
2. Properties 1.5, 9.6, and 12.1 (image caching) are all testing the same behavior
3. Properties 4.8, 5.8, 6.5, and 7.6 (rescheduling on preference change) are all testing the same behavior
4. Properties 6.1 and 7.2 (applying both constraints) are testing the same behavior
5. Properties 10.2, 10.3, and 11.3 (migration with defaults) can be combined
6. Properties 11.1 and 11.2 (preserving records during migration) can be combined

**Consolidated Properties:**
- Time window validation: One property covering start < end validation
- Image caching: One property covering cache behavior for all image sources
- Preference change rescheduling: One property covering all preference changes
- Constraint application: One property covering both schedule mode and time window
- Migration preservation: One property covering all data preservation during migration

### Correctness Properties

Property 1: Image loading with valid URI
*For any* motivation with a valid imageUri, loading the image should succeed and display the image content
**Validates: Requirements 1.1**

Property 2: Image loading placeholder during load
*For any* motivation being loaded, the UI state should include a loading indicator until the image loads or fails
**Validates: Requirements 1.2**

Property 3: Image loading fallback on error
*For any* motivation with an invalid or unreachable imageUri, the system should display a fallback placeholder image without crashing
**Validates: Requirements 1.3, 9.5**

Property 4: Image caching reduces network requests
*For any* image loaded twice, the second load should use the cached version and not make a network request
**Validates: Requirements 1.5, 9.6, 12.1**

Property 5: Image size varies by context
*For any* motivation, the image displayed in the detail screen should be larger than the image displayed in the home screen, which should be larger than the thumbnail in the history screen
**Validates: Requirements 1.7, 1.8**

Property 6: Home screen displays latest motivation with image
*For any* home screen state with available motivations, the latest motivation and its personality image should both be present in the UI
**Validates: Requirements 2.2**

Property 7: Home screen displays accurate counts
*For any* home screen state, the displayed today's count should equal the number of deliveries today, and the unseen count should equal the number of undelivered motivations
**Validates: Requirements 2.3**

Property 8: Home screen text contrast meets accessibility standards
*For any* text element on the home screen, the contrast ratio between text and background should meet WCAG AA standards (minimum 4.5:1 for normal text)
**Validates: Requirements 2.6**

Property 9: Home screen renders on different configurations
*For any* screen size and orientation combination, the home screen should render without errors or layout issues
**Validates: Requirements 2.7**

Property 10: Manual notification delivers new motivation
*For any* manual notification trigger when unseen motivations exist, a new motivation should be selected and delivered
**Validates: Requirements 3.2**

Property 11: Manual and scheduled notifications use same selection logic
*For any* motivation selection, whether triggered manually or by schedule, the selection algorithm should be identical
**Validates: Requirements 3.3**

Property 12: Manual notification recorded in history
*For any* manual notification delivered, a corresponding DeliveryHistory record should be created
**Validates: Requirements 3.4**

Property 13: Manual notification updates home screen
*For any* manual notification trigger, the home screen UI state should update to display the newly delivered motivation
**Validates: Requirements 3.5**

Property 14: Manual notification doesn't affect scheduled notifications
*For any* manual notification trigger, the scheduled notification times should remain unchanged
**Validates: Requirements 3.7**

Property 15: Schedule mode persisted to database
*For any* schedule mode selection, the value should be persisted to UserPreferences in the database
**Validates: Requirements 4.2**

Property 16: All Days mode activates all days
*For any* schedule with ALL_DAYS mode, all seven days of the week should be active for notifications
**Validates: Requirements 4.3**

Property 17: Weekdays Only mode activates Monday-Friday
*For any* schedule with WEEKDAYS_ONLY mode, only Monday through Friday should be active for notifications
**Validates: Requirements 4.4**

Property 18: Weekends Only mode activates Saturday-Sunday
*For any* schedule with WEEKENDS_ONLY mode, only Saturday and Sunday should be active for notifications
**Validates: Requirements 4.5**

Property 19: Custom Days mode respects user selection
*For any* schedule with CUSTOM_DAYS mode, only the user-selected days should be active for notifications
**Validates: Requirements 4.6**

Property 20: Scheduled notifications respect schedule mode
*For any* scheduled notification time, the day of week should be active according to the current schedule mode
**Validates: Requirements 4.7**

Property 21: Time window validation enforces start before end
*For any* time window configuration, if start time is not before end time, validation should fail with an error message
**Validates: Requirements 5.2, 5.3**

Property 22: Time window persisted to database
*For any* time window configuration, the start and end times should be persisted to UserPreferences in the database
**Validates: Requirements 5.4**

Property 23: Scheduled notifications within time window
*For any* scheduled notification time, the time of day should fall within the configured time window
**Validates: Requirements 5.5**

Property 24: Notifications evenly distributed in time window
*For any* set of scheduled notification times on a single day, the times should be evenly spaced within the time window
**Validates: Requirements 5.6**

Property 25: Preference changes trigger rescheduling
*For any* change to schedule mode, time window, or notificationsPerDay, all pending notifications should be cancelled and rescheduled according to the new preferences
**Validates: Requirements 4.8, 5.8, 6.5, 7.6**

Property 26: Both constraints applied to scheduled notifications
*For any* scheduled notification, both the schedule mode (day-of-week) constraint and the time window (time-of-day) constraint should be satisfied
**Validates: Requirements 6.1, 7.2**

Property 27: Excluded days have no notifications
*For any* day excluded by the schedule mode, no notifications should be scheduled for that day
**Validates: Requirements 6.2**

Property 28: Included days have notifications in time window
*For any* day included by the schedule mode, notifications should be scheduled within the time window for that day
**Validates: Requirements 6.3**

Property 29: Next notification scheduled after delivery
*For any* notification delivery, a new notification should be automatically scheduled according to current preferences
**Validates: Requirements 7.4**

Property 30: Notifications rescheduled after reboot
*For any* simulated device reboot, notifications should be rescheduled based on persisted preferences
**Validates: Requirements 7.5**

Property 31: Disabling app cancels notifications
*For any* app disable action, all pending WorkManager notification requests should be cancelled
**Validates: Requirements 7.7**

Property 32: Custom days UI appears for custom mode
*For any* settings screen state with CUSTOM_DAYS mode selected, the day-of-week selection UI should be visible
**Validates: Requirements 8.2**

Property 33: Current settings displayed correctly
*For any* settings screen state, the displayed schedule mode and time window should match the values in UserPreferences
**Validates: Requirements 8.4**

Property 34: Invalid settings trigger error messages
*For any* invalid settings input (e.g., start time after end time, time window too narrow), an error message should be displayed
**Validates: Requirements 8.6**

Property 35: Existing settings preserved in v2
*For any* settings screen in v2, the notificationsPerDay and preferredThemes settings should still be present and functional
**Validates: Requirements 8.7**

Property 36: Drawable resource URIs load successfully
*For any* imageUri with android.resource:// scheme, the image should load successfully
**Validates: Requirements 9.1**

Property 37: HTTPS URIs load successfully
*For any* imageUri with https:// scheme, the image should load successfully
**Validates: Requirements 9.2**

Property 38: Null or empty imageUri shows placeholder
*For any* motivation with null or empty imageUri, the placeholder image should be displayed
**Validates: Requirements 9.4**

Property 39: Migration adds new fields with defaults
*For any* UserPreferences record migrated from v1 to v2, the new fields (scheduleMode, customDays) should be present with default values (ALL_DAYS, empty set)
**Validates: Requirements 10.2, 10.3, 11.3**

Property 40: Migration error handling
*For any* migration error, the system should catch the error and handle it gracefully without crashing
**Validates: Requirements 10.6**

Property 41: All data preserved during migration
*For any* database migration from v1 to v2, all MotivationItem records, DeliveryHistory records, and existing UserPreferences fields should be preserved unchanged
**Validates: Requirements 11.1, 11.2, 11.3**

Property 42: Default settings maintain v1 behavior
*For any* user who doesn't change the new v2 settings, the notification scheduling behavior should be identical to v1
**Validates: Requirements 11.4**

Property 43: Existing imageUri values remain valid
*For any* MotivationItem record with an imageUri from v1, the imageUri should still load successfully in v2
**Validates: Requirements 11.5**

Property 44: Image loading doesn't block UI thread
*For any* image loading operation, the main UI thread should remain responsive and not be blocked
**Validates: Requirements 12.2**

Property 45: Unchanged preferences don't trigger rescheduling
*For any* settings screen interaction that doesn't change preference values, notification rescheduling should not be triggered
**Validates: Requirements 12.6**

## Error Handling

### Image Loading Errors

1. **Network Errors**: When remote images fail to load due to network issues, display placeholder and log error
2. **Invalid URI**: When imageUri is malformed, display placeholder and log warning
3. **Resource Not Found**: When drawable resource doesn't exist, display placeholder and log error
4. **Out of Memory**: When image loading causes memory pressure, use Coil's memory cache eviction

### Validation Errors

1. **Invalid Time Window**: When start time >= end time, show error message and prevent saving
2. **Time Window Too Narrow**: When time window can't accommodate notificationsPerDay with minimum 30-minute intervals, show warning
3. **No Custom Days Selected**: When CUSTOM_DAYS mode is selected but no days are chosen, show error message

### Scheduling Errors

1. **No Valid Time Found**: When calculateNextNotificationTime returns null (no valid time in next 14 days), log warning and retry with extended window
2. **WorkManager Failure**: When WorkManager scheduling fails, log error and retry with exponential backoff
3. **Database Error**: When preference updates fail, show error message and revert to previous values

### Migration Errors

1. **Migration Failure**: When database migration fails, catch exception, log error, and show user-friendly error message with option to clear data
2. **Data Corruption**: When migrated data fails validation, log error and use default values

## Testing Strategy

### Dual Testing Approach

The testing strategy combines unit tests for specific examples and edge cases with property-based tests for universal properties. This dual approach ensures comprehensive coverage:

- **Unit tests**: Validate specific examples, edge cases, and error conditions
- **Property tests**: Verify universal properties across all inputs using randomized testing

### Unit Testing

Unit tests focus on:
1. **Specific examples**: Testing concrete scenarios like "ALL_DAYS mode includes Monday"
2. **Edge cases**: Testing boundary conditions like empty time windows, no unseen motivations
3. **Error conditions**: Testing error handling for invalid URIs, migration failures
4. **Integration points**: Testing ViewModel interactions with repositories

### Property-Based Testing

Property tests use **Kotest Property Testing** library for Kotlin. Each property test:
- Runs minimum 100 iterations with randomized inputs
- References its design document property number
- Uses tag format: **Feature: history-motivation-coach-v2, Property {number}: {property_text}**

**Example Property Test Structure:**

```kotlin
class ScheduleModePropertiesTest : StringSpec({
    "Property 16: All Days mode activates all days".config(
        tags = setOf(
            Tag("Feature: history-motivation-coach-v2"),
            Tag("Property 16: All Days mode activates all days")
        ),
        invocations = 100
    ) {
        checkAll(Arb.userPreferences()) { prefs ->
            val prefsWithAllDays = prefs.copy(scheduleMode = ScheduleMode.ALL_DAYS)
            val activeDays = prefsWithAllDays.scheduleMode.getActiveDays(prefsWithAllDays.customDays)
            
            activeDays shouldBe DayOfWeek.values().toSet()
        }
    }
})
```

### Test Coverage Requirements

1. **Image Loading**: Properties 1-5, 36-38, 43-44
2. **Home Screen**: Properties 6-9
3. **Manual Notifications**: Properties 10-14
4. **Schedule Modes**: Properties 15-20, 25-28
5. **Time Windows**: Properties 21-24, 26, 28
6. **Settings UI**: Properties 32-35
7. **Migration**: Properties 39-43
8. **Performance**: Properties 44-45

### Integration Testing

Integration tests validate:
1. End-to-end user flows (manual notification trigger → delivery → history update)
2. Settings changes → notification rescheduling → WorkManager verification
3. Database migration → data verification → app functionality

### UI Testing

UI tests using Compose Testing validate:
1. Home screen displays personality images correctly
2. Manual notification button triggers notification
3. Settings screen allows schedule mode and time window configuration
4. Error messages display for invalid configurations
