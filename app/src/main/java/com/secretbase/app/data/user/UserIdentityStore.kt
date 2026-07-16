package com.secretbase.app.data.user

import android.content.Context
import com.secretbase.app.data.message.SecretBaseUsers

class UserIdentityStore(
    context: Context,
) {
    private val sharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun currentUserId(): String? =
        sharedPreferences
            .getString(KEY_CURRENT_USER_ID, null)
            ?.takeIf(SecretBaseUsers::isSupported)

    fun saveCurrentUserId(userId: String) {
        require(SecretBaseUsers.isSupported(userId)) {
            "Unsupported user id: $userId"
        }
        sharedPreferences.edit().putString(KEY_CURRENT_USER_ID, userId).apply()
    }

    fun coupleId(): String? =
        sharedPreferences
            .getString(KEY_COUPLE_ID, null)
            ?.takeIf { it.isNotBlank() }

    fun authSession(): StoredAuthSession? {
        val accessToken = sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val refreshToken = sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val userId = sharedPreferences.getString(KEY_AUTH_USER_ID, null)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val expiresAtEpochSeconds = sharedPreferences.getLong(KEY_EXPIRES_AT, 0L)
        if (expiresAtEpochSeconds <= 0L) return null

        return StoredAuthSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
            expiresAtEpochSeconds = expiresAtEpochSeconds,
        )
    }

    fun hasAuthenticatedIdentity(): Boolean =
        currentUserId() != null && coupleId() != null && authSession() != null

    fun saveAuthSession(session: StoredAuthSession) {
        sharedPreferences.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putString(KEY_AUTH_USER_ID, session.userId)
            .putLong(KEY_EXPIRES_AT, session.expiresAtEpochSeconds)
            .apply()
    }

    fun saveAuthenticatedIdentity(
        userId: String,
        coupleId: String,
        session: StoredAuthSession,
    ) {
        require(SecretBaseUsers.isSupported(userId)) {
            "Unsupported user id: $userId"
        }
        require(coupleId.isNotBlank()) { "Couple id must not be blank" }

        sharedPreferences.edit()
            .putString(KEY_CURRENT_USER_ID, userId)
            .putString(KEY_COUPLE_ID, coupleId)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putString(KEY_AUTH_USER_ID, session.userId)
            .putLong(KEY_EXPIRES_AT, session.expiresAtEpochSeconds)
            .apply()
    }

    fun clearAuthenticatedIdentity() {
        sharedPreferences.edit().clear().apply()
    }

    fun clearAuthSessionKeepingRole() {
        sharedPreferences.edit()
            .remove(KEY_COUPLE_ID)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_AUTH_USER_ID)
            .remove(KEY_EXPIRES_AT)
            .apply()
    }

    private companion object {
        private const val PREFS_NAME = "secret_base_identity"
        private const val KEY_CURRENT_USER_ID = "current_user_id"
        private const val KEY_COUPLE_ID = "couple_id"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_AUTH_USER_ID = "auth_user_id"
        private const val KEY_EXPIRES_AT = "expires_at_epoch_seconds"
    }
}

data class StoredAuthSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val expiresAtEpochSeconds: Long,
)
