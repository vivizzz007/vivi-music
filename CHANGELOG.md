# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Desktop-specific changes are marked with `[DE]`. Desktop releases use a combined
version `<mobile>_DE-<desktop>` (e.g. `6.0.5_DE-1.0.0`), where the desktop part is
the program's own SemVer. `[APK]` marks mobile-only changes.

## [Unreleased]

## [6.2.1_DE-1.22.1] - 2026-08-15

### Fixed

- [DE] Audio download no longer fails with "HTTP 403 downloading audio".
  googlevideo ties a stream URL to the client that requested it, so the player
  now downloads with the same User-Agent used to resolve the URL (and a
  `Range: bytes=0-` header), instead of a fixed browser UA that YouTube
  rejected.

## [6.2.1_DE-1.22.0] - 2026-08-15

### Added

- [DE] Search now has mobile-style filter chips (All / Songs / Videos / Albums /
  Artists / Playlists) backed by the innerTube filtered search, plus live query
  suggestions as you type.
- [DE] Library tabs now use Material 3 filter chips and the Songs tab gains a
  "Shuffle all" action.
- [DE] Album and Playlist screens gain a "Shuffle" action next to "Play all".

## [6.2.1_DE-1.21.0] - 2026-08-15

### Added

- [DE] Settings are now organized into mobile-style sub-screens (Language,
  Updates, Appearance, Player & audio, Account, Devices, Content, Lyrics,
  Privacy, Storage, About) instead of one long scrollable page.
- [DE] New Content sub-screen: pick the YouTube content language and region
  (innerTube `hl`/`gl`), applied live and persisted across restarts.
- [DE] New Lyrics sub-screen with a "synced lyrics" toggle that enables or
  disables line-by-line highlighting.
- [DE] New Privacy sub-screen to clear the session, cache and downloaded
  installers.

## [6.2.1_DE-1.20.1] - 2026-08-15

### Changed

- [DE] The auto-release no longer builds the APK itself: it waits for the
  existing "CI & Debug Build" (`build.yml`) run on the same commit and attaches
  its debug APK artifact to the desktop release. The dedicated
  `release-mobile.yml` build workflow was removed.

## [6.2.1_DE-1.20.0] - 2026-08-15

### Fixed

- [DE] Audio playback now works. YouTube serves its `audio/mp4` streams as
  fragmented MP4 (DASH fMP4, `ftyp` brand "dash"), whose samples live in
  `moof`/`trun` boxes instead of the `moov` sample table — which `jaad`'s
  `MP4Container` demuxer does not understand, so every track failed with
  "No audio frames to decode". The player now walks the fragments directly
  with jcodec and decodes with the bundled jaad AAC decoder.
