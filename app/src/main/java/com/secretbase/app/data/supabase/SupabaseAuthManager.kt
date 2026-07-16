package com.secretbase.app.data.supabase

import com.secretbase.app.data.message.SecretBaseUsers
import com.secretbase.app.data.user.StoredAuthSession
import com.secretbase.app.data.user.UserIdentityStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class SupabaseAuthManager(
    private val identityStore: UserIdentityStore,
) {
    private val sessionMutex = Mutex()

    suspend fun bindIdentity(
        role: String,
        pairingCode: String,
    ): Result<BoundIdentity> = runCatching {
        require(SecretBaseUsers.isSupported(role)) { "请选择正确的身份" }
        require(pairingCode.isNotBlank()) { "请输入配对码" }

        sessionMutex.withLock {
            val session = validSessionLocked(createWhenMissing = true)
            identityStore.saveAuthSession(session)
            val membership = joinSecretBase(
                accessToken = session.accessToken,
                role = role,
                pairingCode = pairingCode.trim(),
            )
            identityStore.saveAuthenticatedIdentity(
                userId = membership.role,
                coupleId = membership.coupleId,
                session = session,
            )
            BoundIdentity(
                role = membership.role,
                coupleId = membership.coupleId,
            )
        }
    }

    suspend fun validAccessToken(): String = sessionMutex.withLock {
        validSessionLocked(createWhenMissing = false).accessToken
    }

    private suspend fun validSessionLocked(createWhenMissing: Boolean): StoredAuthSession {
        val stored = identityStore.authSession()
        val now = System.currentTimeMillis() / 1_000L
        if (stored != null && stored.expiresAtEpochSeconds > now + REFRESH_MARGIN_SECONDS) {
            return stored
        }

        if (stored != null) {
            return refreshSession(stored.refreshToken).also(identityStore::saveAuthSession)
        }

        check(createWhenMissing) { "身份会话已失效，请重新绑定" }
        return createAnonymousSession().also(identityStore::saveAuthSession)
    }

    private suspend fun createAnonymousSession(): StoredAuthSession {
        val response = authRequest(
            path = "signup",
            body = JSONObject().put("data", JSONObject()).toString(),
        )
        return parseAuthSessionResponse(response)
    }

    private suspend fun refreshSession(refreshToken: String): StoredAuthSession {
        val response = authRequest(
            path = "token?grant_type=refresh_token",
            body = JSONObject().put("refresh_token", refreshToken).toString(),
        )
        return parseAuthSessionResponse(response)
    }

    private suspend fun joinSecretBase(
        accessToken: String,
        role: String,
        pairingCode: String,
    ): BoundIdentity {
        val response = restRequest(
            path = "rpc/join_secret_base",
            accessToken = accessToken,
            body = JSONObject()
                .put("p_pairing_code", pairingCode)
                .put("p_role", role)
                .toString(),
        )
        return parseBoundIdentityResponse(response)
    }

    private suspend fun authRequest(
        path: String,
        body: String,
    ): JSONObject = withContext(Dispatchers.IO) {
        val connection = openConnection("${SupabaseConfig.url}/auth/v1/$path")
        connection.requestMethod = "POST"
        connection.setRequestProperty("apikey", SupabaseConfig.publishableKey)
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.doOutput = true
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        JSONObject(connection.readResponse("Supabase authentication"))
    }

    private suspend fun restRequest(
        path: String,
        accessToken: String,
        body: String,
    ): String = withContext(Dispatchers.IO) {
        val connection = openConnection("${SupabaseConfig.url}/rest/v1/$path")
        connection.requestMethod = "POST"
        connection.setRequestProperty("apikey", SupabaseConfig.publishableKey)
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.doOutput = true
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        connection.readResponse("Secret base pairing")
    }

    private fun openConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = REQUEST_TIMEOUT_MS
            readTimeout = REQUEST_TIMEOUT_MS
        }

    private fun HttpURLConnection.readResponse(operation: String): String {
        val status = responseCode
        val response = runCatching {
            val stream = if (status in 200..299) inputStream else errorStream
            stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        }.getOrDefault("")
        disconnect()

        if (status !in 200..299) {
            val serverMessage = runCatching {
                JSONObject(response).optString("message")
                    .ifBlank { JSONObject(response).optString("error_description") }
            }.getOrDefault("")
            val message = when {
                serverMessage.contains("anonymous", ignoreCase = true) &&
                    serverMessage.contains("disabled", ignoreCase = true) ->
                    "Supabase 尚未启用匿名登录，请先在控制台打开 Anonymous Sign-Ins"

                else -> serverMessage.ifBlank { "$operation failed with HTTP $status" }
            }
            throw IOException(message)
        }
        return response
    }

    private companion object {
        private const val REQUEST_TIMEOUT_MS = 20_000
        private const val REFRESH_MARGIN_SECONDS = 60L
        private const val DEFAULT_SESSION_SECONDS = 3_600L
    }
}

data class BoundIdentity(
    val role: String,
    val coupleId: String,
)

internal fun parseAuthSessionResponse(
    response: JSONObject,
    nowEpochSeconds: Long = System.currentTimeMillis() / 1_000L,
): StoredAuthSession {
    val expiresIn = response.optLong("expires_in", 3_600L)
    val userId = response.optJSONObject("user")?.optString("id").orEmpty()
    if (userId.isBlank()) throw IOException("Supabase 没有返回用户身份")
    return StoredAuthSession(
        accessToken = response.getString("access_token"),
        refreshToken = response.getString("refresh_token"),
        userId = userId,
        expiresAtEpochSeconds = nowEpochSeconds + expiresIn,
    )
}

internal fun parseBoundIdentityResponse(response: String): BoundIdentity {
    val json = JSONObject(response)
    val role = json.getString("role")
    require(SecretBaseUsers.isSupported(role)) { "Supabase 返回了无效身份" }
    return BoundIdentity(
        role = role,
        coupleId = json.getString("couple_id"),
    )
}
