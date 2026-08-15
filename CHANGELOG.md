# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Desktop-specific changes are marked with `[DE]` (e.g. `- [DE] New desktop
feature.`). Desktop releases use a combined version `<mobile>_DE-<desktop>`
(e.g. `6.0.5_DE-1.0.0`), where the desktop part is the program's own SemVer.

## [Unreleased]

### Added

- Added LAN discovery to the Android Devices screen: "Find desktop" (mDNS/NSD
  `_vivimusic._tcp`) and "Scan QR code" auto-fill the relay server URL when
  pairing with VIVI Music DE over the same Wi-Fi.
- [DE] Added LAN discovery aids to the desktop Device sync section: a QR code
  encoding the local relay address, and mDNS service registration
  (`_vivimusic._tcp`) so the Android app can discover/scan the desktop.
- [DE] Added offline LAN (same Wi-Fi) device pairing: the desktop can start a
  local WebSocket relay from Settings → Device sync, so the Android app can
  pair directly without the cloud relay.
- [DE] Added an Updates section in Settings: an automatic update check on
  startup plus a manual "Check for updates" button, an opt-in toggle to include
  pre-releases, and a download link when a newer desktop release is available.
- [DE] Integrated self-contained audio playback (Phase 4): AAC stream
  resolution (NewPipe + ANDROID_VR) and a pure-Java AAC decoder (`jaad`)
  played through Java Sound — no external player or native codec required.
  The mini-player and Player screen now actually play/pause/resume songs and
  show the playback position.
- [DE] Added YouTube login on desktop: paste the music.youtube.com `Cookie`
  header (no WebView needed), which auto-extracts the account's
  `DATASYNC_ID`/`VISITOR_DATA`, validates the session and persists it locally.
  History now works when signed in, and Library gained Songs / Albums / Artists
  / Playlists tabs (liked songs, albums, artists and playlists).
- [DE] Added in-app update downloads: the Updates section detects the right
  installer for the host OS/arch (MSI/AppImage/DMG with EXE/DEB/PKG fallback),
  downloads it with progress % + speed, opens it, and can delete downloaded
  installers.
- [DE] Added a changelog screen (About → Changelog) showing the bundled
  `CHANGELOG.md` plus the latest GitHub release notes.
- [DE] Added Player & audio settings (autoplay next track) and a Storage
  section (cache size + clear cache) to Settings.
- [DE] Wired device sync end-to-end: the desktop now pushes its playback
  (track, queue, position, play/pause) and settings, and applies incoming
  snapshots — remote playback starts on the desktop player and language /
  theme / accent follow the phone. A persistent `DesktopSyncManager` owns the
  client + LAN relay for the whole app lifetime (no more state loss when
  leaving Settings), with echo suppression to avoid ping-pong loops.
- Android now pushes its playback to the desktop and applies incoming playback
  snapshots (desktop → phone), so starting a song on either device resumes on
  the other.
- [DE] Added the animated canvas to the Player: a blurred, slowly-zooming
  (Ken Burns) artwork background behind the track. Canvas artwork is resolved
  from the same providers as the Android app (Apple Music / Tidal / VIVI Music
  canvas); animated GIF/WebP URLs play via Coil, while video canvases
  (MP4/HLS) fall back to static art + zoom.
- [DE] Rebuilt the Player screen as a full Material 3 player: a seek slider
  with elapsed/total time, a volume slider, shuffle and repeat (off/all/one),
  proper Material icons and a large artwork presentation. Playback now reports
  the track duration and supports seeking (the stream is cached locally), the
  volume is adjustable, and the mini-player shows a progress bar.
- [DE] Added optional manual `DATASYNC_ID` / `VISITOR_DATA` fields to the
  desktop login screen as a fallback for when the automatic extraction from
  the music.youtube.com shell fails.
- [DE] Added synced lyrics: the Lyrics screen now parses LRC timestamps,
  highlights the current line and auto-scrolls to it as the song plays
  (falls back to plain text when lyrics aren't synced).
- [DE] Added drag-to-reorder to the Queue screen (drag the ⠿ handle), reusing
  the same `sh.calvin.reorderable` library as the Android app.
- [DE] Added a full playback queue: "add to queue" on every song row, "Play
  all" on albums/playlists, next/previous, auto-advance, and a Queue screen
  (jump / remove / clear).
- [DE] Added a History screen (sidebar) listing the user's listening history.
- [DE] Added a light/dark/system theme with a selectable accent color palette
  (Settings → Appearance), applied across the whole desktop app.
- [DE] Full desktop UI: sidebar navigation with Home, Search, Album, Artist,
  Playlist, Library, Player, Lyrics and Settings screens, artwork thumbnails,
  and an Apple Music–style mini-player. (Library is a placeholder pending
  login; audio playback and the animated canvas are deferred to later phases.)
- Added a "Devices" section in the Android Settings to pair the phone with
  VIVI Music DE (relay server URL, generate/join pairing code, unpair).

### Fixed

- [DE] Fixed dark mode not repainting the page background: the app root now
  paints the theme's `background` color, so switching to dark converts the
  whole window instead of leaving the native light background showing through.
- [DE] Fixed most text not adapting to the dark/light theme: Material 3's
  `MaterialTheme` does not set `LocalContentColor`, so text without an explicit
  color fell back to black. The app root now provides `LocalContentColor =
  onBackground`, so titles, headers and other uncolored text follow the theme.
- [DE] Fixed the "Open installer" button not launching the downloaded
  installer: opening now falls back to the OS's native opener (`cmd /c start`
  on Windows, `open` on macOS, `xdg-open` on Linux) when `Desktop.open()`
  fails, and reports an error instead of failing silently.
- [DE] Fixed the desktop player showing "could not resolve the audio stream":
  the stream resolver now uses the same multi-client fallback chain as the
  mobile app (ANDROID_VR + 11 fallback clients, n-param deobfuscation and URL
  validation) instead of a single ANDROID_VR attempt that YouTube often answers
  with `LOGIN_REQUIRED`.
- [DE] Fixed the auto-release workflow's invalid YAML: `continue-on-error` is not
  allowed on a job that calls a reusable workflow, so the Android APK build is
  now made optional with per-step `continue-on-error` inside `build-android.yml`
  instead (the desktop release no longer requires the APK to succeed).
- [DE] Fixed desktop device pairing: the desktop client now actually connects
  (the `connect()` call was unreachable) and defaults to the same relay URL as
  the Android app instead of the local `wss://localhost:8080` placeholder, so
  "Generate code" produces a code.
