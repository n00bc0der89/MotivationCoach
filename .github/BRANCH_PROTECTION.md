# Branch Protection Rules

This document describes the GitHub repository ruleset protecting the `main` branch.

---

## Ruleset Overview

**Ruleset ID:** 12347400  
**Name:** Protect main branch  
**Target:** Branch (`refs/heads/main`)  
**Enforcement:** Active  
**Created:** February 1, 2026

**View on GitHub:** https://github.com/n00bc0der89/MotivationCoach/rules/12347400

---

## Protection Rules

### 1. Pull Request Required ✅

All changes to the `main` branch must go through a pull request.

**Requirements:**
- ✅ **1 approving review required** before merging
- ✅ **Dismiss stale reviews** when new commits are pushed
- ✅ **Require conversation resolution** before merging
- ✅ All merge methods allowed (merge, squash, rebase)

**What this means:**
- You cannot push directly to `main`
- At least one other person must approve your PR
- If you push new changes, previous approvals are dismissed
- All review comments must be resolved before merging

### 2. Required Status Checks ✅

CI/CD checks must pass before merging.

**Requirements:**
- ✅ **Build check must pass** (GitHub Actions workflow)
- ✅ **Branches must be up to date** before merging
- ✅ Status checks enforced on all commits

**What this means:**
- The `build` job in GitHub Actions must succeed
- Your branch must be up-to-date with `main` before merging
- All tests must pass before you can merge

### 3. Prevent Force Pushes ✅

Force pushes and history rewrites are blocked.

**Requirements:**
- ✅ **No force pushes** to `main`
- ✅ **No history rewriting** on `main`

**What this means:**
- `git push --force` is blocked on `main`
- History on `main` is immutable
- Protects against accidental history loss

### 4. Prevent Branch Deletion ✅

The `main` branch cannot be deleted.

**Requirements:**
- ✅ **Branch deletion blocked**

**What this means:**
- The `main` branch is permanent
- Protects against accidental deletion
- Ensures repository always has a stable branch

---

## Bypass Actors

**Current bypass actors:** None

No users or teams can bypass these rules. All contributors, including repository admins, must follow the same workflow.

---

## Workflow for Contributors

### Making Changes

1. **Create a feature branch:**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes and commit:**
   ```bash
   git add .
   git commit -m "feat: Add your feature"
   ```

3. **Push your branch:**
   ```bash
   git push origin feature/your-feature-name
   ```

4. **Create a pull request:**
   - Go to GitHub repository
   - Click "Pull requests" → "New pull request"
   - Select your branch
   - Fill out the PR template
   - Submit for review

5. **Address review feedback:**
   - Make requested changes
   - Push new commits to your branch
   - Resolve all conversations

6. **Wait for approval:**
   - At least 1 reviewer must approve
   - All status checks must pass
   - All conversations must be resolved

7. **Merge your PR:**
   - Once approved and checks pass, merge
   - Choose merge method (merge, squash, or rebase)
   - Delete your feature branch after merging

### What Happens if You Try to Push Directly

```bash
$ git push origin main
remote: error: GH013: Repository rule violations found for refs/heads/main.
remote: 
remote: - Changes must be made through a pull request.
remote: 
To https://github.com/n00bc0der89/MotivationCoach.git
 ! [remote rejected] main -> main (push declined due to repository rule violations)
error: failed to push some refs to 'https://github.com/n00bc0der89/MotivationCoach.git'
```

**Solution:** Create a branch and open a pull request instead.

---

## Status Checks

### Required Checks

The following GitHub Actions workflow checks must pass:

1. **build** - Compiles the project and runs tests
   - Defined in: `.github/workflows/android-ci.yml`
   - Runs on: Every push and pull request
   - Checks:
     - ✅ Code compiles without errors
     - ✅ All unit tests pass
     - ✅ Gradle build succeeds

### Viewing Check Status

On your pull request:
1. Scroll to the bottom
2. Look for "All checks have passed" or "Some checks were not successful"
3. Click "Details" to see logs if checks fail

---

## Pull Request Review Process

### For Authors

1. **Create a clear PR:**
   - Use the PR template
   - Write a descriptive title
   - Explain what and why
   - Link related issues

2. **Respond to feedback:**
   - Address all comments
   - Ask questions if unclear
   - Make requested changes
   - Mark conversations as resolved

3. **Keep PR updated:**
   - Merge `main` into your branch if conflicts arise
   - Ensure checks pass after each push
   - Rebase if requested by reviewers

