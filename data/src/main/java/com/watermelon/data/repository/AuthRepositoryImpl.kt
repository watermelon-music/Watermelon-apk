package com.watermelon.data.repository

import com.watermelon.domain.model.User
import com.watermelon.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor() : AuthRepository {

    override suspend fun signUp(email: String, password: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun signIn(email: String, password: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun signOut(): Result<Unit> =
        Result.success(Unit)

    override suspend fun resetPassword(email: String): Result<Unit> =
        Result.success(Unit)

    override fun isAuthenticated(): Flow<Boolean> = flowOf(false)

    override fun getCurrentUser(): Flow<User?> = flowOf(null)
}
