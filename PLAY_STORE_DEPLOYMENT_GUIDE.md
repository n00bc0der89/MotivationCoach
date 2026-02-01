# Google Play Store Deployment Guide
## History Motivation Coach

This guide walks you through preparing, packaging, and publishing your Android app to the Google Play Store.

---

## 📋 Prerequisites Checklist

Before you begin, ensure you have:

- [ ] Google Play Developer account ($25 one-time registration fee)
- [ ] App tested on multiple devices/emulators
- [ ] All required assets prepared (icons, screenshots, descriptions)
- [ ] Privacy policy URL (required for apps with user data)
- [ ] Content rating questionnaire completed

---

## 🔧 Step 1: Prepare Your App for Release

### 1.1 Update Application ID (Optional but Recommended)

Change the package name from the example namespace to your own:

**File: `app/build.gradle.kts`**

```kotlin
android {
    namespace = "com.yourcompany.historymotivationcoach"  // Change this
    
    defaultConfig {
        applicationId = "com.yourcompany.historymotivationcoach"  // Change this
        // ... rest of config
    }
}
```

### 1.2 Update Version Information

**File: `app/build.gradle.kts`**

```kotlin
defaultConfig {
    versionCode = 1        // Increment for each release (1, 2, 3...)
    versionName = "1.0.0"  // User-visible version (1.0.0, 1.0.1, etc.)
}
```

### 1.3 Enable Code Shrinking and Obfuscation

**File: `app/build.gradle.kts`**

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true              // Enable code shrinking
        isShrinkResources = true            // Enable resource shrinking
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### 1.4 Configure ProGuard Rules

**File: `app/proguard-rules.pro`**

Add these rules to prevent issues with Room, Kotlin, and Compose:

```proguard
# Keep Room entities
-keep class com.example.historymotivationcoach.data.entity.** { *; }

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker

# Keep serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep data classes used for JSON
-keepclassmembers class com.example.historymotivationcoach.data.entity.** {
    <fields>;
    <init>(...);
}
```

---

## 🔐 Step 2: Generate a Signing Key

You need a signing key to publish your app. This is a one-time setup.

### 2.1 Create Keystore File

Run this command in your project root:

```bash
keytool -genkey -v -keystore my-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias my-key-alias
```

You'll be prompted for:
- Keystore password (remember this!)
- Key password (remember this!)
- Your name, organization, city, state, country

**⚠️ CRITICAL: Backup this keystore file! If you lose it, you can never update your app!**

### 2.2 Store Keystore Securely

Move the keystore outside your project directory:

```bash
mkdir -p ~/.android/keystores
mv my-release-key.jks ~/.android/keystores/
```

### 2.3 Configure Signing in Gradle

**File: `app/build.gradle.kts`**

Add this before the `android` block:

