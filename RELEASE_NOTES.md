# Watermelon Release Notes

## v1.0.14 (versionCode 15)
**Date:** 2026-06-19

### 🐛 Bug Fixes
- **Login / Sign-in**: Fixed generic "Something went wrong" error — app now shows the **real reason** login failed (wrong password, unverified email, network error, rate limit, etc.)
- **Sign-up**: Fixed silent failure when passwords don't match — now shows clear inline error message
- **Sign-up**: Fixed navigation not working after successful registration — app now correctly navigates to the email verification screen
- **Sign-up / Login**: Added input validation — empty fields are caught immediately before hitting the server
- **Startup**: Removed `updateYoutubeDL()` call from app startup — this was causing slow launches and potential ANR on some devices
- **Home refresh**: Added `refresh()` function to HomeViewModel — home screen can now be manually refreshed

### 🔒 Production Hardening
- Added release signing configuration to `app/build.gradle.kts`
- Added comprehensive ProGuard / R8 rules for all libraries (Hilt, Supabase, Firebase, Razorpay, OkHttp, Media3, Coil, YoutubeDL, NewPipe, Kotlin serialization)
- Timber debug logs (`d`, `v`, `i`) are stripped from release builds via ProGuard

---

## v1.0.13 (versionCode 14)
- Release signing with debug keystore for consistent installs
- Repository migration to watermelon-music organization
- Broadcast refresh support

## v1.0.12 (versionCode 13)
- Home screen recommendations & queue improvements
- Image quality improvements (maxresdefault thumbnails with hqdefault fallback)

## v1.0.11 (versionCode 12)
- SMTP / email transactional setup via Resend API
- Email verification flow improvements

## v1.0.10 (versionCode 11)
- Initial Play Store-ready build
- Premium subscription via Razorpay
- Download manager
