# Design Document: History Motivation Coach

## Overview

The History Motivation Coach is an Android application built using modern Android development practices with Kotlin, Jetpack Compose for UI, Room for local persistence, and WorkManager for reliable background notification scheduling. The architecture follows MVVM (Model-View-ViewModel) pattern with a clear separation between data, business logic, and presentation layers.

The core challenge is ensuring that motivational content never repeats while maintaining reliable notification delivery across device reboots and various Android power management scenarios. The design achieves this through a deterministic content selection algorithm backed by a persistent delivery history.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐ │
│  │   Home   │  │ History  │  │ Settings │  │ Detail  │ │
│  │  Screen  │  │  Screen  │  │  Screen  │  │ Screen  │ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬────┘ │
│       │             │              │             │       │
│  ┌────▼─────────────▼──────────────▼─────────────▼────┐ │
│  │              ViewModels (State Management)          │ │
│  └────┬─────────────┬──────────────┬─────────────┬────┘ │
└───────┼─────────────┼──────────────┼─────────────┼──────┘
        │             │              │             │
┌───────▼─────────────▼──────────────▼─────────────▼──────┐
│                   Business Logic Layer                   │
│  ┌──────────────────┐  ┌────────────────────────────┐   │
│  │ Content Selector │  │ Notification Scheduler     │   │
│  │  - Random select │  │  - WorkManager integration │   │
│  │  - Theme filter  │  │  - Time computation        │   │
│  └────────┬─────────┘  └────────┬───────────────────┘   │
│           │                     │                        │
│  ┌────────▼─────────────────────▼───────────────────┐   │
│  │           Repository Layer                        │   │
│  │  - MotivationRepository                           │   │
│  │  - PreferencesRepository                          │   │
│  └────────┬──────────────────────────────────────────┘   │
└───────────┼──────────────────────────────────────────────┘
            │
┌───────────▼──────────────────────────────────────────────┐
│                    Data Layer                             │
│  ┌──────────────────┐  ┌────────────────────────────┐    │
│  │   Room Database  │  │   WorkManager Workers      │    │
│  │  - MotivationDao │  │  - NotificationWorker      │    │
│  │  - HistoryDao    │  │  - SchedulerWorker         │    │
│  │  - PrefsDao      │  └────────────────────────────┘    │
│  └──────────────────┘                                     │
└───────────────────────────────────────────────────────────┘
```

### Component Responsibilities

**Presentation Layer:**
- Jetpack Compose UI screens
- ViewModels manage UI state and handle user interactions
- Navigation component manages screen transitions

**Business Logic Layer:**
- ContentSelector: Implements non-repeating selection algorithm
- NotificationScheduler: Computes notification times and schedules work
- Repositories: Abstract data access and provide clean APIs to ViewModels

**Data Layer:**
- Room Database: Persistent storage for content, history, and preferences
- WorkManager: Reliable background task execution
- DataStore: Simple key-value preferences (optional, for non-structured settings)

## Components and Interfaces

### 1. Data Models

#### MotivationItem Entity
```kotlin
@Entity(tableName = "motivation_items")
data class MotivationItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "quote")
    val quote: String,
    
    @ColumnInfo(name = "author")
    val author: String,
    
    @ColumnInfo(name = "context")
    val context: String? = null,
    
    @ColumnInfo(name = "image_uri")
    val imageUri: String,
    
    @ColumnInfo(name = "themes")
    val themes: List<String>,
    
    @ColumnInfo(name = "source_name")
    val sourceName: String,
    
    @ColumnInfo(name = "source_url")
    val sourceUrl: String? = null,
    
    @ColumnInfo(name = "license")
    val license: String
)
```

#### DeliveryHistory Entity
```kotlin
@Entity(
    tableName = "delivery_history",
    foreignKeys = [
        ForeignKey(
            entity = MotivationItem::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("item_id"), Index("date_key"), Index("shown_at")]
)
data class DeliveryHistory(
    @PrimaryKey(autoGenerate = true)
    val historyId: Long = 0,
    
    @ColumnInfo(name = "item_id")
    val itemId: Long,
    
    @ColumnInfo(name = "shown_at")
    val shownAt: Long, // Unix timestamp
    
    @ColumnInfo(name = "date_key")
    val dateKey: String, // YYYY-MM-DD format
    
    @ColumnInfo(name = "notification_id")
    val notificationId: Int,
    
    @ColumnInfo(name = "delivery_status")
    val deliveryStatus: DeliveryStatus
)

enum class DeliveryStatus {
    DELIVERED,
    OPENED,
    DISMISSED
}
```

#### UserPreferences Entity
```kotlin
@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey
    val id: Int = 1, // Single row table
    
    @ColumnInfo(name = "notifications_per_day")
    val notificationsPerDay: Int = 3,
    
    @ColumnInfo(name = "schedule_mode")
    val scheduleMode: ScheduleMode = ScheduleMode.TIME_WINDOW,
    
    @ColumnInfo(name = "start_time")
    val startTime: String = "09:00", // HH:mm format
    
    @ColumnInfo(name = "end_time")
    val endTime: String = "21:00", // HH:mm format
    
    @ColumnInfo(name = "fixed_times")
    val fixedTimes: List<String> = emptyList(), // List of HH:mm strings
    
    @ColumnInfo(name = "enabled")
    val enabled: Boolean = true,
    
    @ColumnInfo(name = "preferred_themes")
    val preferredThemes: List<String> = emptyList()
)

