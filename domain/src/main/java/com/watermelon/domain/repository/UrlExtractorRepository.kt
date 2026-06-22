package com.watermelon.domain.repository

import com.watermelon.domain.model.YtDlpMetadata

interface UrlExtractorRepository {
    suspend fun extractAudioUrl(sourceUrl: String): Result<String>
    suspend fun extractMetadata(sourceUrl: String): Result<YtDlpMetadata>
    fun invalidateCache(sourceUrl: String)
}
