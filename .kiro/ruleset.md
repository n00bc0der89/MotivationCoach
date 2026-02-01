# RuleSet: History Motivation Coach Android App

This document defines the standard rules and guidelines for developing, testing, and maintaining the History Motivation Coach Android application.

---

## 1. Code Style and Standards

### 1.1 Kotlin Coding Conventions
- Follow official Kotlin coding conventions: https://kotlinlang.org/docs/coding-conventions.html
- Use meaningful variable and function names that clearly express intent
- Prefer `val` over `var` for immutability
- Use data classes for simple data holders
- Leverage Kotlin's null safety features (`?.`, `?:`, `!!` sparingly)
- Use extension functions to enhance readability
- Prefer sealed classes for restricted class hierarchies

### 1.2 File Organization
- One public class per file
- File name should match the class name
- Organize imports alphabetically
- Remove unused imports
- Group related functions together
- Keep files under 500 lines when possible

### 1.3 Naming Conventions
- **Classes/Interfaces:** PascalCase (e.g., `MotivationRepository`, `ContentSelector`)
- **Functions/Variables:** camelCase (e.g., `getUnseenMotivations`, `notificationCount`)
- **Constants:** UPPER_SNAKE_CASE (e.g., `MAX_NOTIFICATIONS_PER_DAY`, `DEFAULT_START_HOUR`)
- **Packages:** lowercase (e.g., `com.example.historymotivationcoach.data`)
- **Resources:** snake_case (e.g., `ic_notification.xml`, `activity_main.xml`)

### 1.4 Code Comments
- Write self-documenting code that minimizes need for comments
- Use KDoc for public APIs and complex functions
- Explain "why" not "what" in comments
- Keep comments up-to-date with code changes
- Use TODO comments sparingly with issue references

---

## 2. Architecture and Design

### 2.1 MVVM Architecture
- **Model:** Data layer (Room entities, DAOs, repositories)
- **View:** Compose UI screens and components
- **ViewModel:** State management and business logic coordination
- Maintain clear separation of concerns
- ViewModels should not reference Android framework classes (except AndroidViewModel when needed)

### 2.2 Dependency Injection
- Use constructor injection for dependencies
- Keep constructors simple and focused
- Avoid circular dependencies
- Make classes open or use interfaces for testability

### 2.3 Repository Pattern
- Repositories are the single source of truth for data
- Abstract data sources (database, network, preferences)
- Provide clean APIs to ViewModels
- Handle data transformations in repositories
- Mark repository classes as `open` for test fakes

### 2.4 State Management
- Use StateFlow for UI state in ViewModels
- Use sealed classes for UI states (Loading, Success, Error, Empty)
- Emit immutable state objects
- Handle all possible states in UI
- Use viewModelScope for coroutines

### 2.5 Error Handling
- Catch exceptions at appropriate boundaries
- Provide user-friendly error messages
- Log errors for debugging
- Never swallow exceptions silently
- Use Result or sealed classes for operation outcomes

---

## 3. Testing Standards

### 3.1 Test Coverage Requirements
- **Unit Tests:** All business logic, ViewModels, repositories
- **Integration Tests:** Component interactions, end-to-end flows
- **Property-Based Tests:** Critical correctness properties
- **UI Tests:** Accessibility and user interactions
- Aim for 80%+ code coverage on business logic

### 3.2 Unit Testing
- Use JUnit 4 as the base framework
- Use MockK for Kotlin-friendly mocking
- Test one thing per test method
- Use descriptive test names: `should_returnUnseenMotivations_when_historyExists()`
- Follow Arrange-Act-Assert pattern
- Keep tests independent and isolated
- Mock external dependencies

### 3.3 Property-Based Testing
- Use Kotest for property-based tests
- Run 100+ iterations for standard properties
- Run 1000+ iterations for critical properties
- Test universal correctness properties
- Focus on invariants that must always hold
- Document which requirements each property validates

**Critical Properties:**
- Non-repetition: Users never see the same motivation twice
- No duplicate notifications: Unique scheduling per time slot
- Concurrent safety: Thread-safe database operations
- Seed data validation: All required fields present
- Idempotence: Operations produce same result when repeated

### 3.4 Integration Testing
- Use fake repositories instead of mocks when possible
- Test realistic scenarios and user flows
- Verify component interactions
- Test error paths and edge cases
- Use in-memory database for data layer tests