```kotlin
// Load keystore properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = java.util.Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(java.io.FileInputStream(keystorePropertiesFile))
}

android {
    // ... existing config
    
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String? ?: "")
            storePassword = keystoreProperties["storePassword"] as String? ?: ""
            keyAlias = keystoreProperties["keyAlias"] as String? ?: ""
            keyPassword = keystoreProperties["keyPassword"] as String? ?: ""
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### 2.4 Create Keystore Properties File

**File: `keystore.properties` (in project root)**

```properties
storeFile=/path/to/your/keystore.jks
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=my-key-alias
keyPassword=YOUR_KEY_PASSWORD
```

**⚠️ Add to .gitignore:**

```bash
echo "keystore.properties" >> .gitignore
```

---

## 📦 Step 3: Build Release APK/AAB

### 3.1 Build Android App Bundle (AAB) - Recommended

Google Play requires AAB format for new apps:

```bash
./gradlew bundleRelease
```

Output location: `app/build/outputs/bundle/release/app-release.aab`

### 3.2 Build APK (Alternative)

If you need an APK for testing:

```bash
./gradlew assembleRelease
```

Output location: `app/build/outputs/apk/release/app-release.apk`

### 3.3 Test the Release Build

Install and test the release APK on a real device:

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

Test thoroughly:
- [ ] App launches correctly
- [ ] Notifications work
- [ ] Database operations work
- [ ] All screens navigate properly
- [ ] No crashes or errors

---

## 🎨 Step 4: Prepare Store Assets

### 4.1 App Icon

You already have launcher icons in:
- `app/src/main/res/mipmap-*/ic_launcher.png`

Ensure you have all required densities:
- mdpi (48x48)
- hdpi (72x72)
- xhdpi (96x96)
- xxhdpi (144x144)
- xxxhdpi (192x192)

### 4.2 Feature Graphic

Create a feature graphic (1024x500 pixels) showcasing your app.

### 4.3 Screenshots

Take screenshots on different devices:
- **Phone**: Minimum 2, maximum 8 screenshots
- **7-inch tablet**: Optional but recommended
- **10-inch tablet**: Optional but recommended

Recommended sizes:
- Phone: 1080x1920 or 1080x2340
- Tablet: 1536x2048

### 4.4 App Description

**Short Description** (80 characters max):
```
Daily motivation from history's greatest minds. Never see the same quote twice!
```

**Full Description** (4000 characters max):
```
History Motivation Coach delivers inspiring quotes from historical figures directly to your phone through scheduled notifications. Each quote is paired with context and beautiful imagery, ensuring you never see the same motivation twice.

✨ KEY FEATURES:

📅 Customizable Schedule
• Choose 1-10 notifications per day
• Set a time window for even distribution
• Or pick specific times for your motivations

🎯 Never Repeats
• Unique content every time
• Track your motivation history
• Replay classics when you've seen them all

📚 Rich Content
• Historical quotes with context
• Beautiful accompanying images
• Source attribution and licensing info

🎨 Beautiful Design
• Modern Material Design 3
• Dark mode support
• Accessible and easy to use

📊 History Timeline
• Browse past motivations by date
• See what inspired you each day
• Tap any quote to see full details

⚙️ Full Control
• Enable/disable notifications anytime
• Customize notification frequency
• Clear history and start fresh

🔒 Privacy First
• All data stored locally on your device
• No account required
• No data collection or tracking

Perfect for anyone seeking daily inspiration, personal growth, or a dose of wisdom from history's greatest thinkers.

Download now and start your journey of daily motivation!
```

### 4.5 Privacy Policy

Create a privacy policy (required). Here's a template:

```markdown
# Privacy Policy for History Motivation Coach

Last updated: [DATE]

## Data Collection
History Motivation Coach does not collect, transmit, or share any personal data. All data is stored locally on your device.

## Data Storage
- Motivation content and delivery history are stored in a local database
- User preferences are stored locally
- No data is transmitted to external servers

## Permissions
- POST_NOTIFICATIONS: Required to send motivational notifications
- RECEIVE_BOOT_COMPLETED: Required to reschedule notifications after device restart
- SCHEDULE_EXACT_ALARM: Required for precise notification timing

## Third-Party Services
This app does not use any third-party analytics, advertising, or tracking services.

