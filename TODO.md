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
- [ ] Deploy the relay on Render/Hugging Face and set the real URL (`deviceSyncServerUrl` / desktop). The placeholder `wss://vivimusic-device-sync.onrender.com` currently returns `x-render-routing: no-server` from Render — the service is NOT deployed yet.
- [x] Pairing screen in the Android app (Settings).
- [x] Offline LAN (same Wi-Fi) pairing via a local desktop WebSocket relay (desktop hosts, Android connects by setting the relay URL to `ws://<lan-ip>:<port>`).

## Phase 2 — Sync: queue + playback position
- [ ] Capture the queue and position from the Android player (`pushPlayback` is already exposed).
- [ ] Resume on desktop: apply `pendingPlayback` (requires the desktop player, Phase 4).

## Phase 3 — Sync: library
- [ ] Sync liked songs, albums, artists and playlists (`LibrarySnapshot` schema already present).

## Phase 4 — Desktop audio playback
- [x] JVM audio backend: pure-Java `jaad` AAC decoder + Java Sound (play/pause/resume, position).
- [~] Port stream resolution: NewPipe + ANDROID_VR player response (done); PoToken fallback chain, proxy, HLS and seek are pending.

## Phase 5 — Desktop persistence + authentication
- [ ] Replace Room with SQLDelight / file storage.
- [ ] YouTube login (browser OAuth) and proxy.
- [ ] Full desktop settings layer (same keys as the Android app).

## Phase 6 — Full desktop UI
- [x] Screens: Home, Search, Album, Artist, Playlist, Library, Player, Lyrics, Settings (Library is a placeholder until Phase 5 login; Player/Lyrics are UI-only until Phase 4 playback).
- [~] Apple Music–style mini-player (done) and animated canvas (pending — the canvas modules are Android-only and need a desktop port).

## Phase 7 — End-to-end encryption
- [ ] Per-pair key exchanged during pairing.
- [ ] Snapshots encrypted before sending (the relay no longer reads the data).

## Infra / release
- [ ] `WINDOWS_SIGNING_CERT` + `WINDOWS_SIGNING_PASSWORD` secrets to sign the Windows installer.
- [ ] Bump the version in `version.txt` on every release.
- [ ] (Optional) include the Android APK in the same GitHub Release as the desktop.
