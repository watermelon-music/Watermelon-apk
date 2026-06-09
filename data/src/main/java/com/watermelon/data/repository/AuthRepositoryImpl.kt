package com.watermelon.data.repository

import android.content.Context
import com.watermelon.data.BuildConfig
import com.watermelon.data.remote.supabase.model.ProfileRow
import com.watermelon.domain.model.SubscriptionPlan
import com.watermelon.domain.model.User
import com.watermelon.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun signUp(email: String, password: String): Result<Unit> = runCatching {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        Unit
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        Unit
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        client.auth.signOut()
        prefs.edit().clear().apply()
        Unit
    }

    override suspend fun resetPassword(email: String): Result<Unit> = runCatching {
        client.auth.resetPasswordForEmail(email)
    }

    override fun isAuthenticated(): Flow<Boolean> {
        return client.auth.sessionStatus.map { it is SessionStatus.Authenticated }
    }

    override fun getCurrentUser(): Flow<User?> {
        return client.auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    val supaUser = status.session.user ?: return@map fallbackLocalUser()
                    val profile = runCatching {
                        client.postgrest.from("profiles")
                            .select { filter { eq("id", supaUser.id) } }
                            .decodeSingleOrNull<ProfileRow>()
                    }.getOrNull()

                    User(
                        id = supaUser.id,
                        email = supaUser.email ?: "",
                        displayName = profile?.display_name
                            ?: supaUser.email?.substringBefore("@")
                            ?: "User",
                        avatarUrl = profile?.avatar_url
                            ?: "https://api.dicebear.com/10.x/identicon/png?seed=${supaUser.email ?: supaUser.id}",
                        plan = runCatching {
                            SubscriptionPlan.valueOf(profile?.plan ?: "FREE")
                        }.getOrDefault(SubscriptionPlan.FREE)
                    )
                }
                else -> fallbackLocalUser()
            }
        }
    }

    override suspend fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id ?: fallbackLocalUser()?.id
    }

    private fun fallbackLocalUser(): User? {
        if (!prefs.getBoolean(KEY_LOGGED_IN, false)) return null
        val email = prefs.getString(KEY_EMAIL, "user@watermelon.app") ?: "user@watermelon.app"
        val planName = prefs.getString(KEY_PLAN, SubscriptionPlan.FREE.name)
        return User(
            id = "local_user",
            email = email,
            displayName = "User",
            avatarUrl = "https://api.dicebear.com/10.x/identicon/png?seed=$email",
            plan = SubscriptionPlan.valueOf(planName ?: SubscriptionPlan.FREE.name)
        )
    }

    companion object {
        private const val PREFS_NAME = "watermelon_auth"
        private const val KEY_LOGGED_IN = "is_logged_in"
        private const val KEY_EMAIL = "auth_email"
        private const val KEY_PLAN = "auth_plan"
    }
}