### For Reviewers

1. **Review thoroughly:**
   - Check code correctness
   - Verify tests are included
   - Look for security issues
   - Ensure documentation is updated

2. **Provide constructive feedback:**
   - Be specific and actionable
   - Explain reasoning
   - Suggest improvements
   - Be respectful

3. **Approve when satisfied:**
   - All concerns addressed
   - Code meets standards
   - Tests pass
   - Documentation complete

---

## Emergency Procedures

### Hotfix Process

For critical bugs in production:

1. **Create hotfix branch from main:**
   ```bash
   git checkout main
   git pull
   git checkout -b hotfix/critical-bug-fix
   ```

2. **Make minimal fix:**
   - Fix only the critical issue
   - Add test to prevent regression
   - Update version if needed

3. **Create PR with "hotfix" label:**
   - Mark as urgent
   - Request immediate review
   - Explain the critical nature

4. **Fast-track review:**
   - Get quick approval
   - Ensure checks pass
   - Merge immediately

5. **Follow up:**
   - Monitor deployment
   - Create issue for root cause analysis
   - Plan long-term fix if needed

### Temporarily Disabling Rules

**Not recommended**, but if absolutely necessary:

1. Repository admin can temporarily disable the ruleset
2. Make the emergency change
3. Re-enable the ruleset immediately
4. Create a follow-up PR to document the change
5. Conduct post-mortem to prevent future emergencies

---

## Modifying Protection Rules

### To Update Rules

1. **Via GitHub UI:**
   - Go to Settings → Rules → Rulesets
   - Click "Protect main branch"
   - Edit rules as needed
   - Save changes

2. **Via GitHub API:**
   ```bash
   gh api --method PUT /repos/n00bc0der89/MotivationCoach/rulesets/12347400 \
     --input updated-ruleset.json
   ```

3. **Document changes:**
   - Update this file
   - Notify team of changes
   - Update related documentation

### Proposed Changes

To propose changes to branch protection:

1. Open an issue with "repository" label
2. Explain the proposed change and reasoning
3. Discuss with team
4. Get consensus
5. Update rules
6. Update documentation

---

## Benefits of Branch Protection

### Code Quality
- ✅ All code is reviewed before merging
- ✅ Tests must pass before merging
- ✅ Catches bugs early
- ✅ Maintains consistent code style

### Collaboration
- ✅ Encourages discussion and knowledge sharing
- ✅ Multiple eyes on every change
- ✅ Reduces single points of failure
- ✅ Builds team ownership

### Safety
- ✅ Prevents accidental force pushes
- ✅ Protects against branch deletion
- ✅ Maintains clean history
- ✅ Enables easy rollbacks

### Compliance
- ✅ Audit trail of all changes
- ✅ Clear approval process
- ✅ Documented decision-making
- ✅ Meets best practices

---

## Troubleshooting

### "Push declined due to repository rule violations"

**Problem:** Trying to push directly to `main`

**Solution:** Create a branch and open a PR
```bash
git checkout -b feature/my-changes
git push origin feature/my-changes
# Then create PR on GitHub
```

### "Required status check 'build' is expected"

**Problem:** CI check hasn't run or failed

**Solution:** 
1. Wait for CI to complete
2. If failed, check logs and fix issues
3. Push fixes to your branch
4. CI will run again automatically

### "Pull request reviews are required"

**Problem:** No one has approved your PR

**Solution:**
1. Request review from team members
2. Address any feedback
3. Wait for approval
4. Then merge

### "Branch is out of date"

**Problem:** `main` has new commits since you branched

**Solution:**
```bash
git checkout main
git pull
git checkout your-branch
git merge main
# Resolve any conflicts
git push
```

---

## Related Documentation

- **Contributing Guidelines:** `.github/CONTRIBUTING.md`
- **Pull Request Template:** `.github/PULL_REQUEST_TEMPLATE.md`
- **CI/CD Workflow:** `.github/workflows/android-ci.yml`
- **RuleSet Standards:** `.kiro/ruleset.md`
- **Security Policy:** `.github/SECURITY.md`

---

## Questions?

- Check the [Contributing Guidelines](.github/CONTRIBUTING.md)
- Open an issue with the "question" label
- Ask in pull request comments
- Review GitHub's [branch protection documentation](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets)

---

**Last Updated:** February 1, 2026  
**Ruleset Version:** 1.0  
**Maintained by:** Repository Team