### 3.5 Accessibility Testing
- Verify all interactive elements have content descriptions
- Test with TalkBack enabled
- Ensure minimum touch target size (48dp)
- Test keyboard navigation
- Validate color contrast ratios

### 3.6 Test Organization
- Co-locate tests with source files when possible
- Mirror source directory structure in test directory
- Group related tests in inner classes
- Use test fixtures for common setup
- Clean up resources in teardown methods

---

## 4. Jetpack Compose UI

### 4.1 Composable Functions
- Keep composables small and focused
- Extract reusable components
- Use preview annotations for development
- Prefer stateless composables
- Hoist state to appropriate level
- Use remember for expensive computations

### 4.2 State Management in UI
- Observe state with `collectAsState()`
- Use `derivedStateOf` for computed values
- Avoid side effects in composition
- Use `LaunchedEffect` for one-time events
- Use `DisposableEffect` for cleanup

### 4.3 Material Design 3
- Use Material 3 components consistently
- Follow Material Design guidelines
- Use theme colors from MaterialTheme.colorScheme
- Support both light and dark themes
- Use proper elevation and shadows

### 4.4 Accessibility in Compose
- Add contentDescription to all images and icons
- Use semantics modifiers for screen readers
- Ensure proper focus order
- Provide meaningful labels for interactive elements
- Test with TalkBack

### 4.5 Performance
- Avoid unnecessary recompositions
- Use `remember` and `derivedStateOf` appropriately
- Lazy load lists with LazyColumn/LazyRow
- Use keys in lazy lists for stability
- Profile with Compose Layout Inspector

---

## 5. Database and Data Layer

### 5.1 Room Database
- Use Room for local persistence
- Define entities with proper annotations
- Create DAOs with suspend functions
- Use Flow for reactive queries
- Add database indexes for frequently queried columns
- Version database schema properly

### 5.2 Database Migrations
- Always provide migration paths
- Test migrations thoroughly
- Never lose user data
- Document migration logic
- Use fallback to destructive migration only in development

### 5.3 Data Validation
- Validate data at entry points
- Use Room's @ColumnInfo constraints
- Check for null/empty values
- Validate foreign key relationships
- Handle constraint violations gracefully

### 5.4 Query Optimization
- Use indexes on frequently queried columns
- Avoid N+1 query problems
- Use transactions for multiple operations
- Limit query results when appropriate
- Profile slow queries

---

## 6. Background Work and Notifications

### 6.1 WorkManager
- Use WorkManager for deferrable background work
- Set appropriate constraints (network, battery, etc.)
- Use unique work names to prevent duplicates
- Handle work cancellation gracefully
- Test worker classes thoroughly

### 6.2 Notification Best Practices
- Create notification channels for Android O+
- Use meaningful notification content
- Provide notification actions when appropriate
- Handle notification permissions (Android 13+)
- Respect user notification preferences
- Test notification delivery

### 6.3 Scheduling
- Use deterministic work names for idempotency
- Schedule work at appropriate times
- Handle device reboots
- Reschedule work when preferences change
- Avoid over-scheduling

---

## 7. Security and Privacy

### 7.1 Secrets Management
- NEVER commit secrets to version control
- Use `keystore.properties` for signing credentials
- Keep `keystore.properties` in `.gitignore`
- Use environment variables for sensitive config
- Rotate secrets regularly

### 7.2 Data Privacy
- Store data locally only
- Don't transmit user data without consent
- Provide clear privacy policy
- Allow users to delete their data
- Minimize data collection

### 7.3 Code Security
- Validate all user inputs
- Use parameterized queries (Room handles this)
- Keep dependencies updated
- Use ProGuard/R8 for release builds
- Review security advisories

### 7.4 Keystore Management
- Generate strong keystores
- Use strong passwords
- Back up keystores securely
- Never share keystores publicly
- Store keystores outside version control

---

## 8. Build and Release

### 8.1 Gradle Configuration
- Use Kotlin DSL for build scripts
- Keep Gradle and plugin versions updated
- Use version catalogs for dependency management
- Configure build types (debug, release)
- Enable code shrinking for release builds

