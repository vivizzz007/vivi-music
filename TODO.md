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

## Phase 2 — Sync: queue + playback position
- [x] Capture the queue and position from the Android player (`pushPlayback` is wired from MusicService on track/play changes).
- [x] Resume on desktop: apply `pendingPlayback`; the desktop also pushes its playback and applies incoming snapshots (bidirectional), with echo suppression.
- [x] Precise sync: seek is pushed instantly (both sides), same-track commands apply as a lightweight in-place seek (no restart), and positions carry a timestamp (`positionAtMs`) so receivers extrapolate while playing; PING/PONG clock-sync (relay + `server.js`) removes clock skew between phone and PC.
- [x] Drift auto-correction: while playing, the position is re-pushed every 5s (`SyncServer.RESYNC_TICK_MS`) so players re-align continuously; a 250 ms tolerance (`RESYNC_TOLERANCE_MS`) skips near-no-op seeks to avoid audio glitches.
- [x] Volume sync: the volume slider (0..1) is carried in the playback snapshot and mirrored on both devices.
- [x] Paired device names shown on both sides (desktop announces its real hostname, mobile shows the paired desktop name and vice versa).

## Phase 3 — Sync: library
- [x] Sync liked songs, albums, artists and playlists at the transport level: the mobile app observes its library (liked songs, bookmarked albums/artists/playlists) and pushes a `LibrarySnapshot`; the desktop receives, persists and exposes it (and pushes its own). UI-side apply on desktop waits for the desktop local-library store (Phase 5).

## Phase 4 — Desktop audio playback
- [x] JVM audio backend: pure-Java `jaad` AAC decoder + Java Sound (play/pause/resume, position). The seek slider now reports a correct track duration (derived from the decoded AAC sample count when the container's `mdhd` duration is 0, as in YouTube fMP4), seeking preserves the pause state, and stale/truncated cached audio files are detected and re-downloaded.
- [x] Port stream resolution: NewPipe + the multi-client innerTube fallback chain, seek, and a multi-URL retry (tries every candidate and retries without `Range` on 403) are done. PoToken, proxy and HLS are Android-only / not needed by the desktop player (fMP4/AAC via jcodec + jaad).

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
- [x] Settings sub-screens port (Appearance, Player & audio, Account, Content, Lyrics, Privacy, Storage, Updates, About), keeping the custom Devices screen — with real functionality (pure black, audio quality, remember shuffle/repeat, persistent queue, lyrics text size, content language/region).
- [x] History / Changelog / Login screens (history from YouTube, live changelog fetched from the repo, cookie-based login with DATASYNC_ID/VISITOR_DATA fallback).
- [ ] Song / album / artist / playlist context menus port (needed by the playlist system for "Add to playlist").
- [ ] Local playlist system port: create/rename/delete playlists, add/remove songs, a "My playlists" screen, and cross-device playlist sync (extend the library snapshot with playlist name + song ids).

## Phase 9 — In-app updater + developer options (completed)
- [x] In-app update checker: checks on startup, on entering the Updates screen, and periodically (configurable interval: manual / 6h / 12h / 24h / 3 days / 7 days); channel-aware pre-release toggle (`includePreReleases`, default on for non-stable channels).
- [x] Robust asset selection: scans releases newest→oldest and picks the first that actually ships an installer for the host OS; falls back to a clear "open release page" message instead of a fake Download that opens the browser.
- [x] In-app download (progress + speed), "open installer" closes the app after launching, and a "Delete installers" option to clear downloaded update files.
- [x] Non-invasive update notification (Install now / Dismiss) when an update is available.
- [x] Developer options: Settings entry always visible but disabled by default; unlockable via 7 taps on the version code or the dedicated toggle; unlock notification pointing to Settings → Developer options.
- [x] Performance overlay with two profiles — Full (all metrics) and Performance (CPU + RAM + GPU) — movable by dragging (default on), plus an option to show CPU/RAM in the window title bar and a "Title bar only" display mode.

## Infra / release
- [ ] `WINDOWS_SIGNING_CERT` + `WINDOWS_SIGNING_PASSWORD` secrets to sign the Windows installer.
- [x] Bump the version in `version.txt` on every release (mobile line + DE line + channel, advanced automatically per change).
- [x] Include the Android APK in the same GitHub Release as the desktop (debug APK fetched from the existing CI run).
