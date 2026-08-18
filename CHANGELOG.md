# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Desktop-specific changes are marked with `[DE]`. Desktop releases use a combined
version `<mobile>_DE-<desktop>` (e.g. `6.0.5_DE-1.0.0`), where the desktop part is
the program's own SemVer. `[APK]` marks mobile-only changes.

## [Unreleased]

## [6.4.28_DE-1.33.85] - 2026-08-18

### Fixed

- [DE] Player sync (seek bar + play/pause) now follows the peer even over a
  phone hotspot: a seek/play command that arrived while the desktop was still
  resolving its own stream was dropped (our own resolution decides when audio
  starts) and only recovered on the next 5s re-sync tick. The latest peer
  snapshot is now re-applied the moment our stream finishes resolving, so the
  slower resolution over a hotspot no longer leaves seek/play-pause unsynced.

## [6.4.28_DE-1.33.84] - 2026-08-18

### Added

- [DE] Stream cache now offers a "Forever" option past 60 minutes: the resolved
  stream URL is kept for the whole app session instead of expiring.

## [6.4.28_DE-1.33.83] - 2026-08-18

### Changed

- [DE] The player design variants now actually differ: Classic is the
  two-column layout, New is a single-column hero with a pill play button,
  V2 keeps two columns with the title overlaid on the artwork, and Expressive
  is a single-column hero with the title overlaid on the largest artwork.

## [6.4.28_DE-1.33.82] - 2026-08-18

### Changed

- [DE] UI density scale now also offers values above 100% (110, 120, 125, 130,
  140, 150, 180, 200) in addition to the existing 100/85/75/65/55%.

## [6.4.28_DE-1.33.81] - 2026-08-18

### Fixed

- [DE+APK] The "Sync VIVI volume" toggle is now part of the shared settings
  snapshot, so enabling/disabling it on either device reflects on the other.

## [6.4.27_DE-1.33.80] - 2026-08-18

### Changed

- [DE] The VIVI Wrapped card is now hidden from the Home screen by default.
  It can be re-enabled via Settings → VIVI Wrapped → "Show on Home".

## [6.4.27_DE-1.33.79] - 2026-08-18

### Added

- [DE] Settings → VIVI Wrapped sub-screen: the session listening-stats card
  now lives in its own settings sub-menu, like the mobile app. The Home card
  stays as a quick glance.
- [DE] Appearance now hosts the player personalization: a new "Player
  design" row (Material 3 style) opens the design / background / rotating
  thumbnail / mini-player style screen.

### Fixed

- [DE] Raw keys no longer appear as "code language" UI: 10 keys were missing
  from the desktop string table (`remove_from_queue`, `pause_search_history`,
  `pause_listen_history`, `quick_picks`, `search_history`, `listen_history`,
  `clear_search_history`, `clear_search_history_confirm`, `theme`, `ok`) and
  are now wired to the Android translations (fallback OK).
- [DE] Queue screen: the swipe-left remove hint ("✕ remove from queue")
  duplicated the row's X button; the hint was removed — the X is now the
  single remove control (swipe-left still works).
- [DE] Full player: add-to-playlist now sits under the song title, next to
  the Queue button; the duplicate header Queue shortcut and the old
  bottom-row buttons were removed.
- [DE] Startup volume guard: if the Windows master volume is muted, VIVI
  Music DE unmutes it and sets it to 0% so a paired mobile device can always
  control it (a muted master ignores volume writes).

### Removed

- [DE] Quick settings (Tune) button in the sidebar.

## [6.4.27_DE-1.33.78] - 2026-08-18

### Changed

- [DE] The stream resolution cache TTL is now configurable: a slider in
  Settings → Player & audio (1–60 minutes, default 10) controls how long a
  resolved stream URL is reused before the resolution chain runs again.
  (Resolution only — the audio decode/playback core is untouched.)

## [6.4.27_DE-1.33.77] - 2026-08-17

### Changed

- [DE] Stream resolution cache: resolved audio URLs are cached in memory for
  up to 10 minutes, so replaying or retrying a track doesn't re-run the whole
  resolution chain when we already have a valid stream. (Resolution only —
  the audio decode/playback core is untouched.)

## [6.4.27_DE-1.33.76] - 2026-08-17

### Added

- [DE] Integrations sub-screen (Settings → Integrations):
  - Discord Rich Presence over the local IPC pipe (Windows; toggle + your
    Discord application ID; shows the current track).
  - Last.fm scrobbling: enable toggle, session key field, now-playing
    update and auto-scrobble near the end of each track (credentials via
    the LASTFM_API_KEY / LASTFM_SECRET env vars, like mobile build config).
  Fully translated.

## [6.4.27_DE-1.33.75] - 2026-08-17

### Added

- [DE] Advanced lyrics: line-spacing slider (1.0–2.0, Settings → Lyrics)
  and a thumbnail with play/pause overlay on the Lyrics screen (port of the
  mobile advanced-lyrics controls). Fully translated.

## [6.4.27_DE-1.33.74] - 2026-08-17

### Added

- [DE] Quick settings popup: a Tune button at the bottom of the sidebar
  opens a compact panel with theme (System/Light/Dark), pure black toggle,
  accent swatches and a shortcut to the full Appearance settings (port of
  the mobile quick-settings shortcut). Fully translated.

## [6.4.27_DE-1.33.73] - 2026-08-17

### Added

- [DE] Local search history: recent searches appear as chips on the Search
  screen (saved on submit / suggestion click, max 12) with a clear button.
- [DE] Privacy sub-screen (Settings → Privacy): "Pause listen history"
  (hides the History screen from the sidebar) and "Pause search history"
  (stops saving new searches) toggles + "Clear search history".
  Fully translated.

## [6.4.27_DE-1.33.72] - 2026-08-17

### Added

- [DE] Home: "Quick Picks vs Last Listen" toggle (chip row) so only the
  chosen section shows, like on mobile.
- [DE] Home: "Randomize" button that shuffles the order of the home
  sections (persisted).
- [DE] Home: "VIVI Wrapped · This session" card with tracks played,
  listening time and top song of the current session. Fully translated.

## [6.4.27_DE-1.33.71] - 2026-08-17

### Added

- [DE] Sort chips in the Library (all tabs): A–Z / Z–A, plus "By artist"
  for the songs tab. Fully translated.

## [6.4.27_DE-1.33.70] - 2026-08-17

### Added

- [DE] Dynamic theme (Material You): the "Dynamic" accent swatch now reads
  the OS accent color instead of a fixed seed — Windows DWM accent, macOS
  accent (defaults), GNOME accent (gsettings) — with fallback to the default
  palette. Re-detected each time Dynamic is picked.

