package com.watermelon.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.data.local.dao.CachedSongDao
import com.watermelon.domain.autoplay.AutoplayEngine
import com.watermelon.domain.model.User
import com.watermelon.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val cachedSongDao: CachedSongDao,
    private val autoplayEngine: AutoplayEngine
) : ViewModel() {

    val user: StateFlow<User?> = authRepository.getCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _autoplayEnabled = MutableStateFlow(autoplayEngine.isAutoplayEnabled())
    val autoplayEnabled: StateFlow<Boolean> = _autoplayEnabled.asStateFlow()

    private val _cacheCleared = MutableStateFlow(false)
    val cacheCleared: StateFlow<Boolean> = _cacheCleared.asStateFlow()

    private val _deleteState = MutableStateFlow(DeleteAccountState())
    val deleteState: StateFlow<DeleteAccountState> = _deleteState.asStateFlow()

    fun setAutoplayEnabled(enabled: Boolean) {
        autoplayEngine.setAutoplayEnabled(enabled)
        _autoplayEnabled.value = enabled
    }

    fun clearHistory() {
        viewModelScope.launch {
            autoplayEngine.clearAll()
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onComplete()
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            cachedSongDao.clearAll()
            _cacheCleared.value = true
        }
    }

    fun resetCacheFlag() {
        _cacheCleared.value = false
    }

    fun deleteAccount(onComplete: () -> Unit) {
        viewModelScope.launch {
            _deleteState.update { it.copy(isDeleting = true, error = null) }
            val result = authRepository.deleteAccount()
            if (result.isSuccess) {
                _deleteState.update { DeleteAccountState(deleted = true) }
                onComplete()
            } else {
                val ex = result.exceptionOrNull()
                val msg = ex?.message?.takeIf { it.isNotBlank() }
                    ?: "Failed to delete account. Try again."
                _deleteState.update { it.copy(isDeleting = false, error = msg) }
            }
        }
    }

    fun clearDeleteError() {
        _deleteState.update { it.copy(error = null) }
    }
}

data class DeleteAccountState(
    val isDeleting: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null
)
