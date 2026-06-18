package com.watermelon.domain.repository

import com.watermelon.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signUp(username: String, email: String, password: String): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun resendVerificationEmail(email: String): Result<Unit>
    suspend fun isEmailVerified(): Boolean
    suspend fun updateDisplayName(name: String): Result<Unit>
    suspend fun updateUsername(name: String): Result<Unit>
    suspend fun updateAvatar(url: String): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
    suspend fun refreshUser(): User?
    fun isAuthenticated(): Flow<Boolean>
    fun getCurrentUser(): Flow<User?>
    suspend fun getCurrentUserId(): String?
    suspend fun getCurrentUserEmail(): String?
    suspend fun getCurrentAccessToken(): String?
    suspend fun fetchLatestActiveBroadcast(): com.watermelon.domain.model.Broadcast?
}