### 8.2 Release Builds
- Sign release builds with keystore
- Enable ProGuard/R8 minification
- Enable resource shrinking
- Test release builds thoroughly
- Use Android App Bundle (AAB) for Play Store

### 8.3 Versioning
- Follow semantic versioning (MAJOR.MINOR.PATCH)
- Update version code for each release
- Update version name for user-facing releases
- Tag releases in git
- Maintain changelog

### 8.4 ProGuard/R8 Rules
- Keep Room entities and DAOs
- Keep Kotlin metadata
- Keep serialization classes
- Test obfuscated builds
- Document custom rules

---

## 9. Git and Version Control

### 9.1 Commit Messages
- Use conventional commits format
- Start with type: feat, fix, docs, test, refactor, chore
- Write clear, concise commit messages
- Reference issue numbers when applicable
- Keep commits atomic and focused

**Examples:**
```
feat: Add notification sound preferences
fix: Resolve crash on empty history
docs: Update setup instructions
test: Add property tests for content selector
refactor: Simplify notification scheduling logic
```

### 9.2 Branching Strategy
- `main` branch is always stable
- Create feature branches from `main`
- Use descriptive branch names: `feature/notification-sounds`
- Delete branches after merging
- Keep branches short-lived

### 9.3 Pull Requests
- Use PR template
- Write clear PR descriptions
- Link related issues
- Request reviews from team members
- Address review feedback promptly
- Ensure CI passes before merging

### 9.4 Code Review
- Review for correctness, not style
- Check for security issues
- Verify tests are included
- Ensure documentation is updated
- Be constructive and respectful
- Approve only when satisfied

---

## 10. CI/CD and Automation

### 10.1 Continuous Integration
- Run tests on every push
- Run tests on every PR
- Build project to verify compilation
- Upload test results and artifacts
- Fail fast on errors

### 10.2 Automated Testing
- Run unit tests in CI
- Run integration tests in CI
- Run property-based tests in CI
- Generate test coverage reports
- Track coverage trends

### 10.3 Dependency Management
- Use Dependabot for updates
- Review dependency updates weekly
- Test updates before merging
- Keep security patches current
- Monitor for vulnerabilities

### 10.4 Quality Gates
- Require tests to pass before merge
- Require code review approval
- Enforce branch protection rules
- Check for security vulnerabilities
- Validate code style (optional)

---

## 11. Documentation

### 11.1 Code Documentation
- Document public APIs with KDoc
- Explain complex algorithms
- Document assumptions and constraints
- Keep documentation up-to-date
- Use examples in documentation

### 11.2 Project Documentation
- Maintain comprehensive README
- Document setup instructions
- Provide architecture overview
- Document key design decisions
- Include troubleshooting guide

### 11.3 API Documentation
- Document repository methods
- Document ViewModel state and events
- Document worker inputs and outputs
- Document database schema
- Document notification channels

---

## 12. Performance and Optimization

### 12.1 App Performance
- Profile app with Android Profiler
- Optimize database queries
- Use lazy loading for lists
- Cache images appropriately
- Minimize main thread work

### 12.2 Memory Management
- Avoid memory leaks
- Use weak references when appropriate
- Clean up resources in onCleared()
- Profile memory usage
- Handle low memory conditions

### 12.3 Battery Optimization
- Minimize background work
- Use appropriate WorkManager constraints
- Batch network requests
- Respect Doze mode
- Test battery impact

### 12.4 APK Size
- Enable code shrinking
- Enable resource shrinking
- Use vector drawables
- Compress images
- Remove unused resources

---

## 13. Accessibility

### 13.1 Content Descriptions
- Add contentDescription to all images
- Provide meaningful labels
- Describe button actions
- Avoid redundant descriptions
- Test with TalkBack

### 13.2 Touch Targets
- Minimum 48dp touch target size
- Provide adequate spacing
- Make interactive elements obvious
- Support both touch and keyboard

### 13.3 Color and Contrast
- Ensure sufficient color contrast
- Don't rely on color alone
- Support dark mode
- Test with color blindness simulators

### 13.4 Screen Readers
- Test with TalkBack enabled
- Provide logical focus order
- Group related content
- Announce state changes

---

## 14. Error Handling and Logging

### 14.1 Error Handling
- Catch exceptions at boundaries
- Provide user-friendly messages
- Log errors for debugging
- Handle all error states in UI
- Gracefully degrade functionality

