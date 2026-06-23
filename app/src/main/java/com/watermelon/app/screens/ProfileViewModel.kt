package com.watermelon.app.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.domain.model.AchievementBadge
import com.watermelon.domain.model.ProfileStats
import com.watermelon.domain.model.User
import com.watermelon.domain.repository.AuthRepository
import com.watermelon.domain.repository.ProfileStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileStatsRepository: ProfileStatsRepository,
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _profileStats = MutableStateFlow<ProfileStats?>(null)
    val profileStats: StateFlow<ProfileStats?> = _profileStats.asStateFlow()

    private val _achievements = MutableStateFlow<List<AchievementBadge>>(emptyList())
    val achievements: StateFlow<List<AchievementBadge>> = _achievements.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _editState = MutableStateFlow(EditState())
    val editState: StateFlow<EditState> = _editState.asStateFlow()

    init {
        loadUser()
        loadProfileStats()
        loadAchievements()
    }

    private fun loadUser() {
        authRepository.getCurrentUser()
            .onEach { user ->
                _user.value = user
                _isPremium.value = user?.plan?.name == "PREMIUM"
            }
            .catch { e -> Timber.e(e, "getCurrentUser flow error") }
            .launchIn(viewModelScope)
    }

    private fun loadProfileStats() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching { profileStatsRepository.getProfileStats().getOrThrow() }
            result.onSuccess { _profileStats.value = it }
                .onFailure {
                    Timber.e(it, "Profile stats fetch failed")
                    _profileStats.value = ProfileStats()
                }
            _isLoading.value = false
        }
    }

    private fun loadAchievements() {
        viewModelScope.launch {
            val result = runCatching { profileStatsRepository.getAchievements().getOrThrow() }
            result.onSuccess { _achievements.value = it }
                .onFailure { Timber.e(it, "Achievements fetch failed") }
        }
    }

    fun toggleEdit() {
        val current = _user.value
        _editState.update {
            it.copy(
                isEditing = !it.isEditing,
                displayName = current?.displayName ?: "",
                username = current?.username ?: "",
                avatarUrl = current?.avatarUrl ?: ""
            )
        }
    }

    fun setDisplayName(name: String) {
        _editState.update { it.copy(displayName = name) }
    }

    fun setUsername(name: String) {
        _editState.update { it.copy(username = name) }
    }

    fun saveProfile() {
        viewModelScope.launch {
            _editState.update { it.copy(isSaving = true, error = null) }
            val state = _editState.value
            var error: String? = null

            if (state.displayName.isNotBlank()) {
                authRepository.updateDisplayName(state.displayName.trim())
                    .onFailure { error = "Failed to update display name" }
            }
            if (state.username.isNotBlank()) {
                authRepository.updateUsername(state.username.trim())
                    .onFailure { error = "Failed to update username" }
            }
            if (state.avatarUrl.isNotBlank() && state.avatarUrl != _user.value?.avatarUrl) {
                authRepository.updateAvatar(state.avatarUrl)
                    .onFailure { error = "Failed to update avatar" }
            }

            if (error == null) {
                authRepository.refreshUser()?.let { _user.value = it }
            }
            _editState.update {
                it.copy(
                    isSaving = false,
                    isEditing = error != null,
                    error = error
                )
            }
        }
    }

    fun updateAvatar(seed: String, style: String = "toon-head") {
        val url = "https://api.dicebear.com/10.x/$style/svg?seed=$seed"
        _editState.update { it.copy(avatarUrl = url) }
    }

    fun setAvatarUrl(url: String) {
        _editState.update { it.copy(avatarUrl = url) }
    }

    fun clearEditError() {
        _editState.update { it.copy(error = null) }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun calculateLevelProgress(xpTotal: Long, level: Int): Float {
        val currentLevelBase = (level * level * 100).toLong()
        val nextLevelBase = ((level + 1) * (level + 1) * 100).toLong()
        val xpIntoLevel = xpTotal - currentLevelBase
        val xpNeeded = nextLevelBase - currentLevelBase
        return if (xpNeeded > 0) (xpIntoLevel.toFloat() / xpNeeded.toFloat()).coerceIn(0f, 1f) else 1f
    }
}

data class EditState(
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val displayName: String = "",
    val username: String = "",
    val avatarUrl: String = "",
    val error: String? = null
)