package com.watermelon.data.remote.podcastindex

import com.watermelon.data.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest

class PodcastIndexAuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val epoch = System.currentTimeMillis() / 1000L
        val apiKey = BuildConfig.PODCAST_INDEX_API_KEY
        val secret = BuildConfig.PODCAST_INDEX_SECRET
        val hash = sha1("$apiKey$secret$epoch")

        val request = chain.request().newBuilder()
            .header("X-Auth-Date", epoch.toString())
            .header("X-Auth-Key", apiKey)
            .header("Authorization", hash)
            .header("User-Agent", "Watermelon/1.0")
            .build()

        return chain.proceed(request)
    }

    private fun sha1(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
