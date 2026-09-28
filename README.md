<p align="center">
  <img src="xargoosh-logo.png" alt="Xargoosh Music Player Logo" width="220">
</p>

<h1 align="center">Xargoosh Music Player</h1>

<p align="center">
  <strong>A private, local-first Android music player crafted for audiophiles and personal music collections.</strong>
</p>

<p align="center">
  <a href="https://github.com/arshiatxd/XargooshMusicPlayer/releases"><img src="https://img.shields.io/badge/Release-v1.1.0-blue.svg?style=for-the-badge&logo=android" alt="Release v1.1.0"></a>
  <a href="CHANGELOG.md"><img src="https://img.shields.io/badge/Changelog-v1.1.0-orange.svg?style=for-the-badge" alt="Changelog"></a>
  <a href="#license"><img src="https://img.shields.io/badge/License-Proprietary%20%2F%20Closed%20Source-red.svg?style=for-the-badge" alt="License"></a>
  <a href="#requirements"><img src="https://img.shields.io/badge/Platform-Android%2010%2B%20(API%2029%2B)-success.svg?style=for-the-badge&logo=android" alt="Android 10+"></a>
  <a href="PRIVACY_POLICY.md"><img src="https://img.shields.io/badge/Privacy-100%25%20Local--First-brightgreen.svg?style=for-the-badge" alt="Local First"></a>
  <a href="#features"><img src="https://img.shields.io/badge/Ads%20%2F%20Tracking-None-purple.svg?style=for-the-badge" alt="No Ads No Tracking"></a>
</p>

---

## 📖 Overview

**Xargoosh** is built for people who value true ownership of their music. In an era dominated by subscriptions, compression, algorithms, and telemetry, Xargoosh treats your personal local audio library with the craftsmanship it deserves.

No accounts, no advertisements, no tracking, and no background data collection. Your music, playlists, playback statistics, and audio settings stay strictly on your device.

---

## ✨ Features

### ⚡ Lightning Fast & Responsive
- **Instant Media Scanning:** Decoupled cursor indexing and Room database batch upserts scan libraries of **3,000+ tracks in under 500 milliseconds**.
- **Snapshot Caching:** Bypasses unchanged audio files on subsequent launches for near-instant application startup.

### 🔍 Universal Search Engine
- **Tiered Relevancy Scoring:** Advanced search ranking prioritizing exact titles, prefixes, artist names, albums, word boundaries, acronyms, and fuzzy matches.
- **Multilingual & Diacritics Normalization:** Automatic Unicode NFD diacritics removal and full Arabic/Persian letter unification (`ي` $\rightarrow$ `ی`, `ك` $\rightarrow$ `ک`, tatweel, and harakat stripping).
- **Comprehensive Coverage:** Real-time search across all library sections: Songs, Albums, Artists, Genres, Folders, and Playlists.
- **Typo Tolerance:** Integrated Levenshtein edit-distance calculation to forgive typos on-the-fly.

### 🎵 High-Fidelity Audio Engine
- **Gapless Playback:** Seamless transitions between continuous tracks and live performances.
- **On-Demand ReplayGain Normalization:** Balances loudness differences without distortion, resolved lazily during playback without blocking the UI or initial scans.
- **Parametric Equalizer & Bass Boost:** Tailor your sound profile with rich audio presets and custom bands.
- **Playback Controls:** Precise queue management, crossfade transitions, customizable sleep timer, and speed control.

### 💎 Aero & Glassmorphic UI
- **Modern Jetpack Compose Architecture:** Smooth 120Hz-ready transitions and fluid touch gestures.
- **Aero Glass & Blur Tokens:** Elegant translucent surfaces that adapt dynamically to album art accents.
- **True Dark / AMOLED & Light Themes:** Crisp contrast, battery-efficient dark modes, and customizable color themes.
- **Redesigned Home Screen Widgets:** Material 3 Compact (4×1), Wide (4×2), and Square (3×3) widgets with frosted pill controls and dynamic accent colors.

### 🌌 Immersive Real-Time Visualizers
- Experience your music visually with custom, low-latency audio visualizers:
  - *Aurora*, *Galaxy*, *Neural Network*, *Nier Wave*, *Circular Spectrum*, *Fluid Particles*, *Ribbon*, and *Spectrum Bars*.
- Fullscreen visualizer screen with ambient background rendering for synchronized lyrics.

