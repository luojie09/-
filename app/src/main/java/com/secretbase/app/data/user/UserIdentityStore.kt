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

    private companion object {
        private const val PREFS_NAME = "secret_base_identity"
        private const val KEY_CURRENT_USER_ID = "current_user_id"
    }
}
