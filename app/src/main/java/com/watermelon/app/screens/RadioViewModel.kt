package com.watermelon.app.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.data.remote.radio.RadioBrowserApi
import com.watermelon.data.remote.radio.RadioStationDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RadioViewModel @Inject constructor(
    private val api: RadioBrowserApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(RadioUiState())
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()

    private val musicCategories = listOf(
        "pop", "rock", "jazz", "classical", "hiphop", "electronic", "bollywood"
    )

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val categoryList = musicCategories.map { tag ->
                async {
                    try {
                        val stations = api.getStationsByTag(tag, limit = 50)
                            .filter { !it.url.isNullOrBlank() }
                            .sortedWith(
                                compareByDescending<RadioStationDto> { it.votes }
                                    .thenByDescending { it.bitrate }
                            )
                            .distinctBy { it.name }
                            .take(30)

                        val languages = stations.mapNotNull { it.language }
                            .flatMap { it.split(",").map { l -> l.trim() } }
                            .filter { it.isNotBlank() }
                            .distinct()
                            .sorted()

                        RadioCategory(
                            name = tag.replaceFirstChar { it.uppercase() },
                            tag = tag,
                            languages = languages,
                            stations = stations
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to load $tag stations")
                        RadioCategory(
                            name = tag.replaceFirstChar { it.uppercase() },
                            tag = tag,
                            languages = emptyList(),
                            stations = fallbackStations(tag)
                        )
                    }
                }
            }.awaitAll()

            _uiState.value = RadioUiState(
                categories = categoryList,
                isLoading = false
            )
        }
    }

    fun selectCategory(category: RadioCategory) {
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            selectedLanguage = category.languages.firstOrNull()
        )
    }

    fun selectLanguage(language: String) {
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedCategory = null,
            selectedLanguage = null
        )
    }

    private fun fallbackStations(tag: String): List<RadioStationDto> {
        return when (tag.lowercase()) {
            "pop" -> listOf(
                RadioStationDto("BBC Radio 1", "http://stream.live.vc.bbcmedia.co.uk/bbc_radio_one", country = "United Kingdom", bitrate = 128, tags = "pop", votes = 5000),
                RadioStationDto("Capital FM", "https://media-ssl.musicradio.com/Capital", country = "United Kingdom", bitrate = 128, tags = "pop", votes = 4000),
                RadioStationDto("Radio Mirchi", "https://mirchitap.akamaized.net/mirchitap_mp3", country = "India", bitrate = 64, tags = "pop,bollywood", votes = 3000),
                RadioStationDto("Hit Radio FFH", "https://mp3.ffh.de/radioffh/hqlivestream.mp3", country = "Germany", bitrate = 128, tags = "pop", votes = 2000)
            )
            "rock" -> listOf(
                RadioStationDto("Classic Rock Florida", "https://listen.181fm.com/181-classicrock_128k.mp3", country = "United States", bitrate = 128, tags = "rock", votes = 3500),
                RadioStationDto("Radio BOB", "https://streams.radiobob.de/bob-national/mp3-192/streams.radiobob.de/", country = "Germany", bitrate = 192, tags = "rock", votes = 3000),
                RadioStationDto("Rock Antenne", "https://stream.rockantenne.de/rockantenne/stream/mp3", country = "Germany", bitrate = 128, tags = "rock", votes = 2500)
            )
            "jazz" -> listOf(
                RadioStationDto("Jazz24", "https://live.wostreaming.net/direct/ppm-jazz24mp3-ibc1", country = "United States", bitrate = 128, tags = "jazz", votes = 2000),
                RadioStationDto("Radio Swiss Jazz", "http://stream.srg-ssr.ch/m/rsj/mp3_128", country = "Switzerland", bitrate = 128, tags = "jazz", votes = 1800)
            )
            "classical" -> listOf(
                RadioStationDto("Classic FM", "https://media-ssl.musicradio.com/ClassicFM", country = "United Kingdom", bitrate = 128, tags = "classical", votes = 4000),
                RadioStationDto("ABC Classic", "http://live-radio01.mediahubaustralia.com/CLASIC/mp3/", country = "Australia", bitrate = 128, tags = "classical", votes = 1500)
            )
            "bollywood" -> listOf(
                RadioStationDto("Radio Mirchi Bollywood", "https://mirchitap.akamaized.net/mirchitap_mp3", country = "India", bitrate = 64, tags = "bollywood", votes = 5000),
                RadioStationDto("Bollywood Radio", "https://stream.zeno.fm/rm4i9pdpxrquv", country = "India", bitrate = 128, tags = "bollywood", votes = 3000)
            )
            "hiphop" -> listOf(
                RadioStationDto("Hot 97", "https://playerservices.streamtheworld.com/api/livestream-redirect/WQHTFM.mp3", country = "United States", bitrate = 128, tags = "hiphop", votes = 3500),
                RadioStationDto("BBC 1Xtra", "http://stream.live.vc.bbcmedia.co.uk/bbc_1xtra", country = "United Kingdom", bitrate = 128, tags = "hiphop", votes = 2500)
            )
            "electronic" -> listOf(
                RadioStationDto("DI.FM Vocal Trance", "https://prem4.di.fm/vocaltrance_hi?", country = "United States", bitrate = 256, tags = "electronic", votes = 3000),
                RadioStationDto("Techno Club Radio", "https://stream.laut.fm/techno-club-radio", country = "Germany", bitrate = 128, tags = "electronic", votes = 2000)
            )
            else -> emptyList()
        }
    }
}

data class RadioUiState(
    val categories: List<RadioCategory> = emptyList(),
    val selectedCategory: RadioCategory? = null,
    val selectedLanguage: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class RadioCategory(
    val name: String,
    val tag: String,
    val languages: List<String>,
    val stations: List<RadioStationDto>
)
