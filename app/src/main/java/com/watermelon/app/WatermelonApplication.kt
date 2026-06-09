package com.watermelon.app

import android.app.Application
import coil.Coil
import coil.ImageLoader
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.watermelon.app.config.KillSwitchConfig
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltAndroidApp
class WatermelonApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Coil.setImageLoader {
            ImageLoader.Builder(this@WatermelonApplication)
                .crossfade(true)
                .build()
        }
        // Firebase init
        FirebaseApp.initializeApp(this)
        Firebase.crashlytics.isCrashlyticsCollectionEnabled = true
        Firebase.analytics.setAnalyticsCollectionEnabled(true)
        initRemoteConfig()

        // YoutubeDL init off main thread — prevents ANR at startup.
        Thread {
            runCatching {
                YoutubeDL.getInstance().init(this@WatermelonApplication)
                YoutubeDL.getInstance().updateYoutubeDL(this@WatermelonApplication)
                Timber.i("YoutubeDL initialized in background")
            }.onFailure { Timber.e(it, "YoutubeDL init failed") }
        }.start()
    }

    private fun initRemoteConfig() {
        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 300
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(
            mapOf(
                "disable_youtube" to false,
                "disable_audius" to false,
                "disable_jamendo" to false,
                "force_update" to false,
                "free_max_playlists" to 3L
            )
        ).addOnCompleteListener {
            Timber.d("RemoteConfig defaults set")
        }
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                remoteConfig.fetch().addOnCompleteListener { fetchTask ->
                    if (fetchTask.isSuccessful) {
                        remoteConfig.activate().addOnCompleteListener { actTask ->
                            if (actTask.isSuccessful) {
                                KillSwitchConfig.update(
                                    disableYouTube = remoteConfig.getBoolean("disable_youtube"),
                                    disableAudius = remoteConfig.getBoolean("disable_audius"),
                                    disableJamendo = remoteConfig.getBoolean("disable_jamendo"),
                                    forceUpdate = remoteConfig.getBoolean("force_update"),
                                    freeMaxPlaylists = remoteConfig.getLong("free_max_playlists").toInt()
                                )
                                Timber.i("Remote config activated")
                            }
                        }
                    }
                }
            }.onFailure { Timber.e(it, "RemoteConfig fetch failed") }
        }
    }
}
