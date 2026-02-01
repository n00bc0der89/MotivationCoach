# Implementation Plan: History Motivation Coach

## Overview

This implementation plan breaks down the History Motivation Coach Android app into discrete, incremental coding tasks. The approach follows a bottom-up strategy: starting with data layer (Room database), then business logic, WorkManager integration, and finally UI screens. Each task builds on previous work, with testing integrated throughout to catch errors early.

## Tasks

- [x] 1. Set up Android project structure and dependencies
  - Create new Android project with Kotlin and Jetpack Compose
  - Add dependencies: Room, WorkManager, Coil, Kotest, Compose Navigation
  - Configure build.gradle with required plugins (kapt, compose)
  - Set minimum SDK to API 28 (Android 9)
  - _Requirements: 10.1, 14.1_

- [x] 2. Implement Room database schema and entities
  - [x] 2.1 Create MotivationItem entity with all required fields
    - Define @Entity with table name "motivation_items"
    - Add fields: id, quote, author, context, imageUri, themes, sourceName, sourceUrl, license
    - Implement type converters for List<String> (themes)
    - _Requirements: 10.1, 14.2_
  
  - [x] 2.2 Create DeliveryHistory entity with foreign key relationship
    - Define @Entity with table name "delivery_history"
    - Add fields: historyId, itemId, shownAt, dateKey, notificationId, deliveryStatus
    - Configure foreign key to MotivationItem with CASCADE delete
    - Add indexes on itemId, dateKey, and shownAt
    - _Requirements: 6.1, 14.3_
  
  - [x] 2.3 Create UserPreferences entity
    - Define @Entity with table name "user_preferences"
    - Add fields: id, notificationsPerDay, scheduleMode, startTime, endTime, fixedTimes, enabled, preferredThemes
    - Implement type converters for ScheduleMode enum and List<String>
    - _Requirements: 1.1, 2.1, 14.4_
  
  - [x] 2.4 Create AppDatabase class with type converters
    - Define @Database with all three entities
    - Register type converters
    - Implement singleton getInstance() method
    - _Requirements: 14.1_
  
  - [ ]* 2.5 Write property test for database schema validation
    - **Property 22: Motivation Item Field Completeness**
    - **Validates: Requirements 10.1, 10.4**

- [x] 3. Implement Data Access Objects (DAOs)
  - [x] 3.1 Create MotivationDao with unseen item queries
    - Implement getUnseenItems() query (NOT IN delivery_history)
    - Implement getUnseenItemsByThemes() for theme filtering
    - Implement getItemById() for single item lookup
    - Implement insertAll() for batch insertion
    - Implement getUnseenCount() for remaining content count
    - _Requirements: 4.1, 5.4, 16.1_
  
  - [x] 3.2 Create HistoryDao with date-based queries
    - Implement insert() for recording deliveries
    - Implement getHistoryByDate() for daily history
    - Implement getAllDateKeys() for history grouping
    - Implement getMotivationsWithHistoryByDate() with @Transaction
    - Implement clearAll() for Replay Classics
    - Implement updateDeliveryStatus() for status tracking
    - _Requirements: 6.1, 7.1, 8.1_
  
  - [x] 3.3 Create PreferencesDao with Flow support
    - Implement getPreferences() for one-time read
    - Implement savePreferences() with REPLACE strategy
    - Implement getPreferencesFlow() for reactive updates
    - _Requirements: 13.4_
  
  - [ ]* 3.4 Write property test for referential integrity
    - **Property 13: Referential Integrity**
    - **Validates: Requirements 6.4**
  
  - [ ]* 3.5 Write property test for history persistence
    - **Property 11: History Persistence**
    - **Validates: Requirements 6.2**

- [x] 4. Implement Repository layer
  - [x] 4.1 Create MotivationRepository
    - Implement selectRandomUnseen() with theme preference support
    - Implement recordDelivery() with timestamp and dateKey generation
    - Implement getUnseenCount()
    - Implement getHistoryByDate()
    - Implement getAllDateKeys()
    - Implement clearHistory()
    - Add helper function formatDateKey() for YYYY-MM-DD format
    - _Requirements: 4.1, 4.2, 5.4, 6.1, 7.1, 8.1_
  
  - [x] 4.2 Create PreferencesRepository
    - Implement getPreferences() with default fallback
    - Implement getPreferencesFlow() with default mapping
    - Implement updatePreferences()
    - _Requirements: 13.4_
  
  - [ ]* 4.3 Write property test for non-repeating selection
    - **Property 7: Non-Repeating Selection**
    - **Validates: Requirements 4.1, 16.1**
  
  - [ ]* 4.4 Write property test for delivery recording
    - **Property 8: Delivery Recording**
    - **Validates: Requirements 4.2, 6.1, 16.4**
  
  - [ ]* 4.5 Write property test for unseen count accuracy
    - **Property 10: Unseen Count Accuracy**
    - **Validates: Requirements 5.4**

