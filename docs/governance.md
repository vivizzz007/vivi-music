# The Governance File: Vivi Music Coding Standards

## 1. Core Principles
- **Safety First**: No hidden concurrency. Use structured concurrency (`SupervisorJob`, `viewModelScope`).
- **Zero Warnings**: Treat warnings as errors.
- **Explicit API**: Public APIs must have explicit visibility and types.
- **Immutability**: Prefer `val` over `var`. Use `@Immutable` for stable Compose classes.

## 2. Style Guide (Automated)
We enforce the standard **Android/Google Kotlin Style Guide**.
- **Tooling**: Ktlint & Detekt.
- **Max Line Length**: 100 characters.
- **Indentation**: 4 spaces.
- **Wildcard Imports**: Forbidden (`import java.util.*` -> NO).

## 3. Architecture Rules
- **Modules**: Feature-based modularization (future state). Currently package-by-feature.
- **Components**:
  - Use **Single File Components (SFC)**: Co-locate Composable, Preview, and private helpers in one file.
  - ViewModels must allow `StateFlow` observation; no direct mutable state exposure.

## 4. Git Governance
- **Branches**: `feature/xyz`, `fix/abc`. Main is protected.
- **Commits**: Conventional Commits (`feat:`, `fix:`, `chore:`, `refactor:`).
- **PRs**: Must pass CI (Build + Test + Lint).

## 5. Security & Privacy
- **Logging**: No sensitive user data in `Timber` logs.
- **Permissions**: Request minimal permissions. Handle denial gracefully.
