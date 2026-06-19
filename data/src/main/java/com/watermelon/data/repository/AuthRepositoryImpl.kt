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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient
) : AuthRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun signUp(username: String, email: String, password: String): Result<Unit> = runCatching {
        client.auth.signUpWith(Email, redirectUrl = "https://watermelon-api-oxx2.onrender.com/confirm") {
            this.email = email
            this.password = password
            this.data = buildJsonObject {
                put("username", username)
                put("display_name", username)
            }
        }
        val session = client.auth.currentSessionOrNull()
        val hasSession = session != null
        val verified = session?.user?.emailConfirmedAt != null
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, hasSession)
            .putBoolean(KEY_EMAIL_VERIFIED, verified)
            .putString(KEY_EMAIL, email)
            .apply()
        Unit
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val session = client.auth.currentSessionOrNull()
        val hasSession = session != null
        val verified = session?.user?.emailConfirmedAt != null
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, hasSession)
            .putBoolean(KEY_EMAIL_VERIFIED, verified)
            .putString(KEY_EMAIL, email)
            .apply()
        Unit
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        client.auth.signOut()
        prefs.edit().clear().apply()
        Unit
    }

    override suspend fun resetPassword(email: String): Result<Unit> = runCatching {
        client.auth.resetPasswordForEmail(email, redirectUrl = "watermelon://reset-password")
    }

    override suspend fun resendVerificationEmail(email: String): Result<Unit> = runCatching {
        val body = okhttp3.FormBody.Builder()
            .add("type", "signup")
            .add("email", email)
            .build()
        val request = Request.Builder()
            .url("${BuildConfig.SUPABASE_URL}/auth/v1/resend")
            .addHeader("apikey", BuildConfig.SUPABASE_KEY)
            .post(body)
            .build()
        val response = withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute()
        }
        if (!response.isSuccessful) {
            throw IllegalStateException("Resend failed: ${response.code}")
        }
        Unit
    }

    override suspend fun isEmailVerified(): Boolean {
        // Force a server round-trip so we see the *current* email_confirmed_at
        // value — the SDK-cached user object is stale right after a signup or
        // after the user clicks the email link in another app.
        runCatching {
            client.auth.refreshCurrentSession()
        }
        val user = client.auth.currentUserOrNull()
        if (user != null) {
            val verified = user.emailConfirmedAt != null
            prefs.edit().putBoolean(KEY_EMAIL_VERIFIED, verified).apply()
            return verified
        }
        // No live session — fall back to the cached flag so an offline open
        // doesn't kick a previously-verified user back to the verify screen.
        return prefs.getBoolean(KEY_EMAIL_VERIFIED, false)
    }

    override suspend fun updateDisplayName(name: String): Result<Unit> = runCatching {
        val uid = getCurrentUserId() ?: throw IllegalStateException("Not logged in")
        if (uid == "local_user") {
            prefs.edit().putString(KEY_DISPLAY_NAME, name).apply()
        } else {
            client.postgrest.from("profiles").update({
                set("display_name", name)
            }) {
                filter { eq("id", uid) }
            }
        }
        Unit
    }

    override suspend fun updateUsername(name: String): Result<Unit> = runCatching {
        val uid = getCurrentUserId() ?: throw IllegalStateException("Not logged in")
        if (uid == "local_user") {
            prefs.edit().putString(KEY_USERNAME, name).apply()
        } else {
            client.postgrest.from("profiles").update({
                set("username", name)
            }) {
                filter { eq("id", uid) }
            }
        }
        Unit
    }

    override suspend fun updateAvatar(url: String): Result<Unit> = runCatching {
        val uid = getCurrentUserId() ?: throw IllegalStateException("Not logged in")
        if (uid == "local_user") {
            prefs.edit().putString(KEY_AVATAR_URL, url).apply()
        } else {
            client.postgrest.from("profiles").update({
                set("avatar_url", url)
            }) {
                filter { eq("id", uid) }
            }
        }
        Unit
    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        val token = getCurrentAccessToken() ?: throw IllegalStateException("No session")
        val base = BuildConfig.SUPABASE_URL.removeSuffix("/")
        val url = "$base/auth/v1/user"
        val body = "{}".toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("apikey", BuildConfig.SUPABASE_KEY)
            .addHeader("Content-Type", "application/json")
            .delete(body)
            .build()
        val response = withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute()
        }
        if (!response.isSuccessful) {
            val bodyString = response.body?.string() ?: ""
            throw IllegalStateException("Delete failed: ${response.code} - $bodyString")
        }
        response.body?.close()
        signOut()
        Unit
    }

    override fun isAuthenticated(): Flow<Boolean> {
        // Only trust Supabase's actual session status. Drop intermediate states
        // (LoadingFromStorage / NetworkError) so the splash screen waits until
        // we know for sure rather than racing on a stale local pref.
        return client.auth.sessionStatus
            .filter { status ->
                status is SessionStatus.Authenticated || status is SessionStatus.NotAuthenticated
            }
            .map { status ->
                val authed = status is SessionStatus.Authenticated
                // Keep the local pref mirror in sync for legacy callers.
                prefs.edit().putBoolean(KEY_LOGGED_IN, authed).apply()
                authed
            }
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
                        username = profile?.username
                            ?: supaUser.email?.substringBefore("@")
                            ?: "User",
                        displayName = profile?.display_name
                            ?: supaUser.email?.substringBefore("@")
                            ?: "User",
                        avatarUrl = profile?.avatar_url
                            ?: "https://api.dicebear.com/10.x/toon-head/svg?seed=${profile?.username ?: supaUser.email ?: supaUser.id}",
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

    override suspend fun getCurrentUserEmail(): String? {
        return client.auth.currentUserOrNull()?.email ?: fallbackLocalUser()?.email
    }

    override suspend fun refreshUser(): User? {
        val supaUser = client.auth.currentUserOrNull() ?: return fallbackLocalUser()
        val profile = runCatching {
            client.postgrest.from("profiles")
                .select { filter { eq("id", supaUser.id) } }
                .decodeSingleOrNull<ProfileRow>()
        }.getOrNull()
        return User(
            id = supaUser.id,
            email = supaUser.email ?: "",
            username = profile?.username
                ?: supaUser.email?.substringBefore("@")
                ?: "User",
            displayName = profile?.display_name
                ?: supaUser.email?.substringBefore("@")
                ?: "User",
            avatarUrl = profile?.avatar_url
                ?: "https://api.dicebear.com/10.x/toon-head/svg?seed=${profile?.username ?: supaUser.email ?: supaUser.id}",
            plan = runCatching {
                SubscriptionPlan.valueOf(profile?.plan ?: "FREE")
            }.getOrDefault(SubscriptionPlan.FREE)
        )
    }

    override suspend fun getCurrentAccessToken(): String? {
        return client.auth.currentSessionOrNull()?.accessToken
    }

    private fun fallbackLocalUser(): User? {
        if (!prefs.getBoolean(KEY_LOGGED_IN, false)) return null
        val email = prefs.getString(KEY_EMAIL, "user@watermelon.app") ?: "user@watermelon.app"
        val planName = prefs.getString(KEY_PLAN, SubscriptionPlan.FREE.name)
        val defaultUsername = email.substringBefore("@")
        return User(
            id = "local_user",
            email = email,
            username = prefs.getString(KEY_USERNAME, defaultUsername) ?: defaultUsername,
            displayName = prefs.getString(KEY_DISPLAY_NAME, "User") ?: "User",
            avatarUrl = prefs.getString(KEY_AVATAR_URL, "https://api.dicebear.com/10.x/toon-head/svg?seed=$email") ?: "https://api.dicebear.com/10.x/toon-head/svg?seed=$email",
            plan = runCatching {
                SubscriptionPlan.valueOf(planName ?: SubscriptionPlan.FREE.name)
            }.getOrDefault(SubscriptionPlan.FREE)
        )
    }

    override suspend fun fetchLatestActiveBroadcast(): com.watermelon.domain.model.Broadcast? = runCatching {
        val row = client.postgrest.from("broadcasts")
            .select {
                filter { eq("active", true) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(1)
            }
            .decodeList<BroadcastRow>()
            .firstOrNull() ?: return null

        com.watermelon.domain.model.Broadcast(
            id = row.id,
            message = row.message,
            sender = row.sender,
            active = row.active,
            createdAt = row.created_at
        )
    }.onFailure { timber.log.Timber.e(it, "fetchLatestActiveBroadcast failed") }.getOrNull()

    companion object {
        private const val PREFS_NAME = "watermelon_auth"
        private const val KEY_LOGGED_IN = "is_logged_in"
        private const val KEY_EMAIL_VERIFIED = "auth_email_verified"
        private const val KEY_EMAIL = "auth_email"
        private const val KEY_PLAN = "auth_plan"
        private const val KEY_DISPLAY_NAME = "auth_display_name"
        private const val KEY_USERNAME = "auth_username"
        private const val KEY_AVATAR_URL = "auth_avatar_url"
    }
}

@Serializable
private data class BroadcastRow(
    val id: Long,
    val message: String,
    val sender: String,
    val active: Boolean,
    val created_at: String
)
