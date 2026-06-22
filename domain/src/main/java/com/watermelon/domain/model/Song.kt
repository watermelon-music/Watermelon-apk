package com.watermelon.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Song(
    val id: String,
    val title: String,
    val artistId: String,
    val artistName: String,
    val albumId: String?,
    val albumName: String?,
    val durationMs: Long,
    val coverUrl: String?,
    val audioUrl: String?,
    val genre: String?,
    val releaseDate: String?
) : Parcelable