## [6.4.27_DE-1.33.69] - 2026-08-17

### Added

- [DE] Song swipe gestures in the Queue screen: swipe a row right to play
  it, swipe left to remove it from the queue (action hints appear behind
  the row while dragging). Fully translated.

## [6.4.27_DE-1.33.68] - 2026-08-17

### Added

- [DE] Mini-player styles: Standard / Apple / Outline / Pure black
  (Settings → Player & audio → Player design → Mini player), replacing the
  old Apple-only toggle. Fully translated.
- [DE] Swipe-to-expand on the mini player: drag it up to open the full
  player (with a drag handle hint at the top).

## [6.4.27_DE-1.33.67] - 2026-08-17

### Added

- [DE] Player design variants: Classic / New / V2 / Expressive (Settings →
  Player & audio → Player design), reworking the full-player layout and the
  Apple Music-style rounded mini-player variant. Fully translated.
- [DE] Player background styles: Gradient / Blur / Glow / Apple Music /
  Live mesh (Settings → Player & audio → Player design), animated behind
  the full player. Fully translated.
- [DE] Rotating artwork option (settings toggle): the album art rotates
  slowly while playing, like the mobile rotating-thumbnail option.

## [6.4.27_DE-1.33.66] - 2026-08-17

### Added

- [DE] Player slider styles: Slim / Squiggly / Wavy (Settings → Player & audio),
  applied to the seek bar and the volume slider via a custom `ViviSlider`.
  Fully translated.

## [6.4.27_DE-1.33.65] - 2026-08-17

### Added

- [DE] Screen transitions between navigations: Off / Fade / Slide (Settings →
  Appearance → Screen transitions), applied with `AnimatedContent` around the
  main screen switch. Fully translated.

## [6.4.27_DE-1.33.64] - 2026-08-17

### Added

- [DE] UI density scale (100 / 85 / 75 / 65 / 55 %) applied to the whole
  interface via a density override (Settings → Appearance → Density & grid),
  plus a custom adaptive grid item size (small / medium / large / extra
  large) used by the album / artist / playlist grids. Fully translated.

## [6.4.27_DE-1.33.63] - 2026-08-17

### Fixed

- [DE] The desktop no longer stays silent (appears in the Windows mixer but
  emits no sound) when a synced track change starts. The desktop held
  (`startPaused`) whenever the peer was still resolving, and the peer held for
  the desktop's own resolution, so both paused and neither ever started. The
  desktop now starts when the peer says it is playing and ignores the peer's
  play/pause echoes while it is still resolving its own stream; the phone keeps
  holding for the desktop and both resume together once the desktop is ready.

## [6.4.27_DE-1.33.62] - 2026-08-17

### Fixed

- [DE+APK] A track change initiated from the phone no longer plays ahead of
  the desktop while the desktop is still resolving its stream. The
  resolving/ready transition was being swallowed by the 1.5s echo-suppression
  window that runs after applying a remote snapshot, so the desktop's
  `isResolving=true` push never reached the phone. Resolving transitions now
  bypass echo suppression on both sides, so the phone holds while the desktop
  buffers and resumes the moment the desktop is ready (and vice versa).

## [6.4.26_DE-1.33.61] - 2026-08-17

### Fixed

- [APK] Restore swap is no longer silent on failure: the database target
  directory is created if missing (clean install), and if the staged
  settings/database copy fails the staged backup is kept and the error is
  logged (with a stack trace) so the restore can be retried on the next launch
  instead of the backup being deleted without being applied.

## [6.4.25_DE-1.33.61] - 2026-08-17

### Fixed

- [APK] The restore picker now accepts any file (`*/*`) so old `.backup` files
  created by the original 6.0.5 app (which have no registered MIME type) always
  appear in the file selector instead of being hidden/unselectable.

## [6.4.24_DE-1.33.61] - 2026-08-17

### Fixed

- [DE+APK] Playback start is now synchronized while a device is still resolving
  its stream. The desktop marks the snapshot as `isResolving` from the moment it
  starts resolving until audio actually flows (first position report), and the
  mobile marks it while ExoPlayer is `STATE_BUFFERING`. The receiver now
  prepares the queue but holds playback (instead of playing ahead of the peer),
  and `effectivePosition` no longer extrapolates a frozen position while the
  peer is resolving. This fixes the phone starting the track before the desktop
  had finished resolving/downloading.

## [6.4.23_DE-1.33.60] - 2026-08-17

### Fixed

- [APK] Update check from the fork source (`PiBOH/vivi-music`) now works:
  the updater extracts the mobile version from the combined desktop tag
  (`6.4.22_DE-1.33.60-nightly` → `6.4.22`) before comparing, and accepts the
  fork's `VIVIMusic-<version>-debug.apk` asset instead of only `vivi.apk`.

## [6.4.22_DE-1.33.60] - 2026-08-17

### Fixed

- [DE] Native Windows toast notifications now actually appear in the Action
  Center. The AUMID registration was failing with `0x80070057` because the
  `SHGetPropertyStoreFromParsingName` P/Invoke was missing the
  `GETPROPERTYSTOREFLAGS flags` parameter, and the `PROPVARIANT` was declared
  as a sequential struct instead of an explicit-layout class. Both are fixed,
  so the Start-menu shortcut gets its `System.AppUserModel.ID` correctly.

## [6.4.22_DE-1.33.59] - 2026-08-17

### Fixed

- [DE] Developer options network stats (down/up speed + total traffic) now show
  real values on non-English Windows. They were parsing the localized
  `netstat -e` output ("Byte" / "Ricevuti"/"Trasmessi" instead of "Bytes"),
  which never matched and left the values at "—". Replaced with
  `Get-NetAdapterStatistics` (culture-invariant property names).

## [6.4.22_DE-1.33.58] - 2026-08-17

### Fixed

- [DE+APK] Playback sync no longer "jumps back": explicit user seeks are now
  flagged and applied exactly on the peer (both directions, no tolerance),
  while the periodic drift-tic only catch up FORWARD. This stops the device
  that is slightly ahead (the leader) from being dragged back every 5s by the
  follower's stale position, which was the visible seekbar jump-back.

## [6.4.21_DE-1.33.57] - 2026-08-17

### Fixed

- [DE] The in-app update notification no longer auto-dismisses while it is
  showing the download progress bar; the timer pauses during a download and
  resumes after it finishes.

## [6.4.21_DE-1.33.56] - 2026-08-17

### Added

- [DE] A "Send test notification" button in Settings → Notifications so native
  notifications can be triggered on demand.

### Changed

- [DE] Native notification path now writes a diagnostic log to
  `~/.vivimusic/native-notify.log` (which branch is used, AUMID registration
  result, and PowerShell output) to help diagnose Windows toast issues.