enum class ScheduleMode {
    TIME_WINDOW,
    FIXED_TIMES
}
```

### 2. Database Access Objects (DAOs)

#### MotivationDao
```kotlin
@Dao
interface MotivationDao {
    @Query("SELECT * FROM motivation_items WHERE id NOT IN (SELECT item_id FROM delivery_history)")
    suspend fun getUnseenItems(): List<MotivationItem>
    
    @Query("SELECT * FROM motivation_items WHERE id NOT IN (SELECT item_id FROM delivery_history) AND themes IN (:themes)")
    suspend fun getUnseenItemsByThemes(themes: List<String>): List<MotivationItem>
    
    @Query("SELECT * FROM motivation_items WHERE id = :id")
    suspend fun getItemById(id: Long): MotivationItem?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MotivationItem>)
    
    @Query("SELECT COUNT(*) FROM motivation_items WHERE id NOT IN (SELECT item_id FROM delivery_history)")
    suspend fun getUnseenCount(): Int
}
```

#### HistoryDao
```kotlin
@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(history: DeliveryHistory): Long
    
    @Query("SELECT * FROM delivery_history WHERE date_key = :dateKey ORDER BY shown_at DESC")
    suspend fun getHistoryByDate(dateKey: String): List<DeliveryHistory>
    
    @Query("SELECT DISTINCT date_key FROM delivery_history ORDER BY date_key DESC")
    suspend fun getAllDateKeys(): List<String>
    
    @Transaction
    @Query("""
        SELECT m.*, h.shown_at, h.date_key, h.delivery_status 
        FROM motivation_items m 
        INNER JOIN delivery_history h ON m.id = h.item_id 
        WHERE h.date_key = :dateKey 
        ORDER BY h.shown_at DESC
    """)
    suspend fun getMotivationsWithHistoryByDate(dateKey: String): List<MotivationWithHistory>
    
    @Query("DELETE FROM delivery_history")
    suspend fun clearAll()
    
    @Query("UPDATE delivery_history SET delivery_status = :status WHERE history_id = :historyId")
    suspend fun updateDeliveryStatus(historyId: Long, status: DeliveryStatus)
}

data class MotivationWithHistory(
    @Embedded val item: MotivationItem,
    @ColumnInfo(name = "shown_at") val shownAt: Long,
    @ColumnInfo(name = "date_key") val dateKey: String,
    @ColumnInfo(name = "delivery_status") val deliveryStatus: DeliveryStatus
)
```

#### PreferencesDao
```kotlin
@Dao
interface PreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE id = 1")
    suspend fun getPreferences(): UserPreferences?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreferences(prefs: UserPreferences)
    
    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun getPreferencesFlow(): Flow<UserPreferences?>
}
```

### 3. Repository Layer

#### MotivationRepository
```kotlin
class MotivationRepository(
    private val motivationDao: MotivationDao,
    private val historyDao: HistoryDao
) {
    suspend fun selectRandomUnseen(preferredThemes: List<String>): MotivationItem? {
        val unseenItems = if (preferredThemes.isEmpty()) {
            motivationDao.getUnseenItems()
        } else {
            val themed = motivationDao.getUnseenItemsByThemes(preferredThemes)
            if (themed.isEmpty()) motivationDao.getUnseenItems() else themed
        }
        
        return unseenItems.randomOrNull()
    }
    
    suspend fun recordDelivery(itemId: Long, notificationId: Int): Long {
        val now = System.currentTimeMillis()
        val dateKey = formatDateKey(now)
        
        val history = DeliveryHistory(
            itemId = itemId,
            shownAt = now,
            dateKey = dateKey,
            notificationId = notificationId,
            deliveryStatus = DeliveryStatus.DELIVERED
        )
        
        return historyDao.insert(history)
    }
    
    suspend fun getUnseenCount(): Int = motivationDao.getUnseenCount()
    
    suspend fun getHistoryByDate(dateKey: String): List<MotivationWithHistory> =
        historyDao.getMotivationsWithHistoryByDate(dateKey)
    
    suspend fun getAllDateKeys(): List<String> = historyDao.getAllDateKeys()
    
    suspend fun clearHistory() = historyDao.clearAll()
    
    private fun formatDateKey(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return format.format(date)
    }
}
```

#### PreferencesRepository
```kotlin
class PreferencesRepository(private val preferencesDao: PreferencesDao) {
    
