package com.watermelon.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Playlist(
    val id: String,
    val name: String,
    val description: String?,
    val coverUrl: String?,
    val ownerId: String,
    val songs: List<PlaylistSong> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val shareCode: String? = null,
    val isPublic: Boolean = false,
    val shareCount: Long = 0,
    val saveCount: Long = 0,
    val copyCount: Long = 0
) : Parcelable

@Serializable
@Parcelize
data class PlaylistSong(
    val songId: String,
    val position: Int,
    val title: String = "",
    val artist: String = "",
    val coverUrl: String? = null,
    val audioUrl: String? = null
) : Parcelable
