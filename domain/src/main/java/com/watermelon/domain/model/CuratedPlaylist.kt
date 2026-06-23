package com.watermelon.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class CuratedPlaylist(
    val id: String,
    val title: String,
    val subtitle: String,
    val coverUrl: String?,
    val gradientColors: List<String>, // hex colors like ["#FE3D5C", "#FF8A65"]
    val songs: List<Song>,
    val tag: String // e.g. "chill", "workout", "party"
) : Parcelable