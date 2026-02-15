package com.example.historymotivationcoach

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.historymotivationcoach.data.AppDatabase
import com.example.historymotivationcoach.data.SeedDataLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Application class for History Motivation Coach.
 * 
 * Handles app-level initialization including:
 * - Notification channel creation (Android O+)
 * - Database initialization
 * - Seed data loading
 * - Initial notification scheduling
 * - Coil image loader configuration with caching
 * 
 * Requirements:
 * - 3.1: Create notification channel for displaying motivational notifications
 * - 22.1: Load seed data on first app launch
 * - 22.3: Handle seed loading failures gracefully
 * - 17.2: Configure image loader with caching
 */
class MotivationApplication : Application(), ImageLoaderFactory {
    
    companion object {
        /**
         * Notification channel ID for motivational notifications.
         * Must match the channel ID used in NotificationWorker.
         */
        const val CHANNEL_ID = "motivation_channel"
        private const val TAG = "MotivationApplication"
        
        // Singleton instance for accessing initialization state
        private var instance: MotivationApplication? = null
        
        fun getInstance(): MotivationApplication {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }
    
    // Application-scoped coroutine scope for background initialization
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Initialization state exposed to UI
    private val _initializationState = MutableStateFlow<InitializationState>(InitializationState.Loading)
    val initializationState: StateFlow<InitializationState> = _initializationState.asStateFlow()
    
    /**
     * Represents the initialization state of the application.
     */
    sealed class InitializationState {
        object Loading : InitializationState()
        object Success : InitializationState()
        data class Error(val message: String) : InitializationState()
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Store instance for global access
        instance = this
        
        // Create notification channel for Android O and above
        createNotificationChannel()
        
        // Initialize database and load seed data in background
        initializeApp()
    }
    
    /**
     * Initialize the application by loading seed data on first launch.
     * 
     * This runs in a background coroutine to avoid blocking the main thread.
     * Seed data loading is idempotent - it only loads once on first launch.
     * 
     * After successful initialization, schedules initial notifications based on
     * user preferences.
     * 
     * Requirements:
     * - 22.1: Load seed data from bundled JSON file on first launch
     * - 22.3: Handle failures with user-friendly error message
     * - 15.2: Schedule initial notifications after app initialization
     */
    private fun initializeApp() {
        applicationScope.launch {
            try {
                Log.d(TAG, "Starting app initialization")
                
                // Set loading state
                _initializationState.value = InitializationState.Loading
                
                // Get database instance
                val database = AppDatabase.getInstance(applicationContext)
                
                // Create seed data loader
                val seedDataLoader = SeedDataLoader(
                    context = applicationContext,
                    motivationDao = database.motivationDao()
                )
                
                // Load seed data (this is idempotent - only loads once)
                val result = seedDataLoader.loadSeedData()
                
                result.fold(
                    onSuccess = { count ->
                        if (count > 0) {
                            Log.i(TAG, "Successfully loaded $count motivation items")
                        } else {
                            Log.d(TAG, "Seed data already loaded, skipping")
                        }
                        
                        // Schedule initial notifications
                        scheduleInitialNotifications(database)
                        
                        // Set success state
                        _initializationState.value = InitializationState.Success
                    },
                    onFailure = { exception ->
                        // Log the error for debugging
                        Log.e(TAG, "Failed to load seed data: ${exception.message}", exception)
                        
                        // Set error state with user-friendly message
                        val errorMessage = "Failed to load motivational content. Please restart the app or contact support if the problem persists."
                        _initializationState.value = InitializationState.Error(errorMessage)
                    }
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during app initialization: ${e.message}", e)
                val errorMessage = "An unexpected error occurred. Please restart the app."
                _initializationState.value = InitializationState.Error(errorMessage)
            }
        }
    }
    
    /**
     * Schedule initial notifications after app initialization.
     * 
     * This is called once after successful seed data loading to set up
     * the notification schedule based on user preferences.
     * 
     * Requirements:
     * - 15.2: Schedule notifications on app initialization
     * - 1.3: Respect user notification preferences
     */
    private suspend fun scheduleInitialNotifications(database: AppDatabase) {
        try {
            Log.d(TAG, "Scheduling initial notifications")
            
            // Create repositories
            val preferencesRepository = com.example.historymotivationcoach.data.repository.PreferencesRepository(
                database.preferencesDao()
            )
            
            // Create notification scheduler
            val notificationScheduler: com.example.historymotivationcoach.business.NotificationScheduler = 
                com.example.historymotivationcoach.business.NotificationSchedulerImpl(
                context = applicationContext,
                preferencesRepository = preferencesRepository
            )
            
            // Schedule notifications based on current preferences
            notificationScheduler.scheduleNextNotification()
            
            Log.i(TAG, "Initial notifications scheduled successfully")
        } catch (e: Exception) {
            // Log error but don't fail initialization
            // User can still use the app and manually trigger notifications
            Log.e(TAG, "Failed to schedule initial notifications: ${e.message}", e)
        }
    }
    
    /**
     * Create notification channel for motivational notifications.
     * 
     * This is required for Android O (API 26) and above. The channel allows users
     * to control notification settings (sound, vibration, importance) for the app.
     * 
     * Channel properties:
     * - ID: motivation_channel
     * - Name: Motivational Quotes
     * - Description: Daily motivational quotes and historical wisdom
     * - Importance: DEFAULT (shows notifications, makes sound)
     * 
     * Requirements:
     * - 3.1: Notification channel setup for Android O+
     */
    private fun createNotificationChannel() {
        // Notification channels are only needed on Android O (API 26) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            // Register the channel with the system
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create and configure Coil ImageLoader with caching.
     * 
     * This configures:
     * - Memory cache: 25% of available memory for fast in-memory image access
     * - Disk cache: 50MB for persistent image storage
     * - Cache policies: Enabled for both read and write operations
     * - Debug logging in debug builds
     * 
     * The ImageLoader is automatically used by all AsyncImage composables in the app.
     * 
     * Requirements:
     * - 17.2: Configure image loader with offline caching enabled
     * - 17.3: Support efficient image loading for thumbnails and full-size images
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Use 25% of available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024) // 50MB disk cache
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false) // Always cache images
            .build()
    }
}
