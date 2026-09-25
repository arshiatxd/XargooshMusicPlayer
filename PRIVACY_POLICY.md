# Xargoosh Music Player Privacy Policy

**Effective date:** August 1, 2026  
**Last updated:** September 2026

Xargoosh is a private, local-first Android music player. We believe your music taste, personal files, and listening habits are your own business.

---

## 1. Local-First Philosophy
Xargoosh operates entirely on your device. The app scans audio files from directories you grant permission to access. All library metadata, playlists, playback queue, favorites, listening stats, folder mappings, settings, and cached lyrics are stored locally on your device and are never transmitted to our servers.

## 2. Permissions Required & Why
- **Music & Audio (READ_MEDIA_AUDIO / READ_EXTERNAL_STORAGE):** Required to index, read tags, and play audio files from your local storage.
- **Audio Recording / Microphone (RECORD_AUDIO):** 
  - Used for Android's real-time audio visualizer effects during playback.
  - Used ONLY when you explicitly trigger the "Music Recognition" feature (records up to 12 seconds of ambient audio, transmits it encrypted via HTTPS to AudD for matching, and is immediately discarded from memory). Xargoosh never records in the background and never saves recognition clips to storage.
- **Foreground Service & Media Playback:** Keeps playback smooth and responsive while multitasking or when the screen is locked.
- **Modify System Settings (Optional):** Requested only if you explicitly choose the "Set as ringtone" option on a track.
- **Document Picker / Folder Access:** Allows you to selectively choose custom folders or SD card paths for playback.

## 3. Optional Online Features
- **Synchronized Lyrics (LRCLIB):** Disabled by default. If enabled in Settings and no local `.lrc` or embedded lyrics exist, the app sends the song title, artist, and album over HTTPS to LRCLIB to retrieve matching synchronized lyrics. The result is cached locally.
- **Music Recognition (AudD):** Only activated when you tap the recognition button. Up to 12 seconds of captured audio is sent securely over HTTPS to AudD to identify track metadata.

## 4. Zero Tracking, Zero Ads, Zero Analytics
- **No Ads:** Xargoosh contains no advertising SDKs or third-party ad networks.
- **No Analytics or Telemetry:** We do not track your usage, clicks, song plays, or hardware identifiers.
- **No Accounts Required:** You do not need to register, log in, or provide an email address.
- **No Data Selling:** We do not collect, monetize, or sell user data to any third party.

## 5. Backups & Data Deletion
- Application data is excluded from automated cloud backups to prevent unencrypted personal listening logs from leaving your device.
- Uninstalling Xargoosh completely wipes its local SQLite database, shared preferences, and cached lyrics from your device.

## 6. Contact & Inquiries
For questions or privacy concerns regarding Xargoosh Music Player, please open an issue on our official GitHub repository:
https://github.com/arshiatxd/XargooshMusicPlayer/issues
