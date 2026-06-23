package com.watermelon.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Artist(
    val id: String,
    val name: String,
    val bio: String?,
    val imageUrl: String?,
    val genres: List<String> = emptyList(),
    val subscriberCount: Long = 0,
    val songCount: Int = 0,
    val verified: Boolean = false,
    val bannerUrl: String? = null
) : Parcelable
