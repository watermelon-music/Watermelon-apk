package com.watermelon.app.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object KillSwitchConfig {
    private val _disableYouTube = MutableStateFlow(false)
    val disableYouTube: StateFlow<Boolean> = _disableYouTube.asStateFlow()

    private val _disableAudius = MutableStateFlow(false)
    val disableAudius: StateFlow<Boolean> = _disableAudius.asStateFlow()

    private val _disableJamendo = MutableStateFlow(false)
    val disableJamendo: StateFlow<Boolean> = _disableJamendo.asStateFlow()

    private val _forceUpdate = MutableStateFlow(false)
    val forceUpdate: StateFlow<Boolean> = _forceUpdate.asStateFlow()

    private val _freeMaxPlaylists = MutableStateFlow(3)
    val freeMaxPlaylists: StateFlow<Int> = _freeMaxPlaylists.asStateFlow()

    private val _maintenanceMode = MutableStateFlow(false)
    val maintenanceMode: StateFlow<Boolean> = _maintenanceMode.asStateFlow()

    fun update(
        disableYouTube: Boolean = _disableYouTube.value,
        disableAudius: Boolean = _disableAudius.value,
        disableJamendo: Boolean = _disableJamendo.value,
        forceUpdate: Boolean = _forceUpdate.value,
        freeMaxPlaylists: Int = _freeMaxPlaylists.value,
        maintenanceMode: Boolean = _maintenanceMode.value
    ) {
        _disableYouTube.value = disableYouTube
        _disableAudius.value = disableAudius
        _disableJamendo.value = disableJamendo
        _forceUpdate.value = forceUpdate
        _freeMaxPlaylists.value = freeMaxPlaylists
        _maintenanceMode.value = maintenanceMode
    }
}