## [6.4.21_DE-1.33.55] - 2026-08-17

### Changed

- [DE] The About "website" entry now points to the VIVI Music DE GitHub Pages
  site (`https://piboh.github.io/vivi-music/`).

### Website

- Made the site fully responsive for mobile (collapsible hamburger nav,
  stacking download rows / platform cards).
- Compact sticky footer (always pinned to the bottom of the viewport).
- Removed the Android APK download from the DE site; it now links to the
  upstream VIVI Music site, with credits to VIVIDH P ASHOKAN
  (`https://vivimusic.mkmdevilmi.workers.dev/`).

## [6.4.21_DE-1.33.54] - 2026-08-17

### Fixed

- [DE] Tracks restored from the persistent queue (or whose load failed earlier)
  now actually start on the first Play press: pressing play on a track whose
  stream is not loaded yet triggers a real resolution + load instead of a
  no-op `resume()` that silently did nothing.

## [6.4.21_DE-1.33.53] - 2026-08-17

### Added

- [DE] Windows native notifications now land in the **Action Center / notification
  history** via WinRT toasts (PowerShell helper). On a packaged Windows build the
  app registers an AppUserModelID by creating a Start-menu shortcut with the
  `System.AppUserModel.ID` property (inline C# `Add-Type` + shell property
  store), then shows `ToastGeneric` toasts with the VIVI Music DE logo.
  Clicking a toast launches the app with `--open=<section>` and opens the
  relevant screen (Updates / Developer options / Devices), bringing the window
  to the front; a file-based command mailbox forwards the request to an
  already-running instance. Non-Windows and unpackaged/dev builds keep the
  `SystemTray` balloon fallback.

## [6.4.21_DE-1.33.52] - 2026-08-17

### Changed

- [DE] `version.txt` is reorganized into a self-documenting six-line layout:
  mobile version / mobile version code / mobile channel, then DE version / DE
  version code / DE channel, with comment lines below explaining each field.
  `desktop/build.gradle.kts`, `AppInfo`, and the release/build workflows now
  read the new positions (DE version = line 4, DE channel = line 6, DE version
  code = line 5).

## [6.4.21_DE-1.33.51] - 2026-08-17

### Fixed

- [DE] Opening Appearance → Theme & colors, App font, or Canvas no longer
  crashes with "Vertically scrollable component was measured with an infinity
  maximum height constraints". Those sub-screens had their own
  `verticalScroll` nested inside the settings screen's scrollable scaffold; the
  inner scroll was removed so the content scrolls with the outer scaffold only.

## [6.4.21_DE-1.33.50] - 2026-08-17

### Added

- [APK] Restoring a backup now shows a confirmation dialog with the backup's
  file name, date, and the app version it was created from, before anything is
  applied.

## [6.4.20_DE-1.33.50] - 2026-08-17

### Added

- [APK] Restore now validates the backup's database before applying it (SQLite
  header magic + `PRAGMA integrity_check` + schema-version guard). A corrupt or
  incompatible backup fails with a clear "backup is corrupt" message instead of
  swapping in a bad file and crashing the app on the next launch.

## [6.4.19_DE-1.33.50] - 2026-08-17

### Fixed

- [DE+APK] Playback position no longer jumps back and forth between the two
  devices. The shared-clock offset used to extrapolate the live position was
  only measured 25 s after connecting and converged slowly via a running
  average, so during that window both devices extrapolated from raw local
  clocks and kept seeking each other back/forth by the clock skew. The first
  PING is now sent immediately on connect, the first measurement sets the
  offset directly, and a position is only timestamped once the offset is known
  (older relays fall back to the raw position instead of a skew-corrupted
  extrapolation).

## [6.4.18_DE-1.33.49] - 2026-08-17

### Added

