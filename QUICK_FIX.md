# Quick Fix: jlink Error in Android Studio

## The Problem
```
Error while executing process .../jlink with arguments {...}
```

## The Solution (2 minutes)

### Option 1: Configure Android Studio (Recommended)

1. **Open Settings**: `⌘,` (macOS) or `Ctrl+Alt+S` (Windows/Linux)

2. **Navigate to**: 
   ```
   Build, Execution, Deployment > Build Tools > Gradle
   ```

3. **Change "Gradle JDK"** from "Android Studio Java" to **"Java 17"**

4. **If Java 17 not listed**:
   - Click dropdown → "Add JDK..."
   - Navigate to: `/opt/homebrew/opt/openjdk@17`
   - Click "Open"

5. **Click**: Apply → OK

6. **Sync Project**: Click 🐘 icon or `File > Sync Project with Gradle Files`

✅ **Done!** The error should be gone.

---

### Option 2: Use Command Line (Works Immediately)

```bash
# Build the app
./gradlew assembleDebug

# Run tests
./gradlew test

# Install on device
./gradlew installDebug
```

The command line works perfectly without any configuration changes.

---

## Why This Happens

- Android Studio uses Java 21 by default
- Your project needs Java 17
- Simple mismatch → simple fix

## Verify It Works

After fixing:
- ✅ Gradle sync completes
- ✅ Build succeeds
- ✅ Can run app on emulator

---

## Still Having Issues?

1. **Clean caches**:
   ```bash
   rm -rf .gradle build app/build
   ```

2. **Restart Android Studio**

3. **Verify Java 17 is installed**:
   ```bash
   /opt/homebrew/opt/openjdk@17/bin/java -version
   ```
   Should show: `openjdk version "17.0.18"`

---

**Need more details?** See `ANDROID_STUDIO_JAVA_CONFIG.md`
