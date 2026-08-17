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
- [x] Offline LAN (same Wi-Fi) pairing via a local desktop WebSocket relay (desktop hosts, Android connects by setting the relay URL to `ws://<lan-ip>:<port>`). The advertised IP is resolved from the outbound route (UDP-connect source address, then preferring Wi-Fi/wlan adapters) so it also works when the computer is on the phone's hotspot instead of a shared router; relay start/stop is serialized (Mutex) and the bound-port lookup is guarded so rapid Stop→Start or a failed bind surfaces a status error instead of crashing.
- [x] Mobile LAN discovery: NSD/mDNS "Find desktop" + QR scan in the Devices screen.
- [x] QR pairing auto-fill: the desktop QR now encodes the relay address + the current 6-digit pairing code (`vivimusic://pair?addr=…&code=…`); the mobile scanner parses it and pre-fills both fields so the user only verifies the code and taps Pair. Scanning a QR while already paired first disconnects/un-pairs so the new code starts clean.
- [x] Unpair on close: both relays detect a socket close, wait a 15s grace period for a reconnect, then clear the pair + notify the still-connected peer — so closing either app un-pairs both, while a transient network blip no longer breaks a healthy pairing (only the device's live socket triggers the unpair). (Cloud mode requires a redeploy of `sync-server`; LAN works out of the box.)

## Phase 2 — Sync: queue + playback position
- [x] Capture the queue and position from the Android player (`pushPlayback` is wired from MusicService on track/play changes).
- [x] Resume on desktop: apply `pendingPlayback`; the desktop also pushes its playback and applies incoming snapshots (bidirectional), with echo suppression.
- [x] Precise sync: seek is pushed instantly (both sides), same-track commands apply as a lightweight in-place seek (no restart), and positions carry a timestamp (`positionAtMs`) so receivers extrapolate while playing; PING/PONG clock-sync (relay + `server.js`) removes clock skew between phone and PC.
- [x] Drift auto-correction: while playing, the position is re-pushed every 5s (`SyncServer.RESYNC_TICK_MS`) so players re-align continuously; a 250 ms tolerance (`RESYNC_TOLERANCE_MS`) skips near-no-op seeks to avoid audio glitches. Explicit user seeks are now flagged (`PlaybackSnapshot.userSeek`) and applied exactly on the peer (both directions), while periodic drift-ticks only catch up FORWARD — so the leader device is never dragged back by the follower's stale position (this fixed the visible seekbar "jump back").
- [x] Position-sync jump-back fixed: the relay clock offset used for position extrapolation was measured only 25 s after connect (slow EMA convergence), so right after pairing the two devices extrapolated from raw local clocks and kept seeking each other back/forth by the clock skew. The first PING now fires immediately on connect, the first PONG sets the offset directly (no EMA), and `positionAtMs` is only stamped once the offset is known (otherwise the raw position is used, so older relays don't skew-corrupt the extrapolation).
- [x] Volume sync (two channels): the in-app player volume (`volume`) mirrors the mobile/desktop slider pixel-for-pixel, and the native OS system volume (`systemVolume`) mirrors Android STREAM_MUSIC <-> desktop OS volume (WinMM on Windows, `pactl`/`amixer` on Linux, `osascript` on macOS — all best-effort). The in-app volume channel can now be disabled independently with the "Sync VIVI volume" toggle (Settings → Devices and Settings → Player & audio on both editions); the native OS volume sync stays separate and unaffected. Both sides poll volume (700ms mobile / 500+800ms desktop) with per-field echo guards and **retry on a dropped push** (a push that lands in the echo-suppression window is re-sent on the next tick instead of being lost), so volume syncs reliably even while idle. Windows now drives the **master** volume through WASAPI `IAudioEndpointVolume` (Core Audio COM marshaled onto a dedicated MTA thread) and pins the app's own session (`VIVIMusic` in the mixer) to 100% — WinMM `waveOut*` only moved the per-app session volume, so it was replaced; every native call is guarded against missing symbols.
- [x] Paired device names shown on both sides (desktop announces its real hostname, mobile shows the paired desktop name and vice versa).

## Phase 3 — Sync: library
- [x] Sync liked songs, albums, artists and playlists at the transport level: the mobile app observes its library (liked songs, bookmarked albums/artists/playlists) and pushes a `LibrarySnapshot`; the desktop receives, persists and exposes it (and pushes its own). UI-side apply on desktop waits for the desktop local-library store (Phase 5).

## Phase 4 — Desktop audio playback
- [x] JVM audio backend: pure-Java `jaad` AAC decoder + Java Sound (play/pause/resume, position). The seek slider takes its duration from the player response (`videoDetails.lengthSeconds`) and reports it immediately (the track's known duration is also passed into the queue `NowPlaying` so the range is correct before the stream resolves), and the reported position is clamped to the track length so the slider can't get stuck at the end; the periodic re-sync tick only pushes when the position actually advanced so a stalled player can't drag the paired device back to the same point. The decoded AAC sample count is only a fallback when `lengthSeconds` is missing; seeking preserves the pause state, and stale/truncated cached audio files are detected and re-downloaded. Decoded position is throttled to ~10 fps so the seek slider/lyrics stay smooth and draggable instead of recomposing on every frame (~43 fps). The seek slider is disabled until the duration is known and ignores the live position while dragging, so it can't degenerate into a 0..1 range or snap to the start/end.
- [x] Port stream resolution: NewPipe + the multi-client innerTube fallback chain, seek, and a multi-URL retry (tries every candidate and retries without `Range` on 403) are done. On a resolution/download failure the player rotates the guest identity, re-resolves a fresh stream URL and retries automatically (up to 3 attempts). PoToken, proxy and HLS are Android-only / not needed by the desktop player (fMP4/AAC via jcodec + jaad).
- [x] Load feedback: the full player and mini-player show "Resolving audio…" / "Downloading…" (with a spinner) while the stream is resolved and downloaded.
- [x] First-play reliability: pressing Play on a track whose stream is not loaded yet (e.g. restored from the persistent queue at startup, or a previous load failed) now triggers a real resolution + load via `startCurrent()` instead of a no-op `resume()`, so the track starts on the first press without skipping to the next track and back.

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
- [x] Appearance sub-menu fully ported into three dedicated sub-screens: Theme (4-mode selector System/Light/Dark/Pure black + full 21-color accent palette + live preview card), App font (5 bundled fonts — System/Google Sans/Sans Flex/Outfit/Plus Jakarta Sans — with a live typography preview) and Canvas (enable toggle + source Auto/Apple Music/ViViMusic/Tidal, wired into the player's animated background via `CanvasResolver` + `CanvasSource`). The `.ttf` fonts are bundled under `desktop/src/main/resources/fonts/` and all new strings reuse the Android translations (47 languages). The three sub-screens no longer nest their own `verticalScroll` inside the settings scaffold (that caused the "infinity maximum height constraints" crash).
- [x] History / Changelog / Login screens (history from YouTube, live changelog fetched from the repo, cookie-based login with DATASYNC_ID/VISITOR_DATA fallback).
- [x] About screen: the GitHub repository icon is now a vector (ported from the mobile drawable) so it tints with the accent color and adapts to dark/light mode.
- [x] Backup & restore sub-menu: exports/imports a full ZIP backup (settings + playlists + account/login + library via `BackupManager`) through a native save/open dialog; backups use a single `.vivide.backup` file and legacy single-JSON `.backup` files are still importable. Import preserves the device id + first-launch date, drops any stale pairing, and prompts a restart to apply. Manual backups carry the date + timestamp in their filename (`vivimusic-de_yyyyMMdd_HHmmss.vivide.backup`), matching the automatic backups.
- [x] "Restart now" after a backup restore truly relaunches: `restartApplication()` releases the single-instance lock, spawns a new instance (jpackage launcher via `jpackage.app-path`, or `java -cp … MainKt` in dev), then exits.
- [x] Automatic backups (ported from mobile): optional weekly backup (checked on startup + hourly, 5-backup retention) and optional "backup before update" that runs before opening the installer. Stored backups (`~/.vivimusic/backups/`) are listed in Settings → Backup with per-item restore/delete. New strings (`auto_backup`, `weekly_backup`, `backup_before_update`, `stored_backups`, confirmations) are translated in all 47 languages via Android string reuse.
- [x] [APK] Mobile backup restore is safe and crash-free: the archive is decompressed on a background thread into `filesDir/pending_restore`, then the process exits; `App.onCreate()` swaps the settings + database in before Room/DataStore are opened on the next launch. This never closes the shared Room database while the app is running (the old approach crashed with an uncaught "database is closed" from in-flight queries) and the WAL/SHM sidecars are deleted before swapping `song.db` so the restored DB is never mixed with stale journal frames.
- [x] [APK] Restore now validates the backup's database before applying it (SQLite header magic + `PRAGMA integrity_check` + schema-version guard): a corrupt or incompatible backup fails with a clear "backup is corrupt" message instead of being swapped in and crashing the app on the next launch.
- [x] [APK] Restore confirmation: picking a backup now shows a dialog with the file name, backup date (parsed from the filename timestamp, falling back to the file's last-modified time) and the app version it was created from (parsed from the filename), before anything is applied.
- [~] Song / album / artist / playlist context menus port. The full song context menu (like / library / add-to-playlist / share) is done, plus a dedicated "Add to playlist" button on every song row (and on the Player + Queue screens); album / artist / playlist context menus are still pending.
- [x] Local playlist system port: create/rename/delete playlists, add/remove/reorder songs (drag-to-reorder in the detail screen), a "Playlists" screen + per-playlist detail, and cross-device playlist sync with last-write-wins (the library snapshot now carries the full playlist name + ordered songs + edit timestamp, plus deletion tombstones). Delete confirmation is deferred until after the dialog dismisses, fixing a "layouts are not part of the same hierarchy" crash. Playlist last-write-wins preserves the peer's edit timestamp on apply (the mobile no longer stamps local "now" over it), so desktop create/rename/delete all propagate to the phone.
- [x] Desktop localization: the playlist and song-menu strings (rename / delete playlist / confirmation / empty / not-found / song count / like / library / share, …) are fully translated across all 47 supported languages (desktop-only keys added via a `TRANSLATIONS` table in the generator).
- [x] Desktop localization: apostrophes render correctly everywhere — the generator now decodes Android's `\'`/`\n`/`\t`/`\"`/`\\` resource escapes into real characters before re-encoding them as Kotlin literals (previously `\'` leaked through as a literal backslash-apostrophe).
- [x] Desktop localization: **100% coverage** — every desktop-only key (including developer options and backup/restore) is now translated in all 47 supported languages, with no English fallbacks left. `Localization.kt` is emitted as one top-level function per language to stay under the JVM 64KB `<clinit>` limit (was failing with "Method too large").
- [x] Desktop localization quality: filled the last desktop-only keys that were still falling back to English (device sync, updates, player basics) and overrode wrong-context / too-long Android mappings (short "Check for updates" button, "Error" ≠ "unknown error", "Playlists" ≠ "featured playlists", CPU/GPU kept short) via `scripts/desktop_extra_translations_8.py` and `_9.py`.
- [x] Global text selection removed because it made every dropdown/dialog crash with "layouts are not part of the same hierarchy" (popups inherit the `SelectionContainer` registrar — Compose CMP-2326). Selectable text is now applied per-widget only (player error detail, pairing code).

## Phase 9 — In-app updater + developer options (completed)
- [x] In-app update checker: checks on startup, on entering the Updates screen, and periodically (configurable interval: manual / 6h / 12h / 24h / 3 days / 7 days); channel-aware pre-release toggle (`includePreReleases`, default on for non-stable channels). The interval dropdown dismisses the popup before applying the change, fixing a "layouts are not part of the same hierarchy" crash.
- [x] Robust asset selection: scans releases newest→oldest and picks the first that actually ships an installer for the host OS; falls back to a clear "open release page" message instead of a fake Download that opens the browser. On Windows it prefers the lighter `.exe` (Inno Setup) over `.msi`.
- [x] In-app download (progress + speed), "Close Vivi and open installer" closes the app after launching, and a "Delete installers" option to clear downloaded update files. Both the update banner and the Updates screen detect an already-downloaded installer for the available version and offer "Open installer" instead of re-downloading.
- [x] Update notification ↔ Updates screen sync: a single `UpdateState` (progress + downloaded installer + count) is shared by both surfaces, so acting in one is reflected in the other (and vice versa). The notification also runs the optional "backup before update" and closes the app when it opens the installer, like the Updates screen. The in-app update banner now pauses its auto-dismiss timer while a download is in progress, so it never disappears mid-progress-bar.
- [x] Selectable update source (fork vs original): desktop (default fork `PiBOH/vivi-music`) and mobile (default original `vivizzz007/vivi-music`) let the user choose which GitHub repo the update checks, download and notification URLs read from; threaded through the desktop `UpdateSource`/`UpdateChecker` and the Android updater + `UpdateNotificationHelper`.
- [x] Queue sync is last-write-wins (like playlists): `PlaybackSnapshot.queueUpdatedAt` (shared relay-time frame) is stamped when the local queue/index changes and adopted when a remote queue is applied; on receive each side only replaces its queue when the remote edit is newer (`0` = unknown → apply unconditionally for older peers).
- [x] Repeat mode + shuffle sync in real time (both ways): `PlaybackSnapshot` carries `repeatMode` ("OFF"/"ALL"/"ONE") and `isShuffle`; the desktop pushes them via its `PlaybackSyncKey` and applies them on receive (`PlayerController.setRepeatMode`/`setShuffle`), and the phone pushes them from `onRepeatModeChanged`/`onShuffleModeEnabledChanged` and applies them in `applyRemotePlayback`.
- [x] Notifications cover **all** app notifications (update available, device paired/unpaired, developer options unlocked), each routed through the chosen mode (main window vs native); native system notifications are marked "experimental". Native notifications use the real VIVI Music DE logo (`logo_vmde.png` bundled under `images/`) as their tray/balloon icon, via a single persistent tray icon (created once, high-quality scaling). Every in-app notification (update banner, dev-unlocked hint, generic banner) auto-dismisses after the configured duration.
- [x] Notification history: every notification (in-app and native) is recorded (capped, persisted in `~/.vivimusic/device-sync.json`) and viewable from Settings → Notifications → Notification history, with a "Save notification history" toggle and "Clear history"; the generic in-app notification auto-dismisses after a user-set time (3/5/10/15/30s, default 5s).
- [x] Native notifications landing in the Windows notification center/history: `java.awt.SystemTray` uses legacy `Shell_NotifyIcon` balloons, which Windows 10/11 does **not** surface in the Action Center. Replaced on packaged Windows builds with WinRT toasts via a PowerShell helper (`WindowsToast`): the app registers an AppUserModelID (Start-menu shortcut + `System.AppUserModel.ID` via an inline C# `Add-Type`/property store, exactly like the MS "enable desktop toast" sample) and shows `ToastGeneric` toasts with the VIVI logo. Toasts carry a foreground `launch="--open=<section>"` argument; clicking launches the exe with that arg, the `AppCommand` file mailbox forwards it to a running instance (single-instance guard), and the app opens the mapped screen (Updates/Developer/Devices) and brings the window to the front. Non-Windows and dev (unpackaged) builds fall back to `SystemTray`. To verify on a real machine: install a packaged build, enable native notifications, and check the toast appears in the Action Center and opens the right screen on click. Native notifications are still being diagnosed on real hardware (they sometimes don't appear at all): the path now logs to `~/.vivimusic/native-notify.log` and Settings → Notifications has a "Send test notification" button to reproduce on demand.
- [x] Crash/error dialog "Copy error": a global uncaught-exception handler (`installGlobalErrorDialog`, installed at startup) replaces the default AWT "Error" dialog with one that shows the full stack trace and offers "Copy error" (copies to clipboard) plus "OK".
- [x] Single-instance guard: launching while another instance is running (or starting) exits immediately, keeping the first instance.
- [x] Developer options: Settings entry always visible but disabled by default; unlockable via 7 taps on the version code or the dedicated toggle; unlock notification pointing to Settings → Developer options.
- [x] Performance overlay with two profiles — Full (all metrics) and Performance (CPU + RAM + GPU) — movable by dragging (default on), plus an option to show CPU/RAM in the window title bar and a "Title bar only" display mode. Network down/up speed + total traffic are read culture-invariantly via `Get-NetAdapterStatistics` on Windows (the old `netstat -e` parser only matched the English "Bytes" label and showed "—" on localized Windows).
- [x] Developer options screen reorganized into clear, dividers-separated sections (Display, Monitoring profile, Overlay behaviour, Title bar) for a more "developer" layout.

## Phase 10 — Remaining gaps to reach 100% UI parity (mobile → desktop)
Only allowed difference: the bottom navigation bar becomes the collapsible/expandable sidebar. Everything below is still missing or divergent.

### Screens / sections missing entirely
- [ ] Listen Together (mobile main tab) + its chat and "from topbar" variants.
- [ ] Stats screen.
- [ ] New Release albums screen.
- [ ] Charts screen.
- [ ] Wrapped screen.
- [ ] Song recognition (Shazam) + recognition history.
- [ ] Commit screen (`settings/commits`).
- [ ] Artist sub-tabs: Songs / Albums / Items (mobile `artist/{id}/songs|albums|items`); the DE has a single Artist page.
- [ ] Auto-playlist detail screens (Liked / Downloaded / Top / Cached); the DE only shows them as Library filters.
- [ ] Dedicated Mood & genres screen (the DE navigates to a generic Browse screen).

### Settings sub-screens missing
- [ ] Listen Together settings (hub entry).
- [ ] AI Lyrics Translation (`settings/ai`).
- [ ] Data saver (`settings/datasaver`).
- [ ] Romanization (`settings/content/romanization`).
- [ ] JioSaavn (`settings/player/jio`).
- [ ] Equalizer (`settings/equalizer` + dialog).
- [ ] Spotify import (`settings/spotify`).
- [ ] Integrations hub + Discord / Last.fm / Listen Together settings.
- [ ] Discord login.
- [ ] Notification permission (`settings/update/notification_permission`).

### Already tracked, still open (see Phase 8)
- [~] Album / Artist / Playlist context menus (the song context menu is done).
- [ ] Gradient header on Album / Artist / Playlist (currently a simple row).

### Fine-grained parity (not previously tracked)
- [ ] Dynamic theme (Material You `dynamicTheme`); the DE is a fixed seed palette.
- [ ] Player design variants (`useNewPlayerDesign`, `usePlayerV2`, `useExpressiveAlbumDesign`, `useAppleMiniPlayer`).
- [ ] Player background styles (gradient / blur / glow animated / apple music / live mesh); the DE only has the canvas.
- [ ] Slider styles (squiggly / wavy / slim).
- [ ] Mini-player: swipe-to-expand, outline, pure-black mini, interaction with the (floating) nav bar.
- [ ] Song swipe gestures (swipe to play / remove).
- [ ] Screen transitions (fade/slide, matching `NavigationBuilder`).
- [ ] Animated thumbnails (rotating / swipe / canvas thumbnail).
- [ ] Advanced lyrics (swipe lyrics, romanization, AI translation, line spacing, animation styles, thumbnail play/pause).
- [ ] UI density (density scale 85/75/65/55%) + custom grid size.
- [ ] Sort / filter chips for library / albums / artists / playlists.
- [ ] Home: "Quick Picks vs Last Listen" toggle, "Randomize home order", Wrapped card.
- [ ] Integrations: Discord RPC, Last.fm scrobbling (mobile screens/options).
- [ ] Search/listen history + pause-history privacy toggles.
- [ ] Settings popup (quick settings shortcut).

## Infra / release
- [x] GitHub Pages website for VIVI Music DE: the static site lives in `.websitede/` (index + Downloads/Changelog/Sync/About subpages, dark minimal style matching the original site, logo bundled) and is deployed by `.github/workflows/pages-deploy.yml` via `actions/deploy-pages` on push to `.websitede/**` (or manual dispatch). Download links and the changelog resolve the latest GitHub release client-side through the API, so the site stays current without rebuilding on every release. URL: `https://piboh.github.io/vivi-music/` (requires Pages → Source: GitHub Actions enabled in repo settings). The site is fully responsive (hamburger nav on mobile, stacking download rows), uses a compact sticky footer, links the Android companion back to the upstream VIVI Music site, and credits VIVIDH P ASHOKAN as the original mobile author.
- [x] Inno Setup "Start VIVI Music DE" after install: removed the redundant `launchafterinstall` task that gated the `[Run]` entry, so the final-page checkbox now actually launches `{app}\VIVIMusic.exe`.
- [x] Mobile backup files use the `.vividroid.backup` extension (manual + automatic) while the desktop uses `.vivide.backup`, so the two editions are distinguishable; older `.backup` files remain listed and importable (the `endsWith(".backup")` listing filters still match the new extension, and the filename timestamp regex accepts both).
- [x] Debug APK build fix: `sync_vivi_volume_desc` used a bare apostrophe that aapt2 rejected as "Invalid unicode escape sequence"; escaped it (`\'`) so `assembleUniversalGmsDebug` (and the CI "Build Debug APK" job) completes again.
- [ ] `WINDOWS_SIGNING_CERT` + `WINDOWS_SIGNING_PASSWORD` secrets to sign the Windows installer.
- [x] Bump the version in `version.txt` on every release, advanced automatically per change.
- [x] `version.txt` reorganized into a self-documenting six-line layout (mobile version / mobile version code / mobile channel / DE version / DE version code / DE channel) with `#` comments below each field; `desktop/build.gradle.kts`, `AppInfo.kt` and the release/build workflows updated to the new positions (DE version = line 4, DE channel = line 6, DE version code = line 5).
- [x] Include the Android APK in the same GitHub Release as the desktop (debug APK fetched from the existing CI run).
- [x] Linux `.deb` installs on Debian too: jpackage auto-detects deps with `dpkg -S` on ubuntu-latest, which returns Ubuntu's t64-renamed names (`libasound2t64`, `libglib2.0-0t64`, …) that don't exist on Debian Bookworm. A post-build step (`scripts/fix_deb_depends.py`) rewrites each t64 package to `<name> | <name>t64` alternatives so apt picks the distro's real name.
- [x] Persistent Android debug keystore: CI (`build.yml`) restores the debug keystore from the `DEBUG_KEYSTORE` secret (base64) so the debug APK keeps a stable signature across runs (no more uninstall/reinstall on the phone). Falls back to generating a fresh key when the secret is missing.
- [x] Keep screen on while paired: the Android app sets `FLAG_KEEP_SCREEN_ON` and the desktop keeps the display/system awake (`SetThreadExecutionState` on Windows, `caffeinate` on macOS) while paired, so the OS sleeping the screen can't suspend the network and unpair the two devices.
- [x] Settings persistence is atomic: `DesktopSettings` exposes a synchronized `update(transform)` (read-modify-write) and every call site now uses it instead of `save(load().copy(…))`, so a UI-thread setting change (e.g. notification mode) can't be lost by a concurrent device-sync save.
