# Contributing

## Workflow

This repository uses a pragmatic GitFlow model:

- `main`: production-ready code only
- `develop`: integration branch for the next release
- `feature/<scope>`: new features, branched from `develop`
- `fix/<scope>`: non-production bug fixes, branched from `develop`
- `release/vX.Y.Z`: release hardening, branched from `develop`
- `hotfix/<scope>`: urgent production fixes, branched from `main`
- `chore/<scope>`: maintenance work that is not product behavior

Examples:

- `feature/jwt-refresh-rotation`
- `fix/task-ownership-check`
- `release/v1.2.0`
- `hotfix/login-rate-limit`

## Branch Rules

- Never push directly to `main` or `develop`
- Open pull requests for every change
- Keep branches short-lived and focused on one concern
- Rebase or merge `develop` frequently to avoid long-running drift

## Commit Convention

Use Conventional Commits:

- `feat: add oauth2 success handler`
- `fix: prevent task access across users`
- `test: cover refresh token rotation`
- `docs: document release flow`
- `chore: update docker defaults`
- `ci: run verify on pull requests`

## Pull Requests

Target branch:

- `feature/*`, `fix/*`, `chore/*` -> `develop`
- `release/*` -> `main`
- `hotfix/*` -> `main`

PR requirements:

- CI green
- Scope is clear and limited
- Security-sensitive changes include tests
- API or behavior changes include documentation updates
- No unrelated refactors mixed into the same branch

Recommended merge strategy:

- `feature/*`, `fix/*`, `chore/*`: squash merge into `develop`
- `release/*`, `hotfix/*`: merge commit into `main` to preserve release history

## Definition Of Done

Before opening a PR:

1. Use Java 21 locally.
2. Run `mvn -B verify`.
3. Confirm Swagger or API contract changes are documented.
4. Confirm auth, security, and rate limit changes have test coverage.
5. Confirm no credentials or local config files were added to Git.

## Release Flow

1. Branch `release/vX.Y.Z` from `develop`.
2. Freeze new features on that branch.
3. Fix only release blockers, docs, and version metadata.
4. Run full verification and smoke test the dockerized app.
5. Merge `release/vX.Y.Z` into `main`.
6. Tag `main` with `vX.Y.Z`.
7. Merge the same release branch back into `develop`.

## Hotfix Flow

1. Branch `hotfix/<scope>` from `main`.
2. Apply the minimal safe fix.
3. Add or update regression tests.
4. Merge into `main` and tag a patch release.
5. Merge the hotfix back into `develop`.

## GitHub Settings To Apply

Branch protection cannot be committed into the repository, so configure these in GitHub:

- Protect `main` and `develop`
- Require pull requests before merge
- Require status checks to pass
- Require at least 1 approval
- Dismiss stale approvals on new commits
- Block force pushes
- Block direct deletion

## Ownership

Review routing is defined in `.github/CODEOWNERS`.
