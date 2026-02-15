# Implementation Plan: History Motivation Coach v2

## Overview

This implementation plan breaks down the v2 enhancements into discrete, incremental coding tasks. The plan follows a logical progression: data layer changes (migration), business logic enhancements (scheduling), UI updates (screens and image loading), and testing. Each task builds on previous work and includes validation through tests.

## Tasks

- [x] 1. Database migration and data model updates
  - [x] 1.1 Update UserPreferences entity with new schedule mode fields
    - Add scheduleMode enum field with ALL_DAYS, WEEKDAYS_ONLY, WEEKENDS_ONLY, CUSTOM_DAYS
    - Add customDays Set<DayOfWeek> field
    - Remove old scheduleMode and fixedTimes fields
    - Update default values
    - _Requirements: 4.1, 4.2, 10.1, 10.2_
  
  - [x] 1.2 Implement Room database migration from v1 to v2
    - Create MIGRATION_1_2 object
    - Add new columns with default values
    - Create new table and copy data (to remove old columns)
    - Increment database version to 2
    - _Requirements: 10.1, 10.3, 10.4, 10.5_
  
  - [ ]* 1.3 Write property test for migration data preservation
    - **Property 41: All data preserved during migration**
    - **Validates: Requirements 11.1, 11.2, 11.3**
  
  - [ ]* 1.4 Write property test for migration default values
    - **Property 39: Migration adds new fields with defaults**
    - **Validates: Requirements 10.2, 10.3, 11.3**
  
  - [ ]* 1.5 Write unit test for migration error handling
    - Test migration failure scenarios
    - Verify graceful error handling
    - _Requirements: 10.6_

- [x] 2. Schedule mode logic implementation
  - [x] 2.1 Implement ScheduleMode.getActiveDays() extension function
    - Return all days for ALL_DAYS
    - Return Monday-Friday for WEEKDAYS_ONLY
    - Return Saturday-Sunday for WEEKENDS_ONLY
    - Return customDays for CUSTOM_DAYS
    - _Requirements: 4.3, 4.4, 4.5, 4.6_
  
  - [ ]* 2.2 Write property tests for schedule mode day activation
    - **Property 16: All Days mode activates all days**
    - **Property 17: Weekdays Only mode activates Monday-Friday**
    - **Property 18: Weekends Only mode activates Saturday-Sunday**
    - **Property 19: Custom Days mode respects user selection**
    - **Validates: Requirements 4.3, 4.4, 4.5, 4.6**
  
  - [x] 2.3 Implement TimeWindow data class
    - Add calculateNotificationTimes() method for even distribution
    - Add isWideEnough() validation method
    - _Requirements: 5.6, 5.7_
  
  - [ ]* 2.4 Write property test for notification time distribution
    - **Property 24: Notifications evenly distributed in time window**
    - **Validates: Requirements 5.6**
  
  - [x] 2.5 Implement NotificationSchedule.calculateNextTime() method
    - Apply schedule mode day-of-week constraints
    - Apply time window time-of-day constraints
    - Look ahead up to 14 days for valid time
    - Return null if no valid time found
    - _Requirements: 6.1, 6.2, 6.3, 7.2_
  
  - [ ]* 2.6 Write property tests for notification scheduling constraints
    - **Property 26: Both constraints applied to scheduled notifications**
    - **Property 27: Excluded days have no notifications**
    - **Property 28: Included days have notifications in time window**
    - **Validates: Requirements 6.1, 6.2, 6.3, 7.2**

