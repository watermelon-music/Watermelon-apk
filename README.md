<div align="center">

# 🍉 Watermelon

**A modern, open-source Android music streaming app built with Kotlin & Jetpack Compose.**

Search millions of songs, stream global radio, and manage your library — without a subscription.

[![Version](https://img.shields.io/badge/version-v1.0.31-blue)](https://github.com/watermelon-music/Watermelon-apk/releases)
[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-green)](https://developer.android.com/studio/releases/platforms)
[![License](https://img.shields.io/badge/license-MIT-yellow)](LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/watermelon-music/Watermelon-apk?style=social)](https://github.com/watermelon-music/Watermelon-apk/stargazers)
![Last Commit](https://img.shields.io/github/last-commit/watermelon-music/Watermelon-apk)

</div>

---

## Table of Contents

1. [What is Watermelon?](#what-is-watermelon)
2. [Features](#features)
3. [Screenshots](#screenshots)
4. [User Limits / Premium Plans](#user-limits--premium-plans)
5. [Tech Stack](#tech-stack)
6. [How Autoplay Works](#how-autoplay-works)
7. [Architecture](#architecture)
8. [Getting Started](#getting-started)
9. [Build from Source](#build-from-source)
10. [Contributing](#contributing)
11. [Open Source Dependencies](#open-source-dependencies)
12. [License](#license)
13. [Contact](#contact)
14. [Acknowledgements](#acknowledgements)

---

## What is Watermelon?

Watermelon is a fully-featured music streaming app for Android that lets you search and play millions of songs from YouTube, stream over 30,000 global radio stations, and manage your personal library — all without paying a monthly subscription.

Unlike paid services like **Spotify**, **Apple Music**, or **YouTube Music**, Watermelon is **completely free and open-source**. You own your data, your playlists, and your listening history. There are no algorithmic lock-ins, no ads you cannot skip (unless you choose to support the app), and no walled gardens. It is built for users who want a premium music experience on their own terms.

Whether you are an audiophile who wants to self-host your backend, a developer looking for a production-grade Android reference app, or a casual listener tired of subscription fatigue, Watermelon puts you in control.

---

## Features

### 🎧 Streaming

- **Search & Play** — Search any song on YouTube and stream it instantly with a beautiful player UI.
- **720p HD Cover Images** — Streams YouTube thumbnails at high-resolution `maxresdefault.jpg` (720p) with a custom Coil client-side interceptor that automatically falls back to `hqdefault.jpg` on load errors (404).
- **Lyrics** — Fetch and display lyrics inside the player with a clean, readable UI.
- **Sleep Timer** — Set a timer to stop playback after a chosen duration.

### 📥 Offline & Downloads

- **Downloads** — Securely download songs to app-private storage for offline playback (resolved YouTube HTTP 403 Forbidden errors).
- **Offline Playback** — The player automatically detects downloaded files and plays them directly from local storage instead of re-streaming.
- **Favorites** — Like songs across the app to sync them with local cache and database storage.

### 📚 Playlist & Library

- **Playlists** — Create playlists, add songs on the go, and manage your library.
- **Social Sharing & Previews** — Share playlists via WhatsApp/Telegram with rich visual previews (artwork banner, track counts, description) using custom web preview pages.
- **Playlist Cloning** — Seamlessly import/clone shared playlists owned by other users directly into your personal library with one click.

### 📻 Radio & Discovery

- **Global Radio** — Listen to 30,000+ radio stations from Radio Browser API. Browse by country, language, favorites, and recent.
- **Categories** — Browse curated collections like Trending, Bollywood, Hollywood, Pop, Rock, Jazz, Classical, Electronic, Hip Hop.
- **Autoplay** — Smart autoplay recommendation engine analyzes history, skips, and transitions to suggest similar songs after your queue ends.

### 💎 Premium & Monetization

- **Premium Plans** — RED/WHITE/BLACK plan card designs with red borders (Individual: Black, Best Value: White, Family: Black, Student: White) and verification flows.
- **Ads** — Free users see ads to support development; premium users enjoy an ad-free experience.

### 🔔 Notifications & UX

- **Personalized Daily Notifications** — Daily music engagement alerts (Swiggy/Zomato style) scheduled at randomized intervals (6, 8, 12, 24h) with a custom ringtone (`watermelon_tone.mp3`) and dynamically personalized with the user's name.
- **In-App Auto-Updater** — Seamlessly checks for new releases on app start, downloads APKs in the background, handles install permissions on Android 8.0+, and triggers in-place updates.

---

## Screenshots

> _Screenshots will be added here in a future release._

### 🏠 Home Dashboard

The home screen features curated categories, trending songs, and quick access to your recent listening history. Built entirely with Jetpack Compose for smooth 60fps scrolling and fluid animations.

### 🎵 Player & Queue

The full-screen player offers high-resolution artwork, synchronized lyrics, a sleep timer, playback controls, and an editable queue. Mini-player support lets you browse while you listen.

### 🔍 Search & Discover

Powerful search backed by YouTube metadata with instant results. Browse by category or explore the global radio directory with filtering by country and language.

### 📂 Library & Playlists

Your personal library hub — playlists, favorites, downloads, and recently played tracks. Share playlists with rich visual previews and clone playlists shared by other users.

---

## User Limits / Premium Plans

All core features are available to every user. Premium plans unlock higher limits and an enhanced, ad-free experience.

| Feature | Free User | Premium User |
|---|---|
| Playlist creation | Max **2** playlists | Max **10** playlists |
| Ads | Supported by ads | Ad-free experience |
| Radio | Full access | Full access |
| Downloads | Available | Available |
| Autoplay | Enabled | Enabled with smarter recommendations |

**Premium Plans:**

| Plan | Description |
|---|---|
| **Individual** | Black card design — single user, ad-free, full limits. |
| **Best Value** | White card design — best price-to-feature ratio. |
| **Family** | Black card design — share premium benefits across multiple users. |
| **Student** | White card design — discounted pricing with verification flow. |

---

## Tech Stack

| Technology | Version | Description |
|---|---|---|
| **Kotlin** | 2.0.x | Primary language with Coroutines & Flow |
| **Jetpack Compose** | 1.7.x (BOM 2024.09.00) | Declarative UI with Material Design 3 |
| **Hilt** | 2.52 | Dependency injection |
| **Room** | 2.6.1 | Local database (playlists, downloads, analytics, history) |
| **ExoPlayer** | 1.4.1 (Media3) | Media playback and streaming |
| **Retrofit** | 2.11.0 | Type-safe HTTP client |
| **OkHttp** | 4.12.0 | Network requests and interceptors |
| **Coil** | 2.7.0 | Image loading with custom fallback interceptors |
| **Supabase** | 2.x | Cloud authentication and playlist data sync |
| **YT-DLP** | 2024.x | Audio URL extraction and metadata |
| **NewPipe Extractor** | latest | Alternative audio extraction backend |
| **Radio Browser API** | live | Live radio station directory (30,000+ stations) |

---

## How Autoplay Works

Watermelon's autoplay is backed by a local recommendation engine inside the app:

1. **Analytics Collection** — Every time you play, skip, or transition between songs, the app records data in a local Room database (Play History, Skips, Transitions, Song Scores).
2. **Metadata Extraction** — When suggesting the next track, the engine uses YT-DLP metadata (channel, tags, categories, duration) from YouTube videos.
3. **Scoring Algorithm** — Songs are ranked using configurable weights:
   - Transition frequency (how often one song leads to another)
   - Like-to-skip ratio (positive vs negative signals)
   - Tag similarity (matching artist/channel/tags to the current song)
   - Recency decay (boost recent plays)
4. **Smart Queue Refill** — When your current queue ends, the engine searches YouTube with metadata-aware queries, filters out duplicates and recent songs, picks the highest-scored candidate, and appends it to your queue.

You can toggle Autoplay on or off in **Settings**.

---

## Architecture

The project follows a **modular clean architecture** that separates concerns into independent, testable layers:

```
Watermelon/
├── app/                 Main application shell, navigation, and screen composables
│
├── feature-home/        Home dashboard with categories and trending
├── feature-player/      Full-screen player, queue, mini-player, and playback logic
├── feature-library/     User library, playlists, favorites, and downloads
├── feature-search/      Search UI and results
├── feature-settings/    App settings and autoplay toggle
│
├── data/                Repository implementations, local DB, remote APIs,
│                        URL extraction, and data models
│
├── domain/              Business logic, repository interfaces, domain models,
│                        and the recommendation engine
│
└── core/                Shared design system, navigation, and utilities
```

**Key principles:**

- **Unidirectional Data Flow** — UI observes state from ViewModels backed by repositories.
- **Repository Pattern** — Data layer abstracts local (Room) and remote (Supabase, YouTube, Radio Browser) sources.
- **Dependency Injection** — Hilt wires modules and features without tight coupling.
- **Offline-First** — Downloads and favorites are always available, even without a network.

---

## Getting Started

Follow these steps to build and run Watermelon on your local machine.

### Prerequisites

- **Android Studio** — latest stable release ([download](https://developer.android.com/studio))
- **Android SDK 35**
- **JDK 21** (bundled with Android Studio)
- **Git**

### Steps

1. **Clone the repository**

   ```bash
   git clone https://github.com/watermelon-music/Watermelon-apk.git
   cd Watermelon-apk
   ```

2. **Open in Android Studio**

   Open the `Watermelon-apk` folder as an existing project. Android Studio will detect the Gradle wrapper automatically.

3. **Sync Gradle**

   Click **"Sync Project with Gradle Files"** (elephant icon) and wait for dependencies to resolve. If you encounter network issues, ensure that Maven Central and the Google repositories are reachable.

4. **Build & Run**

   Select a device or emulator running Android 8.0 (API 26) or higher, then click the **Run** button (Shift + F10).

5. **Configure Supabase**

   Watermelon uses Supabase for cloud auth and playlist data. To run the app with full backend support:
   - Create a project on [Supabase](https://supabase.com/) (or [self-host](https://supabase.com/docs/guides/self-hosting)).
   - Copy your `SUPABASE_URL` and `SUPABASE_ANON_KEY` into the project's `local.properties` or environment configuration.
   - Rebuild the project.

---

## Build from Source

To generate a debug APK from the command line:

```bash
./gradlew assembleDebug
```

The output APK will be located at:

```
app/build/outputs/apk/debug/app-debug.apk
```

For a release build:

```bash
./gradlew assembleRelease
```

Make sure you have configured a signing keystore in `keystore.properties` before building a release variant.

---

## Contributing

Contributions are welcome. Whether it is a bug fix, a new feature, or improved documentation, your help makes Watermelon better for everyone.

### How to Contribute

1. **Fork** the repository on GitHub.
2. **Create a branch** for your feature or fix:
   ```bash
   git checkout -b feature/my-new-feature
   ```
3. **Write code** following the existing Kotlin and Compose conventions.
4. **Add tests** where applicable.
5. **Commit** with a clear message:
   ```bash
   git commit -m "feat: add shuffle mode to player queue"
   ```
6. **Push** to your fork and open a **Pull Request** against `main`.

### Code Style

- Follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Use **Compose best practices**: state hoisting, unidirectional data flow, and preview annotations.
- Keep composables small and reusable. Extract business logic into ViewModels.
- Write descriptive commit messages using conventional commits (`feat:`, `fix:`, `docs:`, `refactor:`).

### PR Process

- All pull requests are reviewed before merging.
- Ensure the project builds and passes lint checks (`./gradlew lint` and `./gradlew ktlintCheck`).
- Update the README if your change affects public behavior or setup steps.
- Be respectful and constructive in discussions.

---

## Open Source Dependencies

Watermelon is built on the shoulders of incredible open-source projects.

| Library | Description | Link |
|---|---|---|
| **Jetpack Compose** | Modern Android UI toolkit | [developer.android.com/jetpack/compose](https://developer.android.com/jetpack/compose) |
| **Hilt** | Dependency injection for Android | [dagger.dev/hilt](https://dagger.dev/hilt) |
| **Room** | SQLite abstraction layer | [developer.android.com/training/data-storage/room](https://developer.android.com/training/data-storage/room) |
| **ExoPlayer (Media3)** | Media playback engine | [developer.android.com/media/media3](https://developer.android.com/media/media3) |
| **Retrofit** | HTTP client for Android | [square.github.io/retrofit](https://square.github.io/retrofit) |
| **OkHttp** | Efficient HTTP client | [square.github.io/okhttp](https://square.github.io/okhttp) |
| **Coil** | Image loading for Compose | [coil-kt.github.io/coil](https://coil-kt.github.io/coil) |
| **Supabase Kotlin** | Kotlin client for Supabase | [supabase.com/docs/reference/kotlin](https://supabase.com/docs/reference/kotlin) |
| **YT-DLP** | Audio extraction and metadata | [github.com/yt-dlp/yt-dlp](https://github.com/yt-dlp/yt-dlp) |
| **NewPipe Extractor** | Lightweight extraction backend | [github.com/TeamNewPipe/NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) |
| **Radio Browser API** | Global radio directory | [www.radio-browser.info](https://www.radio-browser.info) |

---

## License

```
MIT License

Copyright (c) 2024 Satyam

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## Contact

- **GitHub Issues** — [github.com/watermelon-music/Watermelon-apk/issues](https://github.com/watermelon-music/Watermelon-apk/issues)
- **Email** — [satyampote9999@gmail.com](mailto:satyampote9999@gmail.com)

For bug reports, feature requests, or questions, please open an issue on GitHub. For private inquiries, feel free to send an email.

---

## Acknowledgements

Watermelon is a **solo passion project** by **Satyam**, built in spare time with the goal of delivering a premium, open-source music experience on Android.

Special thanks to the open-source community — the maintainers of YT-DLP, NewPipe Extractor, Radio Browser, and every Jetpack library contributor — for making projects like this possible.

If you enjoy the app, consider starring the repository and sharing it with friends.

---

<div align="center">

Made with ❤️ by Satyam

</div>
