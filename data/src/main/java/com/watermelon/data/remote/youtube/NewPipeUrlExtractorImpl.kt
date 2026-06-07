package com.watermelon.data.remote.youtube

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.watermelon.domain.repository.UrlExtractorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.stream.StreamInfo
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPipeUrlExtractorImpl @Inject constructor(
    initializer: NewPipeInitializer,
    private val okHttpClient: OkHttpClient
) : UrlExtractorRepository {

    private val youtube by lazy { org.schabi.newpipe.extractor.ServiceList.YouTube }
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
    }

    // Simple in-memory cache for extracted stream URLs
    private val urlCache = mutableMapOf<String, Pair<String, Long>>()
    private val CACHE_TTL_MS = 10 * 60 * 1000L // 10 minutes (Google Video URLs expire fast)

    private fun getCachedUrl(key: String): String? {
        val entry = urlCache[key] ?: return null
        return if (System.currentTimeMillis() - entry.second < CACHE_TTL_MS) entry.first else null
    }

    private fun putCachedUrl(key: String, url: String) {
        urlCache[key] = url to System.currentTimeMillis()
    }

    override fun invalidateCache(sourceUrl: String) {
        val videoId = extractVideoId(sourceUrl) ?: return
        urlCache.remove(videoId)
        Timber.i("Invalidated cache for $videoId")
    }

    override suspend fun extractAudioUrl(sourceUrl: String): Result<String> = withContext(Dispatchers.IO) {
        var lastException: Throwable? = null

        // If it's already a direct audio file (non-YouTube), pass through
        if (sourceUrl.endsWith(".mp3", ignoreCase = true) ||
            sourceUrl.endsWith(".m4a", ignoreCase = true) ||
            sourceUrl.endsWith(".ogg", ignoreCase = true) ||
            sourceUrl.endsWith(".wav", ignoreCase = true) ||
            (!sourceUrl.contains("youtube") && !sourceUrl.contains("youtu.be"))
        ) {
            return@withContext Result.success(sourceUrl)
        }

        val videoId = extractVideoId(sourceUrl)
            ?: return@withContext Result.failure(
                IllegalStateException("Could not extract video ID from $sourceUrl")
            )

        // 0. Local yt-dlp (runs on phone, uses real ISP IP, bypasses datacenter blocks)
        val cached = getCachedUrl(videoId)
        if (cached != null) {
            Timber.i("Audio URL from cache: $cached")
            return@withContext Result.success(cached)
        }
        runCatching {
            val ytDlpUrl = fetchYtDlpAudioUrl(sourceUrl)
                ?: throw IllegalStateException("yt-dlp returned no audio URL")
            putCachedUrl(videoId, ytDlpUrl)
            Timber.i("Audio URL from yt-dlp: $ytDlpUrl")
            return@withContext Result.success(ytDlpUrl)
        }.onFailure { e ->
            Timber.e(e, "yt-dlp failed")
            lastException = e
        }

        // 1. NewPipeExtractor (fast, pure Kotlin — often works when yt-dlp is slow)
        runCatching {
            val pipedUrl = fetchPipedAudioUrl(videoId)
                ?: throw IllegalStateException("Piped returned no audio URL")
            putCachedUrl(videoId, pipedUrl)
            Timber.i("Audio URL from Piped: $pipedUrl")
            return@withContext Result.success(pipedUrl)
        }.onFailure { e ->
            Timber.e(e, "Piped failed")
            lastException = e
        }

        // 3. NewPipeExtractor
        runCatching {
            val streamInfo = StreamInfo.getInfo(youtube, sourceUrl)
            val audioStream = streamInfo.audioStreams
                .filter { it.isUrl || !it.content.isNullOrBlank() }
                .maxByOrNull { it.averageBitrate }
                ?: throw IllegalStateException("No audio stream available")

            val candidate = audioStream.url ?: audioStream.content
            if (candidate.isNullOrBlank()) {
                throw IllegalStateException("Empty audio stream")
            }
            putCachedUrl(videoId, candidate)
            return@withContext Result.success(candidate)
        }.onFailure { e ->
            Timber.e(e, "NewPipeExtractor failed")
            lastException = e
        }

        // 4. Cobalt.tools
        runCatching {
            val cobaltUrl = fetchCobaltAudioUrl(sourceUrl)
                ?: throw IllegalStateException("Cobalt returned no audio URL")
            putCachedUrl(videoId, cobaltUrl)
            Timber.i("Audio URL from Cobalt: $cobaltUrl")
            return@withContext Result.success(cobaltUrl)
        }.onFailure { e ->
            Timber.e(e, "Cobalt failed")
            lastException = e
        }

        Result.failure(lastException ?: IllegalStateException("Extraction failed after all attempts"))
    }

    private val YOUTUBE_VIDEO_ID_REGEX = Regex(
        """(?:youtube\.com/(?:watch\?(?:[^&]*&)*v=|shorts/|live/|embed/|v/)|youtu\.be/)([a-zA-Z0-9_-]{11})"""
    )

    private fun extractVideoId(sourceUrl: String): String? {
        return YOUTUBE_VIDEO_ID_REGEX.find(sourceUrl)?.groupValues?.get(1)
    }

    private fun fetchPipedAudioUrl(videoId: String): String? {
        val pipedInstances = listOf(
            "https://pipedapi.kavin.rocks",
            "https://api.piped.projectmainstreet.org",
            "https://pipedapi.adminforge.de"
        )
        for (baseUrl in pipedInstances) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/streams/$videoId")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use
                    val parsed = json.parseToJsonElement(body).jsonObject
                    val audioStreams = parsed["audioStreams"]?.jsonArray
                    if (!audioStreams.isNullOrEmpty()) {
                        val url = audioStreams[0].jsonObject["url"]?.jsonPrimitive?.content
                        if (!url.isNullOrBlank()) return url
                    }
                }
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }

    private fun fetchYtDlpAudioUrl(sourceUrl: String): String? {
        return try {
            val request = YoutubeDLRequest(sourceUrl)
            request.addOption("-f", "bestaudio[ext=m4a]/bestaudio[ext=webm]/bestaudio/best")
            request.addOption("--no-check-certificate")
            request.addOption("--no-warnings")
            request.addOption("--user-agent", USER_AGENT)
            request.addOption("--extractor-args", "youtube:player_client=android")
            Timber.d("yt-dlp extracting: $sourceUrl")
            val info = YoutubeDL.getInstance().getInfo(request)
            val url = info?.url?.takeIf { it.isNotBlank() }
            Timber.i("yt-dlp extracted URL length=${url?.length}, startsWith=${url?.take(60)}")
            url
        } catch (e: Exception) {
            Timber.e(e, "yt-dlp extraction failed")
            null
        }
    }

    private fun fetchCobaltAudioUrl(sourceUrl: String): String? {
        try {
            val bodyJson = """{"url":"$sourceUrl","downloadMode":"audio","audioFormat":"best"}"""
            val requestBody = bodyJson.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://api.cobalt.tools/api/json")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.e("Cobalt audio fetch failed: HTTP ${response.code}")
                    return null
                }
                val body = response.body?.string() ?: return null
                val parsed = json.parseToJsonElement(body).jsonObject
                val status = parsed["status"]?.jsonPrimitive?.content ?: return null
                return when (status) {
                    "stream", "tunnel" -> parsed["url"]?.jsonPrimitive?.content
                    "picker" -> parsed["picker"]?.jsonArray?.firstOrNull()
                        ?.jsonObject?.get("url")?.jsonPrimitive?.content
                    else -> null
                }
            }
        } catch (_: Exception) {
            return null
        }
    }
}
