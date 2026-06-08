package com.watermelon.data.remote.radio

import retrofit2.http.GET
import retrofit2.http.Query

interface RadioBrowserApi {

    @GET("json/tags")
    suspend fun getTags(): List<RadioTag>

    @GET("json/countries")
    suspend fun getCountries(): List<RadioCountry>

    @GET("json/languages")
    suspend fun getLanguages(): List<RadioLanguage>

    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("tag") tag: String? = null,
        @Query("language") language: String? = null,
        @Query("country") country: String? = null,
        @Query("order") order: String = "votes",
        @Query("reverse") reverse: Boolean = true,
        @Query("limit") limit: Int = 50,
        @Query("hidebroken") hideBroken: Boolean = true
    ): List<RadioStationDto>

    @GET("json/stations/bytagexact")
    suspend fun getStationsByTag(
        @Query("tag") tag: String,
        @Query("limit") limit: Int = 50,
        @Query("hidebroken") hideBroken: Boolean = true
    ): List<RadioStationDto>
}

data class RadioTag(
    val name: String,
    val stationcount: Int
)

data class RadioCountry(
    val name: String,
    val stationcount: Int
)

data class RadioLanguage(
    val name: String,
    val stationcount: Int
)

data class RadioStationDto(
    val name: String?,
    val url: String?,
    val url_resolved: String? = null,
    val homepage: String? = null,
    val country: String? = null,
    val countrycode: String? = null,
    val language: String? = null,
    val tags: String? = null,
    val bitrate: Int = 0,
    val votes: Int = 0
)
