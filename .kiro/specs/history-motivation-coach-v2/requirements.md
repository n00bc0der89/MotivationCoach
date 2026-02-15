# Requirements Document: History Motivation Coach v2

## Introduction

This document specifies the requirements for version 2 of the History Motivation Coach Android app. Version 2 introduces three major enhancements: personality images for famous figures, a redesigned home page with manual notification triggering, and advanced scheduling capabilities with configurable time windows and schedule modes.

The app is built with Kotlin, Jetpack Compose, Room database, and WorkManager. The existing architecture includes data entities (MotivationItem, DeliveryHistory, UserPreferences), repositories, ViewModels, and UI screens. Version 2 builds upon this foundation to provide a more engaging and flexible user experience.

## Glossary

- **System**: The History Motivation Coach Android application
- **Personality_Image**: A visual representation (photo or illustration) of a historical figure, philosopher, or leader associated with a motivational quote
- **Schedule_Mode**: A configuration option that determines when notifications are sent (e.g., work hours only, weekends only, all days, custom days)
- **Time_Window**: A configurable time range (start time to end time) during which notifications can be sent
- **Manual_Notification**: A user-triggered notification that delivers a motivation immediately, independent of the scheduled notifications
- **Home_Screen**: The main screen displaying the latest motivation and app statistics
- **Settings_Screen**: The configuration screen where users manage notification preferences, schedule modes, and time windows
- **MotivationItem**: A Room database entity containing quote, author, context, imageUri, themes, and source information
- **UserPreferences**: A Room database entity storing user configuration including schedule mode, time windows, and notification settings
- **WorkManager**: Android's background task scheduler used for scheduling notifications

## Requirements

### Requirement 1: Personality Image Display

**User Story:** As a user, I want to see images of famous personalities when viewing their quotes, so that I can visually connect with the historical figures and feel more engaged with the content.

#### Acceptance Criteria

1. WHEN a motivation with a valid imageUri is displayed, THE System SHALL load and display the personality image
2. WHEN the personality image is loading, THE System SHALL display a placeholder or loading indicator
3. IF the personality image fails to load, THEN THE System SHALL display a fallback placeholder image
4. WHEN displaying the personality image, THE System SHALL maintain proper aspect ratio and image quality
5. THE System SHALL cache loaded personality images to improve performance and reduce network usage
6. WHEN a user views a motivation in the Home_Screen, THE System SHALL display the personality image prominently
7. WHEN a user views a motivation in the Detail screen, THE System SHALL display a larger version of the personality image
8. WHEN a user views motivations in the History_Screen, THE System SHALL display thumbnail versions of personality images

### Requirement 2: Home Page Redesign

**User Story:** As a user, I want a professionally designed home page with a philosophical theme, so that the app feels polished and aligns with the inspirational nature of the content.

#### Acceptance Criteria

1. THE Home_Screen SHALL use a professional philosophical theme with enhanced visual design
2. THE Home_Screen SHALL display the latest motivation with its personality image in a visually appealing card layout
3. THE Home_Screen SHALL show today's motivation count and total unseen motivations
4. THE Home_Screen SHALL provide clear visual hierarchy emphasizing the quote and author
5. THE Home_Screen SHALL use appropriate typography, spacing, and color scheme consistent with a philosophical aesthetic
6. WHEN the Home_Screen is displayed, THE System SHALL ensure all text is readable with sufficient contrast
7. THE Home_Screen SHALL be responsive and adapt to different screen sizes and orientations

### Requirement 3: Manual Notification Trigger

**User Story:** As a user, I want to trigger notifications manually from the home page, so that I can receive motivational content on-demand when I need inspiration.

#### Acceptance Criteria

