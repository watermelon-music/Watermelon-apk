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
import timber.log.Timber
import javax.inject.Inject

/** Converts a raw Supabase/network exception into a user-friendly message. */
private fun Throwable.toUserMessage(): String {
    val msg = message?.lowercase() ?: ""
    return when {
        msg.contains("invalid login") || msg.contains("invalid credentials") ||
            msg.contains("email not confirmed").not() && msg.contains("invalid") ->
            "Invalid email or password. Please check your credentials."
        msg.contains("email not confirmed") ->
            "Please verify your email before signing in."
        msg.contains("user already registered") || msg.contains("already registered") ->
            "An account with this email already exists. Try signing in instead."
        msg.contains("password") && msg.contains("short") ->
            "Password must be at least 6 characters."
        msg.contains("network") || msg.contains("unable to resolve") || msg.contains("timeout") ->
            "Network error. Please check your connection and try again."
        msg.contains("rate limit") || msg.contains("too many") ->
            "Too many attempts. Please wait a moment and try again."
        else -> message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
    }
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        // Input validation
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email and password.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isSuccess = false, needsEmailVerification = false) }
            val result = authRepository.signIn(email.trim(), password)
            if (result.isSuccess) {
                val verified = authRepository.isEmailVerified()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = verified,
                        needsEmailVerification = !verified,
                        errorMessage = if (!verified) "Please verify your email before signing in." else null
                    )
                }
            } else {
                val ex = result.exceptionOrNull()
                Timber.e(ex, "signIn failed")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = false,
                        errorMessage = ex?.toUserMessage() ?: "Sign in failed. Please try again."
                    )
                }
            }
        }
    }

    fun signUp(username: String, email: String, password: String) {
        // Input validation
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please fill in all fields.") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isSuccess = false, needsEmailVerification = false) }
            val result = authRepository.signUp(username.trim(), email.trim(), password)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = false,
                        needsEmailVerification = true,
                        errorMessage = null
                    )
                }
            } else {
                val ex = result.exceptionOrNull()
                Timber.e(ex, "signUp failed")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = false,
                        needsEmailVerification = false,
                        errorMessage = ex?.toUserMessage() ?: "Sign up failed. Please try again."
                    )
                }
            }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email address.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, resetSent = false) }
            val result = authRepository.resetPassword(email.trim())
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, resetSent = true, errorMessage = null) }
            } else {
                val ex = result.exceptionOrNull()
                Timber.e(ex, "resetPassword failed")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        resetSent = false,
                        errorMessage = ex?.toUserMessage() ?: "Failed to send reset email. Please try again."
                    )
                }
            }
        }
    }

    fun resendVerificationEmail(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, resetSent = false) }
            val result = authRepository.resendVerificationEmail(email.trim())
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, resetSent = true, errorMessage = null) }
            } else {
                val ex = result.exceptionOrNull()
                Timber.e(ex, "resendVerification failed")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        resetSent = false,
                        errorMessage = ex?.toUserMessage() ?: "Failed to resend email. Please try again."
                    )
                }
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
