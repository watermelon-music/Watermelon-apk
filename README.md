# Watermelon

A modern Android music streaming app that lets you search and play millions of songs, stream global radio stations, and manage your personal library — built with **Kotlin**, **Jetpack Compose**, and **YT-DLP**.

## Features

- **Search & Play** — Search any song on YouTube and stream it instantly with a beautiful player UI.
- **Categories** — Browse curated collections like Trending, Bollywood, Hollywood, Pop, Rock, Jazz, Classical, Electronic, Hip Hop.
- **720p HD Cover Images** — Streams YouTube thumbnails at high-resolution `maxresdefault.jpg` (720p) with a custom Coil client-side interceptor that automatically falls back to `hqdefault.jpg` on load errors (404).
- **Global Radio** — Listen to 30,000+ radio stations from Radio Browser API. Browse by country, language, favorites, and recent.
- **Playlists** — Create playlists, add songs on the go, and manage your library.
- **Social Sharing & Previews** — Share playlists via WhatsApp/Telegram with rich visual previews (artwork banner, track counts, description) using custom web preview pages.
- **Playlist Cloning** — Seamlessly import/clone shared playlists owned by other users directly into your personal library with one click.
- **In-App Auto-Updater** — Seamlessly checks for new releases on app start, downloads APKs in the background, handles install permissions on Android 8.0+, and triggers in-place updates.
- **Personalized Daily Notifications** — Daily music engagement alerts (Swiggy/Zomato style) scheduled at randomized intervals (6, 8, 12, 24h) with a custom ringtone (`watermelon_tone.mp3`) and dynamically personalized with the user's name.
- **Favorites** — Like songs across the app to sync them with local cache and database storage.
- **Downloads** — Securely download songs to app-private storage for offline playback (resolved YouTube HTTP 403 Forbidden errors).
- **Autoplay** — Smart autoplay recommendation engine analyzes history, skips, and transitions to suggest similar songs after your queue ends.
- **Lyrics** — Fetch and display lyrics inside the player with a clean, readable UI.
- **Sleep Timer** — Set a timer to stop playback after a chosen duration.
- **Premium Plans** — RED/WHITE/BLACK plan card designs with red borders (Individual: Black, Best Value: White, Family: Black, Student: White) and verification flows.


## User Limits

| Feature | Free User | Premium User |
|---|---|---|
| Playlist creation | Max **2** playlists | Max **10** playlists |
| Ads | Supported by ads | Ad-free experience |
| Radio | Full access | Full access |
| Downloads | Available | Available |
| Autoplay | Enabled | Enabled with smarter recommendations |

Premium plans unlock higher playlist limits and an enhanced experience across the app.

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

## Tech Stack

- **Kotlin** with Coroutines & Flow for reactive async programming
- **Jetpack Compose** for declarative UI with Material Design 3
- **Hilt** for dependency injection
- **Room** for local database (playlists, downloads, listening analytics, play history)
- **ExoPlayer** for media playback and streaming
- **Retrofit + OkHttp** for network requests
- **Supabase** for cloud auth and playlist data
- **YT-DLP / NewPipe Extractor** for audio URL extraction and metadata
- **Radio Browser API** for live radio station directory
- **Coil** for image loading

## Architecture

The project follows a modular clean architecture:

- `app` — Main application shell, navigation, and screen composables
- `feature-home` — Home dashboard with categories and trending
- `feature-player` — Full-screen player, queue, mini-player, and playback logic
- `feature-library` — User library, playlists, favorites, and downloads
- `feature-search` — Search UI
- `feature-settings` — App settings and autoplay toggle
- `data` — Repository implementations, local DB, remote APIs, URL extraction
- `domain` — Business logic, interfaces, models, and recommendation engine
- `core` — Shared design system, navigation, and utilities

## Offline Playback

When a song is downloaded, it is saved to the device's private music directory. The player automatically detects downloaded files and plays them directly from local storage instead of re-streaming, ensuring offline playback works seamlessly.

## Build

```bash
./gradlew assembleDebug
```

Requirements:
- Android Studio (latest stable)
- Android SDK 35
- JDK 21 (bundled with Android Studio)

## License

Proprietary. All rights reserved.

## Contact

For support or questions, reach out through the app settings.
