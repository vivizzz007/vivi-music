# The Cleanup List (Technical Debt)

## Immediate Priority (P0)
- [ ] **Unused Resources**: Run `lint` to identify and remove unused XML strings, colors, and drawables (post-string-merge).
- [ ] **Dead Code**: Remove commented-out legacy code blocks in `MusicService` (pre-refactor artifacts).

## High Priority (P1) - Architecture
- [ ] **ViewModel State**: Migrate legacy `MutableState` usage to strictly `StateFlow` in all ViewModels (as piloted in `LibraryViewModel`).
- [ ] **Hardcoded Colors**: Audit UI for direct `Color(0xFF...)` usage and replace with Theme tokens.

## Medium Priority (P2) - Optimization
- [ ] **Dependency Audit**: Review `libs.versions.toml` for unused libraries or outdated versions.
- [ ] **Proguard Rules**: Refine strict obfuscation rules for Release builds.

## Low Priority (P3)
- [ ] **File Header Standardization**: Ensure consistent license headers across all source files.