- [DE] Complete port of the mobile Appearance sub-menu into three dedicated
  sub-screens: **Theme** (4-mode selector System/Light/Dark/Pure black, the
  full 21-color accent palette and a live preview card), **App font** (the five
  mobile fonts — System, Google Sans, Sans Flex, Outfit, Plus Jakarta Sans —
  bundled into the desktop resources with a live typography preview) and
  **Canvas** (enable toggle + source Auto / Apple Music / ViViMusic / Tidal,
  wired into the player's animated background). All new strings reuse the
  Android translations (47 languages).

## [6.4.18_DE-1.33.48] - 2026-08-17

### Fixed

- [DE+APK] Playlist changes made on the desktop now actually reach the phone.
  The mobile side was stamping the local "now" into `lastUpdateTime` when
  applying a remote playlist, so the next desktop rename/delete compared newer
  than an artificially-updated timestamp and was silently dropped by the
  last-write-wins check. The remote edit timestamp is now preserved, so create /
  rename / delete all propagate.

## [6.4.17_DE-1.33.48] - 2026-08-17

### Fixed

- [APK] Restoring an old backup (e.g. from the original 6.0.5) no longer crashes.
  The restore previously closed the shared Room database while the app's live
  queries were still running, which crashed with an uncaught "database is
  closed" exception. It now stages the backup to `filesDir/pending_restore`,
  exits, and swaps the settings + database in at startup (in `App.onCreate()`)
  before Room/DataStore are opened.

## [6.4.16_DE-1.33.48] - 2026-08-17

### Changed

- [APK] Mobile backups now use the `.vividroid.backup` extension (desktop keeps
  `.vivide.backup`), so the two editions' backup files are clearly
  distinguishable. Older `.backup` files are still listed and importable.

## [6.4.15_DE-1.33.48] - 2026-08-17

### Fixed

- [DE] The Inno Setup installer now actually launches the app when "Start VIVI
  Music DE" is checked on the final page. The `[Run]` entry was gated on both
  the final-page checkbox *and* a separate (unchecked) `launchafterinstall`
  task, so the app never started; the redundant task is removed and the
  final-page checkbox alone controls the launch.

## [6.4.15_DE-1.33.47] - 2026-08-17

### Changed

- [APK] Mobile backup files now use the `.vivide.backup` extension (manual and
  automatic) to match the desktop edition. Older `.backup` files are still listed
  and importable, so nothing is lost.

## [6.4.14_DE-1.33.47] - 2026-08-17

### Fixed

- [APK] The debug APK build (CI) no longer fails during resource merge: the new
  `sync_vivi_volume_desc` string used a bare apostrophe that aapt2 rejected as an
  "Invalid unicode escape sequence"; it is now escaped (`\'`) like the rest of the
  Android strings, so `assembleUniversalGmsDebug` completes again.

## [6.4.13_DE-1.33.47] - 2026-08-17

### Added

- [DE+APK] New "Sync VIVI volume" toggle (Settings → Devices and Settings →
  Player & audio, on both editions). When off, each device keeps its own
  in-app volume slider independent; the native OS (system) volume sync is
  unaffected.

### Fixed

- [DE] The seek slider no longer stays disabled or stuck at the end: the track
  duration is reported as soon as it is known (before the stream resolves) and
  the live position is clamped to the track length so it can't overshoot.
- [DE] Position sync no longer fights itself: the periodic re-sync tick only
  pushes when the position actually advanced, so a stalled/frozen player can't
  repeatedly drag the paired device back to the same point.

## [6.4.12_DE-1.33.46] - 2026-08-16

### Fixed

- [APK] Restoring a backup no longer crashes while choosing the file or when
  reopening the app: the archive is decompressed on a background thread and
  staged to temp files, then the settings + database are swapped in and the
  process is killed in a single synchronous block on the main thread. This
  removes the race where the UI queried the database after it was closed on a
  background thread (the crash introduced by the previous fix), while still
  deleting the WAL/SHM sidecars so the restored DB isn't corrupted on launch.

## [6.4.11_DE-1.33.46] - 2026-08-16

### Fixed

- [DE] All in-app notifications now auto-dismiss after the configured time
  (Settings → Notifications → In-app notification duration): the update
  banner and the developer-options-unlocked hint previously stayed on screen
  until dismissed manually, ignoring the setting.
- [DE] Native notifications keep a single persistent tray icon (created once
  with the VIVI Music DE logo) instead of adding/removing a temporary icon per
  notification, so the logo shows reliably and the icon is scaled with
  high-quality interpolation.

## [6.4.11_DE-1.33.45] - 2026-08-16

### Fixed

- [DE] LAN sync now works when the computer is connected to the phone's
  hotspot: the desktop advertises the address of the interface that actually
  routes to the phone (resolved via the outbound-route source address, then
  preferring Wi-Fi/wlan adapters) instead of the first site-local address,
  which on multi-homed machines was often a virtual adapter the phone could
  not reach. Start/stop of the relay is now serialized and the bound-port
  lookup is guarded, so rapid Stop→Start (or a failed bind) no longer throws
  and crashes the app — failures surface in the status line instead.

## [6.4.11_DE-1.33.44] - 2026-08-16

### Changed

- [DE] Manual backups now include the date and timestamp in their filename
  (`vivimusic-de_yyyyMMdd_HHmmss.vivide.backup`) instead of a fixed
  `vivimusic-de.vivide.backup`. Automatic backups already carried the
  timestamp, and the stored-backups list shows it as `yyyy-MM-dd HH:mm`.

## [6.4.11_DE-1.33.43] - 2026-08-16

### Fixed

- [APK] Restoring a backup no longer freezes and then corrupts the app: the
  restore (settings + DB copy) now runs off the main thread instead of blocking
  the UI, and the WAL/SHM sidecar files are deleted before overwriting the
  Room database, so a restored `song.db` is no longer mixed with stale journal
  frames (which corrupted the DB on the next launch and required an
  uninstall/reinstall). Playback is stopped before the database is touched.

## [6.4.10_DE-1.33.43] - 2026-08-16

### Added

- [DE+APK] Selectable update source: pick whether update checks read from the
  original repo (`vivizzz007/vivi-music`) or the PiBOH fork
  (`PiBOH/vivi-music`). Desktop defaults to the fork, mobile defaults to the
  original. The source is also used for the download/notification URLs.

## [6.4.9_DE-1.33.42] - 2026-08-16

### Added

- [DE] The crash/error dialog now has a "Copy error" button alongside "OK":
  it copies the full message + stack trace to the clipboard. A global
  uncaught-exception handler replaces the default AWT "Error" dialog (OK only).

## [6.4.9_DE-1.33.41] - 2026-08-16

### Changed

- [DE] Backups (manual and automatic) now use a single `.vivide.backup` file
  that contains everything (settings + playlists + account + library). Old
  `.backup` files are still importable.

### Fixed

- [DE] "Restart now" after restoring a backup now actually relaunches the app:
  it releases the single-instance lock, starts a new instance (the jpackage
  launcher when packaged, `java -cp … MainKt` in dev), and then exits.

## [6.4.9_DE-1.33.40] - 2026-08-16

### Fixed

- [DE] Native system notifications now use the real VIVI Music DE logo as their
  icon instead of a placeholder glyph. `logo_vmde.png` is bundled under
  `images/` and loaded for the tray icon (scaled, with a fallback).

## [6.4.9_DE-1.33.39] - 2026-08-16

### Added

- [DE+APK] Repeat mode (off / all / one) and shuffle now sync in real time
  between the phone and the desktop, both ways, like the rest of the playback
  state. `PlaybackSnapshot` carries `repeatMode` ("OFF"/"ALL"/"ONE") and
  `isShuffle`; each side applies them on receive and pushes them on change.

## [6.4.8_DE-1.33.38] - 2026-08-16

### Fixed

- [DE+APK] Queue sync now follows last-write-wins like playlists: `PlaybackSnapshot`
  carries a `queueUpdatedAt` timestamp (shared relay-time frame) and each side
  only replaces its queue when the remote edit is newer, so a mobile edit can't
  be overwritten by an older desktop queue (and vice versa). Older peers
  (`queueUpdatedAt = 0`) still apply unconditionally for compatibility.

## [6.4.7_DE-1.33.37] - 2026-08-16

### Fixed

- [DE] The update notification and the Updates screen now share a single
  download state (`UpdateState`), so downloading/opening an installer from one
  is reflected in the other (and vice versa). The notification no longer
  re-offers a download for an installer the Updates screen already fetched.

## [6.4.7_DE-1.33.36] - 2026-08-16

### Added

- [DE] Full backup & restore, ported from mobile: a backup now includes
  settings, playlists, account/login and library (ZIP with `settings.json` +
  `playlists.json`), and old single-JSON `.backup` files are still importable.
- [DE] Automatic backups: optional weekly backup and an optional "backup before
  update" that runs automatically before opening the installer. Automatic
  backups are stored under `~/.vivimusic/backups/` and can be restored or
  deleted from Settings → Backup.

### Changed

- [DE] Developer options screen reorganized into clear sections (display,
  monitoring profile, overlay behaviour, title bar) separated by dividers.

## [6.4.7_DE-1.33.35] - 2026-08-16

### Fixed

- [DE] Settings (e.g. where notifications are shown) no longer get forgotten on
  restart or update: `DesktopSettings` now saves through an atomic
  read-modify-write (`update`) instead of `save(load().copy(…))`, which could
  race between the UI thread and the device-sync IO coroutines and silently
  drop the value the user had just changed.

## [6.4.7_DE-1.33.34] - 2026-08-16

### Fixed

- [DE] The player seek slider no longer snaps to the start or the end: it is
  disabled until the track duration is known (so it can't degenerate into a
  0..1 range) and, while dragging, the live playback position is ignored so it
  can't fight the drag and yank the thumb back.

## [6.4.7_DE-1.33.33] - 2026-08-16

### Fixed

- [DE+APK] The two devices no longer unpair when one screen turns off: while
  paired, the Android app keeps the screen on (`FLAG_KEEP_SCREEN_ON`) and the
  desktop keeps the display/system awake (kernel32 `SetThreadExecutionState` on
  Windows, `caffeinate` on macOS). This stops the OS from sleeping the display
  and suspending the network, which was tearing down the sync socket.

## [6.4.6_DE-1.33.32] - 2026-08-16

### Changed

- [DE] The CI debug build now restores a persistent debug keystore from the
  `DEBUG_KEYSTORE` GitHub secret instead of generating a fresh key on every
  run, so the debug APK keeps the same signature and can be installed over the
  previous build without uninstalling first. When the secret is absent the
  workflow still falls back to generating a fresh key, so CI never breaks.

## [6.4.6_DE-1.33.31] - 2026-08-16

### Fixed

- [APK] Scanning a QR code now first disconnects and un-pairs an existing
  desktop connection, so the new code can pair to a (possibly different)
  desktop from a clean slate.

## [6.4.5_DE-1.33.31] - 2026-08-16

### Changed

- [DE] On Windows the updater now prefers the Inno Setup `.exe` installer over
  the `.msi` (lighter and more user-friendly). The `.msi` remains the fallback
  when a release has no `.exe`.

## [6.4.5_DE-1.33.30] - 2026-08-16

### Fixed

- [DE] The Updates screen no longer offers to re-download an installer that is
  already on disk: it now detects the previously-downloaded file for the
  available version and shows "Open installer" directly (the update banner
  already behaved this way).

## [6.4.5_DE-1.33.29] - 2026-08-16

### Added

- [DE] Notification history: every notification (in-app and native) is recorded
  and can be reviewed from Settings → Notifications → Notification history, with
  a "Save notification history" toggle and a "Clear history" action.
- [DE] Configurable in-app notification auto-dismiss (3/5/10/15/30 seconds,
  default 5s) in Settings → Notifications.

## [6.4.5_DE-1.33.28] - 2026-08-16

### Changed

- [DE] Windows system volume now drives the **master** volume (the speaker icon
  in the tray) via WASAPI `IAudioEndpointVolume` instead of WinMM, which only
  moved the per-app `VIVIMusic` mixer entry. The app's own session is now
  pinned to 100% so the mixer never quiets VIVI under the master. Sync with
  the phone remains bidirectional (channel `systemVolume`).

## [6.4.5_DE-1.33.27] - 2026-08-16

### Fixed

- [DE] The Linux `.deb` now installs on Debian: jpackage auto-detected
  dependencies on ubuntu-latest and emitted Ubuntu's `t64`-renamed package
  names (e.g. `libasound2t64`, `libglib2.0-0t64`) that don't exist on Debian
  Bookworm. A post-build step rewrites them to `<name> | <name>t64`
  alternatives so apt picks whichever name the distro actually provides.

## [6.4.5_DE-1.33.26] - 2026-08-16

### Changed

- [DE] Clearer updater wording: the update button now reads "Check for
  available updates" (instead of the Android toggle's "Automatically check for
  updates") and "Open installer" now reads "Close Vivi and open installer",
  matching what the button actually does. Updated across all languages.

## [6.4.5_DE-1.33.25] - 2026-08-16

### Fixed

- [DE] Critical startup crash on Windows: the WinMM binding looked up
  `waveOutOpenW`, but `winmm.dll` exports the function with **no A/W suffix**
  (it takes no string argument, so `waveOutOpenW`/`waveOutOpenA` exist only as
  C header macros). JNA threw "Error looking up function 'waveOutOpenW'" and the
  app crashed. Restored the correct `waveOutOpen` symbol and guarded every
  native call so a missing symbol can never crash the app again.

## [6.4.5_DE-1.33.24] - 2026-08-16

### Changed

- [DE] Translation quality pass: filled in the remaining desktop-only keys
  (device sync, updates, player basics) that were still falling back to
  English, and corrected translations whose Android mapping had a different
  (longer or wrong-context) meaning — e.g. "Check for updates" is now a short
  button label instead of "check automatically…", "Error" no longer reads
  "unknown error", and CPU/GPU keep their short technical form.

## [6.4.5_DE-1.33.23] - 2026-08-16

### Fixed

- [DE] Windows Inno Setup installer now always shows the "Select Destination
  Location" page so the install path is visible (and editable), matching the
  MSI installer.

## [6.4.5_DE-1.33.22] - 2026-08-16

### Fixed

- [DE] Native (OS) volume now actually syncs on Windows: the WinMM call used
  `waveOutOpen`, which is a macro — `winmm.dll` only exports `waveOutOpenW`/
  `waveOutOpenA` — so `Native.load` failed and every native volume read/write
  silently no-opped. The default wave device is now opened via `waveOutOpenW`
  (with `StdCallLibrary`).
- [DE+APK] Volume pushes (in-app `volume` and native `systemVolume`) are now
  retried instead of being silently dropped: both sides poll volume and only
  mark it as pushed once the snapshot is actually sent, so a push that lands in
  the echo-suppression window is re-sent on the next tick. The mobile side is
  echo-guarded per-field so an applied remote value isn't bounced back.

## [6.4.4_DE-1.33.21] - 2026-08-16

### Added

- [DE] Single-instance guard: launching the app while another instance is
  already running (or still starting) exits immediately, always keeping the
  first instance that started.
- [DE] Notifications now cover **all** app notifications, not just updates:
  update available, device paired/unpaired and developer-options unlocked all
  route through the chosen notification mode (main window vs native). Native
  system notifications are marked "experimental".

### Changed

- [DE] Completed all remaining desktop-only translations (developer options and
  backup/restore strings) across all 47 supported languages — every key now has
  a real translation instead of an English fallback.
- [DE] `Localization.kt` is now emitted as one top-level function per language
  (instead of a single giant `mapOf`) to stay under the JVM 64KB `<clinit>`
  limit that caused a "Method too large" compiler error.

## [6.4.4_DE-1.33.20] - 2026-08-16

### Fixed

- [DE] Seek bar: decoded position is now reported at ~10 fps (throttled from
  ~43 fps) so the player seek slider no longer fights the constant frame-by-frame
  updates — it stays smooth and can be dragged to any position instead of
  sticking at the start/end.
- [DE] In-app (VIVI) volume sync now also pushes when nothing is playing, and
  uses a per-field echo guard (mirroring the OS-volume loop) so a local change
  is no longer silently dropped by the generic echo-suppression window.
- [DE] Windows OS volume sync: `waveOutGetVolume`/`waveOutSetVolume` were
  called with the `WAVE_MAPPER` constant as if it were an open handle, which
  made every call fail (so Windows native volume never synced). The default
  wave device is now opened first via `waveOutOpen` before reading/writing
  volume.

## [6.4.4_DE-1.33.19] - 2026-08-16

### Added

- [DE] New "Notifications" settings sub-menu: update notifications can now be
  shown either in the main window (in-app, default) or as a native system
  notification (`java.awt.SystemTray`, best-effort across OS).

## [6.4.4_DE-1.33.18] - 2026-08-16

### Added

- [DE] "Add to playlist" is now also available on the full Player screen
  (secondary actions) and on every row of the Queue screen.

## [6.4.4_DE-1.33.17] - 2026-08-16

### Added

- [DE] A dedicated "Add to playlist" button on every song row (Home, Search,
  Album, Artist, Playlist, Library), alongside the ⋮ context menu — no more
  hiding the action behind the menu.

## [6.4.4_DE-1.33.16] - 2026-08-16

### Fixed

- [DE] Seek slider couldn't be dragged to the middle: the track duration is now
  taken from the player response (`videoDetails.lengthSeconds`) and reported
  immediately, so the slider has a correct range (previously the AAC-derived
  fallback could be 0/wrong and the slider only landed on start or end).

### Changed

- [DE+APK] Device-sync volume now uses two separate channels: the in-app player
  volume (mobile slider <-> desktop slider, pixel-synced) and the native OS
  system volume (Android STREAM_MUSIC <-> desktop OS volume). The desktop reads
  and writes its OS volume via WinMM (Windows), `pactl`/`amixer` (Linux) and
  `osascript` (macOS), all best-effort and guarded.

## [6.4.3_DE-1.33.15] - 2026-08-16

### Fixed

- [DE] Crash ("layouts are not part of the same hierarchy") when interacting
  with any dropdown or dialog (update-check frequency, playlist delete, …).
  Root cause was the global `SelectionContainer`: popup components
  (`DropdownMenu`, `AlertDialog`) inherit its selection registrar and crash on
  pointer events (Compose CMP-2326). The global wrapper is removed; targeted
  selectable text is kept for the player error detail and the pairing code.

## [6.4.3_DE-1.33.14] - 2026-08-16

### Fixed

- [DE] Crash ("layouts are not part of the same hierarchy") when confirming a
  playlist deletion. The confirmation dialog now dismisses first and the row
  removal is deferred to the next frame, so the playlist list no longer
  reflows while the dialog window is being torn down.

## [6.4.3_DE-1.33.13] - 2026-08-16

### Fixed

- [DE] Apostrophes now render correctly everywhere instead of showing a
  literal `\'`. The desktop localization generator now decodes Android's
  `\'` resource escape (plus `\n`/`\t`/`\"`/`\\`) into real characters before
  re-encoding them as Kotlin string literals.

## [6.4.3_DE-1.33.12] - 2026-08-16

### Changed

- [DE] GitHub release titles now use the `Vivi Music <mobile>_DE <desktop>`
  format (e.g. `Vivi Music 6.4.3_DE 1.33.12`) instead of `Vivi Music DE
  <mobile>_DE-<desktop>`. Tags remain unchanged.

## [6.4.3_DE-1.33.11] - 2026-08-16

### Fixed

- [DE] Crash when changing the update-check interval: selecting an option in
  the frequency dropdown no longer throws "layouts are not part of the same
  hierarchy". The popup is now dismissed before the interval state (which
  reflows that row) is applied.