1. THE Home_Screen SHALL display a "Send Manual Notification" button or action
2. WHEN a user taps the manual notification button, THE System SHALL immediately select and deliver a new motivation
3. WHEN a Manual_Notification is triggered, THE System SHALL follow the same content selection logic as scheduled notifications
4. WHEN a Manual_Notification is delivered, THE System SHALL record it in the delivery history
5. WHEN a Manual_Notification is triggered, THE System SHALL update the Home_Screen to display the newly delivered motivation
6. IF no unseen motivations are available, THEN THE System SHALL inform the user and prevent manual notification triggering
7. WHEN a Manual_Notification is triggered, THE System SHALL not interfere with scheduled notifications

### Requirement 4: Schedule Modes

**User Story:** As a user, I want to configure when notifications are sent using schedule modes, so that I receive motivations only during times that suit my lifestyle and work patterns.

#### Acceptance Criteria

1. THE Settings_Screen SHALL provide schedule mode options including: All Days, Weekdays Only, Weekends Only, and Custom Days
2. WHEN a user selects a schedule mode, THE System SHALL persist the selection in UserPreferences
3. WHEN the "All Days" mode is selected, THE System SHALL schedule notifications every day of the week
4. WHEN the "Weekdays Only" mode is selected, THE System SHALL schedule notifications only on Monday through Friday
5. WHEN the "Weekends Only" mode is selected, THE System SHALL schedule notifications only on Saturday and Sunday
6. WHEN the "Custom Days" mode is selected, THE System SHALL allow users to select specific days of the week
7. THE System SHALL respect the selected schedule mode when scheduling notifications via WorkManager
8. WHEN a schedule mode is changed, THE System SHALL reschedule all pending notifications according to the new mode

### Requirement 5: Time Window Configuration

**User Story:** As a user, I want to set start and end times for when notifications should be sent, so that I don't receive motivations during inappropriate hours like late at night or early morning.

#### Acceptance Criteria

1. THE Settings_Screen SHALL provide time pickers for configuring notification start time and end time
2. WHEN a user sets a start time, THE System SHALL validate that it is before the end time
3. WHEN a user sets an end time, THE System SHALL validate that it is after the start time
4. THE System SHALL persist the configured Time_Window in UserPreferences
5. WHEN scheduling notifications, THE System SHALL only schedule them within the configured Time_Window
6. THE System SHALL distribute notifications evenly within the Time_Window based on notificationsPerDay setting
7. IF the Time_Window is too narrow for the requested notificationsPerDay, THEN THE System SHALL inform the user
8. WHEN the Time_Window is changed, THE System SHALL reschedule all pending notifications according to the new window

### Requirement 6: Schedule Mode and Time Window Integration

**User Story:** As a system administrator, I want schedule modes and time windows to work together seamlessly, so that notifications are sent only on selected days within the configured time range.

#### Acceptance Criteria

1. WHEN scheduling notifications, THE System SHALL apply both the schedule mode and Time_Window constraints
2. WHEN a day is excluded by the schedule mode, THE System SHALL not schedule any notifications for that day regardless of the Time_Window
3. WHEN a day is included by the schedule mode, THE System SHALL schedule notifications within the Time_Window for that day
4. THE System SHALL maintain consistency between schedule mode and Time_Window settings in the database
5. WHEN either schedule mode or Time_Window is modified, THE System SHALL recalculate and reschedule all pending notifications

### Requirement 7: Notification Scheduling with WorkManager

**User Story:** As a developer, I want the notification scheduling logic to properly integrate schedule modes and time windows with WorkManager, so that the system reliably delivers notifications according to user preferences.

#### Acceptance Criteria

1. THE System SHALL use WorkManager to schedule notifications based on schedule mode and Time_Window
2. WHEN calculating notification times, THE System SHALL respect both day-of-week constraints and time-of-day constraints
3. THE System SHALL schedule notifications as OneTimeWorkRequests with exact timing
4. WHEN a notification is delivered, THE System SHALL automatically schedule the next notification according to current preferences
5. THE System SHALL handle device reboots by rescheduling notifications based on persisted preferences
6. THE System SHALL cancel and reschedule all pending notifications when preferences change
7. WHEN the app is disabled, THE System SHALL cancel all pending notification work requests

### Requirement 8: Settings UI for Schedule Configuration

