package com.watermelon.app.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.domain.model.User
import com.watermelon.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    init {
        authRepository.getCurrentUser()
            .onEach { _user.value = it }
            .launchIn(viewModelScope)
    }

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _editState = MutableStateFlow(EditState())
    val editState: StateFlow<EditState> = _editState.asStateFlow()

    fun toggleEdit() {
        val current = _user.value
        _editState.update {
            it.copy(
                isEditing = !it.isEditing,
                displayName = current?.displayName ?: "",
                username = current?.username ?: ""
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
        viewModelScope.launch {
            val url = "https://api.dicebear.com/10.x/$style/svg?seed=$seed"
            authRepository.updateAvatar(url)
                .onSuccess { authRepository.refreshUser()?.let { _user.value = it } }
        }
    }

    fun clearEditError() {
        _editState.update { it.copy(error = null) }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}

data class EditState(
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val displayName: String = "",
    val username: String = "",
    val error: String? = null
)
