package com.watermelon.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.domain.model.DownloadedSong
import com.watermelon.domain.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val tracks: StateFlow<List<DownloadedTrack>> = downloadRepository.getDownloads()
        .map { list -> list.map { it.toUiModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            downloadRepository.cleanupMissingFiles()
            _isRefreshing.value = false
        }
    }

    fun deleteTrack(id: String) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(id)
        }
    }

    fun playTrack(filePath: String) {
        // TODO: wire with player to play local file
    }
}

private fun DownloadedSong.toUiModel(): DownloadedTrack = DownloadedTrack(
    id = songId,
    title = title,
    artistName = artist,
    coverUrl = coverUrl ?: "",
    filePath = localFilePath
)
