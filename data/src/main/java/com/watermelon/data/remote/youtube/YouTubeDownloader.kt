package com.watermelon.data.remote.youtube

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeDownloader @Inject constructor(
    private val okHttpClient: OkHttpClient
) : Downloader() {

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val body = request.dataToSend()?.toRequestBody()

        val requestBuilder = okhttp3.Request.Builder()
            .url(request.url())
            .method(request.httpMethod(), body)

        request.headers()?.forEach { (key, values) ->
            values.forEach { value ->
                requestBuilder.addHeader(key, value)
            }
        }

        val response = okHttpClient.newCall(requestBuilder.build()).execute()

        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            response.body?.string() ?: "",
            response.request.url.toString()
        )
    }
}