- [DE] The stream resolver now prefers AAC-LC (codec `mp4a.40.2`, itag 140/141)
  over HE-AAC/SBR (`mp4a.40.5`, itag 139), which jaad cannot decode ("FIL
  element overread").
- [DE] Language sync with the mobile app now maps the differing locale codes
  (mobile `no`/`pt-PT`/`zh-CN`/`zh-TW` ↔ desktop `nb`/`pt`/`zh-rCN`/`zh-rTW`),
  so changing the language on one device is correctly reflected on the other.

### Added

- [DE] All text (errors, options, settings, LAN server details, etc.) is now
  selectable and copyable across the whole desktop app.

## [6.2.1_DE-1.19.3] - 2026-08-15

### Changed

- [DE] APK delivery for desktop releases: a dedicated workflow
  (`release-mobile.yml`, a copy of the mobile CI adapted for `vivi-music-de`)
  builds and signs the GMS + FOSS APKs on this branch and attaches them to the
  release created by `auto-release.yml`. The in-pipeline `build-android` job was
  removed from `auto-release.yml` to avoid building the APK twice.

## [6.2.1_DE-1.19.2] - 2026-08-15

### Fixed

- [DE] Content now scales with the window: the Player artwork resizes with the
  window width (180–360dp) instead of being fixed at 300dp, and Library cards
  fill their adaptive grid cells instead of a fixed 140dp width.

## [6.2.1_DE-1.19.1] - 2026-08-15

### Changed

- [APK] Bumped the mobile version to 6.2.1 (versionCode 77).
- The desktop auto-release now builds and attaches the Android APK directly from
  this branch instead of trying to download it from the mobile CI release
  (`release.yml` runs on `main`, which had fallen out of sync), so the APK now
  reliably appears among the release assets.

## [6.2.0_DE-1.19.1] - 2026-08-15

### Changed

- [APK] The pairing-code field on Android now opens the numeric keypad and only
  accepts the 6 digits of the code shown by the desktop.

## [6.2.0_DE-1.19.0] - 2026-08-15

### Changed

- [DE] The About → Changelog screen now matches the mobile app: a horizontally
  scrollable row of version chips on top, and, for the selected version, a bold
  primary title plus its Added/Fixed/Changed sections rendered as bullet items
  (instead of a flat markdown dump of the whole file).

## [6.2.0_DE-1.18.0] - 2026-08-15

### Added

- [DE] The desktop pairing code now shows a live countdown until it expires
  (5 minutes) and offers a "Generate new code" button to mint a fresh code.

### Changed

- [DE] The desktop Device sync section is now generate-only: it no longer shows
  an "enter code" field. The desktop generates the 6-digit code, and the phone
  enters it.
- [APK] The Android Devices screen is now insert-only: the "Generate code"
  button was removed, leaving just the "enter code" + Pair flow that reads the
  code shown by the desktop.

## [6.2.0_DE-1.17.1] - 2026-08-15

### Fixed

- [DE] Fixed desktop audio not playing: the player reused a stale cache file when
  switching tracks, silently swallowed every decode/download/audio-device error
  (so playback stopped with no message), and did not verify the downloaded file.
  Failures now surface a clear reason in the player, and the decode pipeline is
  more robust.
- [DE] The app now closes itself right after launching an update installer so the
  installer can replace the running files (updates could not install otherwise).

### Changed

- [DE] The About → Changelog screen now fetches `CHANGELOG.md` live from the
  repository (falling back to the bundled copy when offline), so it always shows
  the current changelog without waiting for a new build.

## [6.2.0_DE-1.17.0] - 2026-08-15

### Added

- [DE] Ported the Home screen to the Android app's style: filter chips, mobile-style
  section headers (label + bold primary title, "Play all" button, chevron), songs-only
  sections rendered as horizontal song lists (Quick picks style), mixed sections as
  card carousels, and a Mood & genres section whose buttons open a new generic
  Browse screen.
- [DE] The QR code in Device sync is now always rendered on a solid white card so it
  scans reliably in both light and dark themes.

## [6.2.0_DE-1.16.2] - 2026-08-15

### Fixed

- [DE] Fixed the changelog being unclear about desktop versions: releases are
  now versioned as `<mobile>_DE-<de>` sections in `CHANGELOG.md` (instead of
  everything accumulating under "Unreleased"), and the changelog screen shows
  the current DE version and channel at the top.

## [6.2.0_DE-1.16.1] - 2026-08-15

### Fixed

- [DE] Fixed the language selector always showing English: the desktop string
  table now ships real translations for 45 languages (generated from the
  Android app's `strings.xml` / `vivi_strings.xml` via
  `scripts/generate_desktop_localization.py`). Keys without a translation
  still fall back to English.

## [6.2.0_DE-1.16.0] - 2026-08-15

### Added

- [DE] Started the pixel-perfect UI port from the Android app: the theme now
  uses the same seed-based Material 3 palette (materialKolor TonalSpot, seed =
  accent color) as the mobile app, and the fixed text sidebar was replaced with
  a collapsible/expandable sidebar with Material icons + labels (persisted).

## [6.2.0_DE-1.15.1] - 2026-08-15

### Fixed

- [DE] Fixed the update check picking the wrong release: it now selects the
  desktop release with the highest `_DE-<version>` tag instead of the first /
  "latest" entry (GitHub orders releases by publish date, not by version), so
  an older tag no longer masks a newer one. The release list window was also
  raised to 100 and the changelog notes follow the same highest-version rule.

## [6.2.0_DE-1.15.0] - 2026-08-15

### Added

- [DE] Wired device sync end-to-end: the desktop now pushes its playback
  (track, queue, position, play/pause) and settings, and applies incoming
  snapshots — remote playback starts on the desktop player and language /
  theme / accent follow the phone. A persistent `DesktopSyncManager` owns the
  client + LAN relay for the whole app lifetime (no more state loss when
  leaving Settings), with echo suppression to avoid ping-pong loops.
- [APK] Android now pushes its playback to the desktop and applies incoming
  playback snapshots (desktop → phone), so starting a song on either device
  resumes on the other.

## [6.2.0_DE-1.14.3] - 2026-08-15

### Fixed

- [DE] Fixed most text not adapting to the dark/light theme: Material 3's
  `MaterialTheme` does not set `LocalContentColor`, so text without an explicit
  color fell back to black. The app root now provides `LocalContentColor =
  onBackground`, so titles, headers and other uncolored text follow the theme.

## [6.2.0_DE-1.14.2] - 2026-08-15

### Changed

- [DE] Debounced the Lyrics screen position updates: the highlighted line is
  now polled ~5×/s and only recomposed when it changes, instead of recomposing
  the list on every decoded-frame position update (~40×/s).

## [6.2.0_DE-1.14.1] - 2026-08-15

### Fixed

- [DE] Fixed the "Open installer" button not launching the downloaded
  installer: opening now falls back to the OS's native opener (`cmd /c start`
  on Windows, `open` on macOS, `xdg-open` on Linux) when `Desktop.open()`
  fails, and reports an error instead of failing silently.

