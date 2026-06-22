package com.watermelon.domain.model

data class SearchHistory(
    val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)
