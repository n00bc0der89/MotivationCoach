# Android Studio Setup Guide

## Installation Status
✅ Android Studio has been installed via Homebrew to: `/Applications/Android Studio.app`

## Next Steps to Complete Setup

### 1. Launch Android Studio
Open Android Studio from your Applications folder or run:
```bash
open -a "Android Studio"
```

### 2. Complete Initial Setup Wizard
When Android Studio launches for the first time, you'll see a setup wizard:

1. **Welcome Screen**: Click "Next"
2. **Install Type**: Choose "Standard" (recommended)
3. **Select UI Theme**: Choose your preferred theme (Darcula or Light)
4. **Verify Settings**: Review the components to be installed:
   - Android SDK
   - Android SDK Platform
   - Android Virtual Device
5. **License Agreement**: Accept the license agreements
6. **Downloading Components**: Wait for SDK components to download (this may take several minutes)

### 3. Configure Android SDK Location

After setup completes, the Android SDK will be installed at:
```
~/Library/Android/sdk
```

### 4. Set Environment Variables

Add these to your `~/.zshrc` file:
```bash
# Android SDK
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/tools
export PATH=$PATH:$ANDROID_HOME/tools/bin

# Java (already configured)
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
```

To apply immediately:
```bash
source ~/.zshrc
```

### 5. Create local.properties File

After Android Studio setup completes, create a `local.properties` file in your project root:
```properties
sdk.dir=/path/to/your/Android/sdk
```

Or run this command from your project directory:
```bash
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
```

### 6. Open Project in Android Studio

1. Click "Open" on the welcome screen
2. Navigate to your project directory
3. Click "Open"
4. Wait for Gradle sync to complete
5. Android Studio will download any missing dependencies

### 7. Verify Build

Once Gradle sync completes, you can build the project:
- From Android Studio: `Build > Make Project` (⌘F9)
- From terminal: `./gradlew build`

### 8. Run Tests

To run unit tests:
- From Android Studio: Right-click on test file > Run
- From terminal: `./gradlew test`

## Required SDK Components

The project requires:
- **Compile SDK**: API 34 (Android 14)
- **Min SDK**: API 28 (Android 9)
- **Target SDK**: API 34 (Android 14)
- **Build Tools**: Latest version
- **Java**: JDK 17 ✅ (already installed)

## Troubleshooting

### If SDK location is not found:
1. Check that `~/Library/Android/sdk` exists
2. Verify `local.properties` file exists in project root
3. Ensure `ANDROID_HOME` environment variable is set

### If Gradle sync fails:
1. Check internet connection (Gradle needs to download dependencies)
2. Try: `File > Invalidate Caches / Restart`
3. Try: `./gradlew clean build --refresh-dependencies`

### If Java version issues:
The project is configured for Java 17, which is already installed at:
```
/opt/homebrew/opt/openjdk@17
```

## Current Project Status

### ✅ Completed Tasks
- Task 5: Data layer checkpoint
- Task 6.1: ContentSelector class
- Task 6.2: NotificationScheduler class
- Task 7.1: NotificationWorker
- Task 7.2: SchedulerWorker
- Task 7.3: Notification channel
- Task 8.1: Seed data JSON (102 quotes)
- Task 8.2: SeedDataLoader class

### 📋 Next Tasks
- Task 8.3: Integrate SeedDataLoader into Application
- Task 9: Business logic checkpoint
- Tasks 10-14: ViewModels and UI screens
- Tasks 15-20: Testing and polish

## Quick Start After Setup

Once Android Studio is configured:

1. **Build the project:**
   ```bash
   ./gradlew build
   ```

2. **Run unit tests:**
   ```bash
   ./gradlew test
   ```

3. **Run on emulator/device:**
   - Create an AVD (Android Virtual Device) in Android Studio
   - Click the "Run" button (▶️) or press ⌃R

## Additional Resources

- [Android Studio User Guide](https://developer.android.com/studio/intro)
- [Configure Android Studio](https://developer.android.com/studio/intro/studio-config)
- [Gradle Build Configuration](https://developer.android.com/studio/build)
