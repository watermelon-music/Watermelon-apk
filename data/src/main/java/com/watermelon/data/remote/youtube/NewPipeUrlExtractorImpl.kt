package com.watermelon.data.remote.youtube

import com.watermelon.domain.repository.UrlExtractorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.stream.StreamInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPipeUrlExtractorImpl @Inject constructor(
    initializer: NewPipeInitializer
) : UrlExtractorRepository {

    private val youtube by lazy { org.schabi.newpipe.extractor.ServiceList.YouTube }

    override suspend fun extractAudioUrl(sourceUrl: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val linkHandler = youtube.getStreamLHFactory().fromUrl(sourceUrl)
            val streamInfo = StreamInfo.getInfo(youtube, linkHandler)
            val audioStream = streamInfo.audioStreams.maxByOrNull { it.bitrate }
                ?: throw IllegalStateException("No audio stream available")
            audioStream.url
        }
    }
}