### 📜 Synchronized Lyrics
- **Offline First:** Automatically reads embedded ID3 synchronized lyrics and local `.lrc` files.
- **Interactive Lyric Scrolling:** Tap any line to seek directly to that exact moment in the song.
- **Optional LRCLIB Integration:** Fetch synchronized lyrics on-demand for tracks without local lyrics (fully optional, disabled by default).

### 🗂️ Library Management & Tag Editor
- **Smart Browsing:** Organize by Artists, Albums, Songs, Genres, Playlists, and direct Folder trees.
- **ID3 & Metadata Editor:** Correct titles, artists, album names, track numbers, and embedded cover art directly from the app.
- **Favorites & Smart Playlists:** Instant playlist creation and history tracking without external dependencies.

### 🎙️ On-Demand Music Recognition
- Identify nearby ambient music with one tap via integrated AudD recognition.
- Captures audio only when activated and immediately purges audio data after matching.

### 🌐 Global Accessibility
- Fully localized in **22 languages**, including full Right-to-Left (RTL) layout support:
  *Persian (Farsi), Arabic, Kurdish, Urdu, Hebrew, English, Spanish, French, German, Japanese, Korean, Chinese (Simplified & Traditional), Russian, Turkish, Portuguese, Hindi, Italian, Indonesian, Polish, and more.*

---

## 📥 Download & Verification

Download the latest official APK from the [Releases](https://github.com/arshiatxd/XargooshMusicPlayer/releases) tab.

### Official Binaries (v1.1.1)

| Asset | Size | SHA-256 Checksum | Signature |
| :--- | :--- | :--- | :--- |
| **`app-release.apk`** | ~66.7 MB | `8e51c201c567feafdf65930351b694e89f8ee0d31a0b4c93b9f5512068e5cc51` | APK Signature Scheme v2 (Verified) |

> [!TIP]
> You can verify the integrity of the downloaded APK using PowerShell:
> ```powershell
> (Get-FileHash -Path .\app-release.apk -Algorithm SHA256).Hash
> ```

### Installation Steps
1. Download **`app-release.apk`** from [Latest Release](https://github.com/arshiatxd/XargooshMusicPlayer/releases/latest).
2. Open the downloaded file on your Android device.
3. If prompted, allow installation from unknown sources in your Android security settings.
4. Launch Xargoosh and grant storage permissions to start listening.

---

## 📱 Requirements

| Specification | Requirement |
| ------------- | ----------- |
| **OS Version** | Android 10.0 (API 29) or newer |
| **Architecture** | ARM64-v8a, armeabi-v7a, x86_64 |
| **Storage** | ~70 MB free space + your music files |
| **Permissions** | Audio / Media Storage access; Microphone (only for visualizer & on-demand recognition) |

---

## 🔒 Privacy & Permissions

Xargoosh operates on a strict **zero-telemetry, local-first** model:
- **No Analytics:** No Google Analytics, Firebase, or tracking SDKs.
- **No Ads:** No banner ads, interstitial popups, or sponsored content.
- **Your Files Are Yours:** Your audio files are read locally and never uploaded anywhere.

For full details, read our complete [PRIVACY_POLICY.md](PRIVACY_POLICY.md).

---

## 📄 License & Closed Source Notice

**Xargoosh Music Player is proprietary, closed-source software.**

Copyright &copy; 2026 Arshia ([@arshiatxd](https://github.com/arshiatxd)). All Rights Reserved.

- Binary releases are provided free for personal, non-commercial use on compatible devices.
- Reverse engineering, decompilation, unauthorized redistribution, extraction of proprietary assets, and commercial resale are strictly prohibited.
- For complete terms and conditions, please consult the [LICENSE](LICENSE) file.

---

## 🤝 Community & Support

- **Changelog:** Detailed version history is available in [CHANGELOG.md](CHANGELOG.md).
- **Bug Reports:** Encountered a crash or unexpected behavior? Open a [Bug Report](https://github.com/arshiatxd/XargooshMusicPlayer/issues/new?template=bug_report.md).
- **Feature Requests:** Have an idea for a new visualizer, theme, or audio setting? Submit a [Feature Request](https://github.com/arshiatxd/XargooshMusicPlayer/issues/new?template=feature_request.md).
- **Security Disclosures:** Please review our [SECURITY.md](SECURITY.md) for responsible disclosure procedures.
