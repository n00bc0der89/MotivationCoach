# Contributing to History Motivation Coach

Thank you for your interest in contributing! This document provides guidelines for contributing to the project.

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/MotivationCoach.git`
3. Create a feature branch: `git checkout -b feature/your-feature-name`
4. Make your changes
5. Run tests: `./gradlew test`
6. Commit your changes: `git commit -m "Add your feature"`
7. Push to your fork: `git push origin feature/your-feature-name`
8. Open a Pull Request

## Development Setup

### Prerequisites
- Android Studio Hedgehog or later
- Java 17 (required for Gradle 8.5)
- Android SDK with API 28+ and API 34

### Local Setup
1. Open the project in Android Studio
2. Create `local.properties` file:
   ```properties
   sdk.dir=/path/to/your/Android/sdk
   ```
3. Sync Gradle files
4. Run the app on an emulator or device

## Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Keep functions small and focused
- Use Jetpack Compose best practices

## Testing

All contributions should include appropriate tests:

### Unit Tests
- Write unit tests for new business logic
- Test edge cases and error conditions
- Use MockK for mocking dependencies

### Property-Based Tests
- Add property-based tests for universal correctness properties
- Use Kotest for property testing
- Run 100+ iterations for standard properties, 1000+ for critical properties

### Integration Tests
- Test component interactions
- Use fake repositories instead of mocks when possible

### Running Tests
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.example.historymotivationcoach.YourTestClass"
```

## Pull Request Guidelines

1. **Keep PRs focused**: One feature or bug fix per PR
2. **Write clear descriptions**: Explain what and why, not just how
3. **Update documentation**: Keep README and docs in sync with code changes
4. **Add tests**: All new features and bug fixes should include tests
5. **Follow the template**: Use the PR template provided
6. **Link issues**: Reference related issues using #issue_number
7. **Pass CI checks**: Ensure all tests pass before requesting review

## Commit Message Guidelines

Use clear, descriptive commit messages:
- `feat: Add notification sound preferences`
- `fix: Resolve crash on empty history`
- `docs: Update setup instructions`
- `test: Add property tests for content selector`
- `refactor: Simplify notification scheduling logic`

## Code Review Process

1. Maintainers will review your PR within a few days
2. Address any feedback or requested changes
3. Once approved, a maintainer will merge your PR

## Reporting Bugs

Use the bug report template and include:
- Clear description of the issue
- Steps to reproduce
- Expected vs actual behavior
- Device and Android version
- Relevant logs or screenshots

## Suggesting Features

Use the feature request template and include:
- Clear description of the feature
- Problem it solves
- Proposed solution
- Benefits to users

## Questions?

Feel free to open an issue with the "question" label if you need help or clarification.

## License

By contributing, you agree that your contributions will be licensed under the same license as the project.
