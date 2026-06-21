<div align="center">

<h1>🍉 Watermelon</h1>

<p><strong>A modern, open-source Android music streaming app built with Kotlin & Jetpack Compose.</strong></p>

<p>
  Search millions of songs, stream global radio, and manage your library — without a subscription.
</p>

<p>
  <a href="https://github.com/watermelon-music/Watermelon-apk/releases">
    <img src="https://img.shields.io/badge/version-v1.0.33-blue?style=for-the-badge" alt="Version">
  </a>
  <a href="https://developer.android.com/studio/releases/platforms">
    <img src="https://img.shields.io/badge/platform-Android%208.0%2B-green?style=for-the-badge" alt="Platform">
  </a>
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/license-MIT-yellow?style=for-the-badge" alt="License">
  </a>
  <a href="https://github.com/watermelon-music/Watermelon-apk/stargazers">
    <img src="https://img.shields.io/github/stars/watermelon-music/Watermelon-apk?style=for-the-badge&color=gold" alt="GitHub Stars">
  </a>
  <img src="https://img.shields.io/github/last-commit/watermelon-music/Watermelon-apk?style=for-the-badge" alt="Last Commit">
</p>

<p>
  <a href="https://watermelon-web.onrender.com/" target="_blank"><strong>🌐 Website</strong></a> •
  <a href="https://github.com/watermelon-music/Watermelon-apk/releases/latest" target="_blank"><strong>⬇️ Download APK</strong></a> •
  <a href="https://watermelon-web.onrender.com/docs" target="_blank"><strong>📖 Docs</strong></a> •
  <a href="https://github.com/watermelon-music/Watermelon-apk/issues" target="_blank"><strong>🐛 Issues</strong></a>
</p>

<img src="https://raw.githubusercontent.com/watermelon-music/Watermelon-apk/main/assets/watermelon-banner.png" width="100%" alt="Watermelon Banner">

</div>

---

## 📑 Table of Contents

