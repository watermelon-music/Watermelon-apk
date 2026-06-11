package com.watermelon.feature.downloads

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

data class DownloadedTrack(
    val id: String,
    val title: String,
    val artistName: String,
    val coverUrl: String,
    val filePath: String
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _tracks = MutableStateFlow<List<DownloadedTrack>>(emptyList())
    val tracks: StateFlow<List<DownloadedTrack>> = _tracks.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            val result = if (musicDir != null && musicDir.exists()) {
                val jsonFiles = musicDir.listFiles { f -> f.extension == "json" } ?: emptyArray()
                jsonFiles.mapNotNull { jsonFile ->
                    try {
                        val json = JSONObject(jsonFile.readText())
                        val baseName = jsonFile.nameWithoutExtension
                        val mp3File = File(musicDir, "$baseName.mp3")
                        if (!mp3File.exists()) return@mapNotNull null
                        DownloadedTrack(
                            id = baseName,
                            title = json.optString("title", baseName),
                            artistName = json.optString("artistName", "Unknown Artist"),
                            coverUrl = json.optString("coverUrl", ""),
                            filePath = mp3File.absolutePath
                        )
                    } catch (_: Exception) {
                        null
                    }
                }.sortedByDescending { File(musicDir, "${it.id}.mp3").lastModified() }
            } else emptyList()
            _tracks.value = result
            _isRefreshing.value = false
        }
    }

    fun deleteTrack(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: return@launch
            File(musicDir, "$id.mp3").delete()
            File(musicDir, "$id.json").delete()
            refresh()
        }
    }

    fun playTrack(filePath: String) {
        // TODO: wire with player to play local file
    }
}
