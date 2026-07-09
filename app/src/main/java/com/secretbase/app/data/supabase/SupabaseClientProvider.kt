package com.secretbase.app.data.supabase

import com.secretbase.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

object SupabaseConfig {
    val url: String get() = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
    val publishableKey: String get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()

    val isConfigured: Boolean
        get() = url.isNotBlank() && publishableKey.isNotBlank()
}

object SupabaseClientProvider {
    val client: SupabaseRestClient by lazy {
        check(SupabaseConfig.isConfigured) {
            "Supabase is not configured. Please set SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY."
        }
        SupabaseRestClient(
            supabaseUrl = SupabaseConfig.url,
            publishableKey = SupabaseConfig.publishableKey,
        )
    }
}

class SupabaseRestClient(
    private val supabaseUrl: String,
    private val publishableKey: String,
) {
    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend inline fun <reified T> select(
        table: String,
        query: String = "select=*",
    ): List<T> {
        val response = request(
            method = "GET",
            path = "$table?$query",
        )
        return json.decodeFromString(response)
    }

    suspend inline fun <reified T> insert(
        table: String,
        row: T,
    ) {
        request(
            method = "POST",
            path = table,
            body = json.encodeToString(row),
        )
    }

    suspend inline fun <reified T> update(
        table: String,
        query: String,
        row: T,
    ) {
        request(
            method = "PATCH",
            path = "$table?$query",
            body = json.encodeToString(row),
        )
    }

    suspend fun delete(
        table: String,
        query: String,
    ) {
        request(
            method = "DELETE",
            path = "$table?$query",
        )
    }

    fun eq(column: String, value: String): String =
        "${encode(column)}=eq.${encode(value)}"

    fun order(column: String, descending: Boolean = false): String =
        "order=${encode(column)}.${if (descending) "desc" else "asc"}"

    fun and(vararg queryParts: String): String =
        queryParts.filter { it.isNotBlank() }.joinToString("&")

    suspend fun healthCheck() {
        request(
            method = "GET",
            path = "messages?select=id&limit=1",
        )
    }

    suspend fun request(
        method: String,
        path: String,
        body: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val connection = (URL("$supabaseUrl/rest/v1/$path").openConnection() as HttpURLConnection)
        connection.requestMethod = method
        connection.connectTimeout = REQUEST_TIMEOUT_MS
        connection.readTimeout = REQUEST_TIMEOUT_MS
        connection.setRequestProperty("apikey", publishableKey)
        connection.setRequestProperty("Authorization", "Bearer $publishableKey")
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        if (method != "GET") {
            connection.setRequestProperty("Prefer", "return=minimal")
        }

        if (body != null) {
            connection.doOutput = true
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }
        }

        val status = connection.responseCode
        val response = runCatching {
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        }.getOrDefault("")

        connection.disconnect()

        if (status !in 200..299) {
            throw IOException("Supabase request failed: $method $path returned HTTP $status $response")
        }
        response
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    private companion object {
        private const val REQUEST_TIMEOUT_MS = 20_000
    }
}
