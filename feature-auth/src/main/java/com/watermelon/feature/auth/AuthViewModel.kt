package com.watermelon.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isSuccess = false, needsEmailVerification = false) }
            val result = authRepository.signIn(email, password)
            if (result.isSuccess) {
                val verified = authRepository.isEmailVerified()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = verified,
                        needsEmailVerification = !verified,
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = false,
                        errorMessage = "Something went wrong. Please try again."
                    )
                }
            }
        }
    }

    fun signUp(username: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isSuccess = false, needsEmailVerification = false) }
            val result = authRepository.signUp(username, email, password)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSuccess = false,
                    needsEmailVerification = result.isSuccess,
                    errorMessage = if (result.isSuccess) null else "Something went wrong. Please try again."
                )
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, resetSent = false) }
            val result = authRepository.resetPassword(email)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    resetSent = result.isSuccess,
                    errorMessage = if (result.isSuccess) null else "Something went wrong. Please try again."
                )
            }
        }
    }

    fun resendVerificationEmail(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, resetSent = false) }
            val result = authRepository.resendVerificationEmail(email)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    resetSent = result.isSuccess,
                    errorMessage = if (result.isSuccess) null else "Something went wrong. Please try again."
                )
            }
        }
    }

    suspend fun isEmailVerified(): Boolean {
        return authRepository.isEmailVerified()
    }

    suspend fun getCurrentEmail(): String? = authRepository.getCurrentUserEmail()

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update { AuthUiState() }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(errorMessage = null, isSuccess = false, resetSent = false, needsEmailVerification = false) }
    }

    val isAuthenticated: StateFlow<Boolean?> = authRepository.isAuthenticated()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val needsEmailVerification: Boolean = false,
    val resetSent: Boolean = false,
    val errorMessage: String? = null
)
