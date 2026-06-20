package com.watermelon.domain.model

data class RemoteConfig(
    val maintenanceMode: Boolean = false,
    val disableYouTube: Boolean = false,
    val disableAudius: Boolean = false,
    val disableJamendo: Boolean = false,
    val freeMaxPlaylists: Int = 3
)
