package com.watermelon.domain.repository

import com.watermelon.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
    suspend fun resetPassword(email: String): Result<Unit>
    fun isAuthenticated(): Flow<Boolean>
    fun getCurrentUser(): Flow<User?>
    suspend fun getCurrentUserId(): String?
}
