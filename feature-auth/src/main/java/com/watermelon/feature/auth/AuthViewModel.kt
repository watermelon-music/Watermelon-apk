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

/** Short user-facing error messages. */
private fun Throwable.toUserMessage(): String {
    Timber.e(this, "Auth error [${this::class.simpleName}]: $message")
    val msg = message?.lowercase() ?: ""
    return when {
        msg.contains("invalid login") || msg.contains("invalid credentials") -> "Wrong email or password"
        msg.contains("email not confirmed") -> "Verify your email first"
        msg.contains("already registered") || msg.contains("already been registered") ||
            msg.contains("user already") -> "Account already exists"
        msg.contains("password") && (msg.contains("short") || msg.contains("least")) -> "Password too short (min 6)"
        msg.contains("password") && msg.contains("weak") -> "Password is too weak"
        msg.contains("network") || msg.contains("unable to resolve") || msg.contains("timeout") ||
            msg.contains("failed to connect") || msg.contains("connection refused") ||
            msg.contains("no address") || msg.contains("unreachable") -> "No internet connection"
        msg.contains("rate limit") || msg.contains("too many") || msg.contains("429") ||
            msg.contains("over_email_send_rate") || msg.contains("email rate") ->
            "Email rate limit hit. Try again in 1 hour."
        msg.contains("empty") && msg.contains("url") -> "Server not configured"
        msg.contains("ssl") || msg.contains("certificate") || msg.contains("handshake") -> "Connection error"
        msg.contains("401") || msg.contains("unauthorized") -> "Wrong email or password"
        msg.contains("403") || msg.contains("forbidden") -> "Access denied"
        msg.contains("invalid_email") || msg.contains("invalid email") -> "Invalid email format"
        msg.contains("invalid redirect") || msg.contains("redirect_to") ->
            "App not allowlisted in Supabase redirect URLs"
        // Surface the actual Supabase signup-trigger error so the operator can fix the SQL trigger
        msg.contains("database error saving new user") ->
            "Sign-up trigger failed (Supabase). Check on_auth_user_created."
        msg.contains("unexpected_failure") -> "Email send failed (Supabase SMTP). Check Resend/SMTP."
        msg.contains("signup") && msg.contains("disabled") -> "Registration is disabled"
        msg.contains("json") || msg.contains("parse") || msg.contains("serializ") -> "Server replied invalid JSON. Try again."
        msg.contains("500") || msg.contains("internal server") -> {
            // Include a short hint from the raw message so the actual cause is visible
            val hint = message?.takeIf { it.isNotBlank() }?.take(140) ?: "Server error"
            "Server: $hint"
        }
        else -> message?.takeIf { it.isNotBlank() }?.take(140) ?: "Try again later"
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
            _uiState.update { it.copy(errorMessage = "Enter email and password") }
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
                        errorMessage = if (!verified) "Verify your email first" else null
                    )
                }
            } else {
                val ex = result.exceptionOrNull()
                Timber.e(ex, "signIn failed")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = false,
                        errorMessage = ex?.toUserMessage() ?: "Sign in failed"
                    )
                }
            }
        }
    }

    fun signUp(username: String, email: String, password: String) {
        // Input validation
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Fill in all fields") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password too short (min 6)") }
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
                        errorMessage = ex?.toUserMessage() ?: "Sign up failed"
                    )
                }
            }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter your email") }
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
                        errorMessage = ex?.toUserMessage() ?: "Reset failed"
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
                        errorMessage = ex?.toUserMessage() ?: "Resend failed"
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
