# TODO — VIVI Music DE (desktop) + sync Android ↔ Desktop

Legenda: `[x]` fatto · `[ ]` da fare · `[~]` in corso

## Fase 0 — Fondamenta desktop (completata)
- [x] Convertire i 7 moduli rete (`innertube`, `spotify`, `lastfm`, `kizzy`, `shazamkit`, `lyricsProvider`, `jiosaavn`) da `com.android.library` a `kotlin("jvm")`.
- [x] Modulo `desktop` (Compose Multiplatform) con PoC di ricerca via `innertube`.
- [x] Icone native: Windows `.ico`, macOS `.icns`, Linux `.png` (da `logo_vmde.png`).
- [x] Workflow CI: build per-OS (Windows/Linux/macOS) + auto-release su GitHub Releases.

## Fase 1 — Sync: pairing + impostazioni (completata)
- [x] Modulo condiviso `sync` (modello dati + client WebSocket OkHttp).
- [x] Relay `sync-server/` (Node.js): pairing a 6 cifre + mailbox per device offline.
- [x] `DeviceSyncManager` Android (Hilt): push/pull del sottoinsieme impostazioni condivise.
- [x] UI pairing desktop + store impostazioni JSON (`~/.vivimusic/`).
- [ ] Deploy del relay su Render/Hugging Face e impostazione URL reale (`deviceSyncServerUrl` / desktop).
- [ ] Schermata di pairing anche nell'app Android (Impostazioni).

## Fase 2 — Sync: coda + posizione di ascolto
- [ ] Catturare coda e posizione dal player Android (`pushPlayback` già esposto).
- [ ] Resume sul desktop: applicare `pendingPlayback` (richiede il player desktop, Fase 4).

## Fase 3 — Sync: libreria
- [ ] Sincronizzare brani piaciuti, album, artisti e playlist (schema `LibrarySnapshot` già presente).

## Fase 4 — Playback audio desktop
- [ ] Backend audio JVM che sostituisce Media3/ExoPlayer (Java Sound / OpenAL / altro).
- [ ] Portare la risoluzione stream da `YTPlayerUtils` (signature decipher, PoToken, proxy, HLS).

## Fase 5 — Persistenza + autenticazione desktop
- [ ] Sostituire Room con SQLDelight / file storage.
- [ ] Login YouTube (OAuth via browser) e proxy.
- [ ] Layer impostazioni desktop completo (stesse chiavi dell'app Android).

## Fase 6 — UI desktop completa
- [ ] Schermate: Home, Search, Album, Artist, Playlist, Library, Player, Lyrics, Settings.
- [ ] Mini-player e canvas stile Apple Music.

## Fase 7 — Crittografia end-to-end
- [ ] Chiave per-pair scambiata durante il pairing.
- [ ] Snapshot cifrati prima dell'invio (il relay non legge più i dati).

## Infra / release
- [ ] Secret `WINDOWS_SIGNING_CERT` + `WINDOWS_SIGNING_PASSWORD` per firmare MSI/EXE.
- [ ] Bump versione in `version.txt` a ogni release.
- [ ] (Facoltativo) includere l'APK Android nella stessa GitHub Release del desktop.
