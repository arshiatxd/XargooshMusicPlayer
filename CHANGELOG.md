# Changelog

All notable changes to the **Xargoosh Music Player** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.0] - 2026-09-28

### ⚡ Performance & Media Scanner
- **Ultra-Fast Initial Scan**: Decoupled blocking disk I/O and ID3 tag parsing from the media discovery pipeline. Media library synchronization now queries Android's `MediaStore` cursor and performs batch Room database upserts in under **500 ms** for libraries exceeding 3,000+ songs.
- **On-Demand ReplayGain Normalization**: Audio loudness normalization tags (`REPLAYGAIN_TRACK_GAIN`, `REPLAYGAIN_TRACK_PEAK`, etc.) are now read on-demand on a background I/O thread when tracks are queued or played, eliminating initial scan delays while maintaining high-fidelity volume normalization.
- **Snapshot Caching**: Subsequent app launches and scans bypass unchanged files entirely by verifying modified timestamps, providing instantaneous startup.

### 🔍 Search Engine
- **Universal Relevancy Scoring**: Integrated a multi-tiered search engine across all library sections:
  - **Songs / Tracks**: Tiered scoring (Exact Title > Title Prefix > Exact Artist > Exact Album > Word Boundary > Multi-Token > Acronym > Fuzzy Levenshtein).
  - **Albums**: Deep matching against album titles, album artists, and nested track listings.
  - **Artists**: Fuzzy matching, acronym matching (e.g. `rhcp` for *Red Hot Chili Peppers*), and artist catalog search.
  - **Genres, Folders & Playlists**: Real-time filtering and relevance ranking.
- **Unicode & Multilingual Normalization**: Full diacritics stripping (NFD) and Arabic/Persian letter unification (`ي` $\rightarrow$ `ی`, `ك` $\rightarrow$ `ک`, tatweel, and harakat stripping) ensuring seamless cross-script matching.
- **Typo-Tolerant Search**: Built-in Levenshtein edit-distance calculation to forgive minor typos.

### 📱 Android Home-Screen Widgets
- **Redesigned Material 3 Layouts**: Re-architected Glance app widgets with standard Android 12+ rounded corners (24–28dp) and responsive padding (10–14dp) preventing launcher clipping.
- **Enhanced Playback Controls**: Added frosted translucent pill containers for previous/next actions and vibrant, dynamic accent backgrounds for the play/pause button.
- **Optimized Grid Dimensions**: Updated widget provider bounds for 3×3, 4×2, and 4×1 launcher home-screen grids.
- **Text & Layout Collision Protection**: Added spacing guards preventing long song titles from overlapping playback controls.

### 🔐 Security & Signing
- **Official V2 Signed Release**: Packaged and signed with official keystore using **APK Signature Scheme v2**.
- **Package Checksum**:
  - File: `app-release.apk`
  - SHA-256: `84f09df869d63f8171e3ed83031b936963896dd921fff1f8fa2610a7170556de`