- [x] 3. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. NotificationScheduler implementation
  - [x] 4.1 Update NotificationScheduler interface
    - Add triggerManualNotification() method
    - Update scheduleNextNotification() to use new schedule logic
    - Add rescheduleAllNotifications() method
    - _Requirements: 3.2, 4.7, 4.8, 5.8_
  
  - [x] 4.2 Implement NotificationSchedulerImpl with WorkManager integration
    - Use calculateNextTime() for scheduling
    - Schedule OneTimeWorkRequests with exact timing
    - Handle rescheduling on preference changes
    - Implement manual notification triggering
    - _Requirements: 3.2, 4.7, 7.1, 7.3, 7.4_
  
  - [ ]* 4.3 Write property test for scheduled notification constraints
    - **Property 20: Scheduled notifications respect schedule mode**
    - **Property 23: Scheduled notifications within time window**
    - **Validates: Requirements 4.7, 5.5**
  
  - [ ]* 4.4 Write property test for preference change rescheduling
    - **Property 25: Preference changes trigger rescheduling**
    - **Validates: Requirements 4.8, 5.8, 6.5, 7.6**
  
  - [ ]* 4.5 Write property test for next notification scheduling
    - **Property 29: Next notification scheduled after delivery**
    - **Validates: Requirements 7.4**
  
  - [ ]* 4.6 Write unit test for device reboot handling
    - Test notification rescheduling after simulated reboot
    - _Requirements: 7.5_
  
  - [ ]* 4.7 Write property test for app disable cancellation
    - **Property 31: Disabling app cancels notifications**
    - **Validates: Requirements 7.7**

- [x] 5. Image loading integration with Coil
  - [x] 5.1 Add Coil dependency to build.gradle.kts
    - Add Coil Compose library
    - Configure image caching
    - _Requirements: 9.3_
  
  - [x] 5.2 Create PersonalityImage composable component
    - Use AsyncImage from Coil
    - Configure placeholder and error images
    - Support different content scales for different contexts
    - _Requirements: 1.1, 1.2, 1.3, 9.1, 9.2, 9.4_
  
  - [ ]* 5.3 Write property tests for image loading
    - **Property 1: Image loading with valid URI**
    - **Property 3: Image loading fallback on error**
    - **Property 36: Drawable resource URIs load successfully**
    - **Property 37: HTTPS URIs load successfully**
    - **Property 38: Null or empty imageUri shows placeholder**
    - **Validates: Requirements 1.1, 1.3, 9.1, 9.2, 9.4, 9.5**
  
  - [ ]* 5.4 Write property test for image caching
    - **Property 4: Image caching reduces network requests**
    - **Validates: Requirements 1.5, 9.6, 12.1**
  
  - [ ]* 5.5 Write property test for image size variation
    - **Property 5: Image size varies by context**
    - **Validates: Requirements 1.7, 1.8**
  
  - [ ]* 5.6 Write property test for async image loading
    - **Property 44: Image loading doesn't block UI thread**
    - **Validates: Requirements 12.2**

- [x] 6. HomeScreen redesign and manual notification
  - [x] 6.1 Update HomeViewModel with manual notification support
    - Add manualTriggerState StateFlow
    - Implement triggerManualNotification() method
    - Add refreshLatestMotivation() method
    - Inject NotificationScheduler dependency
    - _Requirements: 3.2, 3.4, 3.5_
  
  - [x] 6.2 Redesign HomeScreen UI with philosophical theme
    - Update card layout with enhanced visual design
    - Integrate PersonalityImage component
    - Add "Send Manual Notification" button
    - Update typography and spacing
    - Ensure responsive layout
    - _Requirements: 2.1, 2.2, 2.4, 2.5, 2.7, 3.1_
  
  - [ ]* 6.3 Write property tests for home screen display
    - **Property 6: Home screen displays latest motivation with image**
    - **Property 7: Home screen displays accurate counts**
    - **Property 9: Home screen renders on different configurations**
    - **Validates: Requirements 2.2, 2.3, 2.7**
  
  - [ ]* 6.4 Write property test for text contrast
    - **Property 8: Home screen text contrast meets accessibility standards**
    - **Validates: Requirements 2.6**
  
  - [ ]* 6.5 Write property tests for manual notification
    - **Property 10: Manual notification delivers new motivation**
    - **Property 11: Manual and scheduled notifications use same selection logic**
    - **Property 12: Manual notification recorded in history**
    - **Property 13: Manual notification updates home screen**
    - **Property 14: Manual notification doesn't affect scheduled notifications**
    - **Validates: Requirements 3.2, 3.3, 3.4, 3.5, 3.7**
  
  - [ ]* 6.6 Write unit test for empty state handling
    - Test manual notification when no unseen motivations available
    - _Requirements: 3.6_

