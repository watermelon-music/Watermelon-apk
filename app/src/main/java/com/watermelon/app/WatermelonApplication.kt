package com.watermelon.app

import android.app.Application
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class WatermelonApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // Initialize yt-dlp — extracts binary from APK assets.
        // Wrap in broad catch: if native extraction fails, app must still open.
        try {
            YoutubeDL.getInstance().init(this)
            Timber.i("YoutubeDL initialized")

            // Update yt-dlp binary in background (optional, best-effort)
            Thread {
                try {
                    val status = YoutubeDL.getInstance().updateYoutubeDL(this)
                    Timber.i("YoutubeDL update status: $status")
                } catch (e: Exception) {
                    Timber.e(e, "YoutubeDL update failed")
                }
            }.start()
        } catch (e: Throwable) {
            // Catch Throwable (includes UnsatisfiedLinkError, native crashes)
            // so the app UI still opens even if playback extraction is broken.
            Timber.e(e, "CRITICAL: YoutubeDL init crashed — playback will not work")
        }
    }
}
