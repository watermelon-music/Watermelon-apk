package com.watermelon.domain.model

data class Broadcast(
    val id: Long,
    val message: String,
    val sender: String,
    val active: Boolean,
    val createdAt: String
)
