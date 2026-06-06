package com.watermelon.app

import android.app.Application
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class WatermelonApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        try {
            YoutubeDL.getInstance().init(this)
        } catch (e: YoutubeDLException) {
            Timber.e(e, "YoutubeDL init failed")
        }
    }
}
