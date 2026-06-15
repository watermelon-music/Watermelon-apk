package com.watermelon.data.repository

import com.watermelon.domain.model.YtDlpMetadata
import com.watermelon.domain.repository.UrlExtractorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Placeholder implementation.
 *
 * To enable yt-dlp / NewPipeExtractor:
 * 1. Host a lightweight backend (e.g., Supabase Edge Function or small VPS) that runs yt-dlp
 *    with filters for public-domain / Creative Commons / user-owned content only.
 * 2. Call that backend here and return the extracted direct audio URL.
 *
 * This class currently accepts direct audio URLs (.mp3, .m4a, .ogg) without modification,
 * or throws for other URLs until a legal backend is wired.
 */
@Singleton
class YtDlpUrlExtractorMockImpl @Inject constructor() : UrlExtractorRepository {

    override fun invalidateCache(sourceUrl: String) {
        // No-op for mock
    }

    override suspend fun extractAudioUrl(sourceUrl: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val directExtensions = listOf(".mp3", ".m4a", ".ogg", ".opus", ".wav", ".flac")
            when {
                directExtensions.any { sourceUrl.endsWith(it, ignoreCase = true) } -> sourceUrl
                else -> throw IllegalArgumentException(
                    "Non-direct URL requires a legal backend extractor. " +
                    "Configure a yt-dlp proxy that only processes public-domain / CC / user-owned content."
                )
            }
        }
    }

    override suspend fun extractMetadata(sourceUrl: String): Result<YtDlpMetadata> {
        return Result.failure(UnsupportedOperationException("Mock extractor does not support metadata extraction"))
    }
}
