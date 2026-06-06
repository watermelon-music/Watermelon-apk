package com.watermelon.data.remote.youtube

import com.watermelon.domain.repository.UrlExtractorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
        var lastException: Throwable? = null
        repeat(3) { attempt ->
            runCatching {
                val streamInfo = StreamInfo.getInfo(youtube, sourceUrl)
                val audioStream = streamInfo.audioStreams
                    .filter { it.isUrl }
                    .maxByOrNull { it.averageBitrate }
                    ?: throw IllegalStateException("No audio stream available")
                return@withContext Result.success(audioStream.content)
            }.onFailure { e ->
                lastException = e
                if (attempt < 2) delay(1000L * (attempt + 1))
            }
        }
        Result.failure(lastException ?: IllegalStateException("Extraction failed after 3 attempts"))
    }
}