- [x] 5. Checkpoint - Ensure data layer tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 6. Implement business logic components
  - [x] 6.1 Create ContentSelector class
    - Implement selectNextMotivation() using repository
    - Implement isContentExhausted() check
    - Handle theme preference filtering
    - _Requirements: 4.1, 4.4, 16.2, 16.3_
  
  - [x] 6.2 Create NotificationScheduler class
    - Implement scheduleNotifications() main method
    - Implement computeNotificationTimes() for both modes
    - Implement parseTime() helper for time string parsing
    - Implement scheduleDailyRescheduler() for midnight refresh
    - Implement cancelAllNotifications() cleanup
    - Use deterministic work names (motivation_YYYYMMDD_index)
    - _Requirements: 1.3, 2.2, 2.3, 2.4, 15.2, 15.3_
  
  - [ ]* 6.3 Write property test for even distribution in time window mode
    - **Property 2: Even Distribution in Time Window Mode**
    - **Validates: Requirements 2.2**
  
  - [ ]* 6.4 Write property test for fixed times scheduling accuracy
    - **Property 3: Fixed Times Scheduling Accuracy**
    - **Validates: Requirements 2.3**
  
  - [ ]* 6.5 Write property test for scheduling determinism
    - **Property 29: Scheduling Determinism**
    - **Validates: Requirements 15.3**
  
  - [ ]* 6.6 Write property test for theme-biased selection
    - **Property 31: Theme-Biased Selection**
    - **Validates: Requirements 16.3**

- [ ] 7. Implement WorkManager workers
  - [x] 7.1 Create NotificationWorker
    - Implement doWork() with content selection logic
    - Get dependencies (database, repositories, selector)
    - Check for content exhaustion
    - Select next motivation item
    - Record delivery in history
    - Build and show notification with PendingIntent
    - Handle errors gracefully
    - _Requirements: 3.1, 3.2, 4.1, 4.2, 4.4_
  
  - [x] 7.2 Create SchedulerWorker for daily rescheduling
    - Implement doWork() to call NotificationScheduler
    - Handle errors and retry logic
    - _Requirements: 15.2_
  
  - [x] 7.3 Create notification channel in Application class
    - Define CHANNEL_ID constant
    - Create notification channel with appropriate importance
    - Set channel name and description
    - _Requirements: 3.1_
  
  - [ ]* 7.4 Write unit test for notification content completeness
    - Test that notification includes quote, author, image, themes
    - _Requirements: 3.1_
  
  - [ ]* 7.5 Write property test for notification intent correctness
    - **Property 6: Notification Intent Correctness**
    - **Validates: Requirements 3.2**

- [ ] 8. Implement seed data loading
  - [x] 8.1 Create seed data JSON file in assets
    - Create motivations.json with minimum 100 items
    - Include diverse quotes with proper attribution
    - Ensure all items have required fields
    - Use android.resource:// URIs for bundled images
    - _Requirements: 10.2, 17.1_
  
  - [x] 8.2 Create SeedDataLoader class
    - Implement loadSeedData() to parse JSON
    - Validate each item before insertion
    - Handle parsing errors gracefully
    - Track loading status in SharedPreferences
    - Ensure idempotent loading (only once)
    - _Requirements: 22.1, 22.2, 22.4_
  
  - [x] 8.3 Call SeedDataLoader on first app launch
    - Check if seed data already loaded
    - Load seed data in background coroutine
    - Show loading indicator during seed load
    - Handle failures with user-friendly error message
    - _Requirements: 22.1, 22.3_
  
  - [x] 8.4 Write property test for seed data validation
    - **Property 39: Seed Data Validation**
    - **Validates: Requirements 22.2**
  
  - [x] 8.5 Write property test for seed loading idempotence
    - **Property 40: Seed Loading Idempotence**
    - **Validates: Requirements 22.4**

