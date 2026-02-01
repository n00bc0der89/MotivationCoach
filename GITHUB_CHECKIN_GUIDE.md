# GitHub Public Repository Checkin Guide

This guide ensures your repository is safe for public GitHub without exposing secrets or personal information.

## 🚨 CRITICAL: Before You Push

### Step 1: Remove Sensitive Files
These files contain secrets and MUST NOT be committed:

```bash
# Verify these files are NOT tracked by git
git status

# If they appear, remove them from git tracking
git rm --cached keystore.properties
git rm --cached local.properties
```

**Files to NEVER commit:**
- ✅ `keystore.properties` - Contains actual passwords and keystore paths
- ✅ `local.properties` - Contains your local SDK path
- ✅ `*.jks`, `*.keystore` - Your signing keys
- ✅ `*.apk`, `*.aab` - Compiled binaries

### Step 2: Verify .gitignore
Your `.gitignore` file should already exclude these files. Verify:

```bash
cat .gitignore | grep -E "keystore.properties|local.properties|\.jks|\.keystore"
```

### Step 3: Run Safety Verification
Run the automated verification script:

```bash
./verify-repo-safety.sh
```

This script checks for:
- Sensitive files (keystore.properties, local.properties)
- Hardcoded secrets (passwords, API keys, tokens)
- Personal information (file paths, email addresses)
- Build artifacts
- Required GitHub configuration files

**You MUST resolve all critical errors before proceeding!**

### Step 4: Initialize Git Repository
If not already initialized:

```bash
git init
git add .
git commit -m "Initial commit: History Motivation Coach Android app"
```

### Step 5: Create GitHub Repository
1. Go to https://github.com/new
2. Create a new public repository
3. Name it (e.g., "history-motivation-coach")
4. Do NOT initialize with README (you already have one)
5. Click "Create repository"

### Step 6: Push to GitHub
```bash
# Add remote
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git

# Push to GitHub
git branch -M main
git push -u origin main
```

## ✅ What's Safe to Commit

### Configuration Files
- ✅ `.gitignore` - Exclusion rules
- ✅ `keystore.properties.template` - Template with placeholders
- ✅ `gradle.properties` - Project settings (no secrets)
- ✅ `build.gradle.kts` - Build configuration
- ✅ `settings.gradle.kts` - Project settings

### Source Code
- ✅ All `.kt` files in `app/src/main/`
- ✅ All `.kt` test files in `app/src/test/`
- ✅ All `.xml` resource files
- ✅ `AndroidManifest.xml`
- ✅ `motivations.json` seed data

### Documentation
- ✅ `README.md` - Project overview
- ✅ `PRIVACY_POLICY.md` - Privacy policy
- ✅ All setup guides (cleaned of personal paths)
- ✅ `.github/` directory with workflows and templates

### Scripts
- ✅ `gradlew`, `gradlew.bat` - Gradle wrapper scripts
- ✅ `build-release.sh` - Build script
- ✅ `verify-repo-safety.sh` - Safety verification script

## 🔒 Security Best Practices

### For Repository Maintainers
1. **Never commit secrets** - Use environment variables or secure vaults
2. **Review PRs carefully** - Check for accidentally committed secrets
3. **Enable branch protection** - Require reviews before merging
4. **Use Dependabot** - Keep dependencies updated (already configured)
5. **Monitor security advisories** - GitHub will alert you to vulnerabilities

### For Contributors
1. **Read CONTRIBUTING.md** - Follow contribution guidelines
2. **Use the PR template** - Provide clear descriptions
3. **Run tests locally** - Ensure `./gradlew test` passes
4. **Check the pre-commit checklist** - See `.github/PRE_COMMIT_CHECKLIST.md`

### For Users Cloning the Repo
1. **Create your own keystore** - Follow `SIGNING_KEY_EXPLAINED.md`
2. **Copy template files**:
   ```bash
   cp keystore.properties.template keystore.properties
   # Edit keystore.properties with your values
   ```
3. **Create local.properties**:
   ```bash
   echo "sdk.dir=/path/to/your/Android/sdk" > local.properties
   ```

## 📋 Post-Push Checklist

After pushing to GitHub:

- [ ] Verify repository is public
- [ ] Check that sensitive files are NOT visible on GitHub
- [ ] Verify CI/CD workflow runs successfully
- [ ] Update repository description and topics
- [ ] Add repository URL to your profile
- [ ] Enable GitHub Pages (if needed)
- [ ] Configure branch protection rules
- [ ] Add collaborators (if any)

## 🛠️ GitHub Repository Settings

### Recommended Settings
1. **General**
   - Enable issues
   - Enable discussions (optional)
   - Enable projects (optional)

2. **Branches**
   - Set `main` as default branch
   - Enable branch protection:
     - Require pull request reviews
     - Require status checks to pass
     - Require branches to be up to date

3. **Security**
   - Enable Dependabot alerts
   - Enable Dependabot security updates
   - Enable secret scanning (if available)

4. **Actions**
   - Allow all actions
   - Workflow permissions: Read and write

## 🚀 Next Steps

After successful GitHub push:

1. **Add badges to README** (optional):
   ```markdown
   ![Android CI](https://github.com/YOUR_USERNAME/YOUR_REPO/workflows/Android%20CI/badge.svg)
   ![License](https://img.shields.io/badge/license-MIT-blue.svg)
   ```

2. **Create first release**:
   - Tag your commit: `git tag v1.0.0`
   - Push tag: `git push origin v1.0.0`
   - Create release on GitHub with release notes

3. **Share your project**:
   - Add to your portfolio
   - Share on social media
   - Submit to Android app showcases

## ⚠️ If You Accidentally Commit Secrets

If you accidentally push secrets to GitHub:

1. **Immediately rotate the secrets** (change passwords, regenerate keys)
2. **Remove from git history**:
   ```bash
   git filter-branch --force --index-filter \
     "git rm --cached --ignore-unmatch keystore.properties" \
     --prune-empty --tag-name-filter cat -- --all
   
   git push origin --force --all
   ```
3. **Consider the secret compromised** - Even after removal, it may have been scraped
4. **Generate new signing keys** if keystore was exposed
5. **Contact GitHub support** if needed

## 📞 Need Help?

- Check `.github/CONTRIBUTING.md` for contribution guidelines
- Check `.github/SECURITY.md` for security policies
- Open an issue with the "question" label
- Review the pre-commit checklist: `.github/PRE_COMMIT_CHECKLIST.md`

## ✨ You're Ready!

Once the verification script passes with no critical errors, you're ready to push to public GitHub. Good luck with your project! 🚀
