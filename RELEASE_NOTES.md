# Watermelon v1.0.0 - Release Notes

## Build Info
- **Version**: 1.0.0
- **Version Code**: 1
- **APK**: `releases/watermelon-v1.0.0-debug.apk`
- **Date**: 2026-06-11
- **JDK**: 21
- **Gradle**: 8.13

## What's New

### Auth & Accounts
- **Email Verification Gate**: Unverified users are routed to `VERIFY_EMAIL` after login/register. Deep links (`watermelon://confirm`) supported.
- **Hardened Auth Flows**: `resendVerificationEmail`, `resetPassword`, `deleteAccount` all wired via raw Supabase REST (bypassing Kotlin client mismatches).
- **Delete Account Dialog**: Type "DELETE" to confirm, with red destructive button.

### Profile
- **Edit Mode**: Toggle edit in Profile screen to change display name and username.
- **Avatar Picker**: 8 Dicebear styles (Toon, Adventure, Avatar, Ears, Bot, Emoji, Lorelei, Notion) + Random shuffle button.
- **Auto-Refresh**: Profile refreshes immediately after saving.

### Media & Player
- **Search Queue**: Tapping a song in Search now loads the entire result list as a queue — Next/Prev works across search results.
- **Sleep Timer**: Live countdown (`MM:SS`) visible in Player top bar.
- **Media Notification**: Custom `PlayerNotificationManager` with Palette vibrant-color extraction from album art.

### UI & Themes
- **Login/Register Backgrounds**: `bg_login.png` wired behind auth screens with dark gradient overlay.
- **Splash Logo**: Updated to `app_logo.png`.
- **Theme Premium Lock**: Free users see only System/Light/Dark themes. All others show with a lock icon.

### Playlists
- **Cover Collage**: Library and Playlist Detail show a 2x2 collage from the first 4 song covers.
- **Creation Limits**: Free = 3 playlists max, Premium = 10 max.
- **Real-time Refresh**: Creating a playlist reflects immediately without app restart.

### Radio
- **India Auto-Select**: Default country selected on first load.
- **Shimmer Skeletons**: Loading states for countries and stations.

### Telegram Admin Bot
- **Reply Keyboard**: Persistent 2-column menu (Dashboard, Users, Stats, Pending, etc.).
- **Inline Approve/Reject**: `/pending` requests show inline buttons.
- **`/myid` Command**: Any user can get their chat ID to configure `TELEGRAM_ADMIN_CHAT_ID`.
- **Bold HTML + Emojis**: Dashboard and top-users formatted with medals.

### Supabase Backend
- `profiles` trigger `handle_new_user` captures `username` + `display_name` from `raw_user_meta_data`.
- Listening history, favorites, playlists, and premium requests tables ready.

## Known Limitations
- **Debug APK**: This is an unsigned debug build. For Play Store release, configure `app/release/keystore.jks` and update `build.gradle.kts` signing config.
- **Media3 Notification**: `setMediaSessionToken` omitted due to `MediaSessionCompat` classpath visibility in Media3 1.2.1. Lockscreen controls still work via `MediaSession` itself.
- **Playlist Sharing / Deep Links**: Planned for v1.1.0.

## How to Push to GitHub
If the repo does not exist yet:

```powershell
# Option A: Using GitHub CLI (recommended)
winget install --id GitHub.cli
gh auth login
gh repo create SatyamPote/Watermelon-apk --public --source=. --remote=origin --push

# Option B: Manual
# 1. Create https://github.com/SatyamPote/Watermelon-apk (empty, no README)
# 2. Then run:
git push -u origin main --tags
```

## Release Checklist
- [x] Clean build (`./gradlew.bat assembleDebug`)
- [x] Git tagged (`v1.0.0`)
- [x] APK copied to `releases/`
- [ ] Create GitHub release and attach APK
- [ ] Upload to Firebase App Distribution / Play Console (future)
