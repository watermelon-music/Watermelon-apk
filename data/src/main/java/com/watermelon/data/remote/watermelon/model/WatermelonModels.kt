package com.watermelon.data.remote.watermelon.model

import com.google.gson.annotations.SerializedName

data class WatermelonSearchResult(
    val id: String,
    val title: String,
    val artist: String,
    val duration: String,
    val thumbnail: String
)

data class WatermelonSong(
    val id: String,
    val title: String,
    val artist: String,
    val duration: String,
    val thumbnail: String,
    @SerializedName("streamUrl")
    val streamUrl: String,
    @SerializedName("downloadUrl")
    val downloadUrl: String
)

data class WatermelonStream(
    @SerializedName("streamUrl")
    val streamUrl: String
)

data class WatermelonHealth(
    @SerializedName("status")
    val status: String,
    @SerializedName("timestamp")
    val timestamp: String
)
