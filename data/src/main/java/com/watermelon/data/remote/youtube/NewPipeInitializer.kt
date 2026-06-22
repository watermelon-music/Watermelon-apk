package com.watermelon.data.remote.youtube

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPipeInitializer @Inject constructor(
    private val downloader: YouTubeDownloader
) {
    @Volatile
    private var initialized = false

    fun ensureInitialized() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            NewPipe.init(downloader, Localization.DEFAULT, ContentCountry("US"))
            try {
                val clazz = Class.forName(
                    "org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor"
                )
                val field = clazz.getDeclaredField("fetchIosClient")
                field.isAccessible = true
                field.setBoolean(null, true)
            } catch (_: Exception) {
                // Reflection failed, iOS client stays disabled
            }
            initialized = true
        }
    }
}
