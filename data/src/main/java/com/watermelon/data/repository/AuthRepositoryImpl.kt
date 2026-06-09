package com.watermelon.data.repository

import android.content.Context
import com.watermelon.domain.model.User
import com.watermelon.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun signUp(email: String, password: String): Result<Unit> {
        prefs.edit().putBoolean(KEY_LOGGED_IN, true).apply()
        return Result.success(Unit)
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        prefs.edit().putBoolean(KEY_LOGGED_IN, true).apply()
        return Result.success(Unit)
    }

    override suspend fun signOut(): Result<Unit> {
        prefs.edit().putBoolean(KEY_LOGGED_IN, false).apply()
        return Result.success(Unit)
    }

    override suspend fun resetPassword(email: String): Result<Unit> =
        Result.success(Unit)

    override fun isAuthenticated(): Flow<Boolean> =
        flowOf(prefs.getBoolean(KEY_LOGGED_IN, false))

    override fun getCurrentUser(): Flow<User?> = flowOf(null)

    override suspend fun getCurrentUserId(): String? {
        return null // TODO: return Firebase UID or local user ID when auth is fully wired
    }

    companion object {
        private const val PREFS_NAME = "watermelon_auth"
        private const val KEY_LOGGED_IN = "is_logged_in"
    }
}
