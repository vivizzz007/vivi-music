# Privacy Policy & Terms of Service for ViviMusic

*Last updated: November 2026*

ViviMusic is an open-source, community-driven music client designed with a privacy-first architecture. This document explains how ViviMusic handles your data, your privacy rights, and the terms governing your use of the application.

---

## 1. Core Privacy Principles

- **Zero Tracking**: ViviMusic contains no analytics, telemetry, user profiling, advertising SDKs, or third-party trackers.
- **Local-Only Storage**: All your personal data—including playlists, listening history, search history, downloaded tracks, and app preferences—is stored exclusively on your device.
- **Direct Network Communication**: Streaming and metadata queries are sent directly from your device to official media servers. ViviMusic does not route your music streaming through intermediary proxy servers that collect or monitor your listening habits.

---

## 2. Information We Handle

### A. Data Stored on Your Device
- **Playlists & Favorites**: Music playlists, pinned albums, artists, and liked songs you curate within the app.
- **Listening & Search History**: Search queries and playback history (which can be paused or cleared at any time in Settings > Privacy).
- **Downloaded Media**: Audio files downloaded for offline playback are kept solely within the storage location designated on your device.
- **Settings & Preferences**: Audio quality selections, UI theme, update check preferences, and custom configurations.
- **Optional Account Token**: If you choose to sign in to access your YouTube Music library, your authentication token is stored securely in private app storage on your device and is only used to authorize requests to YouTube Music.

### B. Network Communications
When using ViviMusic, your device communicates with external endpoints strictly to provide application functionality:
- **YouTube Music / Google Endpoints**: Retrieves audio streams, album art, search results, and metadata. These requests originate directly from your IP address to Google/YouTube servers.
- **App Updates**: If "Automatic update check" is enabled, the app checks the public GitHub Releases API to notify you of newer versions.
- **Optional Integrations**:
  - **SponsorBlock**: If enabled, connects to the community SponsorBlock API to skip non-music segments.
  - **JioSaavn**: If enabled, streams audio and retrieves metadata directly from JioSaavn endpoints.
  - **Discord RPC**: If enabled, broadcasts current playing track metadata to your local Discord client.

None of these requests transmit your personal identifying information to ViviMusic developers.

---

## 3. Permissions

ViviMusic requests only minimal system permissions required to operate:
- **Notifications (`POST_NOTIFICATIONS`)**: Required on Android 13+ to show playback media controls in the system notification shade and to alert you of app updates.
- **Install Unknown Apps (`REQUEST_INSTALL_PACKAGES`)** *(GMS edition)*: Allows the built-in updater to seamlessly install verified APK updates when you request them.
- **Foreground Service**: Keeps music playing continuously in the background when the screen is turned off or when switching apps.

---

## 4. Terms of Service

1. **Free & Open Source**: ViviMusic is distributed free of charge under the [GNU General Public License v3.0 (GPL-3.0)](https://github.com/vivizzz007/vivi-music/blob/main/LICENSE). You are free to inspect, modify, and redistribute the source code in accordance with the license terms.
2. **As-Is Disclaimer**: ViviMusic is provided "as is", without warranty of any kind, express or implied. The developers are not liable for any damages or data loss resulting from the use of this software.
3. **Third-Party Services**: ViviMusic is an independent third-party client and is not affiliated with, endorsed by, or sponsored by Google LLC, YouTube, or any other media provider. Your use of third-party platforms remains subject to their respective terms and policies.

---

## 5. Changes to This Policy

Because ViviMusic is open-source, any updates to this policy will be committed directly to this file in the official repository. You can review revision histories publicly at any time.

---

## 6. Community & Contact

- **GitHub**: [GitHub](https://github.com/vivizzz007/vivi-music)
- **Telegram**: [Telegram](https://t.me/vivimusicapp)
- **License**: [GNU GPL-3.0](https://github.com/vivizzz007/vivi-music/blob/main/LICENSE)
- **Author**: Vividh P Ashokan
