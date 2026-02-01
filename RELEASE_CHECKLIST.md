# Release Checklist for Google Play Store

Use this checklist to ensure you've completed all steps before publishing.

## 🔧 Pre-Release Configuration

- [ ] Updated `applicationId` in `app/build.gradle.kts` (if changing from example)
- [ ] Updated `versionCode` to 1 (or higher for updates)
- [ ] Updated `versionName` to "1.0.0" (or appropriate version)
- [ ] Enabled code shrinking (`isMinifyEnabled = true`)
- [ ] Enabled resource shrinking (`isShrinkResources = true`)
- [ ] Reviewed and updated ProGuard rules in `app/proguard-rules.pro`

## 🔐 Signing Configuration

- [ ] Created keystore file using keytool
- [ ] Backed up keystore file to secure location(s)
- [ ] Created `keystore.properties` from template
- [ ] Filled in all keystore details in `keystore.properties`
- [ ] Verified `keystore.properties` is in `.gitignore`
- [ ] Tested signing configuration works

## 📦 Build & Test

- [ ] Ran `./gradlew clean`
- [ ] Ran `./gradlew test` - all tests pass
- [ ] Built release AAB: `./gradlew bundleRelease`
- [ ] Built release APK: `./gradlew assembleRelease`
- [ ] Installed and tested release APK on real device
- [ ] Verified notifications work in release build
- [ ] Verified database operations work in release build
- [ ] Verified all screens and navigation work
- [ ] Tested on multiple Android versions (if possible)
- [ ] Tested on different screen sizes (if possible)

## 🎨 Store Assets

- [ ] App icon (512x512 PNG) prepared
- [ ] Feature graphic (1024x500 PNG) created
- [ ] Phone screenshots (2-8 images, 1080x1920 or similar)
- [ ] Tablet screenshots (optional, 1536x2048 or similar)
- [ ] All images are high quality and representative

## 📝 Store Listing Content

- [ ] App name finalized: "History Motivation Coach"
- [ ] Short description written (80 chars max)
- [ ] Full description written (4000 chars max)
- [ ] Privacy policy created and hosted online
- [ ] Privacy policy URL obtained
- [ ] Contact email address ready
- [ ] Website URL (optional)

## 🏪 Google Play Console Setup

- [ ] Created Google Play Developer account ($25 paid)
- [ ] Account verified
- [ ] Created new app in Play Console
- [ ] Completed "Main store listing" section
- [ ] Uploaded all graphics and screenshots
- [ ] Completed "Content rating" questionnaire
- [ ] Completed "Target audience" section
- [ ] Completed "Data safety" form
- [ ] Completed "App access" section
- [ ] Reviewed all policy sections

## 🚀 Release

- [ ] Uploaded AAB to Production track
- [ ] Added release notes for version 1.0.0
- [ ] Reviewed all sections - all green checkmarks
- [ ] Started rollout to Production
- [ ] Received confirmation email from Google

## 📊 Post-Launch

- [ ] Monitored for review approval (1-3 days)
- [ ] Checked for any policy violations
- [ ] Set up alerts for crash reports
- [ ] Prepared to respond to user reviews
- [ ] Planned first update/improvements

## 🔄 For Future Updates

When releasing updates, remember to:
- [ ] Increment `versionCode` (2, 3, 4...)
- [ ] Update `versionName` (1.0.1, 1.1.0, etc.)
- [ ] Write clear release notes
- [ ] Test thoroughly before uploading
- [ ] Use staged rollout for major changes

---

## Quick Commands

```bash
# Build release
./build-release.sh

# Or manually:
./gradlew clean
./gradlew test
./gradlew bundleRelease

# Install APK for testing
adb install app/build/outputs/apk/release/app-release.apk

# Check APK size
ls -lh app/build/outputs/apk/release/app-release.apk

# Check AAB size
ls -lh app/build/outputs/bundle/release/app-release.aab
```

---

## Important Files

- `PLAY_STORE_DEPLOYMENT_GUIDE.md` - Complete deployment guide
- `PRIVACY_POLICY.md` - Privacy policy template
- `keystore.properties.template` - Keystore configuration template
- `build-release.sh` - Automated build script

---

## Support Resources

- [Google Play Console](https://play.google.com/console)
- [Play Console Help](https://support.google.com/googleplay/android-developer)
- [Android Developer Docs](https://developer.android.com/distribute)

---

**Remember**: Never commit your keystore file or keystore.properties to version control!

Good luck with your launch! 🚀
