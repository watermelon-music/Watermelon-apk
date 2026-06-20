package com.watermelon.data.repository

import android.content.Context
import com.watermelon.data.BuildConfig
import com.watermelon.data.remote.supabase.model.ProfileRow
import com.watermelon.domain.model.RemoteConfig
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
        client.auth.signUpWith(Email) {
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
        // If the user re-created an account in the same process after a
        // previous delete, the old deleted session may still be cached.
        if (!hasSession) {
            kotlin.runCatching { client.auth.signOut() }
        }
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, hasSession)
            .putBoolean(KEY_EMAIL_VERIFIED, verified)
            .putString(KEY_EMAIL, email)
            .putString(KEY_PASSWORD, password)
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

        val userId = session?.user?.id
        if (userId != null && isProfileBanned(userId)) {
            client.auth.signOut()
            prefs.edit().clear().apply()
            throw IllegalStateException("Account has been banned.")
        }

        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, hasSession)
            .putBoolean(KEY_EMAIL_VERIFIED, verified)
            .putString(KEY_EMAIL, email)
            .putString(KEY_PASSWORD, password)
            .apply()
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

    override suspend fun resendVerificationEmail(email: String): Result<Unit> = runCatching {
        val json = "{\"type\":\"signup\",\"email\":\"$email\"}"
        val jsonBody = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("${BuildConfig.SUPABASE_URL}/auth/v1/resend")
            .addHeader("apikey", BuildConfig.SUPABASE_KEY)
            .post(jsonBody)
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
        val fresh = runCatching { client.auth.refreshCurrentSession() }
        val user = client.auth.currentUserOrNull()
        if (user != null) {
            // Session exists. Make sure it isn't a ghost from a deleted account.
            if (fresh.isFailure) {
                val ex = fresh.exceptionOrNull()
                val msg = ex?.message?.lowercase() ?: ""
                val isNetworkError = ex is java.io.IOException ||
                    msg.contains("unable to resolve") ||
                    msg.contains("timeout") ||
                    msg.contains("connection") ||
                    msg.contains("unreachable") ||
                    msg.contains("ssl") ||
                    msg.contains("handshake") ||
                    msg.contains("network")
                if (!isNetworkError) {
                    // Auth-level failure (user deleted, token revoked) — purge
                    kotlin.runCatching { client.auth.signOut() }
                    prefs.edit().clear().apply()
                    return false
                }
            }
            val verified = user.emailConfirmedAt != null
            prefs.edit().putBoolean(KEY_EMAIL_VERIFIED, verified).apply()
            return verified
        }
        // No live session — most likely the signup didn't create one because
        // email confirmation is required. Auto-sign in with stored credentials
        // so the user actually has a real session before we let them into Home.
        return runCatching {
            val email = prefs.getString(KEY_EMAIL, null) ?: return false
            val password = prefs.getString(KEY_PASSWORD, null) ?: return false
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val newSession = client.auth.currentSessionOrNull()
            val hasSession = newSession != null
            val verified = newSession?.user?.emailConfirmedAt != null
            prefs.edit()
                .putBoolean(KEY_LOGGED_IN, hasSession)
                .putBoolean(KEY_EMAIL_VERIFIED, verified)
                .apply()
            if (verified) {
                prefs.edit().remove(KEY_PASSWORD).apply()
            }
            verified
        }.onFailure { timber.log.Timber.e(it, "Auto sign-in after verification failed") }
            .getOrDefault(false)
    }

    override suspend fun updateDisplayName(name: String): Result<Unit> = runCatching {
        val uid = getCurrentUserId() ?: throw IllegalStateException("Not logged in")
        client.postgrest.from("profiles").update({
            set("display_name", name)
        }) {
            filter { eq("id", uid) }
        }
        Unit
    }

    override suspend fun updateUsername(name: String): Result<Unit> = runCatching {
        val uid = getCurrentUserId() ?: throw IllegalStateException("Not logged in")
        client.postgrest.from("profiles").update({
            set("username", name)
        }) {
            filter { eq("id", uid) }
        }
        Unit
    }

    override suspend fun updateAvatar(url: String): Result<Unit> = runCatching {
        val uid = getCurrentUserId() ?: throw IllegalStateException("Not logged in")
        client.postgrest.from("profiles").update({
            set("avatar_url", url)
        }) {
            filter { eq("id", uid) }
        }
        Unit
    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        val token = getCurrentAccessToken() ?: throw IllegalStateException("No session")
        val base = BuildConfig.WATERMELON_API_URL.removeSuffix("/")
        val url = "$base/auth/delete-user"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .delete()
            .build()
        val response = withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute()
        }
        if (!response.isSuccessful) {
            val bodyString = response.body?.string() ?: ""
            throw IllegalStateException("Delete failed: ${response.code} - $bodyString")
        }
        response.body?.close()
        Unit
    }.also {
        // Wipe local state - the backend already deleted the user on Supabase.
        // MUST sign out to clear the in-memory session, otherwise the old
        // (now-deleted) user's session stays cached and corrupts all future
        // auth calls (FK violations on playlists, duplicate-delete failures, etc.)
        // .also runs unconditionally so the leak is fixed even when the
        // server-side delete fails (e.g. stale token for already-deleted user).
        kotlin.runCatching { client.auth.signOut() }
        prefs.edit().clear().apply()
    }

    override fun isAuthenticated(): Flow<Boolean> {
        return client.auth.sessionStatus
            .filter { status ->
                status is SessionStatus.Authenticated || status is SessionStatus.NotAuthenticated
            }
            .map { status ->
                val authed = status is SessionStatus.Authenticated
                if (authed) {
                    val verified = (status as SessionStatus.Authenticated)
                        .session.user?.emailConfirmedAt != null
                    prefs.edit()
                        .putBoolean(KEY_LOGGED_IN, true)
                        .putBoolean(KEY_EMAIL_VERIFIED, verified)
                        .apply()
                } else {
                    prefs.edit()
                        .putBoolean(KEY_LOGGED_IN, false)
                        .putBoolean(KEY_EMAIL_VERIFIED, false)
                        .apply()
                }
                authed
            }
    }

    override fun getCurrentUser(): Flow<User?> {
        return client.auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    val supaUser = status.session.user ?: return@map null
                    val profile = runCatching {
                        client.postgrest.from("profiles")
                            .select { filter { eq("id", supaUser.id) } }
                            .decodeSingleOrNull<ProfileRow>()
                    }.getOrNull()

                    if (profile?.is_banned == true) {
                        client.auth.signOut()
                        prefs.edit().clear().apply()
                        return@map null
                    }

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
                else -> null
            }
        }
    }

    private fun isProfileBanned(userId: String): Boolean {
        val profile = runCatching {
            client.postgrest.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<ProfileRow>()
        }.getOrNull()
        return profile?.is_banned == true
    }

    override suspend fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }

    override suspend fun getCurrentUserEmail(): String? {
        return client.auth.currentUserOrNull()?.email
            ?: prefs.getString(KEY_EMAIL, null)
    }

    override suspend fun refreshUser(): User? {
        val supaUser = client.auth.currentUserOrNull() ?: return null
        val profile = runCatching {
            client.postgrest.from("profiles")
                .select { filter { eq("id", supaUser.id) } }
                .decodeSingleOrNull<ProfileRow>()
        }.getOrNull()
        if (profile?.is_banned == true) {
            client.auth.signOut()
            prefs.edit().clear().apply()
            return null
        }
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

    override suspend fun checkRemoteConfig(): RemoteConfig? = runCatching {
        val row = client.postgrest.from("remote_config")
            .select()
            .limit(1)
            .decodeSingleOrNull<RemoteConfigRow>()
        if (row == null) return@runCatching null
        RemoteConfig(
            maintenanceMode = row.maintenance_mode,
            disableYouTube = row.disable_youtube,
            disableAudius = row.disable_audius,
            disableJamendo = row.disable_jamendo,
            freeMaxPlaylists = row.free_max_playlists
        )
    }.onFailure { timber.log.Timber.e(it, "checkRemoteConfig failed") }.getOrNull()

    companion object {
        private const val PREFS_NAME = "watermelon_auth"
        private const val KEY_LOGGED_IN = "is_logged_in"
        private const val KEY_EMAIL_VERIFIED = "auth_email_verified"
        private const val KEY_EMAIL = "auth_email"
        private const val KEY_PASSWORD = "auth_password_temp"
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

@Serializable
private data class RemoteConfigRow(
    val id: Int,
    val maintenance_mode: Boolean = false,
    val disable_youtube: Boolean = false,
    val disable_audius: Boolean = false,
    val disable_jamendo: Boolean = false,
    val free_max_playlists: Int = 3
)
