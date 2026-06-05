package com.watermelon.data.remote.youtube

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPipeInitializer @Inject constructor(
    downloader: YouTubeDownloader
) {
    init {
        NewPipe.init(downloader, Localization.DEFAULT, ContentCountry.DEFAULT)
    }
}
