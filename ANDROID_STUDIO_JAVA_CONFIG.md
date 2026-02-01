# Configure Android Studio to Use Java 17

## Quick Fix for "jlink" Error

The error you're seeing happens because Android Studio is trying to use its bundled Java 21, but the project needs Java 17.

## Step-by-Step Configuration

### 1. Open Android Studio Settings

**macOS**: 
- `Android Studio > Settings` 
- Or press `⌘,` (Command + Comma)

**Windows/Linux**: 
- `File > Settings`
- Or press `Ctrl+Alt+S`

### 2. Navigate to Gradle Settings

In the Settings window:
1. Expand: `Build, Execution, Deployment`
2. Expand: `Build Tools`
3. Click: `Gradle`

### 3. Change Gradle JDK

You'll see a section called "Gradle JDK" with a dropdown menu.

**Current setting** (causing the error):
- Probably shows: "Android Studio Java" or "jbr-21" or similar

**Change to**:
- Select: `Java 17` from the dropdown

### 4. If Java 17 is Not in the List

If you don't see "Java 17" in the dropdown:

1. Click the dropdown
2. Select: `Add JDK...`
3. In the file browser, navigate to:
   ```
   /opt/homebrew/opt/openjdk@17
   ```
4. Click `Open`
5. Now select the newly added Java 17

### 5. Apply Changes

1. Click `Apply` button
2. Click `OK` button

### 6. Sync Project

After changing the JDK:
1. Click the "Sync Project with Gradle Files" button (🐘 icon in toolbar)
2. Or: `File > Sync Project with Gradle Files`
3. Wait for sync to complete (should succeed now!)

## Visual Reference

```
Settings Window
├── Build, Execution, Deployment
│   └── Build Tools
│       └── Gradle
│           ├── Use Gradle from: 'gradle-wrapper.properties' file ✓
│           └── Gradle JDK: [Java 17] ← CHANGE THIS
```

## Verification

After configuration, you should see:
- ✅ Gradle sync completes successfully
- ✅ No "jlink" errors
- ✅ Build succeeds

## Alternative: Use Command Line

If you prefer not to use Android Studio's UI, you can build from terminal:

```bash
# Build the app
./gradlew assembleDebug

# Run tests
./gradlew test

# Install on device/emulator
./gradlew installDebug
```

The command line works perfectly because it uses the system Java 17.

## Common Issues

### "Could not find Java 17"

If Java 17 is not installed:
```bash
brew install openjdk@17
```

### "Invalid JDK path"

Make sure the path is exactly:
```
/opt/homebrew/opt/openjdk@17
```

### Sync still fails

1. Close Android Studio
2. Delete caches:
   ```bash
   rm -rf .gradle build app/build
   ```
3. Reopen Android Studio
4. Verify Gradle JDK setting again
5. Sync project

## Why This Happens

- Android Studio bundles Java 21 (latest)
- Your project uses Android Gradle Plugin 8.2.2
- AGP 8.2.2 works best with Java 17
- The `jlink` tool in Java 21 has compatibility issues with older AGP versions
- Solution: Tell Android Studio to use Java 17 instead

## Next Steps

Once configured:
1. ✅ Gradle sync should work
2. ✅ Build should succeed
3. ✅ You can run the app on emulator/device

**Ready to run!** 🚀
