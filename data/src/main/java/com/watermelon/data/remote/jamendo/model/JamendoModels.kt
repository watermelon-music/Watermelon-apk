package com.watermelon.data.remote.jamendo.model

data class JamendoTracksResponse(
    val headers: JamendoHeaders,
    val results: List<JamendoTrack>
)

data class JamendoHeaders(
    val status: String,
    val code: Int,
    val error_message: String?,
    val warnings: String?,
    val results_count: Int
)

data class JamendoTrack(
    val id: String,
    val name: String,
    val duration: Int,
    val artist_id: String,
    val artist_name: String,
    val artist_idstr: String,
    val album_name: String,
    val album_id: String,
    val album_image: String,
    val audio: String,
    val audiodownload: String?,
    val image: String,
    val releasedate: String,
    val musicinfo: JamendoMusicInfo?
)

data class JamendoMusicInfo(
    val tags: JamendoTags?
)

data class JamendoTags(
    val genres: List<String>?
)
