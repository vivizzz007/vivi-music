<div align="center">
  <img src="assets/ctrl_music_logo.png" width="160" alt="Ctrl Music logo"/>
  <h1>Ctrl Music</h1>
  <p>An ad-free, open-source music player for Android with an Apple Music-style look.</p>
</div>

## Features

- Search and stream the full **YouTube Music** catalog, with no ads
- **Spotify**: sign in to bring in your liked songs and playlists (matched to YouTube for playback)
- **Downloads** for offline listening, plus **Save to phone** to export any download as an `.m4a` file to `Music/Ctrl Music`
- **Synced lyrics** from several free sources (no AI translation)
- **Animated covers** from Spotify Canvas, Apple Music and Tidal
- Dark charcoal design (`#1F1F1F`) with a rose-gold accent (`#B5928D`)
- No trackers, no analytics, no Google Play Services in the build

## Download

Grab `CtrlMusic.apk` from [Releases](https://github.com/ctrlfxx/ctrlmusic/releases) (64-bit phones; `CtrlMusic-universal.apk` for older devices).
Every push also builds an APK under the **Actions** tab.

## Build a release

Push a tag such as `v1.0.0`. GitHub Actions builds signed APKs and publishes a release.
Signing uses the `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` repository secrets.

## Credits and license

Ctrl Music is a fork of [VIVI Music](https://github.com/vivizzz007/vivi-music) by Vividh P Ashokan,
which builds on [Metrolist](https://github.com/mostafaalagamy/Metrolist) and InnerTune.
The original README is kept in [README.vivi-music.md](README.vivi-music.md).

Licensed under the [GNU GPL v3.0](LICENSE). The proprietary Musixmatch module from upstream is not included.
Ctrl Music is not affiliated with YouTube, Google, Spotify or Apple.