## Contact
For questions about this privacy policy, contact: [YOUR EMAIL]
```

Host this on GitHub Pages, your website, or use a service like PrivacyPolicies.com.

---

## 🚀 Step 5: Create Play Store Listing

### 5.1 Create Developer Account

1. Go to [Google Play Console](https://play.google.com/console)
2. Pay the $25 one-time registration fee
3. Complete account verification

### 5.2 Create New App

1. Click "Create app"
2. Fill in:
   - **App name**: History Motivation Coach
   - **Default language**: English (United States)
   - **App or game**: App
   - **Free or paid**: Free
3. Accept declarations

### 5.3 Complete Store Listing

Navigate to "Store presence" → "Main store listing":

1. **App details**:
   - Short description (from Step 4.4)
   - Full description (from Step 4.4)

2. **Graphics**:
   - App icon (512x512 PNG)
   - Feature graphic (1024x500 PNG)
   - Phone screenshots (2-8 images)
   - Tablet screenshots (optional)

3. **Categorization**:
   - App category: Lifestyle or Productivity
   - Tags: motivation, quotes, inspiration, self-improvement

4. **Contact details**:
   - Email address
   - Website (optional)
   - Privacy policy URL (required)

5. **Store settings**:
   - Merchandise: No
   - Ads: No (unless you add ads)

### 5.4 Content Rating

1. Go to "Policy" → "App content"
2. Complete the content rating questionnaire
3. Your app should receive an "Everyone" rating

### 5.5 Target Audience

1. Select target age groups (likely "18 and over" or "All ages")
2. Confirm app is not primarily for children

### 5.6 Data Safety

1. Go to "Policy" → "Data safety"
2. Declare:
   - **Does your app collect or share user data?** No
   - **Is all data encrypted in transit?** Yes (local only)
   - **Can users request data deletion?** Yes (via app uninstall)

### 5.7 App Access

1. Declare if app requires special access
2. For this app: No special access needed

---

## 📤 Step 6: Upload and Release

### 6.1 Create Release

1. Go to "Release" → "Production"
2. Click "Create new release"
3. Upload your AAB file (`app-release.aab`)
4. Add release notes:

```
Version 1.0.0 - Initial Release

• Daily motivational quotes from historical figures
• Customizable notification schedule (1-10 per day)
• Never see the same quote twice
• Beautiful Material Design 3 interface
• Dark mode support
• Complete history timeline
• Privacy-focused: all data stored locally
```

### 6.2 Review and Rollout

1. Review all sections (must be complete)
2. Click "Review release"
3. Fix any issues flagged by Google
4. Click "Start rollout to Production"

### 6.3 Wait for Review

- Google typically reviews apps within 1-3 days
- You'll receive email notifications about status
- Check Play Console for any issues

---

## 🔄 Step 7: Post-Launch

### 7.1 Monitor Performance

- Check crash reports in Play Console
- Monitor user reviews and ratings
- Track installation metrics

### 7.2 Respond to Reviews

- Reply to user feedback
- Address issues promptly
- Thank users for positive reviews

### 7.3 Plan Updates

When releasing updates:

1. Increment `versionCode` and `versionName`
2. Build new AAB
3. Create new release in Play Console
4. Add release notes describing changes

---

## 🛠️ Quick Commands Reference

```bash
# Build release AAB
./gradlew bundleRelease

# Build release APK
./gradlew assembleRelease

# Run tests before release
./gradlew test

# Clean build
./gradlew clean

# Install release APK for testing
adb install app/build/outputs/apk/release/app-release.apk
```

---

## ⚠️ Important Reminders

1. **Backup your keystore** - Store it in multiple secure locations
2. **Test thoroughly** - Test the release build on real devices
3. **Privacy policy** - Required for Play Store submission
4. **Version management** - Always increment versionCode for updates
5. **ProGuard testing** - Test release builds to catch ProGuard issues
6. **Permissions** - Only request necessary permissions
7. **Screenshots** - Use high-quality, representative screenshots
8. **Description** - Be clear about what your app does

---

## 📞 Support Resources

- [Google Play Console Help](https://support.google.com/googleplay/android-developer)
- [Android Developer Documentation](https://developer.android.com/distribute)
- [Play Store Policies](https://play.google.com/about/developer-content-policy/)

---

## ✅ Pre-Launch Checklist

- [ ] Updated applicationId and namespace
- [ ] Incremented versionCode and versionName
- [ ] Enabled code shrinking and obfuscation
- [ ] Created and secured keystore
- [ ] Configured signing in build.gradle
- [ ] Built and tested release AAB/APK
- [ ] Prepared all store assets (icons, screenshots, graphics)
- [ ] Written app descriptions
- [ ] Created and hosted privacy policy
- [ ] Created Google Play Developer account
- [ ] Completed store listing
- [ ] Completed content rating
- [ ] Completed data safety form
- [ ] Uploaded AAB to Play Console
- [ ] Reviewed all sections in Play Console
- [ ] Started rollout to production

Good luck with your launch! 🚀
