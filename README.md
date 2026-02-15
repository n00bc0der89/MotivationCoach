# History Motivation Coach

An Android application that delivers daily motivational content through scheduled notifications.

## Project Setup

### Requirements
- Android Studio Hedgehog or later
- Java 17 (required for Gradle 8.5)
- Minimum SDK: API 28 (Android 9)
- Target SDK: API 34
- Kotlin 1.9.20
- Gradle 8.5

### Dependencies

#### Core Libraries
- **Jetpack Compose**: Modern declarative UI toolkit
- **Room Database**: Local data persistence
- **WorkManager**: Reliable background task scheduling
- **Coil**: Image loading and caching
- **Navigation Compose**: Screen navigation

#### Testing Libraries
- **Kotest**: Property-based testing framework
- **JUnit 4**: Unit testing framework
- **MockK**: Modern mocking library for Kotlin
- **Mockito**: Java mocking framework
- **Compose UI Test**: Jetpack Compose testing framework for UI verification
- **Espresso**: Android UI testing

### Build Configuration

The project uses Kotlin DSL for Gradle configuration with the following key features:
- **Gradle 8.5**: Upgraded for Java 17+ compatibility
- **Android Gradle Plugin 8.2.2**: Enhanced stability and Java 21 compatibility
- **KSP (Kotlin Symbol Processing)**: Version 1.9.20-1.0.14 for Room annotation processing
- **Compose Compiler**: Integrated with Kotlin 1.9.20
- **Java 17 Compatibility**: Required for Gradle 8.5
- **Release Signing**: Configured with keystore properties for Play Store deployment
- **Code Optimization**: ProGuard/R8 enabled with code shrinking and resource shrinking for release builds

**Important**: If using Android Studio, configure it to use Java 17 instead of the bundled Java 21:
1. Go to `Settings > Build, Execution, Deployment > Build Tools > Gradle`
2. Set "Gradle JDK" to Java 17 (typically at `/opt/homebrew/opt/openjdk@17` on macOS)
3. Sync project with Gradle files

See `GRADLE_FIX.md` for detailed setup instructions.

### Release Build Configuration

The project is configured for Play Store deployment with:
- **Keystore Signing**: Loads signing credentials from `keystore.properties` file
- **Graceful Fallback**: Builds succeed even without keystore (for development)
- **Code Shrinking**: R8 minification reduces APK size and improves performance
- **Resource Shrinking**: Removes unused resources from final build
- **ProGuard Rules**: Custom rules in `proguard-rules.pro` protect Room entities and Kotlin metadata

To build a release APK/AAB:
```bash
# Build Android App Bundle (for Play Store)
./gradlew bundleRelease

# Build APK (for direct distribution)
./gradlew assembleRelease
```

See `PLAY_STORE_DEPLOYMENT_GUIDE.md` for complete deployment instructions.

### Permissions

