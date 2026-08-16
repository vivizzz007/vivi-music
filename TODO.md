# TODO — VIVI Music DE (desktop) + Android ↔ Desktop sync

Legend: `[x]` done · `[ ]` to do · `[~]` in progress

## Phase 0 — Desktop foundation (completed)
- [x] Convert the 7 network modules (`innertube`, `spotify`, `lastfm`, `kizzy`, `shazamkit`, `lyricsProvider`, `jiosaavn`) from `com.android.library` to `kotlin("jvm")`.
- [x] `desktop` module (Compose Multiplatform) with a search PoC via `innertube`.
- [x] Native icons: Windows `.ico`, macOS `.icns`, Linux `.png` (from `logo_vmde.png`).
- [x] CI workflows: per-OS builds (Windows/Linux/macOS) + auto-release to GitHub Releases.

## Phase 1 — Sync: pairing + settings (completed)
- [x] Shared `sync` module (data model + OkHttp WebSocket client).
- [x] `sync-server/` relay (Node.js): 6-digit pairing + mailbox for offline devices.
- [x] Android `DeviceSyncManager` (Hilt): push/pull of the shared settings subset.
- [x] Desktop pairing UI + JSON settings store (`~/.vivimusic/`).
- [~] Cloud relay: `wss://vivimusic-device-sync.onrender.com` is set as the default URL on both mobile and desktop. The relay still needs a (re)deploy of the updated `sync-server/server.js` (PING/PONG timestamp echo used for clock-synced playback position) — until then, cloud sync falls back to raw local clocks (LAN is already exact).
- [x] Pairing screen in the Android app (Settings).
- [x] Offline LAN (same Wi-Fi) pairing via a local desktop WebSocket relay (desktop hosts, Android connects by setting the relay URL to `ws://<lan-ip>:<port>`).
- [x] Mobile LAN discovery: NSD/mDNS "Find desktop" + QR scan in the Devices screen.
- [x] QR pairing auto-fill: the desktop QR now encodes the relay address + the current 6-digit pairing code (`vivimusic://pair?addr=…&code=…`); the mobile scanner parses it and pre-fills both fields so the user only verifies the code and taps Pair.
- [x] Unpair on close: both relays detect a socket close, wait a 15s grace period for a reconnect, then clear the pair + notify the still-connected peer — so closing either app un-pairs both, while a transient network blip no longer breaks a healthy pairing (only the device's live socket triggers the unpair). (Cloud mode requires a redeploy of `sync-server`; LAN works out of the box.)

## Phase 2 — Sync: queue + playback position
- [x] Capture the queue and position from the Android player (`pushPlayback` is wired from MusicService on track/play changes).
- [x] Resume on desktop: apply `pendingPlayback`; the desktop also pushes its playback and applies incoming snapshots (bidirectional), with echo suppression.
- [x] Precise sync: seek is pushed instantly (both sides), same-track commands apply as a lightweight in-place seek (no restart), and positions carry a timestamp (`positionAtMs`) so receivers extrapolate while playing; PING/PONG clock-sync (relay + `server.js`) removes clock skew between phone and PC.
- [x] Drift auto-correction: while playing, the position is re-pushed every 5s (`SyncServer.RESYNC_TICK_MS`) so players re-align continuously; a 250 ms tolerance (`RESYNC_TOLERANCE_MS`) skips near-no-op seeks to avoid audio glitches.
- [x] Volume sync (two channels): the in-app player volume (`volume`) mirrors the mobile/desktop slider pixel-for-pixel, and the native OS system volume (`systemVolume`) mirrors Android STREAM_MUSIC <-> desktop OS volume (WinMM on Windows, `pactl`/`amixer` on Linux, `osascript` on macOS — all best-effort). Both sides poll volume (700ms mobile / 500+800ms desktop) with per-field echo guards and **retry on a dropped push** (a push that lands in the echo-suppression window is re-sent on the next tick instead of being lost), so volume syncs reliably even while idle. Windows now drives the **master** volume through WASAPI `IAudioEndpointVolume` (Core Audio COM marshaled onto a dedicated MTA thread) and pins the app's own session (`VIVIMusic` in the mixer) to 100% — WinMM `waveOut*` only moved the per-app session volume, so it was replaced; every native call is guarded against missing symbols.
- [x] Paired device names shown on both sides (desktop announces its real hostname, mobile shows the paired desktop name and vice versa).

## Phase 3 — Sync: library
- [x] Sync liked songs, albums, artists and playlists at the transport level: the mobile app observes its library (liked songs, bookmarked albums/artists/playlists) and pushes a `LibrarySnapshot`; the desktop receives, persists and exposes it (and pushes its own). UI-side apply on desktop waits for the desktop local-library store (Phase 5).

## Phase 4 — Desktop audio playback
- [x] JVM audio backend: pure-Java `jaad` AAC decoder + Java Sound (play/pause/resume, position). The seek slider takes its duration from the player response (`videoDetails.lengthSeconds`) and reports it immediately, so the range is always correct (the decoded AAC sample count is only a fallback when `lengthSeconds` is missing); seeking preserves the pause state, and stale/truncated cached audio files are detected and re-downloaded. Decoded position is throttled to ~10 fps so the seek slider/lyrics stay smooth and draggable instead of recomposing on every frame (~43 fps).
- [x] Port stream resolution: NewPipe + the multi-client innerTube fallback chain, seek, and a multi-URL retry (tries every candidate and retries without `Range` on 403) are done. On a resolution/download failure the player rotates the guest identity, re-resolves a fresh stream URL and retries automatically (up to 3 attempts). PoToken, proxy and HLS are Android-only / not needed by the desktop player (fMP4/AAC via jcodec + jaad).
- [x] Load feedback: the full player and mini-player show "Resolving audio…" / "Downloading…" (with a spinner) while the stream is resolved and downloaded.

## Phase 5 — Desktop persistence + authentication
- [x] Persistence: the desktop uses a JSON file store (`DesktopSettings` under `~/.vivimusic/device-sync.json`); Room is Android-only and stays there.
- [~] YouTube login: cookie-based login with `DATASYNC_ID`/`VISITOR_DATA` fallback is done; browser OAuth + proxy are still pending.
- [x] Full desktop settings layer (same keys as the Android app).

## Phase 6 — Full desktop UI
- [x] Screens: Home, Search, Album, Artist, Playlist, Library, Player, Lyrics, Settings (Library is a placeholder until Phase 5 login; Player/Lyrics are UI-only until Phase 4 playback).
- [x] Apple Music–style mini-player, animated canvas and a full Material 3 Player (seek, volume, shuffle, repeat) are done.

## Phase 7 — End-to-end encryption
- [ ] Per-pair key exchanged during pairing.
- [ ] Snapshots encrypted before sending (the relay no longer reads the data).

## Phase 8 — Pixel-perfect UI port (mobile → desktop)
The desktop should look exactly like the Android app, except:
- the bottom navigation bar becomes a collapsible/expandable sidebar;
- the Devices (device sync) screen keeps its desktop-specific layout.

- [x] Foundation: seed-based Material 3 palette (materialKolor TonalSpot) matching the mobile theme, and a collapsible/expandable sidebar with Material icons + labels (persisted).
- [x] Home screen port: filter chips, mobile-style section headers (label + bold primary title, Play all, chevron), songs-only sections rendered as horizontal song lists (Quick picks style), mixed sections as card carousels, and a Mood & genres section (buttons navigate to a generic Browse screen). QR code always rendered on a white background.
- [x] Search screen port (search bar, live suggestions, filter chips All/Songs/Videos/Albums/Artists/Playlists, result rows/grids).
- [x] Library screen port (Material 3 filter-chip tabs, songs list + adaptive grids, Shuffle all).
- [x] Album / Artist / Playlist detail screens port (header artwork, play/shuffle buttons, track lists, related-item carousels). Note: the header is a simple row rather than the mobile gradient header.
- [x] Player + mini-player + queue + lyrics (large responsive artwork, animated canvas background, seek/volume, shuffle/repeat, drag-to-reorder queue, synced lyrics highlighting with configurable text size).
- [x] Settings sub-screens port (Appearance, Player & audio, Account, Content, Lyrics, Privacy, Storage, Updates, About, Notifications), keeping the custom Devices screen — with real functionality (pure black, audio quality, remember shuffle/repeat, persistent queue, lyrics text size, content language/region, update-notification placement in-app vs native).
- [x] History / Changelog / Login screens (history from YouTube, live changelog fetched from the repo, cookie-based login with DATASYNC_ID/VISITOR_DATA fallback).
- [x] About screen: the GitHub repository icon is now a vector (ported from the mobile drawable) so it tints with the accent color and adapts to dark/light mode.
- [x] Backup & restore sub-menu: export the desktop settings to a `.backup` (JSON) file and import them back (native save/open dialog). Import preserves the device id + first-launch date, drops any stale pairing, and prompts a restart to apply.
- [~] Song / album / artist / playlist context menus port. The full song context menu (like / library / add-to-playlist / share) is done, plus a dedicated "Add to playlist" button on every song row (and on the Player + Queue screens); album / artist / playlist context menus are still pending.
- [x] Local playlist system port: create/rename/delete playlists, add/remove/reorder songs (drag-to-reorder in the detail screen), a "Playlists" screen + per-playlist detail, and cross-device playlist sync with last-write-wins (the library snapshot now carries the full playlist name + ordered songs + edit timestamp, plus deletion tombstones). Delete confirmation is deferred until after the dialog dismisses, fixing a "layouts are not part of the same hierarchy" crash.
- [x] Desktop localization: the playlist and song-menu strings (rename / delete playlist / confirmation / empty / not-found / song count / like / library / share, …) are fully translated across all 47 supported languages (desktop-only keys added via a `TRANSLATIONS` table in the generator).
- [x] Desktop localization: apostrophes render correctly everywhere — the generator now decodes Android's `\'`/`\n`/`\t`/`\"`/`\\` resource escapes into real characters before re-encoding them as Kotlin literals (previously `\'` leaked through as a literal backslash-apostrophe).
- [x] Desktop localization: **100% coverage** — every desktop-only key (including developer options and backup/restore) is now translated in all 47 supported languages, with no English fallbacks left. `Localization.kt` is emitted as one top-level function per language to stay under the JVM 64KB `<clinit>` limit (was failing with "Method too large").
- [x] Desktop localization quality: filled the last desktop-only keys that were still falling back to English (device sync, updates, player basics) and overrode wrong-context / too-long Android mappings (short "Check for updates" button, "Error" ≠ "unknown error", "Playlists" ≠ "featured playlists", CPU/GPU kept short) via `scripts/desktop_extra_translations_8.py` and `_9.py`.
- [x] Global text selection removed because it made every dropdown/dialog crash with "layouts are not part of the same hierarchy" (popups inherit the `SelectionContainer` registrar — Compose CMP-2326). Selectable text is now applied per-widget only (player error detail, pairing code).

## Phase 9 — In-app updater + developer options (completed)
- [x] In-app update checker: checks on startup, on entering the Updates screen, and periodically (configurable interval: manual / 6h / 12h / 24h / 3 days / 7 days); channel-aware pre-release toggle (`includePreReleases`, default on for non-stable channels). The interval dropdown dismisses the popup before applying the change, fixing a "layouts are not part of the same hierarchy" crash.
- [x] Robust asset selection: scans releases newest→oldest and picks the first that actually ships an installer for the host OS; falls back to a clear "open release page" message instead of a fake Download that opens the browser. On Windows it prefers the lighter `.exe` (Inno Setup) over `.msi`.
- [x] In-app download (progress + speed), "Close Vivi and open installer" closes the app after launching, and a "Delete installers" option to clear downloaded update files. Both the update banner and the Updates screen detect an already-downloaded installer for the available version and offer "Open installer" instead of re-downloading.
- [x] Notifications cover **all** app notifications (update available, device paired/unpaired, developer options unlocked), each routed through the chosen mode (main window vs native); native system notifications are marked "experimental".
- [x] Notification history: every notification (in-app and native) is recorded (capped, persisted in `~/.vivimusic/device-sync.json`) and viewable from Settings → Notifications → Notification history, with a "Save notification history" toggle and "Clear history"; the generic in-app notification auto-dismisses after a user-set time (3/5/10/15/30s, default 5s).
- [ ] Native notifications landing in the Windows notification center/history: `java.awt.SystemTray` uses legacy `Shell_NotifyIcon` balloons, which Windows 10/11 does **not** surface in the Action Center. To show there, WinRT toast notifications are required (AppUserModelID + Start-menu shortcut registration) — deferred as a larger, per-OS change.
- [x] Single-instance guard: launching while another instance is running (or starting) exits immediately, keeping the first instance.
- [x] Developer options: Settings entry always visible but disabled by default; unlockable via 7 taps on the version code or the dedicated toggle; unlock notification pointing to Settings → Developer options.
- [x] Performance overlay with two profiles — Full (all metrics) and Performance (CPU + RAM + GPU) — movable by dragging (default on), plus an option to show CPU/RAM in the window title bar and a "Title bar only" display mode.

## Infra / release
- [ ] `WINDOWS_SIGNING_CERT` + `WINDOWS_SIGNING_PASSWORD` secrets to sign the Windows installer.
- [x] Bump the version in `version.txt` on every release (mobile line + DE line + channel, advanced automatically per change).
- [x] Include the Android APK in the same GitHub Release as the desktop (debug APK fetched from the existing CI run).
- [x] Linux `.deb` installs on Debian too: jpackage auto-detects deps with `dpkg -S` on ubuntu-latest, which returns Ubuntu's t64-renamed names (`libasound2t64`, `libglib2.0-0t64`, …) that don't exist on Debian Bookworm. A post-build step (`scripts/fix_deb_depends.py`) rewrites each t64 package to `<name> | <name>t64` alternatives so apt picks the distro's real name.
