package com.watermelon.data.remote.audius.model

data class AudiusTracksResponse(
    val data: List<AudiusTrack>
)

data class AudiusTrack(
    val id: String,
    val title: String,
    val duration: Int,
    val user: AudiusUser,
    val artwork: AudiusArtwork?
)

data class AudiusUser(
    val id: String,
    val name: String
)

data class AudiusArtwork(
    val _150x150: String?,
    val _480x480: String?,
    val _1000x1000: String?
)
