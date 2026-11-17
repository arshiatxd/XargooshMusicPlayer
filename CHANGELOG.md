# Changelog

All notable changes to the **Xargoosh Music Player** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.3] - 2026-11-16

### 🐛 Playback, Queue & Audio Focus Fixes
- **Shuffle Toggle & Position Stability Fix**: Replaced destructive queue item swap-rebuilds with non-destructive ExoPlayer `ShuffleOrder` timeline shuffling so toggling shuffle from the app, widgets, or system notification bar never resets or skips playback position or hides the Next button.
- **Notification Player Controls**: Added **Shuffle** and **Repeat Mode** toggle actions directly alongside previous, play/pause, and next controls in the system media notification (kept cleanly within the 5-slot notification bar without a favorite icon).
- **Search → Play Next Fix**: Selecting **Play Next** on a search result while music is playing now reliably inserts the track immediately after the active song in both normal and shuffled playback without disturbing audio output.
- **Pause on Other Audio Fix**: Switched from ExoPlayer's internal audio focus handler to explicit `AudioFocusRequest` + active external playback monitoring so third-party apps (social feeds, browsers, calls, voice notes) reliably pause playback.
- **Play All Sequential Mode**: Fixed **Play All** actions across Folders, Albums, Artists, and Genres to preserve sequential track order.

### 🎨 UI, Playback Cover & Performance Optimizations
- **Blurred Album Cover Backdrop (Normal & Tabbed)**: Added a fast, cached blurred album cover background behind both **Normal** and **Tabbed** Now Playing displays (behind visualizer/lyrics or pure theme gradient) with translucent sheet harmonization.
- **Tabbed Player Gestures & Lyrics Visualizer Fix**: Enabled smooth manual drag-up/down expansion on the tabbed bottom sheet with nested-scroll protection for `Up next`, `Lyrics`, and `Related` lists; removed redundant `Up next` / `Lyrics` buttons beneath transport controls; eliminated overlapping visualizer conflicts when opening/closing the Lyrics sheet; and prevented circular spectrum clipping while giving the Normal player bottom bar extra safe inset margin.
- **Deep Latency & Control Click-Speed Overhaul**:
  - Offloaded Android `Visualizer` HAL initialization onto a serialized I/O worker with same-session reuse across pause/resume and tightened manual pause fade response (`50 ms`).
  - Migrated queue JSON persistence out of `SharedPreferences` into dedicated atomic files (`filesDir`) alongside `O(1)` lookup indices and `Dispatchers.Default` queue updates, eliminating main-thread UI stalls on control taps (`Shuffle`, `Repeat`, `Play/Pause`, `Previous`, `Next`).
  - Isolated 60 FPS visualizer frame streams and sheet alpha updates (`graphicsLayer`), cached Aero/Glass gradient brushes and track palettes, gated the transition fade monitor strictly to active fade-through playback, debounced widget/state snapshots, and optimized scroll observers across all library tabs.
- **Cleaner Menu Glassmorphism**: Removed ambient background glow halos behind menus, cards, and the arc tab indicator while retaining crisp frosted borders.
- **Multilingual Sorting & Compact Alphabet Fast-Scroller**: Added locale-aware collation ordering foreign scripts cleanly (Latin `A–Z`, foreign letters, then `0–9`/`#`) and a 50%-smaller, compact right-side alphabet slider showing `A–Z`, `Я` (grouping all non-Latin tracks by default), and `#`, expanding to native letters automatically when a non-Latin app language is chosen.

### 📱 Home-Screen Widgets Suite (5 Distinct Styles)
- **Consolidated & Reliable Widget Lineup**: Streamlined the launcher picker by removing duplicate rectangular variants and keeping **5 distinct widgets** (**Player Card**, **Lyrics Card 4×3**, **Vinyl Turntable 2×2**, **Aero Pod Player 4×2**, and **Equalizer Spectrum 4×2**) with replicated `previewImage` + `previewLayout` previews, compact binder bitmaps, and active-widget snapshot gating.
- **Full Widget Controls**: Added **Favorite (Heart)**, **Shuffle**, and **Repeat** actions directly alongside transport controls on the unified blurred **Player Card** and across the widget suite.