**User Story:** As a user, I want an intuitive settings interface for configuring schedule modes and time windows, so that I can easily customize when I receive motivations.

#### Acceptance Criteria

1. THE Settings_Screen SHALL display schedule mode options in a clear, selectable format (radio buttons or dropdown)
2. WHEN "Custom Days" mode is selected, THE Settings_Screen SHALL display day-of-week checkboxes
3. THE Settings_Screen SHALL display time pickers for start time and end time configuration
4. THE Settings_Screen SHALL show the current schedule mode and Time_Window settings
5. WHEN a user changes settings, THE Settings_Screen SHALL provide immediate visual feedback
6. THE Settings_Screen SHALL validate user inputs and display error messages for invalid configurations
7. THE Settings_Screen SHALL maintain the existing settings for notifications per day and preferred themes
8. THE Settings_Screen SHALL use Material Design 3 components consistent with the app's design language

### Requirement 9: Image Asset Management

**User Story:** As a developer, I want a systematic approach to managing personality images, so that the app can efficiently load and display images for all motivational content.

#### Acceptance Criteria

1. THE System SHALL support loading images from drawable resources using android.resource:// URIs
2. THE System SHALL support loading images from remote URLs using https:// URIs
3. WHEN loading images, THE System SHALL use an image loading library (e.g., Coil) for efficient caching and loading
4. THE System SHALL provide a default placeholder image for motivations without personality images
5. THE System SHALL handle image loading errors gracefully without crashing
6. WHEN images are loaded from remote sources, THE System SHALL cache them locally for offline access
7. THE System SHALL optimize image loading to minimize memory usage and prevent OutOfMemory errors

### Requirement 10: Data Migration

**User Story:** As a developer, I want to handle database schema changes properly, so that existing users can upgrade to v2 without losing their data or experiencing crashes.

#### Acceptance Criteria

1. WHEN the app is upgraded to v2, THE System SHALL migrate the existing UserPreferences schema to include new schedule mode fields
2. THE System SHALL provide default values for new schedule mode fields during migration
3. THE System SHALL preserve existing user preferences (notificationsPerDay, startTime, endTime, enabled, preferredThemes) during migration
4. THE System SHALL increment the Room database version number
5. THE System SHALL implement a Room Migration strategy to handle the schema change
6. IF migration fails, THEN THE System SHALL handle the error gracefully and inform the user
7. THE System SHALL test the migration path to ensure data integrity

### Requirement 11: Backward Compatibility

**User Story:** As a user upgrading from v1, I want my existing settings and history to be preserved, so that I don't lose my progress or have to reconfigure the app.

#### Acceptance Criteria

1. WHEN upgrading from v1 to v2, THE System SHALL preserve all existing MotivationItem records
2. WHEN upgrading from v1 to v2, THE System SHALL preserve all existing DeliveryHistory records
3. WHEN upgrading from v1 to v2, THE System SHALL preserve existing UserPreferences and apply sensible defaults for new fields
4. THE System SHALL maintain the existing notification scheduling behavior for users who don't modify new settings
5. THE System SHALL ensure that existing imageUri values in MotivationItem records remain valid
6. WHEN upgrading, THE System SHALL not require users to reconfigure their preferences unless they want to use new features

### Requirement 12: Performance and Resource Management

**User Story:** As a user, I want the app to perform smoothly and efficiently, so that image loading and notification scheduling don't drain my battery or consume excessive data.

#### Acceptance Criteria

1. WHEN loading personality images, THE System SHALL use efficient image caching to minimize network requests
2. THE System SHALL load images asynchronously to prevent UI blocking
3. THE System SHALL optimize image sizes for different display contexts (thumbnail, card, full-screen)
4. THE System SHALL limit memory usage when loading multiple images in the History_Screen
5. WHEN scheduling notifications, THE System SHALL minimize battery usage by using WorkManager's optimized scheduling
6. THE System SHALL avoid unnecessary rescheduling of notifications when preferences haven't changed
7. THE System SHALL release image resources properly to prevent memory leaks