- [x] 7. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. SettingsScreen enhancements for schedule configuration
  - [x] 8.1 Update SettingsViewModel with schedule mode support
    - Add updateScheduleMode() method
    - Add updateCustomDays() method
    - Add updateTimeWindow() method with validation
    - Add validateTimeWindow() method
    - Add validationState StateFlow
    - _Requirements: 4.2, 5.2, 5.3, 5.4, 8.6_
  
  - [x] 8.2 Update SettingsScreen UI with schedule mode options
    - Add schedule mode radio buttons/dropdown
    - Add custom days checkboxes (shown when CUSTOM_DAYS selected)
    - Update time pickers for start/end time
    - Add validation error messages
    - Maintain existing settings (notificationsPerDay, preferredThemes)
    - _Requirements: 4.1, 5.1, 8.1, 8.2, 8.3, 8.6, 8.7_
  
  - [ ]* 8.3 Write property tests for settings persistence
    - **Property 15: Schedule mode persisted to database**
    - **Property 22: Time window persisted to database**
    - **Validates: Requirements 4.2, 5.4**
  
  - [ ]* 8.4 Write property test for time window validation
    - **Property 21: Time window validation enforces start before end**
    - **Validates: Requirements 5.2, 5.3**
  
  - [ ]* 8.5 Write property tests for settings UI
    - **Property 32: Custom days UI appears for custom mode**
    - **Property 33: Current settings displayed correctly**
    - **Property 34: Invalid settings trigger error messages**
    - **Property 35: Existing settings preserved in v2**
    - **Validates: Requirements 8.2, 8.4, 8.6, 8.7**
  
  - [ ]* 8.6 Write unit test for narrow time window warning
    - Test validation when time window is too narrow for notificationsPerDay
    - _Requirements: 5.7_

- [x] 9. HistoryScreen image integration
  - [x] 9.1 Update HistoryScreen to display personality image thumbnails
    - Integrate PersonalityImage component with thumbnail size
    - Update list item layout to include image
    - Optimize for scrolling performance
    - _Requirements: 1.8, 12.4_
  
  - [ ]* 9.2 Write integration test for history screen image display
    - Test that all history items display images correctly
    - Test scrolling performance with many images
    - _Requirements: 1.8, 12.4_

- [x] 10. DetailScreen image enhancement
  - [x] 10.1 Update DetailScreen to display larger personality image
    - Integrate PersonalityImage component with full size
    - Update layout to accommodate larger image
    - _Requirements: 1.7_
  
  - [ ]* 10.2 Write integration test for detail screen image display
    - Test that detail screen displays larger image than home screen
    - _Requirements: 1.7_

- [x] 11. Backward compatibility and default behavior
  - [x]* 11.1 Write property test for v1 behavior preservation
    - **Property 42: Default settings maintain v1 behavior**
    - **Validates: Requirements 11.4**
  
  - [x]* 11.2 Write property test for existing imageUri validity
    - **Property 43: Existing imageUri values remain valid**
    - **Validates: Requirements 11.5**
  
  - [x]* 11.3 Write property test for unnecessary rescheduling prevention
    - **Property 45: Unchanged preferences don't trigger rescheduling**
    - **Validates: Requirements 12.6**

- [ ] 12. Integration testing and end-to-end flows
  - [ ]* 12.1 Write integration test for manual notification flow
    - Test: Home screen → Manual trigger → Notification delivery → History update
    - _Requirements: 3.2, 3.4, 3.5_
  
  - [ ]* 12.2 Write integration test for settings change flow
    - Test: Settings change → Notification rescheduling → WorkManager verification
    - _Requirements: 4.8, 5.8, 6.5_
  
  - [ ]* 12.3 Write integration test for migration flow
    - Test: v1 database → Migration → v2 database → App functionality
    - _Requirements: 10.1, 10.2, 10.3, 11.1, 11.2, 11.3_

- [ ] 13. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- Integration tests validate end-to-end user flows
- The implementation follows a logical progression: data layer → business logic → UI → testing