## [6.2.0_DE-1.14.0] - 2026-08-15

### Added

- [DE] Added optional manual `DATASYNC_ID` / `VISITOR_DATA` fields to the
  desktop login screen as a fallback for when the automatic extraction from
  the music.youtube.com shell fails.

## [6.2.0_DE-1.13.0] - 2026-08-15

### Added

- [DE] Rebuilt the Player screen as a full Material 3 player: a seek slider
  with elapsed/total time, a volume slider, shuffle and repeat (off/all/one),
  proper Material icons and a large artwork presentation. Playback now reports
  the track duration and supports seeking (the stream is cached locally), the
  volume is adjustable, and the mini-player shows a progress bar.

## [6.2.0_DE-1.12.1] - 2026-08-15

### Fixed

- [DE] Fixed dark mode not repainting the page background: the app root now
  paints the theme's `background` color, so switching to dark converts the
  whole window instead of leaving the native light background showing through.

## [6.2.0_DE-1.12.0] - 2026-08-15

### Added

- [DE] Added the animated canvas to the Player: a blurred, slowly-zooming
  (Ken Burns) artwork background behind the track. Canvas artwork is resolved
  from the same providers as the Android app (Apple Music / Tidal / VIVI Music
  canvas); animated GIF/WebP URLs play via Coil, while video canvases
  (MP4/HLS) fall back to static art + zoom.

## [6.2.0_DE-1.11.0] - 2026-08-15

### Added

