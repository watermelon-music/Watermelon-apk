package com.watermelon.domain.model

data class YtDlpMetadata(
    val id: String,
    val title: String,
    val artist: String? = null,
    val channel: String? = null,
    val tags: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val durationSec: Long? = null,
    val description: String? = null
)