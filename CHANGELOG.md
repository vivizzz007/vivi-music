# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [6.0.5] - 2026-08-14

### Added

- Compose Multiplatform desktop target (`desktop` module) reusing the
  pure-JVM network modules.
- Native desktop icons (Windows `.ico`, macOS `.icns`, Linux `.png`).
- Per-OS GitHub Actions builds (MSI/EXE, DEB/AppImage, DMG/PKG) and an
  auto-release workflow.
- Cross-device sync foundation: shared `sync` module, Node.js WebSocket relay
  (`sync-server/`), Android `DeviceSyncManager`, and desktop pairing UI.

### Changed

- Converted `innertube`, `spotify`, `lastfm`, `kizzy`, `shazamkit`,
  `jiosaavn`, and `lyricsProvider` to pure-JVM Kotlin modules so they can be
  shared between Android and desktop.

### Security

- Cross-device sync traffic is currently TLS-only; end-to-end encryption is
  planned for a future release.