### 14.2 Logging
- Use appropriate log levels (DEBUG, INFO, WARN, ERROR)
- Don't log sensitive information
- Use structured logging
- Remove debug logs in release builds
- Include context in log messages

### 14.3 Crash Reporting
- Consider crash reporting service (optional)
- Handle uncaught exceptions
- Provide crash recovery
- Test crash scenarios
- Monitor crash rates

---

## 15. Maintenance and Updates

### 15.1 Dependency Updates
- Review updates weekly
- Test updates thoroughly
- Update one dependency at a time
- Read changelogs before updating
- Monitor for breaking changes

### 15.2 Android Version Support
- Support minimum SDK 28 (Android 9)
- Target latest stable SDK
- Test on multiple Android versions
- Handle API level differences
- Deprecate old versions gradually

### 15.3 Technical Debt
- Track technical debt in issues
- Allocate time for refactoring
- Improve code quality incrementally
- Document known issues
- Prioritize critical debt

---

## 16. Specific Project Rules

### 16.1 Non-Repetition Guarantee
- NEVER show the same motivation twice
- Record delivery immediately before showing
- Test non-repetition with property-based tests
- Handle content exhaustion gracefully
- Provide "Replay Classics" feature

### 16.2 Notification Scheduling
- Use deterministic work names
- Prevent duplicate notifications
- Reschedule daily at midnight
- Handle preference changes
- Survive device reboots

### 16.3 Seed Data
- Load seed data only once
- Validate all seed data fields
- Handle loading errors gracefully
- Provide loading state feedback
- Test idempotence

### 16.4 Image Caching
- Configure Coil with memory cache (25% of available memory)
- Configure disk cache (50MB)
- Handle cache misses gracefully
- Provide placeholder images
- Test offline scenarios

---

## 17. Pre-Commit Checklist

Before committing code, verify:
- [ ] Code compiles without errors
- [ ] All tests pass locally
- [ ] New code has tests
- [ ] Code follows style guidelines
- [ ] No secrets or sensitive data
- [ ] Documentation is updated
- [ ] Commit message is clear
- [ ] No debug code or TODOs (or documented)

---

## 18. Pre-Release Checklist

Before releasing, verify:
- [ ] All tests pass
- [ ] Property-based tests pass (1000+ iterations)
- [ ] Manual testing completed
- [ ] Accessibility tested with TalkBack
- [ ] Release notes prepared
- [ ] Version numbers updated
- [ ] Keystore configured correctly
- [ ] ProGuard rules tested
- [ ] APK/AAB size is reasonable
- [ ] No known critical bugs

---

## 19. Contributing Guidelines

### 19.1 For New Contributors
- Read README.md and CONTRIBUTING.md
- Set up development environment
- Run tests to verify setup
- Start with good first issues
- Ask questions in issues/discussions

### 19.2 For Code Contributions
- Fork the repository
- Create a feature branch
- Write tests for new features
- Follow code style guidelines
- Submit a pull request
- Respond to review feedback

### 19.3 For Bug Reports
- Use bug report template
- Provide reproduction steps
- Include device and Android version
- Attach logs if available
- Check for existing issues first

---

## 20. Resources and References

### 20.1 Official Documentation
- Kotlin: https://kotlinlang.org/docs/
- Android: https://developer.android.com/
- Jetpack Compose: https://developer.android.com/jetpack/compose
- Room: https://developer.android.com/training/data-storage/room
- WorkManager: https://developer.android.com/topic/libraries/architecture/workmanager

### 20.2 Testing Resources
- Kotest: https://kotest.io/
- MockK: https://mockk.io/
- Property-Based Testing: https://hypothesis.works/articles/what-is-property-based-testing/

### 20.3 Best Practices
- Android Architecture Guide: https://developer.android.com/topic/architecture
- Material Design 3: https://m3.material.io/
- Accessibility: https://developer.android.com/guide/topics/ui/accessibility

---

## Enforcement

These rules are enforced through:
- Code review process
- Automated CI/CD checks
- Pre-commit verification script
- Team discussions and retrospectives
- Regular rule updates based on learnings

---

**Last Updated:** February 1, 2026  
**Version:** 1.0.0  
**Maintainer:** Project Team
