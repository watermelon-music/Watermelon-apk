package com.watermelon.domain.model

data class RadioStation(
    val stationuuid: String?,
    val name: String?,
    val url: String?,
    val urlResolved: String?,
    val homepage: String?,
    val favicon: String?,
    val country: String?,
    val countrycode: String?,
    val language: String?,
    val tags: String?,
    val bitrate: Int,
    val votes: Int
)

data class RadioCountry(
    val name: String,
    val stationcount: Int
)

data class RadioLanguage(
    val name: String,
    val stationcount: Int
)
