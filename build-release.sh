#!/bin/bash

# Build Release Script for History Motivation Coach
# This script helps you build a release version of your app

set -e  # Exit on error

echo "=========================================="
echo "  History Motivation Coach"
echo "  Release Build Script"
echo "=========================================="
echo ""

# Check if keystore.properties exists
if [ ! -f "keystore.properties" ]; then
    echo "❌ ERROR: keystore.properties not found!"
    echo ""
    echo "Please create keystore.properties from the template:"
    echo "  1. Copy keystore.properties.template to keystore.properties"
    echo "  2. Fill in your keystore details"
    echo ""
    echo "If you don't have a keystore yet, create one with:"
    echo "  keytool -genkey -v -keystore my-release-key.jks \\"
    echo "    -keyalg RSA -keysize 2048 -validity 10000 \\"
    echo "    -alias my-key-alias"
    echo ""
    exit 1
fi

echo "✓ Found keystore.properties"
echo ""

# Clean previous builds
echo "🧹 Cleaning previous builds..."
./gradlew clean
echo ""

# Run tests
echo "🧪 Running tests..."
./gradlew test
if [ $? -ne 0 ]; then
    echo "❌ Tests failed! Please fix failing tests before building release."
    exit 1
fi
echo "✓ All tests passed"
echo ""

# Build release AAB
echo "📦 Building release AAB (Android App Bundle)..."
./gradlew bundleRelease
echo ""

# Build release APK
echo "📦 Building release APK..."
./gradlew assembleRelease
echo ""

# Show output locations
echo "=========================================="
echo "  ✅ Build Complete!"
echo "=========================================="
echo ""
echo "Output files:"
echo "  AAB: app/build/outputs/bundle/release/app-release.aab"
echo "  APK: app/build/outputs/apk/release/app-release.apk"
echo ""
echo "Next steps:"
echo "  1. Test the APK on a real device:"
echo "     adb install app/build/outputs/apk/release/app-release.apk"
echo ""
echo "  2. Upload the AAB to Google Play Console"
echo ""
echo "  3. See PLAY_STORE_DEPLOYMENT_GUIDE.md for full instructions"
echo ""