## [6.4.3_DE-1.33.10] - 2026-08-16

### Fixed

- [DE+APK] Device-sync regression: a transient network drop (or a socket
  reconnecting) no longer tears down a healthy pairing. Both relays now wait a
  15-second grace period for the device to reconnect before unpairing, and only
  the device's live socket triggers the unpair. Closing an app still un-pairs
  the other side (after the grace period).

### Note

- The cloud relay needs a redeploy of `sync-server/server.js` for this to apply
  over `wss://`; the LAN relay is fixed immediately.

## [6.4.3_DE-1.33.9] - 2026-08-16

### Added

- [DE] Backup & restore sub-menu in Settings: export the desktop settings to a
  `.backup` file (native save dialog) and import them back (native open
  dialog). Importing preserves the device id and first-launch date, drops any
  stale pairing, and prompts a restart to apply.

## [6.4.3_DE-1.33.8] - 2026-08-16

### Changed

- [DE] The GitHub logo in About → Community is now a vector icon (ported from
  the mobile app) instead of a static PNG, so it tints with the accent color
  and adapts to dark/light mode.

## [6.4.3_DE-1.33.7] - 2026-08-16

### Changed

- [DE+APK] Closing either app (mobile or desktop) now un-pairs both devices.
  The relays (cloud `sync-server` and the desktop LAN relay) detect the socket
  close, clear the pair, and tell the still-open peer it is no longer paired,
  so it stops showing "paired" for a peer that is gone.

