package com.watermelon.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Album(
    val id: String,
    val title: String,
    val artistId: String,
    val artistName: String,
    val coverUrl: String?,
    val releaseDate: String?,
    val genre: String?,
    val songs: List<Song> = emptyList()
) : Parcelable
