package com.watermelon.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class CommunityPlaylist(
    val id: String,
    val name: String,
    val description: String?,
    val coverUrl: String?,
    val ownerId: String,
    val creatorDisplayName: String,
    val tags: List<String> = emptyList(),
    val likeCount: Long = 0,
    val songCount: Int = 0,
    val isPublic: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable