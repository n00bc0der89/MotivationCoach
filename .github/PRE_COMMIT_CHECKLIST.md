# Pre-Commit Security Checklist

Before committing code to the repository, verify the following:

## ✅ Secrets and Credentials
- [ ] No passwords, API keys, or tokens in code
- [ ] `keystore.properties` is NOT committed (should be in .gitignore)
- [ ] `local.properties` is NOT committed (should be in .gitignore)
- [ ] No `.jks` or `.keystore` files committed
- [ ] No hardcoded credentials in any files

## ✅ Personal Information
- [ ] No personal email addresses in code
- [ ] No personal file paths (e.g., /Users/yourname/)
- [ ] No phone numbers or personal identifiers
- [ ] Author information uses generic examples

## ✅ Configuration Files
- [ ] `.gitignore` is up to date
- [ ] `keystore.properties.template` exists with placeholder values
- [ ] `local.properties` is excluded from version control
- [ ] Environment-specific configs use templates

## ✅ Build Files
- [ ] No compiled binaries (.apk, .aab, .dex)
- [ ] No build directories (build/, .gradle/)
- [ ] No IDE-specific files (.idea/, *.iml)

## ✅ Testing
- [ ] All tests pass: `./gradlew test`
- [ ] No test files with hardcoded credentials
- [ ] Property-based tests run successfully

## ✅ Documentation
- [ ] README is up to date
- [ ] Setup instructions use generic paths
- [ ] No personal information in markdown files
- [ ] License file is present

## Quick Verification Commands

```bash
# Check for potential secrets
grep -r "password\|secret\|api_key\|token" --include="*.kt" --include="*.kts" --include="*.xml" .

# Check for personal paths
grep -r "/Users/[a-z]*" --include="*.md" --include="*.kt" --include="*.kts" .

# Verify .gitignore is working
git status --ignored

# Run tests
./gradlew test
```

## Files That Should NEVER Be Committed
- `keystore.properties` (contains actual passwords)
- `local.properties` (contains local SDK paths)
- `*.jks`, `*.keystore` (signing keys)
- `*.apk`, `*.aab` (compiled binaries)
- `.gradle/`, `build/` (build artifacts)
- `.idea/` (IDE settings)
- `google-services.json` (if using Firebase)

## Safe to Commit
- `keystore.properties.template` (template with placeholders)
- `.gitignore` (exclusion rules)
- `gradle.properties` (project-wide settings without secrets)
- All source code files (*.kt, *.xml)
- Documentation files (*.md)
- GitHub configuration files (.github/)