### 🔐 Security, Obfuscation & Signing
- **R8 Code Obfuscation & Resource Shrinking**: Enabled full R8 minification, class repackaging, debug metadata stripping, and resource shrinking (`app-release.apk` compressed from ~67.2 MB to ~15.0 MB) to protect proprietary logic and prevent reverse engineering.
- **Package Checksum (`v1.1.3`)**:
  - File: `app-release.apk`
  - SHA-256: `e82a01f08c2001ea7410566751fd88c5f08a42ccb77b83153b84e28997bbc5b0`

---

## [1.1.2] - 2026-10-24

### 🔀 Spotify-Style Queue Architecture
- **Immediate Song Selection on Top**: Picking any song of choice from library, playlists, albums, artists, folders, or genres instantly places that track at index `0` ("Now Playing") and begins playback.
- **Dynamic Queue Randomization**: The remaining tracks from the source collection are automatically shuffled behind the selected track, ensuring fresh upcoming listening order.
- **Now Playing Pinned on Top**: Redesigned Queue view with the active track persistently positioned at the very top with animated audio wave visualizer, metadata, and album art.
- **Custom Randomize Buttons**:
  - Top bar action in Queue screen to re-randomize upcoming tracks on demand.
  - Section header pill button in `NEXT IN QUEUE` for one-tap queue shuffling.
  - Custom `Shuffle` button directly in the swipeable bottom sheet `Up Next` tab in Tabbed Player Layout.
- **Collapsible History**: Clean collapsible `PREVIOUSLY PLAYED` section keeping played tracks accessible while prioritizing active upcoming music.

---

## [1.1.1] - 2026-09-28

### 🎨 UI & Layout Redesign
- **Modern Arc Navigation**: Implemented a curved bottom arc tab slider with smooth horizontal pagination across Songs, Playlists, Albums, Artists, Folders, and Genres, featuring faded perimeter edges and dynamic focus opacity.
- **Top Bar Redesign**: Clean header with app branding, a dedicated Music Identifier button, unified Search, and an overflow menu.
- **Horizontal "Recently Played" Carousel**: Added a 10-track swipeable carousel directly beneath the top bar showing album art and track titles; collapses automatically when scrolling down the library.
- **Subtle Faded Divider**: Integrated a low-opacity divider separating recent history from the main song collection.
- **Library Header Bar**: Integrated a real-time song counter on the left alongside sort order and play queue actions on the right.
- **Gapless MiniPlayer Geometry**: Fine-tuned bottom miniplayer offsets flush against the arc tab slider, eliminating empty gaps across all screen aspect ratios.
- **UI Mode Toggle**: Added an option in **Settings > Display** allowing users to switch seamlessly between the Modern Arc UI and the Classic tabbed interface.
- **Cleaned Playback Actions**: Removed redundant menu shuffle/play buttons from the Modern Arc layout for a sleeker presentation.

### ✨ Aero Glassmorphism & Custom Icons
- **Aero Glassmorphic Menus**: Modernized 3-dots popup menus across Settings, library tabs, and track options with 18dp rounded corners, frosted glass background, subtle borders, and color-coded icon badges.
- **Unique Music Identifier Icon**: Introduced a custom vector audio recognition icon (`ic_music_identifier.xml`) clearly distinguishing music identification from the equalizer.

### 🔐 Security & Signing
- **Official V2 Signed Release**: Packaged and signed with official keystore using **APK Signature Scheme v2**.
- **Package Checksum**:
  - File: `app-release.apk`
  - SHA-256: `8e51c201c567feafdf65930351b694e89f8ee0d31a0b4c93b9f5512068e5cc51`

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