- [x] 9. Checkpoint - Ensure business logic and workers tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 10. Implement ViewModels
  - [x] 10.1 Create HomeViewModel
    - Define HomeUiState sealed class (Loading, Success, Empty, Error)
    - Implement loadLatestMotivation() using repository
    - Expose uiState as StateFlow
    - Implement triggerManualNotification() for manual trigger
    - Handle errors and update state accordingly
    - _Requirements: 12.1, 12.2, 12.3, 12.4_
  
  - [x] 10.2 Create HistoryViewModel
    - Define HistoryUiState sealed class and HistoryGroup data class
    - Implement loadHistory() with date grouping
    - Implement formatDateLabel() for Today/Yesterday/dates
    - Expose uiState as StateFlow
    - Handle errors and update state accordingly
    - _Requirements: 7.1, 7.2, 7.3, 8.1, 8.2_
  
  - [x] 10.3 Create SettingsViewModel
    - Expose preferences as StateFlow from repository
    - Implement updateNotificationsPerDay() with validation (1-10)
    - Implement updateScheduleMode() with rescheduling
    - Implement updateTimeWindow() with rescheduling
    - Implement toggleNotifications() with rescheduling
    - Implement clearHistory() for Replay Classics
    - _Requirements: 1.1, 1.3, 2.1, 2.4, 5.3, 13.2, 13.4_
  
  - [ ]* 10.4 Write property test for settings persistence
    - **Property 27: Settings Persistence**
    - **Validates: Requirements 13.4**
  
  - [ ]* 10.5 Write property test for notification toggle effect
    - **Property 28: Notification Toggle Effect**
    - **Validates: Requirements 13.2**
  
  - [ ]* 10.6 Write unit test for notification frequency validation
    - Test that values outside [1,10] are rejected
    - _Requirements: 1.1_

- [x] 11. Implement Compose UI screens
  - [x] 11.1 Create HomeScreen composable
    - Display latest motivation card with full content
    - Show today count and unseen count
    - Handle Loading, Success, Empty, and Error states
    - Implement "Send one now" button
    - Use large typography and proper spacing
    - Support dark mode
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 18.1, 18.2_
  
  - [x] 11.2 Create HistoryScreen composable
    - Display grouped list with date headers (Today, Yesterday, dates)
    - Show history cards with time, author, quote preview, thumbnail
    - Handle Loading, Success, Empty, and Error states
    - Implement LazyColumn for efficient scrolling
    - Support dark mode
    - _Requirements: 7.1, 7.2, 7.3, 8.1, 8.2, 8.4_
  
  - [x] 11.3 Create SettingsScreen composable
    - Display notifications per day slider (1-10)
    - Display schedule mode selector (Time Window / Fixed Times)
    - Display time pickers for start/end times
    - Display notification enable/disable toggle
    - Display "Clear History" button with confirmation dialog
    - Display "Replay Classics" button (when content exhausted)
    - Show warning if system notifications disabled
    - Support dark mode
    - _Requirements: 1.1, 2.1, 3.4, 13.1, 13.2, 13.3_
  
  - [x] 11.4 Create DetailScreen composable
    - Display full quote text with large typography
    - Display author name prominently
    - Display historical context (if available)
    - Display full-size image
    - Display themes as chips
    - Display source attribution with link
    - Display delivery timestamp
    - Support dark mode
    - _Requirements: 9.2, 18.1, 18.2_
  
  - [x] 11.5 Create bottom navigation bar
    - Add three tabs: Home, History, Settings
    - Highlight active tab
    - Handle tab switching
    - Ensure minimum 48dp touch targets
    - _Requirements: 11.1, 11.2, 11.4, 18.3_
  
  - [ ]* 11.6 Write unit test for navigation tab switching
    - Test that selecting each tab displays correct screen
    - _Requirements: 11.2_
  
  - [ ]* 11.7 Write unit test for date label formatting
    - Test Today, Yesterday, and date string labels
    - _Requirements: 8.2_

- [x] 12. Implement navigation and app structure
  - [x] 12.1 Create MainActivity with Compose setup
    - Set up Compose theme with light/dark mode support
    - Initialize navigation controller
    - Set up bottom navigation with NavHost
    - Define navigation routes for all screens
    - Handle deep links from notifications
    - _Requirements: 3.2, 11.1, 18.2_
  
  - [x] 12.2 Create Application class
    - Initialize notification channel
    - Initialize Room database
    - Load seed data on first launch
    - Schedule initial notifications
    - _Requirements: 3.3, 22.1_
  
  - [ ]* 12.3 Write property test for navigation state preservation
    - **Property 24: Navigation State Preservation**
    - **Validates: Requirements 11.3**