1. [What is Watermelon?](#-what-is-watermelon)
2. [TL;DR Quickstart](#-tldr-quickstart)
3. [Live Stats](#-live-stats)
4. [Features](#-features)
   - [Streaming](#-streaming)
   - [Offline & Downloads](#-offline--downloads)
   - [Playlist & Library](#-playlist--library)
   - [Radio & Discovery](#-radio--discovery)
   - [Premium & Monetization](#-premium--monetization)
   - [Notifications & UX](#-notifications--ux)
5. [Screenshots](#-screenshots)
6. [User Limits / Premium Plans](#-user-limits--premium-plans)
7. [Tech Stack](#-tech-stack)
8. [Architecture](#-architecture)
9. [How Autoplay Works](#-how-autoplay-works)
10. [Getting Started](#-getting-started)
11. [Build & Release](#-build--release)
12. [API Backend](#-api-backend)
13. [Self-Hosting](#-self-hosting)
14. [Contributing](#-contributing)
15. [Open Source Dependencies](#-open-source-dependencies)
16. [Ecosystem](#-ecosystem)
17. [License](#-license)
18. [Contact](#-contact)
19. [Acknowledgements](#-acknowledgements)

---

## 🎯 What is Watermelon?

**Watermelon** is a fully-featured music streaming app for Android that lets you search and play millions of songs from YouTube, stream over 30,000 global radio stations, and manage your personal library — all without paying a monthly subscription.

Unlike paid services like **Spotify**, **Apple Music**, or **YouTube Music**, Watermelon is **completely free and open-source**. You own your data, your playlists, and your listening history. There are no algorithmic lock-ins, no ads you cannot skip, and no walled gardens.

### 🚀 Made For

- **Audiophiles** who want to self-host their backend
- **Developers** looking for a production-grade Android reference app with clean architecture
- **Casual listeners** tired of subscription fatigue and want control

---

## ⚡ TL;DR Quickstart

```bash
# Clone
git clone https://github.com/watermelon-music/Watermelon-apk.git
cd Watermelon-apk

# Open in Android Studio and run
# Or build from command line:
./gradlew assembleDebug
```

**Or grab the latest APK:** [Releases](https://github.com/watermelon-music/Watermelon-apk/releases/latest)

---

## 📊 Live Stats

| Metric | Value |
|--------|-------|
| **Total Users** | 6+ |
| **Total Streams** | 36+ |
| **Total Playlists** | 0 |
| **GitHub Stars** | See badge above |
| **APK Downloads** | See Releases |

---

## ✨ Features

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

- **Personalized Daily Notifications** — Daily music engagement alerts scheduled at randomized intervals (6, 8, 12, 24h) with a custom ringtone (`watermelon_tone.mp3`) and dynamically personalized with the user's name.
- **In-App Auto-Updater** — Seamlessly checks for new releases on app start, downloads APKs in the background, handles install permissions on Android 8.0+, and triggers in-place updates.

---

## 📸 Screenshots

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

## 🏷️ User Limits / Premium Plans

All core features are available to every user. Premium plans unlock higher limits and an enhanced, ad-free experience.

| Feature | Free User | Premium User |
|---|---|---|
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

## 🛠 Tech Stack

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

## 🏗 Architecture

The project follows a **modular clean architecture**:

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

**Key Principles:**

- **Unidirectional Data Flow** — UI observes state from ViewModels backed by repositories.
- **Repository Pattern** — Abstracts local (Room) and remote (Supabase, YouTube, Radio Browser) sources.
- **Dependency Injection** — Hilt wires modules and features without tight coupling.
- **Offline-First** — Downloads and favorites are always available, even without a network.

---

## 🤖 How Autoplay Works

Watermelon's autoplay is backed by a local recommendation engine:

1. **Analytics Collection** — Every play, skip, and transition is recorded in a local Room database (Play History, Skips, Transitions, Song Scores).
2. **Metadata Extraction** — Uses YT-DLP metadata (channel, tags, categories, duration) from YouTube videos.
3. **Scoring Algorithm** — Songs are ranked using configurable weights:
   - Transition frequency
   - Like-to-skip ratio
   - Tag similarity
   - Recency decay
4. **Smart Queue Refill** — When your queue ends, the engine searches YouTube with metadata-aware queries, filters duplicates, picks the highest-scored candidate, and appends it.

Toggle Autoplay in **Settings**.

---

## 🚀 Getting Started

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

   Click **"Sync Project with Gradle Files"** (elephant icon) and wait for dependencies to resolve.

4. **Build & Run**

   Select a device or emulator running Android 8.0 (API 26) or higher, then click the **Run** button (Shift + F10).

5. **Configure Supabase** (optional but recommended)

   - Create a project on [Supabase](https://supabase.com/).
   - Copy your `SUPABASE_URL` and `SUPABASE_ANON_KEY` into `local.properties`.
   - Rebuild the project.

---

## 🔨 Build & Release

**Debug APK:**
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

**Release APK:**
```bash
./gradlew assembleRelease
```
Requires a signing keystore configured in `keystore.properties`.

---

## 🔌 API Backend

Watermelon connects to a companion **Node.js + Express API** for audio extraction and metadata:

| Repo | URL | Description |
|---|---|---|
| **watermelon-api** | [GitHub](https://github.com/watermelon-music/watermelon-api) | yt-dlp powered backend for audio URLs and search |

---

## 🌐 Web Platform

| Repo | URL | Description |
|---|---|---|
| **watermelon-web** | [GitHub](https://github.com/watermelon-music/watermelon-web) | Next.js website, docs, and landing page |
| **Live Site** | [watermelon-web.onrender.com](https://watermelon-web.onrender.com/) | Hosted on Render |

---

## 🏠 Self-Hosting

Want full control? You can self-host the entire Watermelon stack:

1. **Backend** — Deploy `watermelon-api` to Render, Railway, or any VPS.
2. **Database** — Use [Supabase](https://supabase.com/) (cloud) or [self-host](https://supabase.com/docs/guides/self-hosting).
3. **Payments** — Configure Razorpay keys for premium subscriptions (optional).
4. **App** — Point the Android app to your self-hosted API by updating the base URL.

See the full [Self-Hosting Guide](https://watermelon-web.onrender.com/docs/self-hosting).

---

## 🤝 Contributing

Contributions are welcome. Bug fixes, new features, and documentation improvements all help.

1. **Fork** the repository.
2. **Create a branch:** `git checkout -b feature/my-new-feature`
3. **Write code** following existing Kotlin and Compose conventions.
4. **Add tests** where applicable.
5. **Commit** with a clear message: `git commit -m "feat: add shuffle mode"`
6. **Push** and open a **Pull Request** against `main`.

### Code Style

- Follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Use **Compose best practices**: state hoisting, unidirectional data flow, preview annotations.
- Keep composables small and reusable.
- Write descriptive commit messages using conventional commits.

### PR Checklist

- Build passes (`./gradlew assembleDebug`)
- Lint passes (`./gradlew lint` / `./gradlew ktlintCheck`)
- README updated if public behavior changes

---

## 📦 Open Source Dependencies

| Library | Description | Link |
|---|---|---|
| **Jetpack Compose** | Modern Android UI toolkit | [developer.android.com/jetpack/compose](https://developer.android.com/jetpack/compose) |
| **Hilt** | Dependency injection | [dagger.dev/hilt](https://dagger.dev/hilt) |
| **Room** | SQLite abstraction layer | [developer.android.com/training/data-storage/room](https://developer.android.com/training/data-storage/room) |
| **ExoPlayer (Media3)** | Media playback engine | [developer.android.com/media/media3](https://developer.android.com/media/media3) |
| **Retrofit** | HTTP client | [square.github.io/retrofit](https://square.github.io/retrofit) |
| **OkHttp** | Efficient HTTP client | [square.github.io/okhttp](https://square.github.io/okhttp) |
| **Coil** | Image loading for Compose | [coil-kt.github.io/coil](https://coil-kt.github.io/coil) |
| **Supabase Kotlin** | Kotlin client for Supabase | [supabase.com/docs/reference/kotlin](https://supabase.com/docs/reference/kotlin) |
| **YT-DLP** | Audio extraction and metadata | [github.com/yt-dlp/yt-dlp](https://github.com/yt-dlp/yt-dlp) |
| **NewPipe Extractor** | Lightweight extraction backend | [github.com/TeamNewPipe/NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) |
| **Radio Browser API** | Global radio directory | [www.radio-browser.info](https://www.radio-browser.info) |

---

## 🌐 Ecosystem

| Project | Tech | Role |
|---|---|---|
| **Watermelon APK** | Kotlin + Jetpack Compose | Android music streaming app (this repo) |
| **watermelon-api** | Node.js + Express | Backend for audio extraction & metadata |
| **watermelon-web** | Next.js + Tailwind CSS | Website, docs, and landing page |

---

## 📄 License

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

## 📬 Contact

- **GitHub Issues** — [github.com/watermelon-music/Watermelon-apk/issues](https://github.com/watermelon-music/Watermelon-apk/issues)
- **Email** — [satyampote9999@gmail.com](mailto:satyampote9999@gmail.com)

For bug reports, feature requests, or questions, please open an issue on GitHub. For private inquiries, feel free to send an email.

---

## 🙏 Acknowledgements

Watermelon is a **solo passion project** by **Satyam**, built in spare time with the goal of delivering a premium, open-source music experience on Android.

Special thanks to the open-source community — the maintainers of YT-DLP, NewPipe Extractor, Radio Browser, and every Jetpack library contributor — for making projects like this possible.

If you enjoy the app, consider starring the repository and sharing it with friends.

---

<div align="center">

Made with ❤️ by Satyam

</div>