### Note

- The cloud relay needs a redeploy of `sync-server/server.js` for this to take
  effect over `wss://`; the LAN relay works immediately.

## [6.4.3_DE-1.33.6] - 2026-08-16

### Fixed

- [DE+APK] Volume now syncs as the **system** volume: raising/lowering the
  Android volume (rocker or player bar) drives the desktop volume and vice
  versa, and the change is pushed immediately (no longer only on the periodic
  re-sync tick). This also fixes the volume bar position not following on the
  other device while the audible level did.

## [6.4.2_DE-1.33.6] - 2026-08-16

### Added

- [DE+APK] The desktop QR code now embeds the relay address **and** the current
  6-digit pairing code (`vivimusic://pair?addr=…&code=…`). Scanning it on the
  phone auto-fills both the server URL and the code, so you only verify the code
  and tap Pair. Plain `ws://` URLs still work for manual entry.

## [6.4.1_DE-1.33.5] - 2026-08-16

### Added

- [DE] The player now shows its load state while a track starts: "Resolving
  audio…" then "Downloading…" with a spinner, in both the full player and the
  mini-player, so you can tell it's working instead of appearing frozen.

## [6.4.1_DE-1.33.4] - 2026-08-16

### Fixed

- [DE] Playback now retries automatically: when the stream fails to resolve or
  download (e.g. a stale googlevideo 403), the player rotates the guest
  identity, re-resolves a fresh stream URL and retries up to 3 attempts before
  surfacing the error.