- [DE] Made `gradlew` executable in the repository and in the desktop build
  workflows (fixes `./gradlew: Permission denied` on Linux/macOS runners).
- [DE] Replaced the retired `macos-13` runner with `macos-15-intel` for the
  Intel macOS build.
- [DE] Removed the Inno Setup wizard images that could make the installer open
  and immediately close on some systems.

### Changed

- Desktop releases now use a combined `<mobile>_DE-<desktop>` version
  (e.g. `6.0.5_DE-1.0.0`): `version.txt` line 1 = mobile version, line 2 = DE
  version, line 3 = channel. The About screen shows the full version + channel;
  desktop changelog entries are marked `[DE]`.
- The release channel is now read from line 3 of `version.txt`: `stable` (or
  empty) publishes a stable release; any other value (`rc`/`beta`/`alpha`/
  `nightly`) publishes a pre-release.
- [DE] The desktop UI is now English-first with a 49-language picker (first
  launch + Language menu); non-English strings fall back to English until
  translated.
- Release tags no longer carry a `v` prefix; non-stable releases append the
  channel to the tag (e.g. `6.0.5_DE-1.0.0-nightly`).
- [DE] The Windows build now produces both an Inno Setup installer and a
  jpackage MSI.
- [DE] The Windows installer now performs a machine-wide install into
  `C:\Program Files\VIVIMusic` (requires admin rights) instead of a per-user
  install into `%LOCALAPPDATA%`.
- [DE] The Windows installer is now a branded Inno Setup wizard and shows a
  "successfully uninstalled" confirmation message after removal.
- [DE] Split the Linux build into independent DEB and AppImage jobs so a
  failure in the AppImage step no longer blocks the DEB package (or the
  release).
- [DE] The auto-release now also builds and attaches the Android APKs
  (GMS + FOSS) to the same release, so each release ships desktop + mobile
  assets together.
- [DE] The auto-release now attaches the mobile APKs by downloading them from
  the mobile CI release (tag `v<mobile>`) instead of rebuilding them in a
  separate Android job, making desktop releases much faster.
- [DE] Release notes now collapse the commit list into an expandable section
  when there are more than 7 commits.

- [DE] Debounced the Lyrics screen position updates: the highlighted line is
  now polled ~5×/s and only recomposed when it changes, instead of recomposing
  the list on every decoded-frame position update (~40×/s).

## [6.0.5] - 2026-08-14

### Added

- [DE] Compose Multiplatform desktop target (`desktop` module) reusing the
  pure-JVM network modules.
- [DE] Native desktop icons (Windows `.ico`, macOS `.icns`, Linux `.png`).
- [DE] Per-OS GitHub Actions builds (MSI/EXE, DEB/AppImage, DMG/PKG) and an
  auto-release workflow.
- [DE] Cross-device sync foundation: shared `sync` module, Node.js WebSocket
  relay (`sync-server/`), Android `DeviceSyncManager`, and desktop pairing UI.

### Changed

- Converted `innertube`, `spotify`, `lastfm`, `kizzy`, `shazamkit`,
  `jiosaavn`, and `lyricsProvider` to pure-JVM Kotlin modules so they can be
  shared between Android and desktop.

### Security

- Cross-device sync traffic is currently TLS-only; end-to-end encryption is
  planned for a future release.