- [ ] 13. Implement image loading with Coil
  - [x] 13.1 Set up Coil image loader
    - Add Coil dependency
    - Configure image loader with caching
    - Create composable for AsyncImage with placeholder
    - _Requirements: 17.2, 17.3_
  
  - [x] 13.2 Add placeholder images for loading and errors
    - Create placeholder drawable
    - Handle image loading failures gracefully
    - _Requirements: 17.3_
  
  - [ ]* 13.3 Write property test for bundled image URI format
    - **Property 32: Bundled Image URI Format**
    - **Validates: Requirements 17.1**

- [x] 14. Implement accessibility features
  - [x] 14.1 Add content descriptions to all interactive elements
    - Add contentDescription to images
    - Add contentDescription to buttons
    - Add contentDescription to navigation tabs
    - Test with TalkBack
    - _Requirements: 18.4_
  
  - [x] 14.2 Write property test for accessibility content descriptions
    - **Property 34: Accessibility Content Descriptions**
    - **Validates: Requirements 18.4**

- [x] 15. Checkpoint - Ensure UI and integration tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 16. Implement critical property-based tests
  - [x]* 16.1 Write property test for global non-repetition (1000 iterations)
    - **Property 37: Global Non-Repetition**
    - **Validates: Requirements 20.2**
    - Run 1000 iterations to thoroughly test non-repetition
  
  - [x]* 16.2 Write property test for no duplicate notifications (1000 iterations)
    - **Property 36: No Duplicate Notifications**
    - **Validates: Requirements 20.1**
    - Run 1000 iterations to ensure scheduling uniqueness
  
  - [x]* 16.3 Write property test for concurrent access safety
    - **Property 38: Concurrent Access Safety**
    - **Validates: Requirements 20.4**
    - Test with random concurrent operations

- [x] 17. Implement error handling and edge cases
  - [x] 17.1 Add error handling for content exhaustion
    - Display exhaustion message when unseen count = 0
    - Show "Replay Classics" option
    - Stop scheduling notifications
    - _Requirements: 4.4, 5.1, 5.2_
  
  - [x] 17.2 Add error handling for system notifications disabled
    - Check notification permission status
    - Display warning in Settings
    - Provide button to open system settings
    - _Requirements: 3.4_
  
  - [x] 17.3 Add error handling for database failures
    - Wrap all database operations in try-catch
    - Display user-friendly error messages
    - Log errors for debugging
    - _Requirements: 22.3_
  
  - [ ]* 17.4 Write unit tests for edge cases
    - Test empty history state
    - Test content exhaustion state
    - Test system notifications disabled
    - Test invalid time strings
    - Test boundary values (1 and 10 notifications per day)

- [x] 18. Performance optimization and testing
  - [x] 18.1 Add database indexes for performance
    - Verify indexes on delivery_history (itemId, dateKey, shownAt)
    - Add composite indexes if needed
    - _Requirements: 19.2_
  
  - [ ]* 18.2 Write property test for history query performance
    - **Property 35: History Query Performance**
    - **Validates: Requirements 19.1**
    - Test with datasets up to 10,000 items
    - Verify queries complete in < 300ms

- [x] 19. Final integration and polish
  - [x] 19.1 Test notification scheduling end-to-end
    - Schedule notifications with various preferences
    - Verify notifications appear at correct times
    - Test notification tap navigation
    - Test after device reboot (manual)
    - _Requirements: 2.2, 2.3, 3.1, 3.2_
  
  - [x] 19.2 Test complete user flows
    - First launch → seed loading → notification setup
    - Receive notification → tap → view detail
    - Browse history → view past motivations
    - Change settings → verify rescheduling
    - Exhaust content → Replay Classics → reset
    - _Requirements: All_
  
  - [x] 19.3 Polish UI and animations
    - Add smooth transitions between screens
    - Add loading indicators
    - Improve empty states
    - Ensure consistent spacing and typography
    - _Requirements: 18.1_

- [x] 20. Final checkpoint - Comprehensive testing
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at key milestones
- Property tests validate universal correctness properties with 100+ iterations
- Critical properties (non-repetition, scheduling uniqueness) use 1000 iterations
- Unit tests validate specific examples, edge cases, and error conditions
- The implementation follows a bottom-up approach: data layer → business logic → UI
- All code should support both light and dark modes
- All interactive elements should have minimum 48dp touch targets
- Use Kotlin coroutines for all asynchronous operations
- Use Jetpack Compose for all UI screens
- Use Room for database persistence
- Use WorkManager for reliable background scheduling
- Use Coil for image loading
- Use Kotest for property-based testing
