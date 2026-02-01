#!/bin/bash

# Repository Safety Verification Script
# Run this before pushing to public GitHub

echo "🔍 Verifying repository safety for public GitHub..."
echo ""

ERRORS=0
WARNINGS=0

# Color codes
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

# Check for sensitive files
echo "📁 Checking for sensitive files..."
SENSITIVE_FILES=("keystore.properties" "local.properties" "*.jks" "*.keystore" "google-services.json")

for pattern in "${SENSITIVE_FILES[@]}"; do
    # Check if files exist AND are tracked by git
    if find . -name "$pattern" -not -path "./.git/*" | grep -q .; then
        # Check if any of these files are tracked by git
        for file in $(find . -name "$pattern" -not -path "./.git/*"); do
            if git ls-files --error-unmatch "$file" 2>/dev/null; then
                echo -e "${RED}❌ CRITICAL: Sensitive file is tracked by git: $file${NC}"
                ERRORS=$((ERRORS + 1))
            fi
        done
    fi
done

# Check for secrets in code
echo ""
echo "🔐 Checking for hardcoded secrets..."
SECRET_PATTERNS=("password=" "api_key=" "secret=" "token=" "private_key=")

for pattern in "${SECRET_PATTERNS[@]}"; do
    if grep -r "$pattern" --include="*.kt" --include="*.kts" --include="*.xml" --exclude-dir=".git" . | grep -v "YOUR_" | grep -v "your_" | grep -v "template" | grep -q .; then
        echo -e "${YELLOW}⚠️  WARNING: Potential secret found with pattern: $pattern${NC}"
        grep -r "$pattern" --include="*.kt" --include="*.kts" --include="*.xml" --exclude-dir=".git" . | grep -v "YOUR_" | grep -v "your_" | grep -v "template"
        WARNINGS=$((WARNINGS + 1))
    fi
done

# Check for personal information
echo ""
echo "👤 Checking for personal information..."
if git ls-files | xargs grep -l "/Users/[a-z]" 2>/dev/null | grep -v "PRE_COMMIT_CHECKLIST.md" | grep -q .; then
    echo -e "${RED}❌ CRITICAL: Found personal file paths in tracked files${NC}"
    git ls-files | xargs grep -l "/Users/[a-z]" 2>/dev/null | grep -v "PRE_COMMIT_CHECKLIST.md"
    ERRORS=$((ERRORS + 1))
fi

# Check for email addresses
if grep -r "@gmail\.com\|@yahoo\.com\|@hotmail\.com" --include="*.kt" --include="*.kts" --include="*.md" --exclude-dir=".git" . | grep -q .; then
    echo -e "${YELLOW}⚠️  WARNING: Found email addresses${NC}"
    grep -r "@gmail\.com\|@yahoo\.com\|@hotmail\.com" --include="*.kt" --include="*.kts" --include="*.md" --exclude-dir=".git" .
    WARNINGS=$((WARNINGS + 1))
fi

# Verify .gitignore exists and has required entries
echo ""
echo "📝 Verifying .gitignore..."
REQUIRED_IGNORES=("keystore.properties" "local.properties" "*.jks" "*.keystore")

if [ ! -f ".gitignore" ]; then
    echo -e "${RED}❌ CRITICAL: .gitignore file not found${NC}"
    ERRORS=$((ERRORS + 1))
else
    for entry in "${REQUIRED_IGNORES[@]}"; do
        if ! grep -q "$entry" .gitignore; then
            echo -e "${RED}❌ CRITICAL: .gitignore missing entry: $entry${NC}"
            ERRORS=$((ERRORS + 1))
        fi
    done
fi

# Check for template files
echo ""
echo "📋 Checking for template files..."
if [ ! -f "keystore.properties.template" ]; then
    echo -e "${YELLOW}⚠️  WARNING: keystore.properties.template not found${NC}"
    WARNINGS=$((WARNINGS + 1))
fi

# Verify GitHub configuration
echo ""
echo "⚙️  Verifying GitHub configuration..."
if [ ! -d ".github" ]; then
    echo -e "${YELLOW}⚠️  WARNING: .github directory not found${NC}"
    WARNINGS=$((WARNINGS + 1))
else
    GITHUB_FILES=("workflows/android-ci.yml" "CONTRIBUTING.md" "SECURITY.md" "PRE_COMMIT_CHECKLIST.md")
    for file in "${GITHUB_FILES[@]}"; do
        if [ ! -f ".github/$file" ]; then
            echo -e "${YELLOW}⚠️  WARNING: .github/$file not found${NC}"
            WARNINGS=$((WARNINGS + 1))
        fi
    done
fi

# Check for build artifacts
echo ""
echo "🏗️  Checking for build artifacts..."
BUILD_ARTIFACTS=("*.apk" "*.aab" "*.dex")

for pattern in "${BUILD_ARTIFACTS[@]}"; do
    if find . -name "$pattern" -not -path "./.git/*" -not -path "*/build/*" | grep -q .; then
        echo -e "${YELLOW}⚠️  WARNING: Found build artifact: $pattern${NC}"
        find . -name "$pattern" -not -path "./.git/*" -not -path "*/build/*"
        WARNINGS=$((WARNINGS + 1))
    fi
done

# Summary
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 VERIFICATION SUMMARY"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✅ All checks passed! Repository is safe for public GitHub.${NC}"
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠️  $WARNINGS warning(s) found. Review before pushing.${NC}"
    exit 0
else
    echo -e "${RED}❌ $ERRORS critical error(s) found!${NC}"
    echo -e "${YELLOW}⚠️  $WARNINGS warning(s) found.${NC}"
    echo ""
    echo -e "${RED}DO NOT push to public GitHub until all errors are resolved!${NC}"
    exit 1
fi
