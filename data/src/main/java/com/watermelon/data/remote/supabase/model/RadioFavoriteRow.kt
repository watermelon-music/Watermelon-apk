package com.watermelon.data.remote.supabase.model

import kotlinx.serialization.Serializable

@Serializable
data class RadioFavoriteRow(
    val user_id: String,
    val station_uuid: String,
    val name: String? = null,
    val url: String? = null,
    val favicon: String? = null,
    val country: String? = null,
    val tags: String? = null
)
