package com.watermelon.domain.repository

interface UrlExtractorRepository {
    suspend fun extractAudioUrl(sourceUrl: String): Result<String>
    fun invalidateCache(sourceUrl: String)
}