The app requires the following Android permissions:
- **INTERNET**: Required for loading remote personality images from URLs (https://)
- **POST_NOTIFICATIONS**: Required for displaying motivational notifications (Android 13+)
- **RECEIVE_BOOT_COMPLETED**: Required for rescheduling notifications after device reboot
- **SCHEDULE_EXACT_ALARM**: Required for precise notification timing

### Getting Started

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Run the app on an emulator or physical device (API 28+)

### Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/historymotivationcoach/
│   │   │   ├── MotivationApplication.kt  # App initialization
│   │   │   ├── MainActivity.kt
│   │   │   ├── data/                     # Data layer
│   │   │   │   ├── AppDatabase.kt
│   │   │   │   ├── Converters.kt
│   │   │   │   ├── SeedDataLoader.kt
│   │   │   │   ├── entity/               # Room entities
│   │   │   │   ├── dao/                  # Data access objects
│   │   │   │   └── repository/           # Repository pattern
│   │   │   ├── business/                 # Business logic
│   │   │   │   ├── ContentSelector.kt
│   │   │   │   ├── NotificationScheduler.kt
│   │   │   │   ├── NotificationWorker.kt
│   │   │   │   └── SchedulerWorker.kt
│   │   │   ├── viewmodel/                # ViewModels
│   │   │   │   ├── HomeViewModel.kt      # ✅ Implemented
│   │   │   │   ├── HistoryViewModel.kt   # ✅ Implemented
│   │   │   │   └── SettingsViewModel.kt  # ✅ Implemented
│   │   │   └── ui/                       # Compose UI
│   │   │   │   ├── components/           # Reusable UI components
│   │   │   │   │   ├── MotivationImage.kt    # ✅ Implemented
│   │   │   │   │   └── PersonalityImage.kt   # ✅ Implemented (v2)
│   │   │   │   ├── screens/
│   │   │   │   │   ├── HomeScreen.kt     # ✅ Implemented
│   │   │   │   │   ├── HistoryScreen.kt  # ✅ Implemented
│   │   │   │   │   ├── SettingsScreen.kt # ✅ Implemented
│   │   │   │   │   └── DetailScreen.kt   # ✅ Implemented
│   │   │   │   ├── navigation/
│   │   │   │   │   └── Navigation.kt     # ✅ Implemented
│   │   │   │   └── theme/                # Material 3 theme
│   │   │   │       ├── Color.kt
│   │   │   │       ├── Theme.kt
│   │   │   │       └── Type.kt
│   │   ├── assets/
│   │   │   └── motivations.json          # 102 seed quotes
│   │   ├── res/                          # Resources
│   │   └── AndroidManifest.xml
│   └── test/                             # Unit tests
└── build.gradle.kts
```

## Architecture

The app follows MVVM (Model-View-ViewModel) architecture with clean separation of concerns:

### Data Layer ✅
- **Room Database**: Persistent storage for motivation items, delivery history, and user preferences
  - **Database Version**: v2 with enhanced schedule mode support
  - **UserPreferences**: Extended with schedule modes (ALL_DAYS, WEEKDAYS_ONLY, WEEKENDS_ONLY, CUSTOM_DAYS) and custom day selection
  - **ScheduleMode Extension**: `getActiveDays()` function converts schedule modes to active day sets
- **DAOs**: Type-safe database access with coroutine support
- **Repositories**: Clean API for data operations with business logic
  - `MotivationRepository`: Open class supporting inheritance for test fakes
  - `PreferencesRepository`: Open class supporting inheritance for test fakes
- **SeedDataLoader**: Loads 102 motivational quotes from JSON on first launch

### Business Logic Layer ✅
- **ContentSelector**: Implements non-repeating content selection with theme filtering
- **NotificationScheduler**: Interface-based design for flexible notification scheduling
  - `NotificationScheduler` interface: Defines scheduling contract with methods for next notification, rescheduling, cancellation, manual triggering, and time calculation
  - `NotificationSchedulerImpl`: WorkManager-based implementation with schedule mode and time window support
  - Supports v2 features: manual notifications, schedule modes (ALL_DAYS, WEEKDAYS_ONLY, WEEKENDS_ONLY, CUSTOM_DAYS), and configurable time windows
- **NotificationWorker**: Delivers notifications at scheduled times
- **SchedulerWorker**: Reschedules notifications daily at midnight

### Presentation Layer ✅
- **ViewModels**: State management with Kotlin Flow
  - `HomeViewModel`: ✅ Manages home screen state with latest motivation, statistics, and manual notification triggering
  - `HistoryViewModel`: ✅ Manages history screen with date grouping and formatting
  - `SettingsViewModel`: ✅ Manages user preferences with comprehensive validation and rescheduling
    - Schedule mode management (ALL_DAYS, WEEKDAYS_ONLY, WEEKENDS_ONLY, CUSTOM_DAYS)
    - Custom day selection for CUSTOM_DAYS mode
    - Time window validation with LocalTime parsing and ChronoUnit calculations
    - Minimum interval checking (30 minutes between notifications)
    - Real-time validation state updates with error messages
    - Automatic notification rescheduling on preference changes
- **Compose UI**: Modern declarative UI with Material 3
  - **Reusable Components**:
    - `MotivationImage`: ✅ Generic image loading component with Coil integration
    - `PersonalityImage`: ✅ Specialized component for personality images with placeholder and error handling (v2)
  - **Screens**:
    - `HomeScreen`: ✅ Displays latest motivation with statistics and empty/error states
    - `HistoryScreen`: ✅ Grouped timeline view with date headers, personality image thumbnails, and smooth animations
      - PersonalityImage integration for 80dp thumbnails with Coil caching
      - Multiple animation effects: slide-in headers, staggered item entry, pulsing loading, scaling empty state
      - Efficient LazyColumn scrolling with proper content padding
      - Rich history cards displaying time, author, quote preview, and thumbnail
    - `SettingsScreen`: ✅ Comprehensive settings with sliders, toggles, and time pickers
    - `DetailScreen`: ✅ Full motivation view with rich content display and personality images
      - Large personality image (400dp height) with Coil integration and crop scaling
      - Animated state transitions (loading, error, content) with crossfade effects
      - Comprehensive content cards: quote, author, historical context, themes, source attribution, delivery info
      - Theme chips displayed as Material 3 SuggestionChips with accessibility support
      - Clickable source links using LocalUriHandler for external navigation
      - Full timestamp formatting (e.g., "December 25, 2024 at 3:45 PM")
      - Delivery status display (Scheduled, Delivered, Failed)
      - Pulsing loading animation with infinite transitions
      - Error state with emoji, message, and back navigation
      - Vertical scrolling for long content with proper spacing
- **Navigation**: ✅ Bottom navigation with three tabs (Home, History, Settings)

### Application Initialization ✅
- **MotivationApplication**: 
  - Creates notification channel for Android O+
  - Initializes database on first launch
  - Loads seed data automatically in background coroutine
  - Schedules initial notifications after successful seed loading
  - Exposes initialization state via StateFlow (Loading, Success, Error)
  - Handles errors gracefully with user-friendly messages and logging
  - Singleton pattern for global access to initialization state
  - Implements ImageLoaderFactory for Coil image caching configuration

## Implementation Status

### Completed Tasks (1-20) ✅
- ✅ Project structure and dependencies
- ✅ Room database schema (MotivationItem, DeliveryHistory, UserPreferences)
- ✅ Data Access Objects with efficient queries
- ✅ Repository layer with clean APIs
- ✅ ContentSelector business logic
- ✅ NotificationScheduler with time computation
- ✅ WorkManager workers (NotificationWorker, SchedulerWorker)
- ✅ Notification channel setup
- ✅ Seed data JSON (102 quotes)
- ✅ SeedDataLoader with validation
- ✅ Application initialization with automatic seed loading and state management
- ✅ Initial notification scheduling after seed loading
- ✅ Coil ImageLoader configuration with memory and disk caching
- ✅ **All ViewModels** (HomeViewModel, HistoryViewModel, SettingsViewModel)
- ✅ **All Compose UI screens** (HomeScreen, HistoryScreen, SettingsScreen, DetailScreen)
- ✅ **Bottom navigation** with three tabs
- ✅ **MainActivity** with navigation and deep linking
- ✅ **Accessibility features** with content descriptions
- ✅ **Error handling** for content exhaustion, disabled notifications, and database failures
- ✅ **Performance optimization** with database indexes
- ✅ **End-to-end integration testing** and user flow validation
- ✅ **UI polish** with animations, loading indicators, and empty states
- ✅ **Release build configuration** with signing, ProGuard, and code optimization

### Property-Based Testing ✅
All critical property-based tests implemented with Kotest:
- ✅ **Property 34: Accessibility Content Descriptions** (Task 14.2)
  - Validates all interactive UI elements have proper content descriptions
  - Tests buttons, switches, sliders, cards, chips, images, navigation items
  - Verifies TalkBack and screen reader compatibility
- ✅ **Property 36: No Duplicate Notifications** (Task 16.2)
  - 1000 iterations testing scheduling uniqueness
  - Validates deterministic work name generation (motivation_YYYYMMDD_index)
  - Ensures no duplicate notifications for same time slot
- ✅ **Property 37: Global Non-Repetition** (Task 16.1)
  - 1000 iterations testing core value proposition
  - Guarantees users never see same motivation twice
  - Tests across various database states and selection scenarios
- ✅ **Property 38: Concurrent Access Safety** (Task 16.3)
  - Tests data consistency with concurrent operations
  - Validates atomic delivery recording
  - Ensures no race conditions in content selection
- ✅ **Property 39: Seed Data Validation** (Task 8.4)
  - Validates all required fields in seed data
  - Ensures proper attribution and licensing
- ✅ **Property 40: Seed Loading Idempotence** (Task 8.5)
  - Guarantees seed data loads only once
  - Tests across multiple initialization attempts
- ✅ **Property 42: Default Settings Maintain v1 Behavior** (Task 11.1, v2)
  - Validates backward compatibility with v1 scheduling
  - Tests that ALL_DAYS mode with default settings behaves identically to v1
  - Verifies all 7 days are active with default v2 settings
  - Ensures smooth upgrade path for existing users
- ✅ **Property 43: Existing imageUri Values Remain Valid** (Task 11.2, v2)
  - Validates imageUri format preservation during v1 to v2 migration
  - Tests android.resource:// and https:// URI schemes
  - Ensures no whitespace or invalid characters in URIs
  - Verifies proper URI structure for both drawable resources and remote images
- ✅ **Property 45: Unchanged Preferences Don't Trigger Rescheduling** (Task 11.3, v2)
  - Validates performance optimization for settings screen
  - Tests that identical preference objects are properly detected as equal
  - Ensures no unnecessary notification rescheduling when values don't change
  - Verifies all preference fields (including v2 additions) are compared correctly

### Implementation Complete 🎉
The History Motivation Coach app is fully implemented and ready for Play Store deployment with:
- Complete data layer with Room database
- Robust business logic with non-repeating content selection
- Reliable WorkManager-based notification scheduling
- Modern Jetpack Compose UI with Material 3
- Comprehensive testing including property-based tests
- Full accessibility support
- Error handling and edge case coverage
- Production-ready release build configuration with code optimization

**Next Steps**: Follow the `PLAY_STORE_DEPLOYMENT_GUIDE.md` to prepare and publish your app to the Google Play Store.

## Key Features

### Core Functionality ✅
- **Non-Repeating Content**: Database-backed guarantee that users never see the same quote twice (validated with 1000-iteration property tests)
- **Manual Notification Trigger** (v2): On-demand motivation delivery via HomeViewModel with state tracking and automatic UI refresh
- **Advanced Scheduling Modes** (v2): 
  - **ALL_DAYS**: Notifications every day of the week
  - **WEEKDAYS_ONLY**: Monday through Friday only
  - **WEEKENDS_ONLY**: Saturday and Sunday only
  - **CUSTOM_DAYS**: User-selectable days of the week
- **Time Window Configuration**: Configurable start and end times for notification delivery
- **Reliable Delivery**: WorkManager ensures notifications survive device reboots
- **Automatic Seed Loading**: 102 quotes load automatically on first launch with state tracking
- **Initial Notification Scheduling**: Notifications are automatically scheduled after successful seed loading
- **Theme Preferences**: Content selection can be biased toward preferred themes
- **Graceful Error Handling**: Comprehensive error handling with user-friendly messages
- **Content Exhaustion Handling**: "Replay Classics" feature to reset history when all content seen

### User Interface ✅
- **Home Screen**: Latest motivation display with statistics (today's count, unseen count)
- **History Screen**: Timeline view with date grouping (Today, Yesterday, dates) and personality image thumbnails
  - Displays 80dp personality image thumbnails for each history item
  - Smooth animations: slide-in for headers, staggered entry for items, pulsing loading indicator, scaling empty state
  - Efficient scrolling with LazyColumn and proper content padding
  - Rich history cards with time, author, quote preview, and thumbnail
  - Integrated PersonalityImage component for consistent image loading
- **Settings Screen**: Comprehensive preferences management with v2 enhancements
  - Notifications per day slider (1-10)
  - Schedule mode selector (All Days / Weekdays Only / Weekends Only / Custom Days)
  - Custom day selection with checkboxes (shown when Custom Days mode is active)
  - Time pickers for configurable time window (start/end times)
  - Real-time validation with error messages for invalid configurations
  - Notification enable/disable toggle
  - Clear History and Replay Classics buttons
  - System notification permission warnings with direct link to system settings
  - Animated UI transitions for mode-specific controls
- **Detail Screen**: Full motivation view with comprehensive content display
  - Large personality image (400dp) with Coil loading and error handling
  - Animated state transitions (loading → content) with crossfade effects
  - Pulsing loading animation with infinite transitions for smooth UX
  - Quote displayed in headline typography with quotation marks
  - Author name prominently displayed with primary color styling
  - Historical context card (when available) with contextual information
  - Theme chips as interactive Material 3 SuggestionChips
  - Source attribution with clickable links to external sources
  - License information display for proper attribution
  - Delivery timestamp with full date/time formatting
  - Delivery status indicator (Scheduled, Delivered, Failed)
  - Error state with emoji, message, and back navigation
  - Vertical scrolling for long content with 16dp spacing between cards
- **Bottom Navigation**: Three tabs (Home, History, Settings) with 48dp touch targets
- **Dark Mode Support**: Full Material 3 theming with automatic dark mode

### Technical Excellence ✅
- **Image Caching**: Coil ImageLoader configured with memory cache (25% of available memory) and disk cache (50MB)
- **Personality Images**: PersonalityImage component used throughout app (HomeScreen, HistoryScreen, DetailScreen) for consistent image loading
- **Smooth Animations**: Multiple animation effects enhance user experience:
  - Slide-in animations for history headers and items
  - Pulsing loading indicators with infinite transitions
  - Scaling animations for empty states
  - Crossfade transitions between UI states
  - Staggered entry animations for list items
- **State Management**: Reactive UI with Kotlin Flow and StateFlow
- **Accessibility**: Content descriptions for all interactive elements, TalkBack support
- **Performance**: Database indexes for efficient queries, lazy loading for history
- **Concurrent Safety**: Thread-safe database operations validated with property tests
- **Deterministic Scheduling**: Unique work names prevent duplicate notifications

## Key Design Decisions

### NotificationScheduler Interface Architecture
The notification scheduling system uses an interface-based design for flexibility and testability:

**Interface: `NotificationScheduler`**
- Defines the contract for notification scheduling operations
- Key methods:
  - `scheduleNextNotification()`: Schedules next notification based on current preferences
  - `rescheduleAllNotifications()`: Cancels and reschedules all pending notifications
  - `cancelAllNotifications()`: Cancels all pending notification work
  - `triggerManualNotification()`: Delivers an immediate manual notification
  - `calculateNextNotificationTime()`: Computes next valid notification time with schedule mode and time window constraints

**Implementation: `NotificationSchedulerImpl`**
- WorkManager-based implementation of the interface
- Integrates with v2 features: schedule modes, time windows, and manual notifications
- Maintains backward compatibility with v1 scheduling logic
- Supports dependency injection for testing

This design enables:
1. **Testability**: Easy to create test doubles and fakes
2. **Flexibility**: Can swap implementations (e.g., for testing or alternative schedulers)
3. **Clear Contracts**: Interface documents expected behavior
4. **Separation of Concerns**: Interface defines "what", implementation defines "how"

### Schedule Mode Logic
The app provides flexible scheduling through four distinct modes:
1. **ALL_DAYS**: Notifications delivered every day of the week
2. **WEEKDAYS_ONLY**: Notifications only on Monday through Friday
3. **WEEKENDS_ONLY**: Notifications only on Saturday and Sunday
4. **CUSTOM_DAYS**: User-selectable days of the week

The `ScheduleMode.getActiveDays()` extension function converts each mode into a set of active `DayOfWeek` values, enabling clean separation between schedule configuration and notification scheduling logic. This design allows the notification scheduler to work with a simple set of active days regardless of the underlying schedule mode.

### Testable Architecture
Repository classes are designed to support testing:
1. `MotivationRepository` and `PreferencesRepository` are open classes
2. Key methods are marked as `open` to allow overriding in test fakes
3. Enables integration testing without complex mocking frameworks
4. Fake implementations can provide controlled test data

### Non-Repeating Content
The app guarantees users never see the same quote twice through:
1. Database query that excludes items in delivery_history
2. Immediate recording of delivery before showing notification
3. "Replay Classics" feature to reset history when exhausted

### Reliable Scheduling
Notifications are scheduled reliably using:
1. **Interface-Based Design**: `NotificationScheduler` interface with `NotificationSchedulerImpl` implementation
2. **WorkManager Integration**: Guaranteed execution that survives device reboots
3. **Deterministic Work Names**: Format `motivation_YYYYMMDD_index` prevents duplicates
4. **Daily Rescheduler**: Midnight refresh ensures notifications stay current
5. **Schedule Mode Support**: Respects day-of-week constraints (ALL_DAYS, WEEKDAYS_ONLY, WEEKENDS_ONLY, CUSTOM_DAYS)
6. **Time Window Constraints**: Notifications only scheduled within configured start/end times
7. **Manual Triggering**: On-demand notifications via `triggerManualNotification()` method

### Idempotent Seed Loading
Seed data loads automatically and only once on first launch:
1. SharedPreferences flag tracks loading status
2. Background coroutine prevents UI blocking
3. StateFlow exposes initialization state to UI (Loading, Success, Error)
4. Graceful error handling with user-friendly messages
5. Validation ensures data quality (102 quotes with proper attribution)
6. Singleton Application instance provides global access to state
7. **Property-based tests verify validation rules and idempotence guarantees**
8. **Automatic notification scheduling** after successful seed loading

### Image Loading and Caching
Coil ImageLoader is configured for optimal performance:
1. **Memory Cache**: 25% of available device memory for fast in-memory access
2. **Disk Cache**: 50MB persistent storage in app cache directory
3. **Cache Policies**: Both read and write caching enabled
4. **Automatic Usage**: All AsyncImage composables use the configured loader
5. **Offline Support**: Cached images available without network connection
6. **Respect Cache Headers**: Disabled to always cache images regardless of HTTP headers
7. **Network Access**: INTERNET permission enables loading remote personality images from https:// URLs

**PersonalityImage Component (v2)**:
The `PersonalityImage` composable provides a specialized, reusable component for displaying personality images with:
- **Coil Integration**: Uses AsyncImage for efficient loading and caching
- **Crossfade Animation**: Smooth transitions when images load
- **Placeholder Support**: Displays placeholder during loading
- **Error Fallback**: Shows placeholder if image fails to load
- **Multiple URI Schemes**: Supports android.resource://, https://, and file:// URIs (requires INTERNET permission for remote URLs)
- **Flexible Scaling**: Configurable ContentScale for different contexts (thumbnail, card, full-screen)
- **Accessibility**: Requires contentDescription parameter for screen reader support
- **Null Safety**: Gracefully handles null or empty imageUri values

Usage example:
```kotlin
PersonalityImage(
    imageUri = motivation.imageUri,
    contentDescription = "Portrait of ${motivation.author}",
    modifier = Modifier.size(200.dp),
    contentScale = ContentScale.Crop
)
```

### HomeViewModel State Management
The HomeViewModel follows modern Android architecture patterns:
1. **Sealed Class UI States**: Type-safe representation of screen states (Loading, Success, Empty, Error, ContentExhausted)
2. **StateFlow**: Reactive state updates using Kotlin Flow for both UI state and manual trigger state
3. **Coroutine Integration**: All data operations run in viewModelScope
4. **Error Handling**: Comprehensive try-catch with user-friendly error messages
5. **Today's Statistics**: Displays latest motivation, today's count, and unseen count
6. **Manual Notification Trigger** (v2): 
   - `triggerManualNotification()`: Delivers on-demand motivations via NotificationScheduler
   - `ManualTriggerState`: Tracks trigger progress (Idle, Loading, Success, Error)
   - Automatic UI refresh after successful manual trigger
   - Content exhaustion checking before triggering
   - Auto-reset to idle state after 2 seconds
7. **NotificationScheduler Integration**: 
   - Injects NotificationScheduler dependency for manual notification delivery
   - Constructor signature: `HomeViewModel(motivationRepository, preferencesRepository, notificationScheduler)`
   - Enables testability through dependency injection (can mock NotificationScheduler in tests)
8. **Date Key Format**: Uses YYYY-MM-DD format for consistent date handling

### SettingsViewModel Implementation (v2)
The SettingsViewModel provides comprehensive preference management with advanced validation:

**Core Features:**
- **Schedule Mode Management**: Supports ALL_DAYS, WEEKDAYS_ONLY, WEEKENDS_ONLY, and CUSTOM_DAYS modes
- **Custom Day Selection**: Allows users to select specific days when CUSTOM_DAYS mode is active
- **Time Window Configuration**: Validates and updates notification start/end times
- **Real-time Validation**: Provides immediate feedback on invalid configurations
- **Automatic Rescheduling**: Triggers notification rescheduling when preferences change

**Validation Logic:**
- **Time Format Validation**: Uses `DateTimeFormatter.ofPattern("HH:mm")` to parse and validate time strings
- **Start Before End**: Ensures start time is before end time using `LocalTime` comparison
- **Minimum Interval**: Validates time window is wide enough for requested notifications (30-minute minimum intervals)
- **Calculation**: Uses `ChronoUnit.MINUTES.between()` to compute available minutes in time window
- **Error Messages**: Provides user-friendly error messages for validation failures

**State Management:**
- `preferences: StateFlow<UserPreferences>`: Reactive preferences from repository
- `notificationsEnabled: StateFlow<Boolean>`: System notification permission status
- `validationState: StateFlow<ValidationState>`: Current validation state (Valid or Invalid with errors)

**Key Methods:**
- `updateScheduleMode(mode: ScheduleMode)`: Changes schedule mode and reschedules notifications
- `updateCustomDays(days: Set<DayOfWeek>)`: Updates custom day selection
- `updateTimeWindow(startTime: String, endTime: String)`: Validates and updates time window
- `validateTimeWindow()`: Comprehensive validation returning `ValidationResult`
- `toggleNotifications(enabled: Boolean)`: Enables/disables notifications with automatic scheduling
- `refreshNotificationStatus()`: Checks system notification permission status

**Dependencies:**
- `Context`: For checking system notification permissions
- `PreferencesRepository`: For persisting user preferences
- `MotivationRepository`: For clearing history (Replay Classics)
- `NotificationScheduler`: For rescheduling notifications on preference changes

### HomeScreen Compose UI
The HomeScreen implements a modern, responsive UI with Material 3:
1. **State-Driven UI**: Observes HomeViewModel state via collectAsState() for reactive updates
2. **Multiple UI States**: Dedicated composables for Loading, Success, Empty, and Error states
3. **Material 3 Design**: Uses Material 3 components (Card, Button, Text) with theme-aware colors
4. **Responsive Layout**: Vertical scrolling with proper spacing and padding
5. **Statistics Display**: Two stat cards showing today's count and unseen count
6. **Motivation Card**: Rich content display with:
   - Full-size image with Coil AsyncImage (200dp height, crop scaling)
   - Quote text in headline typography with quotation marks
   - Author attribution with em dash styling
   - Optional historical context
   - Theme chips (up to 3 displayed)
   - Delivery timestamp in 12-hour format
7. **Empty State**: Encouraging message with sparkle emoji and unseen count
8. **Error State**: Warning emoji, error message, and retry button
9. **Accessibility**: 
   - Content descriptions for images and interactive elements
   - Semantic content descriptions for loading indicators
   - Screen reader support via TalkBack
   - Proper semantic structure for navigation
10. **Dark Mode Support**: Automatic theme switching via MaterialTheme.colorScheme

### DetailScreen Compose UI
The DetailScreen provides a comprehensive view of motivation content with rich visual design:
1. **State-Driven Architecture**: Three distinct states (Loading, Error, Content) with animated transitions
2. **Animated State Transitions**: Crossfade animations between loading, error, and content states using AnimatedContent
3. **Loading State**: 
   - Pulsing circular progress indicator with infinite alpha animation (0.3 to 1.0)
   - "Loading details..." text with synchronized pulsing effect
   - FastOutSlowInEasing for smooth animation curves
4. **Error State**:
   - Warning emoji (⚠️) in display large typography
   - Error message with user-friendly text
   - "Go Back" button for navigation recovery
   - Centered layout with proper spacing
5. **Content Display**:
   - **Large Personality Image**: 400dp height with PersonalityImage component, crop scaling, and Coil caching
   - **Quote Card**: Headline typography with quotation marks and author attribution in primary color
   - **Historical Context Card**: Optional card displaying contextual information when available
   - **Theme Chips**: Material 3 SuggestionChips with accessibility content descriptions
   - **Source Attribution Card**: Source name, clickable URL link (using LocalUriHandler), and license information
   - **Delivery Information Card**: Full timestamp formatting (e.g., "December 25, 2024 at 3:45 PM") and delivery status
6. **Navigation**: TopAppBar with back button and "Motivation Details" title
7. **Scrolling**: Vertical scroll support for long content with 16dp spacing between cards
8. **Material 3 Design**: Consistent card styling, typography hierarchy, and color scheme
9. **Accessibility**: Content descriptions for all interactive elements and images
10. **Dark Mode Support**: Full Material 3 theming with automatic dark mode

**Key Implementation Details**:
- Uses `MotivationRepository` to fetch motivation data by ID
- Searches through all date keys to find the specific motivation in history
- Formats timestamps using SimpleDateFormat with "MMMM d, yyyy 'at' h:mm a" pattern
- Handles null context gracefully (only displays context card when available)
- External links open via LocalUriHandler for proper browser integration
- Scaffold layout with TopAppBar for consistent navigation experience

## Testing

The project includes comprehensive testing with multiple frameworks:

### Testing Frameworks
- **Kotest**: Property-based testing for universal correctness properties
- **JUnit 4**: Standard unit testing framework
- **MockK**: Modern Kotlin-first mocking library with coroutine support
- **Mockito**: Traditional Java mocking framework for compatibility
- **Espresso**: Android UI testing

### Test Coverage
- **Unit Tests**: Business logic, repositories, workers, seed loader, ViewModels
- **Integration Tests**: 
  - ContentSelector with fake repositories (no mocking required)
  - End-to-end notification scheduling and delivery
  - Complete user flows (first launch, notification receipt, settings changes, content exhaustion)
- **Property-Based Tests**: Implemented with Kotest for universal correctness properties
  - **Property 34: Accessibility Content Descriptions** - validates interactive elements have proper content descriptions
  - **Property 36: No Duplicate Notifications** - 1000 iterations ensuring scheduling uniqueness
  - **Property 37: Global Non-Repetition** - 1000 iterations guaranteeing no repeated content
  - **Property 38: Concurrent Access Safety** - validates thread-safe database operations
  - **Property 39: Seed Data Validation** - validates required fields and data quality
  - **Property 40: Seed Loading Idempotence** - ensures single load across multiple calls
  - **Property 42: Default Settings Maintain v1 Behavior** (v2) - validates backward compatibility with v1 scheduling
  - **Property 43: Existing imageUri Values Remain Valid** (v2) - ensures URI format preservation during migration
  - **Property 45: Unchanged Preferences Don't Trigger Rescheduling** (v2) - validates performance optimization
- **Accessibility Tests**: Compose UI testing with property-based verification for screen readers
- **Backward Compatibility Tests** (v2): Property-based tests ensuring smooth v1 to v2 migration
  - Default v2 settings maintain identical behavior to v1
  - Existing data (imageUri values) remain valid after migration
  - Performance optimizations (no unnecessary rescheduling)

### Testing Strategy
The project uses a hybrid testing approach:
- **Fake Repositories**: For integration tests, repositories are subclassed to provide controlled test data
- **Mocking**: MockK and Mockito for unit tests requiring isolated component testing
  - ViewModels are tested with mocked dependencies (repositories, schedulers)
  - Example: `HomeViewModelTest` mocks `MotivationRepository`, `PreferencesRepository`, and `NotificationScheduler`
  - Enables isolated testing of ViewModel logic without database or WorkManager dependencies
- **Property-Based Testing**: Kotest for verifying universal correctness properties across many inputs
  - Critical properties run 1000 iterations for thorough validation
  - Standard properties run 100 iterations
  - Tests cover edge cases, boundary conditions, and concurrent scenarios
- **Compose UI Testing**: Jetpack Compose testing framework for accessibility and UI verification
  - Renders actual UI components in test environment
  - Verifies semantic properties (content descriptions, accessibility labels)
  - Tests interactive elements (buttons, switches, sliders, cards, chips, navigation items)
  - Validates accessibility requirements for screen readers (TalkBack)

### Critical Property-Based Tests
The app includes rigorous property-based testing for core guarantees:

**Property 37: Global Non-Repetition** (1000 iterations)
- Validates the core value proposition: users never see the same motivation twice
- Tests across various database states (partial exhaustion, exact exhaustion, over-exhaustion)
- Verifies content selection algorithm maintains uniqueness
- Ensures proper handling of content exhaustion
- Validates: Requirements 20.2

**Property 36: No Duplicate Notifications** (1000 iterations)
- Ensures reliable notification scheduling without duplicates
- Validates deterministic work name generation (motivation_YYYYMMDD_index)
- Tests across various preference configurations (1-10 notifications/day, both schedule modes)
- Verifies work names are unique per time slot
- Validates: Requirements 20.1

**Property 38: Concurrent Access Safety** (100 iterations)
- Tests data consistency with concurrent database operations
- Validates atomic delivery recording prevents race conditions
- Ensures thread-safe content selection
- Tests mixed concurrent reads and writes
- Validates: Requirements 20.4

These critical tests provide high confidence in the app's correctness and reliability.

### Running Tests
```bash
# Run all unit tests
./gradlew test

# Run tests for a specific module
./gradlew app:test

# Run with coverage
./gradlew testDebugUnitTest jacocoTestReport
```

## Features

## What Makes This Implementation Special

This History Motivation Coach app demonstrates modern Android development best practices:

1. **Formal Correctness**: Property-based testing validates universal correctness properties with thousands of test iterations, providing mathematical confidence in core guarantees like non-repetition and scheduling uniqueness.

2. **Clean Architecture**: Clear separation between data, business logic, and presentation layers with testable components and dependency injection patterns.

3. **Modern Android Stack**: Leverages the latest Jetpack libraries (Compose, Room, WorkManager, Navigation) with Kotlin coroutines and Flow for reactive programming.

4. **Accessibility First**: All UI elements include proper content descriptions, validated through property-based tests, ensuring excellent screen reader support.

5. **Robust Error Handling**: Comprehensive error handling at every layer with user-friendly messages and graceful degradation.

6. **Performance Optimized**: Database indexes, lazy loading, image caching, and efficient queries ensure smooth performance even with large datasets.

7. **Production Ready**: Includes seed data loading, state management, deep linking, dark mode support, and handles edge cases like content exhaustion and disabled notifications.

## License

[Add your license here]
