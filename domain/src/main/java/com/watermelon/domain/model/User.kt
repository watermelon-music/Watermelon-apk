package com.watermelon.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

enum class SubscriptionPlan {
    FREE, PREMIUM_INDIVIDUAL, PREMIUM_FAMILY, STUDENT
}

@Serializable
@Parcelize
data class User(
    val id: String,
    val email: String,
    val displayName: String?,
    val avatarUrl: String?,
    val plan: SubscriptionPlan = SubscriptionPlan.FREE,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable
