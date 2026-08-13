# Xargoosh Music Player Privacy Policy

Effective date: August 1, 2026

Xargoosh is an on-device music player. The app scans audio files that you authorize and stores library metadata, playlists, queue state, favorites, play counts, folder references, settings, and cached lyrics locally on your device.

## Permissions

- Music and audio access is used to discover and play local audio files.
- Microphone/audio-capture permission is used by Android's playback-session Visualizer API and, only after you press the music-recognition button, to capture up to 12 seconds of ambient audio. Recognition audio is held in memory, sent to AudD over HTTPS, and discarded after the request. Xargoosh does not save recognition recordings.
- Foreground media playback keeps music playing while the app is in the background.
- Modify system settings is requested only when you explicitly choose Set as ringtone.
- User-selected folder access is limited to folders selected through Android's system document picker.

## Online Lyrics

Online lyrics lookup is disabled by default. If you enable automatic online lyrics in Settings and synchronized lyrics are unavailable offline, the current song title, artist, album, and duration are sent over HTTPS to LRCLIB solely to search for lyrics. Results are cached locally. Xargoosh does not operate LRCLIB and does not control its retention practices.

## Music Recognition

Music recognition starts only when you press its button. Xargoosh records up to 12 seconds through the microphone and sends that clip over HTTPS to AudD solely to identify the music. Xargoosh keeps the clip in memory and discards it after the request. AudD returns song metadata and links; Xargoosh does not control AudD's retention practices. The bundled public demo access is rate limited and must be replaced with production service configuration before public release.

## Data Sharing and Advertising

Xargoosh contains no advertising SDK, analytics SDK, account system, or tracking service. The app does not sell personal data. Apart from user-enabled LRCLIB fallback and user-initiated AudD music recognition, library and playback data remains on the device.

## Backups

Application data is excluded from Android cloud backup and device-to-device transfer.

## Data Deletion

Uninstalling Xargoosh deletes its local database, preferences, queue, and cached lyrics. Deleting a track from the device is performed only after an explicit user action and any Android system confirmation.

## Contact

Before publication, the distributor must replace this section with a monitored support email address and publish this policy at the same public URL entered in Google Play Console.
