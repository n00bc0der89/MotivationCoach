# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

## Reporting a Vulnerability

If you discover a security vulnerability in this project, please report it by:

1. **DO NOT** open a public issue
2. Email the maintainer directly (check repository owner's profile)
3. Include:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

We will respond within 48 hours and work with you to address the issue.

## Security Best Practices

### For Contributors
- Never commit secrets, API keys, or passwords
- Use `keystore.properties.template` for examples
- Keep `keystore.properties` and `local.properties` in .gitignore
- Review the Pre-Commit Checklist before pushing code
- Use environment variables for sensitive configuration

### For Users
- Keep your keystore file secure and backed up
- Use strong passwords for keystores
- Never share your `keystore.properties` file
- Store signing keys in a secure location
- Enable two-factor authentication on your GitHub account

## Known Security Considerations

### Keystore Management
- The app uses Android keystore for release signing
- Keystore files are excluded from version control
- Users must generate their own keystores for Play Store deployment
- See `SIGNING_KEY_EXPLAINED.md` for detailed information

### Data Privacy
- The app stores motivational content locally using Room database
- No user data is transmitted to external servers
- All data remains on the device
- See `PRIVACY_POLICY.md` for user-facing privacy information

### Permissions
- The app requires notification permissions (Android 13+)
- No network permissions required
- No location or camera permissions
- Minimal permission footprint

## Security Updates

Security updates will be released as patch versions (e.g., 1.0.1) and announced in:
- GitHub Releases
- Repository README
- Security advisories (for critical issues)

## Dependency Security

- Dependabot is enabled for automatic dependency updates
- Regular security audits of third-party libraries
- Gradle dependencies are kept up to date
- See `.github/dependabot.yml` for configuration