- [DE] Added synced lyrics: the Lyrics screen now parses LRC timestamps,
  highlights the current line and auto-scrolls to it as the song plays
  (falls back to plain text when lyrics aren't synced).

## [6.2.0_DE-1.10.0] - 2026-08-15

### Added

- [DE] Added drag-to-reorder to the Queue screen (drag the ⠿ handle), reusing
  the same `sh.calvin.reorderable` library as the Android app.

## [6.2.0_DE-1.9.0] - 2026-08-15

### Added

- [DE] Added YouTube login on desktop: paste the music.youtube.com `Cookie`
  header (no WebView needed), which auto-extracts the account's
  `DATASYNC_ID`/`VISITOR_DATA`, validates the session and persists it locally.
  History now works when signed in, and Library gained Songs / Albums / Artists
  / Playlists tabs (liked songs, albums, artists and playlists).

## [6.2.0_DE-1.8.0] - 2026-08-14

### Added

- [DE] Added an Updates section in Settings with in-app downloads: it detects
  the right installer for the host OS/arch (MSI/AppImage/DMG with EXE/DEB/PKG
  fallback), downloads it with progress % + speed, opens it, and can delete
  downloaded installers.
- [DE] Added Player & audio settings (autoplay next track) and a Storage
  section (cache size + clear cache) to Settings.
- [DE] Added a changelog screen (About → Changelog) showing the bundled
  `CHANGELOG.md` plus the latest GitHub release notes.

## [6.2.0_DE-1.7.0] - 2026-08-14

### Added

- [DE] Added a full playback queue: "add to queue" on every song row, "Play
  all" on albums/playlists, next/previous, auto-advance, and a Queue screen
  (jump / remove / clear).
- [DE] Added a History screen (sidebar) listing the user's listening history.

## [6.2.0_DE-1.6.0] - 2026-08-14

### Added

- [DE] Added a light/dark/system theme with a selectable accent color palette
  (Settings → Appearance), applied across the whole desktop app.

## [6.2.0_DE-1.5.1] - 2026-08-14

### Fixed

- [DE] Fixed the desktop player showing "could not resolve the audio stream":
  the stream resolver now uses the same multi-client fallback chain as the
  mobile app (ANDROID_VR + 11 fallback clients, n-param deobfuscation and URL
  validation) instead of a single ANDROID_VR attempt that YouTube often answers
  with `LOGIN_REQUIRED`.

## [6.2.0_DE-1.5.0] - 2026-08-14

### Added

- [APK] Added LAN discovery to the Android Devices screen: "Find desktop"
  (mDNS/NSD `_vivimusic._tcp`) and "Scan QR code" auto-fill the relay server
  URL when pairing with VIVI Music DE over the same Wi-Fi. (Mobile version
  bumped 6.1.0 → 6.2.0.)

## [6.1.0_DE-1.5.0] - 2026-08-14

### Added

- [DE] Added LAN discovery aids to the desktop Device sync section: a QR code
  encoding the local relay address, and mDNS service registration
  (`_vivimusic._tcp`) so the Android app can discover/scan the desktop.

## [6.1.0_DE-1.4.0] - 2026-08-14

### Added

- [DE] Added offline LAN (same Wi-Fi) device pairing: the desktop can start a
  local WebSocket relay from Settings → Device sync, so the Android app can
  pair directly without the cloud relay.

## [6.1.0_DE-1.3.1] - 2026-08-14

### Changed

- [DE] The auto-release now attaches the mobile APKs by downloading them from
  the mobile CI release (tag `v<mobile>`) instead of rebuilding them in a
  separate Android job, making desktop releases much faster.

## [6.1.0_DE-1.3.0] - 2026-08-14

### Added

- [DE] Added an Updates section in Settings: an automatic update check on
  startup plus a manual "Check for updates" button, an opt-in toggle to include
  pre-releases, and a download link when a newer desktop release is available.

## [6.1.0_DE-1.2.1] - 2026-08-14

### Fixed

- [DE] Fixed the auto-release workflow's invalid YAML: `continue-on-error` is not
  allowed on a job that calls a reusable workflow, so the Android APK build is
  now made optional with per-step `continue-on-error` inside `build-android.yml`
  instead (the desktop release no longer requires the APK to succeed).

## [6.1.0_DE-1.2.0] - 2026-08-14

### Added

- [DE] Integrated self-contained audio playback (Phase 4): AAC stream
  resolution (NewPipe + ANDROID_VR) and a pure-Java AAC decoder (`jaad`)
  played through Java Sound — no external player or native codec required.
  The mini-player and Player screen now actually play/pause/resume songs and
  show the playback position.

## [6.1.0_DE-1.1.2] - 2026-08-14

### Changed

- [DE] Release notes now collapse the commit list into an expandable section
  when there are more than 7 commits.

## [6.1.0_DE-1.1.1] - 2026-08-14

### Changed

- [DE] The Android APK build in the auto-release is now optional (per-step
  best-effort), so a missing signing secret or failed APK build no longer
  blocks the desktop release.

## [6.1.0_DE-1.1.0] - 2026-08-14

### Added

- [DE] Full desktop UI: sidebar navigation with Home, Search, Album, Artist,
  Playlist, Library, Player, Lyrics and Settings screens, artwork thumbnails,
  and an Apple Music–style mini-player. (Library is a placeholder pending
  login; audio playback and the animated canvas are deferred to later phases.)

## [6.1.0_DE-1.0.4] - 2026-08-14

### Changed

- [DE] The auto-release now also builds and attaches the Android APKs
  (GMS + FOSS) to the same release, so each release ships desktop + mobile
  assets together.

## [6.1.0_DE-1.0.3] - 2026-08-14

### Fixed

- [DE] Fixed desktop device pairing: the desktop client now actually connects
  (the `connect()` call was unreachable) and defaults to the same relay URL as
  the Android app instead of the local `wss://localhost:8080` placeholder, so
  "Generate code" produces a code.

## [6.1.0_DE-1.0.2] - 2026-08-14

### Changed

- [DE] Split the Linux build into independent DEB and AppImage jobs so a
  failure in the AppImage step no longer blocks the DEB package (or the
  release).

## [6.1.0_DE-1.0.1] - 2026-08-14

### Changed

- [DE] The Windows build now produces both an Inno Setup installer and a
  jpackage MSI.
- [DE] Removed the Inno Setup wizard images that could make the installer open
  and immediately close on some systems.

## [6.1.0_DE-1.0.0] - 2026-08-14

### Added

- [APK] Added a "Devices" section in the Android Settings to pair the phone with
  VIVI Music DE (relay server URL, generate/join pairing code, unpair).

## [6.0.5_DE-1.0.0] - 2026-08-14

### Added

- [DE] Compose Multiplatform desktop target (`desktop` module) reusing the
  pure-JVM network modules.
- [DE] Native desktop icons (Windows `.ico`, macOS `.icns`, Linux `.png`) using
  the VIVI Music DE logo.
- [DE] Per-OS GitHub Actions builds (MSI/EXE, DEB/AppImage, DMG/PKG) and an
  auto-release workflow.
- [DE] Cross-device sync foundation: shared `sync` module, Node.js WebSocket
  relay (`sync-server/`), Android `DeviceSyncManager`, and desktop pairing UI.

### Changed

- Converted `innertube`, `spotify`, `lastfm`, `kizzy`, `shazamkit`,
  `jiosaavn`, and `lyricsProvider` to pure-JVM Kotlin modules so they can be
  shared between Android and desktop.
- Desktop releases now use a combined `<mobile>_DE-<desktop>` version
  (e.g. `6.0.5_DE-1.0.0`): `version.txt` line 1 = mobile version, line 2 = DE
  version, line 3 = channel. The About screen shows the full version + channel;
  desktop changelog entries are marked `[DE]`.
- The release channel is now read from line 3 of `version.txt`: `stable` (or
  empty) publishes a stable release; any other value (`rc`/`beta`/`alpha`/
  `nightly`) publishes a pre-release.
- Release tags no longer carry a `v` prefix; non-stable releases append the
  channel to the tag (e.g. `6.0.5_DE-1.0.0-nightly`).
- [DE] The desktop UI is now English-first with a 49-language picker (first
  launch + Language menu); non-English strings fall back to English until
  translated.
- [DE] The Windows installer now performs a machine-wide install into
  `C:\Program Files\VIVIMusic` (requires admin rights) instead of a per-user
  install into `%LOCALAPPDATA%`.
- [DE] The Windows installer is now a branded Inno Setup wizard and shows a
  "successfully uninstalled" confirmation message after removal.

### Fixed

- [DE] Made `gradlew` executable in the repository and in the desktop build
  workflows (fixes `./gradlew: Permission denied` on Linux/macOS runners).
- [DE] Replaced the retired `macos-13` runner with `macos-15-intel` for the
  Intel macOS build.

### Security

- Cross-device sync traffic is currently TLS-only; end-to-end encryption is
  planned for a future release.