## [6.4.1_DE-1.33.3] - 2026-08-16

### Changed

- [DE] Completed the translations for the playlist and song-menu strings
  (rename / delete playlist / confirmation / empty / not-found / song count /
  like / library / share, …) across all 47 supported languages. The
  delete-playlist confirmation now shows the desktop message instead of the
  Android "Really delete … %s" template.

## [6.4.1_DE-1.33.2] - 2026-08-16

### Added

- [DE] Full song context menu (⋮): like / unlike, add to / remove from library,
  add to playlist and share (copies the YouTube Music link to the clipboard).
  Like and library actions use the signed-in YouTube account; a non-invasive
  snackbar confirms clipboard copies.

## [6.4.1_DE-1.33.1] - 2026-08-16

### Added

- [DE] Drag-to-reorder inside the playlist detail screen (drag the ⠿ handle);
  the new order is saved and synced like any other playlist edit.

## [6.4.1_DE-1.33.0] - 2026-08-16

### Added

- [DE] Local playlist system: create / rename / delete playlists, add songs to
  a playlist from any song row (Home, Search, Library, Album, Artist, Playlist
  and History), and a per-playlist detail screen with play-all and per-song
  remove.
- [DE+APK] Playlists now sync between the desktop and the phone over the
  device-sync channel: the full playlist (name + ordered songs) plus a
  per-playlist edit timestamp is shared, and edits are merged with
  last-write-wins. The most recently updated copy of each playlist wins,
  deletions propagate as tombstones, and a change made on either device appears
  on the other.

### Changed

- [DE] The sidebar gains a "Playlists" entry that opens the local playlist list.

## [6.4.0_DE-1.32.2] - 2026-08-16

### Fixed

- [DE] The player's seek slider could only land on the start or the end: the
  track duration was reported as 0 because YouTube's fragmented MP4 has an empty
  `mdhd` (jcodec returns `totalDuration == 0`). The duration is now derived from
  the decoded AAC sample count, so the slider spans the whole track and seeking
  works anywhere.
- [DE] Seeking while paused now stays paused instead of forcing playback to
  resume.
- [DE] Stale or truncated cached audio files are now detected and re-downloaded,
  so a leftover `.m4a` from an interrupted download no longer decodes into
  silence.

## [6.4.0_DE-1.32.1] - 2026-08-16

### Fixed

- [DE] "Start LAN server" now retries the pairing-code request until the relay
  answers, so the 6-digit code is generated automatically and reliably right
  after starting the server.

## [6.4.0_DE-1.32.0] - 2026-08-16

### Added

- [DE] Developer options "Title bar only" display mode (stats only in the
  window title, no overlay or separate window).
- [DE] Queue entry in the sidebar (opens the Queue screen directly).
- [DE] "View changelog" button in Settings → Updates.
- [DE+APK] Volume sync: the volume slider now syncs between the two devices.

### Changed

- [DE] Player layout is now two columns: a smaller artwork on the left and the
  seek bar + controls + volume on the right.
- [DE] Clicking the mini-player toggles the full player (open / go back).
- [DE] The About screen's GitHub row now uses the correct GitHub logo.
- [DE] README: smaller logo and @PiBOH added to Special Thanks.

### Fixed

- [DE] Stream resolution is more resilient: it retries transient failures and
  throttles guest-session refreshes so a track can start even with no paired
  device.

## [6.3.0_DE-1.31.0] - 2026-08-15

### Added

- [DE+APK] Periodic re-sync tick: while a track is playing, the position is
  re-pushed every 5 seconds so the paired device auto-corrects drift
  (buffering / clock skew) instead of waiting for the next seek/play/track
  event. A 250 ms tolerance skips near-no-op seeks so the correction doesn't
  glitch the audio.

## [6.2.5_DE-1.30.3] - 2026-08-15

### Changed

- [DE] The Changelog screen now uses a vertical version selector (left list of
  version buttons, like the mobile chips but top-to-bottom) with the selected
  version's details in a pane on the right, instead of stacking every version
  in one long scroll.

## [6.2.5_DE-1.30.2] - 2026-08-15

### Changed

- [DE] The About screen now shows real thumbnails: the developer's avatar
  (`author.png`) instead of the `< >` icon, and the GitHub logo on the
  GitHub Repository row (bundled from `[DE]_images/`).

## [6.2.5_DE-1.30.1] - 2026-08-15

### Changed

- [APK] The Devices entry in Settings now uses the same phone+monitor
  "devices" icon as the desktop edition, instead of the circular sync arrows.

## [6.2.4_DE-1.30.1] - 2026-08-15

### Fixed

- [DE+APK] Both sides now show the paired device's name: the Android Devices
  screen displays the desktop's machine name, and the desktop Device sync
  section displays the phone's make/model. The desktop now advertises its real
  hostname instead of the generic "Desktop", and the peer name is restored from
  the peer's snapshot on reconnect (not just at pairing time).

## [6.2.3_DE-1.30.0] - 2026-08-15

### Added

- [DE] Developer options improvements: the "Developer options" entry is now
  always visible in Settings (showing "Disabled" until enabled), and can be
  unlocked either by tapping the About "Version code" seven times or from that
  screen. Once unlocked, a non-invasive banner points to the settings screen.
  New options: a display profile (Full vs Performance — CPU/RAM/GPU only), a
  "movable overlay" toggle (drag the overlay anywhere on the main window,
  on by default), and a "show in title bar" toggle that puts live CPU/RAM
  usage in the window title.

### Fixed

- [DE] Starting the LAN server now reliably auto-generates the pairing code:
  the code request waits for the local relay connection to be established
  instead of racing the relay startup (which could leave the code ungenerated).

## [6.2.3_DE-1.29.0] - 2026-08-15

### Added

- [DE+APK] Precise, instant player sync between the desktop and the phone.
  Seeking now pushes the new position immediately (both directions) and the
  receiver applies it as a lightweight in-place seek instead of restarting the
  stream, so the two players stay aligned to the second. Playback positions
  now carry a timestamp and both devices estimate their clock offset to the
  relay (PING/PONG), so the live position is extrapolated during playback
  without clock-skew drift.

## [6.2.2_DE-1.28.6] - 2026-08-15

### Fixed

- [DE] Installing both the MSI and the Inno Setup EXE no longer leaves two
  "VIVI Music" entries in "Apps & features": the Inno Setup installer now
  uninstalls any previously-installed jpackage MSI of the app before copying
  its files, so a single uninstall entry remains.

## [6.2.2_DE-1.28.5] - 2026-08-15

### Fixed