    suspend fun getPreferences(): UserPreferences {
        return preferencesDao.getPreferences() ?: UserPreferences()
    }
    
    fun getPreferencesFlow(): Flow<UserPreferences> {
        return preferencesDao.getPreferencesFlow()
            .map { it ?: UserPreferences() }
    }
    
    suspend fun updatePreferences(prefs: UserPreferences) {
        preferencesDao.savePreferences(prefs)
    }
}
```

### 4. Business Logic Components

#### ContentSelector
```kotlin
class ContentSelector(
    private val motivationRepository: MotivationRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend fun selectNextMotivation(): MotivationItem? {
        val prefs = preferencesRepository.getPreferences()
        return motivationRepository.selectRandomUnseen(prefs.preferredThemes)
    }
    
    suspend fun isContentExhausted(): Boolean {
        return motivationRepository.getUnseenCount() == 0
    }
}
```

#### NotificationScheduler
```kotlin
class NotificationScheduler(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository
) {
    suspend fun scheduleNotifications() {
        val prefs = preferencesRepository.getPreferences()
        
        if (!prefs.enabled) {
            cancelAllNotifications()
            return
        }
        
        val times = computeNotificationTimes(prefs)
        
        // Cancel existing work
        WorkManager.getInstance(context).cancelAllWorkByTag(NOTIFICATION_TAG)
        
        // Schedule new work for each time
        times.forEachIndexed { index, time ->
            val workName = "motivation_${getCurrentDateKey()}_$index"
            val delay = time - System.currentTimeMillis()
            
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
        
        // Schedule daily rescheduler at midnight
        scheduleDailyRescheduler()
    }
    
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
        
        return times.filter { it > System.currentTimeMillis() }
    }
    
    private fun parseTime(timeStr: String, baseDate: Calendar): Long {
        val parts = timeStr.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        
        return baseDate.clone().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
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
    
    private fun getNextMidnight(): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
    private fun cancelAllNotifications() {
        WorkManager.getInstance(context).cancelAllWorkByTag(NOTIFICATION_TAG)
    }
    
    private fun getCurrentDateKey(): String {
        val format = SimpleDateFormat("yyyyMMdd", Locale.US)
        return format.format(Date())
    }
    
    companion object {
        const val NOTIFICATION_TAG = "motivation_notification"
        const val SCHEDULER_TAG = "daily_scheduler"
    }
}
```

### 5. WorkManager Workers

#### NotificationWorker
```kotlin
class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val notificationId = inputData.getInt("notification_id", 0)
        
        // Get dependencies (in real app, use DI)
        val database = AppDatabase.getInstance(applicationContext)
        val motivationRepo = MotivationRepository(
            database.motivationDao(),
            database.historyDao()
        )
        val prefsRepo = PreferencesRepository(database.preferencesDao())
        val contentSelector = ContentSelector(motivationRepo, prefsRepo)
        
        // Check if content is exhausted
        if (contentSelector.isContentExhausted()) {
            // Show exhaustion notification or update app state
            return Result.success()
        }
        
        // Select next motivation
        val motivation = contentSelector.selectNextMotivation()
            ?: return Result.failure()
        
        // Record delivery
        motivationRepo.recordDelivery(motivation.id, notificationId)
        
        // Show notification
        showNotification(motivation, notificationId)
        
        return Result.success()
    }
    
    private fun showNotification(item: MotivationItem, notificationId: Int) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra("motivation_id", item.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(item.author)
            .setContentText(item.quote.take(100) + if (item.quote.length > 100) "..." else "")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
    
    companion object {
        const val CHANNEL_ID = "motivation_channel"
    }
}
```

#### SchedulerWorker
```kotlin
class SchedulerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val database = AppDatabase.getInstance(applicationContext)
        val prefsRepo = PreferencesRepository(database.preferencesDao())
        val scheduler = NotificationScheduler(applicationContext, prefsRepo)
        
        scheduler.scheduleNotifications()
        
        return Result.success()
    }
}
```

### 6. ViewModels

#### HomeViewModel
```kotlin
class HomeViewModel(
    private val motivationRepository: MotivationRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadLatestMotivation()
    }
    
    private fun loadLatestMotivation() {
        viewModelScope.launch {
            try {
                val today = getCurrentDateKey()
                val todayHistory = motivationRepository.getHistoryByDate(today)
                val unseenCount = motivationRepository.getUnseenCount()
                
                _uiState.value = if (todayHistory.isEmpty()) {
                    HomeUiState.Empty(unseenCount)
                } else {
                    HomeUiState.Success(
                        latestMotivation = todayHistory.first(),
                        todayCount = todayHistory.size,
                        unseenCount = unseenCount
                    )
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun triggerManualNotification() {
        viewModelScope.launch {
            // Trigger immediate notification work
        }
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val latestMotivation: MotivationWithHistory,
        val todayCount: Int,
        val unseenCount: Int
    ) : HomeUiState()
    data class Empty(val unseenCount: Int) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
```

#### HistoryViewModel
```kotlin
class HistoryViewModel(
    private val motivationRepository: MotivationRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    init {
        loadHistory()
    }
    
    private fun loadHistory() {
        viewModelScope.launch {
            try {
                val dateKeys = motivationRepository.getAllDateKeys()
                val groupedHistory = dateKeys.map { dateKey ->
                    val label = formatDateLabel(dateKey)
                    val items = motivationRepository.getHistoryByDate(dateKey)
                    HistoryGroup(label, dateKey, items)
                }
                
                _uiState.value = if (groupedHistory.isEmpty()) {
                    HistoryUiState.Empty
                } else {
                    HistoryUiState.Success(groupedHistory)
                }
            } catch (e: Exception) {
                _uiState.value = HistoryUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    private fun formatDateLabel(dateKey: String): String {
        val today = getCurrentDateKey()
        val yesterday = getYesterdayDateKey()
        
        return when (dateKey) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> dateKey // Or format as "Month Day, Year"
        }
    }
}

sealed class HistoryUiState {
    object Loading : HistoryUiState()
    data class Success(val groups: List<HistoryGroup>) : HistoryUiState()
    object Empty : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}

data class HistoryGroup(
    val label: String,
    val dateKey: String,
    val items: List<MotivationWithHistory>
)
```

#### SettingsViewModel
```kotlin
class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val motivationRepository: MotivationRepository,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {
    
    val preferences: StateFlow<UserPreferences> = preferencesRepository
        .getPreferencesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )
    
    fun updateNotificationsPerDay(count: Int) {
        viewModelScope.launch {
            val current = preferences.value
            val updated = current.copy(notificationsPerDay = count.coerceIn(1, 10))
            preferencesRepository.updatePreferences(updated)
            notificationScheduler.scheduleNotifications()
        }
    }
    
    fun updateScheduleMode(mode: ScheduleMode) {
        viewModelScope.launch {
            val current = preferences.value
            val updated = current.copy(scheduleMode = mode)
            preferencesRepository.updatePreferences(updated)
            notificationScheduler.scheduleNotifications()
        }
    }
    
    fun updateTimeWindow(startTime: String, endTime: String) {
        viewModelScope.launch {
            val current = preferences.value
            val updated = current.copy(startTime = startTime, endTime = endTime)
            preferencesRepository.updatePreferences(updated)
            notificationScheduler.scheduleNotifications()
        }
    }
    
    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val current = preferences.value
            val updated = current.copy(enabled = enabled)
            preferencesRepository.updatePreferences(updated)
            notificationScheduler.scheduleNotifications()
        }
    }
    
    fun clearHistory() {
        viewModelScope.launch {
            motivationRepository.clearHistory()
        }
    }
}
```

## Data Models

### Room Database Schema

The application uses Room database with three main tables:

**motivation_items table:**
- Primary key: `id` (auto-generated)
- Stores all motivational content
- No foreign keys
- Indexed on `id` for fast lookups

**delivery_history table:**
- Primary key: `historyId` (auto-generated)
- Foreign key: `item_id` references `motivation_items(id)` with CASCADE delete
- Indexes on: `item_id`, `date_key`, `shown_at` for efficient queries
- Tracks when and which items were delivered

**user_preferences table:**
- Primary key: `id` (always 1, single-row table)
- Stores all user configuration
- No foreign keys

### Type Converters

Room requires type converters for complex types:

```kotlin
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }
    
    @TypeConverter
    fun fromScheduleMode(value: ScheduleMode): String {
        return value.name
    }
    
    @TypeConverter
    fun toScheduleMode(value: String): ScheduleMode {
        return ScheduleMode.valueOf(value)
    }
    
    @TypeConverter
    fun fromDeliveryStatus(value: DeliveryStatus): String {
        return value.name
    }
    
    @TypeConverter
    fun toDeliveryStatus(value: String): DeliveryStatus {
        return DeliveryStatus.valueOf(value)
    }
}
```

### Seed Data Format

Initial content is loaded from a JSON file in assets:

```json
{
  "motivations": [
    {
      "quote": "The only way to do great work is to love what you do.",
      "author": "Steve Jobs",
      "context": "From Stanford commencement speech, 2005",
      "imageUri": "android.resource://com.example.motivationcoach/drawable/jobs_stanford",
      "themes": ["work", "passion", "excellence"],
      "sourceName": "Stanford University",
      "sourceUrl": "https://news.stanford.edu/2005/06/14/jobs-061505/",
      "license": "Public Domain"
    }
  ]
}
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Notification Frequency Validation

*For any* notification frequency input value, the system should accept values in the range [1, 10] and reject values outside this range.

**Validates: Requirements 1.1**

### Property 2: Even Distribution in Time Window Mode

*For any* valid start time, end time, and notification count, when Time_Window mode is active, the computed notification times should be evenly distributed between start and end with equal intervals.

**Validates: Requirements 2.2**

### Property 3: Fixed Times Scheduling Accuracy

*For any* list of fixed times provided by the user, when Fixed_Times mode is active, the scheduled notification times should exactly match the user-specified times (up to the configured notifications per day limit).

**Validates: Requirements 2.3**

### Property 4: Mode Switching Cleanup

*For any* scheduling mode change, all previously scheduled notifications should be cancelled and new notifications should be scheduled according to the new mode's configuration.

**Validates: Requirements 2.4**

### Property 5: Notification Content Completeness

*For any* motivation item selected for notification, the notification payload should contain the quote preview, author name, image URI, and themes.

**Validates: Requirements 3.1**

### Property 6: Notification Intent Correctness

*For any* notification displayed, tapping it should create an intent containing the correct motivation item ID for navigation to the detail screen.

**Validates: Requirements 3.2**

### Property 7: Non-Repeating Selection (Core Property)

*For any* content selection operation, the selected motivation item should not exist in the delivery_history table (i.e., should only select from unseen items).

**Validates: Requirements 4.1, 16.1**

### Property 8: Delivery Recording

*For any* motivation item delivered to the user, a corresponding record should be immediately inserted into delivery_history with timestamp, date_key, and delivery status.

**Validates: Requirements 4.2, 6.1, 16.4**

### Property 9: Replay Classics Reset

*For any* database state where the user activates "Replay Classics" mode, the delivery_history table should be cleared and all motivation items should become available for selection again.

**Validates: Requirements 5.3**

### Property 10: Unseen Count Accuracy

*For any* database state, the displayed count of unseen motivation items should equal the count of items in motivation_items that do not have a corresponding entry in delivery_history.

**Validates: Requirements 5.4**

### Property 11: History Persistence

*For any* delivery history record, after closing and reopening the database connection, the record should still exist with all fields intact.

**Validates: Requirements 6.2**

### Property 12: Delivery Status Tracking

*For any* history entry created, it should have a valid delivery status (DELIVERED, OPENED, or DISMISSED).

**Validates: Requirements 6.3**

### Property 13: Referential Integrity

*For any* motivation item that is deleted from motivation_items, all corresponding records in delivery_history should be automatically deleted (cascade delete).

**Validates: Requirements 6.4**

### Property 14: Today Grouping

*For any* set of motivation items delivered on the current date, they should all appear in the history view grouped under the "Today" label.

**Validates: Requirements 7.1**

### Property 15: Today Count Accuracy

*For any* database state, the displayed count of items received today should equal the count of delivery_history records with today's date_key.

**Validates: Requirements 7.2, 12.2**

### Property 16: History Real-Time Updates

*For any* new delivery history record inserted, the ViewModel's state should update to reflect the new history entry within the current session.

**Validates: Requirements 7.4**

### Property 17: Chronological Grouping

*For any* set of delivery history entries, they should be grouped by date_key in descending chronological order (newest dates first).

**Validates: Requirements 8.1**

### Property 18: Date Label Formatting

*For any* date_key, it should be labeled as "Today" if it matches today's date, "Yesterday" if it matches yesterday's date, and formatted as a date string otherwise.

**Validates: Requirements 8.2**

### Property 19: History Entry Display Completeness

*For any* history entry displayed in the list, it should include timestamp, author, quote preview, and image thumbnail.

**Validates: Requirements 8.4**

### Property 20: Detail Navigation

*For any* history entry or notification tapped by the user, the app should navigate to the Detail_Screen with the correct motivation item ID.

**Validates: Requirements 9.1**

### Property 21: Detail View Completeness

*For any* motivation item displayed in the detail view, it should show the full quote text, author name, historical context (if present), full-size image, themes, source attribution, and delivery timestamp.

**Validates: Requirements 9.2**

### Property 22: Motivation Item Field Completeness

*For any* motivation item stored in the database, it should have non-null values for quote, author, imageUri, themes, sourceName, and license fields.

**Validates: Requirements 10.1, 10.4**

### Property 23: Navigation Tab Switching

*For any* navigation tab selected by the user, the app should display the corresponding screen (Home, History, or Settings).

**Validates: Requirements 11.2**

### Property 24: Navigation State Preservation

*For any* active screen when the app is backgrounded, the same screen should be displayed when the app is restored.

**Validates: Requirements 11.3**

### Property 25: Active Tab Highlighting

*For any* currently displayed screen, the corresponding navigation tab should be visually highlighted.

**Validates: Requirements 11.4**

### Property 26: Latest Motivation Display

*For any* database state with at least one delivery history record, the Home_Screen should display the motivation item with the most recent shownAt timestamp.

**Validates: Requirements 12.1**

### Property 27: Settings Persistence

*For any* settings change made by the user, the updated preferences should be immediately persisted to the user_preferences table.

**Validates: Requirements 13.4**

### Property 28: Notification Toggle Effect

*For any* notification enabled/disabled toggle, when disabled, no notification work should be scheduled; when enabled, notifications should be scheduled according to current preferences.

**Validates: Requirements 13.2**

### Property 29: Scheduling Determinism

*For any* date and notification index, the WorkManager work request name should be deterministic and unique (format: "motivation_YYYYMMDD_index").

**Validates: Requirements 15.3**

### Property 30: Random Selection Distribution

*For any* set of unseen motivation items, repeated selections should eventually select all items (each item has non-zero probability of selection).

**Validates: Requirements 16.2**

### Property 31: Theme-Biased Selection

*For any* configured theme preference, when unseen items matching the theme exist, the selection should prefer those items over non-matching items.

**Validates: Requirements 16.3**

### Property 32: Bundled Image URI Format

*For any* motivation item with a bundled image, the imageUri should follow the android.resource:// URI format.

**Validates: Requirements 17.1**

### Property 33: Image Inclusion in Lists

*For any* notification or history list item, it should include an image reference (thumbnail).

**Validates: Requirements 17.3**

### Property 34: Accessibility Content Descriptions

*For any* interactive UI element, it should have a content description set for accessibility.

**Validates: Requirements 18.4**

### Property 35: History Query Performance

*For any* delivery_history query on a dataset up to 10,000 items, the query should complete in less than 300 milliseconds.

**Validates: Requirements 19.1**

### Property 36: No Duplicate Notifications

*For any* scheduling operation, no two work requests should have the same name (preventing duplicate notifications for the same time slot).

**Validates: Requirements 20.1**

### Property 37: Global Non-Repetition (Critical Property)

*For any* sequence of motivation deliveries (excluding Replay Classics resets), no motivation item should be delivered more than once.

**Validates: Requirements 20.2**

### Property 38: Concurrent Access Safety

*For any* set of concurrent database operations (reads and writes), the database should maintain data consistency without corruption.

**Validates: Requirements 20.4**

### Property 39: Seed Data Validation

*For any* motivation item in the seed dataset, items with missing required fields should be rejected during the loading process.

**Validates: Requirements 22.2**

### Property 40: Seed Loading Idempotence

*For any* number of app launches, the seed dataset should only be loaded once (first launch), and subsequent launches should not duplicate the seed data.

**Validates: Requirements 22.4**

## Error Handling

### Notification Scheduling Errors

**Content Exhaustion:**
- When all content has been delivered (unseen count = 0), stop scheduling new notifications
- Display a clear message to the user explaining content exhaustion
- Offer "Replay Classics" option to reset the seen pool
- Log the exhaustion event for debugging

**System Notification Disabled:**
- Check notification permission status before scheduling
- If disabled, display a warning in Settings with instructions
- Provide a button to open system notification settings
- Gracefully handle the disabled state without crashing

**WorkManager Failures:**
- Implement retry logic with exponential backoff for transient failures
- Log all scheduling failures with context
- Fall back to AlarmManager if WorkManager consistently fails
- Notify user if notifications cannot be scheduled after retries

### Database Errors

**Seed Loading Failures:**
- Catch JSON parsing errors and log detailed error messages
- Validate each item before insertion
- Continue loading valid items even if some items fail
- Display error message to user if seed loading completely fails
- Provide option to retry seed loading

**Query Failures:**
- Wrap all database operations in try-catch blocks
- Return empty results or default values on query failures
- Log errors with stack traces for debugging
- Display user-friendly error messages in UI

**Constraint Violations:**
- Handle foreign key violations gracefully
- Validate data before insertion to prevent constraint errors
- Log constraint violations for debugging
- Rollback transactions on errors

### Image Loading Errors

**Missing Images:**
- Provide placeholder image for missing or failed image loads
- Log missing image URIs for debugging
- Continue displaying content even if image fails
- Cache placeholder to avoid repeated load attempts

**Network Failures (for remote images):**
- Use cached images when network is unavailable
- Display placeholder if no cache exists
- Retry image loads with exponential backoff
- Provide offline mode indicator

### State Management Errors

**ViewModel Errors:**
- Catch exceptions in ViewModel operations
- Update UI state to show error messages
- Provide retry actions where appropriate
- Log errors for debugging

**Navigation Errors:**
- Handle missing navigation arguments gracefully
- Provide default values for missing data
- Log navigation errors
- Return to safe screen (Home) on critical navigation failures

## Testing Strategy

### Dual Testing Approach

The History Motivation Coach app requires both unit tests and property-based tests for comprehensive coverage. These testing approaches are complementary:

**Unit Tests** verify specific examples, edge cases, and error conditions:
- Specific time window calculations (e.g., 9 AM to 9 PM with 3 notifications)
- Empty state handling (no history, no unseen items)
- Database schema validation
- Specific navigation flows
- Error handling for specific failure scenarios

**Property-Based Tests** verify universal properties across all inputs:
- Non-repeating content selection across thousands of random selections
- Time distribution algorithms with random start/end times
- Database consistency with random concurrent operations
- Notification scheduling with random preferences
- Data persistence across random state changes

### Property-Based Testing Configuration

**Testing Library:** Use [Kotest Property Testing](https://kotest.io/docs/proptest/property-based-testing.html) for Kotlin

**Configuration:**
- Minimum 100 iterations per property test (due to randomization)
- Increase to 1000 iterations for critical properties (non-repetition, scheduling uniqueness)
- Use deterministic random seeds for reproducibility
- Configure timeouts appropriately for performance tests

**Test Tagging:**
Each property test must reference its design document property using this format:
```kotlin
@Test
fun `Property 7 - Non-Repeating Selection`() = runTest {
    // Feature: history-motivation-coach, Property 7: Non-Repeating Selection
    checkAll(100, Arb.motivationItem(), Arb.deliveryHistory()) { item, history ->
        // Test implementation
    }
}
```

### Unit Testing Focus Areas

**Specific Examples:**
- Default notification frequency is 3
- "Today" label for current date
- "Yesterday" label for previous date
- Empty history displays empty state
- Seed dataset contains minimum 100 items
- Database schema has correct tables and columns

**Edge Cases:**
- Content exhaustion (unseen count = 0)
- Single notification per day
- Maximum notifications per day (10)
- Midnight boundary for date_key calculation
- Empty theme preferences
- System notifications disabled

**Integration Points:**
- WorkManager scheduling integration
- Room database migrations
- Navigation between screens
- Notification tap handling
- Image loading with Coil/Glide

**Error Conditions:**
- Invalid notification frequency (< 1 or > 10)
- Malformed time strings
- Missing required fields in motivation items
- Database constraint violations
- Seed loading failures

### Property-Based Testing Focus Areas

**Critical Properties (1000 iterations):**

**Property 7 - Non-Repeating Selection:**
```kotlin
// Generate random database states with varying seen/unseen items
// Repeatedly call selectNextMotivation()
// Verify no item is selected twice until exhaustion
```

**Property 37 - Global Non-Repetition:**
```kotlin
// Generate random sequence of deliveries
// Track all delivered item IDs
// Verify no duplicates in the sequence
```

**Property 36 - No Duplicate Notifications:**
```kotlin
// Generate random scheduling preferences
// Call scheduleNotifications() multiple times
// Verify all work request names are unique
```

**Standard Properties (100 iterations):**

**Property 2 - Even Distribution:**
```kotlin
// Generate random start times, end times, and counts
// Compute notification times
// Verify intervals are equal (within tolerance)
```

**Property 10 - Unseen Count Accuracy:**
```kotlin
// Generate random database states
// Query unseen count
// Verify count matches (total items - delivered items)
```

**Property 38 - Concurrent Access Safety:**
```kotlin
// Generate random concurrent read/write operations
// Execute operations in parallel
// Verify database consistency after all operations complete
```

### Test Data Generators

**Arbitrary Generators for Property Tests:**

```kotlin
// Generate random motivation items
fun Arb.Companion.motivationItem() = arbitrary {
    MotivationItem(
        id = Arb.long(1..10000).bind(),
        quote = Arb.string(10..500).bind(),
        author = Arb.string(5..50).bind(),
        context = Arb.string(10..200).orNull().bind(),
        imageUri = Arb.imageUri().bind(),
        themes = Arb.list(Arb.string(5..20), 1..5).bind(),
        sourceName = Arb.string(5..50).bind(),
        sourceUrl = Arb.string(10..100).orNull().bind(),
        license = Arb.of("Public Domain", "CC BY 4.0", "CC BY-SA 4.0").bind()
    )
}

// Generate random delivery history
fun Arb.Companion.deliveryHistory() = arbitrary {
    DeliveryHistory(
        historyId = Arb.long(1..10000).bind(),
        itemId = Arb.long(1..10000).bind(),
        shownAt = Arb.long(1000000000000..2000000000000).bind(),
        dateKey = Arb.dateKey().bind(),
        notificationId = Arb.int(0..1000).bind(),
        deliveryStatus = Arb.enum<DeliveryStatus>().bind()
    )
}

// Generate random user preferences
fun Arb.Companion.userPreferences() = arbitrary {
    UserPreferences(
        notificationsPerDay = Arb.int(1..10).bind(),
        scheduleMode = Arb.enum<ScheduleMode>().bind(),
        startTime = Arb.timeString().bind(),
        endTime = Arb.timeString().bind(),
        fixedTimes = Arb.list(Arb.timeString(), 0..10).bind(),
        enabled = Arb.bool().bind(),
        preferredThemes = Arb.list(Arb.string(5..20), 0..5).bind()
    )
}

// Helper generators
fun Arb.Companion.timeString() = arbitrary {
    val hour = Arb.int(0..23).bind()
    val minute = Arb.int(0..59).bind()
    "%02d:%02d".format(hour, minute)
}

fun Arb.Companion.dateKey() = arbitrary {
    val year = Arb.int(2020..2025).bind()
    val month = Arb.int(1..12).bind()
    val day = Arb.int(1..28).bind() // Simplified to avoid invalid dates
    "%04d-%02d-%02d".format(year, month, day)
}

fun Arb.Companion.imageUri() = arbitrary {
    "android.resource://com.example.motivationcoach/drawable/image_${Arb.int(1..100).bind()}"
}
```

### Test Organization

```
app/src/test/kotlin/
├── unit/
│   ├── database/
│   │   ├── MotivationDaoTest.kt
│   │   ├── HistoryDaoTest.kt
│   │   └── PreferencesDaoTest.kt
│   ├── repository/
│   │   ├── MotivationRepositoryTest.kt
│   │   └── PreferencesRepositoryTest.kt
│   ├── business/
│   │   ├── ContentSelectorTest.kt
│   │   └── NotificationSchedulerTest.kt
│   └── viewmodel/
│       ├── HomeViewModelTest.kt
│       ├── HistoryViewModelTest.kt
│       └── SettingsViewModelTest.kt
└── property/
    ├── NonRepetitionPropertiesTest.kt
    ├── SchedulingPropertiesTest.kt
    ├── DataConsistencyPropertiesTest.kt
    └── generators/
        └── Arbitraries.kt

app/src/androidTest/kotlin/
├── integration/
│   ├── NotificationIntegrationTest.kt
│   ├── DatabaseMigrationTest.kt
│   └── NavigationIntegrationTest.kt
└── ui/
    ├── HomeScreenTest.kt
    ├── HistoryScreenTest.kt
    └── SettingsScreenTest.kt
```

### Continuous Integration

**CI Pipeline:**
1. Run all unit tests (fast feedback)
2. Run property-based tests with 100 iterations
3. Run integration tests
4. Nightly: Run property-based tests with 1000 iterations
5. Generate coverage reports (target: 80% code coverage)

**Performance Benchmarks:**
- History query performance test (Property 35)
- Database operation benchmarks
- UI rendering performance tests
- Memory usage profiling

### Manual Testing Checklist

**Notification Testing:**
- [ ] Notifications appear at scheduled times
- [ ] Notification content is complete and readable
- [ ] Tapping notification opens correct detail screen
- [ ] Notifications stop when disabled in settings
- [ ] Notifications resume after device reboot
- [ ] Content exhaustion message appears when appropriate

**UI Testing:**
- [ ] All screens render correctly in light and dark modes
- [ ] Navigation between tabs works smoothly
- [ ] History scrolling is smooth with large datasets
- [ ] Images load correctly (or show placeholders)
- [ ] Empty states display appropriate messages
- [ ] Settings changes take effect immediately

**Data Testing:**
- [ ] History persists across app restarts
- [ ] No duplicate content appears
- [ ] "Replay Classics" correctly resets seen pool
- [ ] Export history produces valid data
- [ ] Seed data loads correctly on first launch
