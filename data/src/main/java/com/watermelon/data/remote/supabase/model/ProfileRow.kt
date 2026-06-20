package com.watermelon.data.remote.supabase.model

import kotlinx.serialization.Serializable
@Serializable

data class ProfileRow(
    val id: String,
    val email: String? = null,
    val username: String? = null,
    val display_name: String? = null,
    val plan: String? = "FREE",
    val avatar_url: String? = null,
    val is_banned: Boolean? = false
)