- [DE] **Critical:** the packaged app still showed "Failed to launch JVM" at
  startup even after bundling the management modules, because `SystemMonitor`
  read a field before its initializer ran (a Kotlin forward-reference) and
  threw a NullPointerException during `DeveloperOptions.load()`. Fixed the
  declaration order and made dev-tools initialization non-fatal.

## [6.2.2_DE-1.28.4] - 2026-08-15

### Fixed

- [APK] The Android APK now reports version `6.2.2` (it was still showing
  `6.2.1`): `app/build.gradle.kts` `versionName`/`versionCode` were out of
  sync with `version.txt`.

## [6.2.2_DE-1.28.3] - 2026-08-15

### Fixed

- [DE] **Critical:** the packaged app no longer fails to start with
  "Failed to launch JVM". The dev tools (CPU/RAM/thread stats) use
  `java.lang.management` and `com.sun.management`, but jlink was bundling only
  the default modules; those two modules are now declared so the packaged
  runtime includes them.

## [6.2.2_DE-1.28.2] - 2026-08-15

### Fixed

- [DE] The Changelog screen now lists every version vertically (newest first)
  in a single scrollable list, so older versions are reachable with the mouse
  wheel — the previous horizontally-scrolling version chips were unusable on
  desktop.

## [6.2.2_DE-1.28.1] - 2026-08-15

### Fixed

- [DE] Audio playback no longer fails with "HTTP 403 downloading audio": the
  desktop now keeps a fresh guest `visitorData` (like the Android app) and
  rotates it once when YouTube flags the request as a bot, so the googlevideo
  CDN stops rejecting the resolved stream URLs.

## [6.2.2_DE-1.28.0] - 2026-08-15

### Added

- [DE] The Updates screen now checks for updates automatically every time it
  is opened, and in the background at a configurable interval (manual only,
  6 hours, 12 hours, 24 hours, 3 days or 7 days), selectable in
  Settings → Updates.

### Fixed

- [DE] "Download" in the Updates screen no longer opens the browser instead of
  downloading in-app: the updater now picks the newest release that actually
  ships an installer for your OS (skipping releases whose build for your
  platform is missing) and only falls back to the release page when no
  installer exists. Nightly/alpha/beta/rc builds now include pre-releases by
  default so they can see updates without toggling the option.

## [6.2.2_DE-1.27.1] - 2026-08-15

### Fixed

- [DE] Content no longer gets clipped when the window is resized smaller: the
  Album/Artist/Playlist headers now shrink their title/artist text (ellipsis)
  instead of overflowing, and the LAN pairing screen ellipsizes and constrains
  the relay address next to the QR code.

## [6.2.2_DE-1.27.0] - 2026-08-15

### Added

- [DE] Developer options: tap the About "Version code" seven times to enable
  them. Once enabled, a new "Developer options" settings screen lets you show
  live CPU, RAM and network stats (download/upload speed + total traffic), the
  GPU device, thread count, uptime, OS/Java info and the paired phone
  name/model — either as a non-invasive collapsible overlay in the main window
  or in a dedicated window.

## [6.2.2_DE-1.26.4] - 2026-08-15

### Fixed

- [DE] Audio playback no longer fails with "HTTP 403 downloading audio": the
  desktop resolver stopped using the `WEB`/`WEB_REMIX` clients (their
  googlevideo URLs require a PoToken the desktop cannot generate) and now uses
  only PoToken-free clients (added `VISIONOS` and `IOS_MUSIC`). It also only
  applies the n-parameter throttle transform to web clients, so
  Android/iOS/VisionOS stream URLs are no longer corrupted into a 403.

## [6.2.2_DE-1.26.3] - 2026-08-15

### Fixed

- [DE] The About screen "Version code" was derived from the SemVer
  (`1.26.0` → `12600`), which looked like a huge number. It is now an explicit
  monotonic counter stored in `version.txt` (line 4) that tracks the number of
  DE releases (currently 57).

## [6.2.2_DE-1.26.2] - 2026-08-15

### Changed

- [DE] Starting the LAN server now automatically generates a pairing code, and
  the code + "Generate code" button are shown to the right of the QR code
  instead of below it.

## [6.2.2_DE-1.26.1] - 2026-08-15

### Fixed

- [DE+APK] Device pairing is now kept in sync across both devices: unpairing
  from the phone or the desktop unpairs the other side, and stopping the LAN
  server notifies the phone to unpair too. Reconnecting to a relay that no
  longer knows the pair (for example after the desktop is restarted) now clears
  the stale "paired" state instead of leaving it stuck.

## [6.2.1_DE-1.26.0] - 2026-08-15

### Added

- [DE] Redesigned the About screen to mirror the mobile layout: centered
  title + version/channel badge, a Developer section (PiBOH — lead developer
  of the DE edition — with website link), a Community section (GitHub repo +
  Telegram), and an App info section showing the first-launch date (not the
  last-update install date), the numeric version code and the GPL-3.0 license
  link.

## [6.2.1_DE-1.25.0] - 2026-08-15

### Added

- [DE+APK] Library sync over the device-sync channel: the mobile app now
  observes its library (liked songs, bookmarked albums/artists/playlists) and
  pushes a `LibrarySnapshot` whenever it changes; the desktop receives,
  persists and exposes it (and pushes its own).

## [6.2.1_DE-1.24.2] - 2026-08-15

### Fixed

- [DE] "HTTP 403 downloading audio" is now far more robust: the resolver
  returns an ordered list of candidate stream URLs (NewPipe plus every
  innerTube client), and the player tries them in order — retrying without the
  `Range` header when a request is refused — instead of giving up after the
  first URL. NewPipe URLs now use the decrypted `getUrl()` result with the
  n-param transform applied.

## [6.2.1_DE-1.24.1] - 2026-08-15

### Changed

- [DE] The update notification now offers "Open installer" (instead of
  downloading again) when the installer for that version is already present
  in the downloads folder.

## [6.2.1_DE-1.24.0] - 2026-08-15

### Added

- [DE] A non-invasive banner now appears when a newer release is available,
  with "Install now" (downloads and launches the installer) and a dismiss
  button. It is shown once per new version.

## [6.2.1_DE-1.23.0] - 2026-08-15

### Added

- [DE] The settings sub-screens now carry real functionality instead of being
  empty shells:
  - **Appearance**: pure black background toggle (true black in dark mode).
  - **Player & audio**: audio quality (Auto / High 256kbps / Low 128kbps,
    wired to the itag 141/140 picker), "remember shuffle and repeat" across
    restarts, and "persistent queue" (the queue is saved and restored between
    sessions).
  - **Lyrics**: adjustable lyrics text size.

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
