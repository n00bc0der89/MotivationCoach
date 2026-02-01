# RuleSet Summary

**Location:** `.kiro/ruleset.md`  
**Version:** 1.0.0  
**Last Updated:** February 1, 2026

---

## Quick Reference

The RuleSet defines comprehensive standards for the History Motivation Coach Android project across 20 major categories:

### Core Development (1-6)
1. **Code Style** - Kotlin conventions, naming, file organization
2. **Architecture** - MVVM, dependency injection, repository pattern
3. **Testing** - Unit, integration, property-based, accessibility tests
4. **Jetpack Compose** - UI components, state management, Material 3
5. **Database** - Room best practices, migrations, query optimization
6. **Background Work** - WorkManager, notifications, scheduling

### Quality & Security (7-10)
7. **Security** - Secrets management, data privacy, keystore handling
8. **Build & Release** - Gradle config, versioning, ProGuard rules
9. **Git Workflow** - Commit messages, branching, pull requests
10. **CI/CD** - Automated testing, dependency management, quality gates

### Documentation & Performance (11-14)
11. **Documentation** - Code docs, project docs, API documentation
12. **Performance** - App optimization, memory, battery, APK size
13. **Accessibility** - Content descriptions, touch targets, screen readers
14. **Error Handling** - Exception handling, logging, crash reporting

### Maintenance & Project-Specific (15-20)
15. **Maintenance** - Dependency updates, Android versions, technical debt
16. **Project Rules** - Non-repetition, scheduling, seed data, caching
17. **Pre-Commit Checklist** - Verification before committing
18. **Pre-Release Checklist** - Verification before releasing
19. **Contributing** - Guidelines for contributors
20. **Resources** - Official docs, testing resources, best practices

---

## Key Highlights

### Testing Standards
- **80%+ code coverage** on business logic
- **Property-based tests** with 1000+ iterations for critical properties
- **Accessibility testing** with TalkBack validation
- Test organization mirrors source structure

### Critical Properties Tested
✅ Non-repetition: Users never see same motivation twice  
✅ No duplicate notifications: Unique scheduling per time slot  
✅ Concurrent safety: Thread-safe database operations  
✅ Seed data validation: All required fields present  
✅ Idempotence: Operations produce same result when repeated

### Security Requirements
🔒 NEVER commit secrets to version control  
🔒 Use `.gitignore` for sensitive files  
🔒 Keep dependencies updated  
🔒 Use ProGuard/R8 for release builds  
🔒 Rotate secrets regularly

### Commit Message Format
```
<type>: <description>

Types: feat, fix, docs, test, refactor, chore
Examples:
  feat: Add notification sound preferences
  fix: Resolve crash on empty history
  docs: Update setup instructions
```

### Architecture Principles
- **MVVM** with clear separation of concerns
- **Repository pattern** as single source of truth
- **StateFlow** for reactive UI state
- **Sealed classes** for UI states
- **Open classes** for testability

---

## Quick Access

### For Developers
- Section 1: Code Style
- Section 2: Architecture
- Section 3: Testing
- Section 17: Pre-Commit Checklist

### For Reviewers
- Section 9: Git Workflow
- Section 10: CI/CD
- Section 19: Contributing

### For Release Managers
- Section 8: Build & Release
- Section 18: Pre-Release Checklist
- Section 15: Maintenance

### For New Contributors
- Section 19: Contributing Guidelines
- Section 20: Resources and References
- README.md and CONTRIBUTING.md

---

## Enforcement

Rules are enforced through:
- ✅ Code review process
- ✅ Automated CI/CD checks (GitHub Actions)
- ✅ Pre-commit verification script (`verify-repo-safety.sh`)
- ✅ Team discussions and retrospectives
- ✅ Regular rule updates based on learnings

---

## Related Documents

- **Full RuleSet:** `.kiro/ruleset.md`
- **Contributing:** `.github/CONTRIBUTING.md`
- **Security:** `.github/SECURITY.md`
- **Pre-Commit Checklist:** `.github/PRE_COMMIT_CHECKLIST.md`
- **README:** `README.md`

---

## Updates

To propose changes to the RuleSet:
1. Open an issue with the "documentation" label
2. Discuss the proposed change
3. Submit a PR with the update
4. Update version number and last updated date

---

**Remember:** These rules exist to maintain code quality, ensure security, and facilitate collaboration. When in doubt, refer to the full RuleSet or ask the team!
