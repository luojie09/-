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
    fun client(
        accessTokenProvider: suspend () -> String,
    ): SupabaseRestClient {
        check(SupabaseConfig.isConfigured) {
            "Supabase is not configured. Please set SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY."
        }
        return SupabaseRestClient(
            supabaseUrl = SupabaseConfig.url,
            publishableKey = SupabaseConfig.publishableKey,
            accessTokenProvider = accessTokenProvider,
        )
    }
}

class SupabaseRestClient(
    private val supabaseUrl: String,
    private val publishableKey: String,
    private val accessTokenProvider: (suspend () -> String)? = null,
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

    suspend inline fun <reified T> upsert(
        table: String,
        row: T,
    ) {
        request(
            method = "POST",
            path = table,
            body = json.encodeToString(row),
            prefer = "resolution=merge-duplicates,return=minimal",
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

    suspend fun uploadStorageObject(
        bucket: String,
        objectPath: String,
        bytes: ByteArray,
        contentType: String,
        upsert: Boolean = true,
    ) {
        val authorizationToken = authorizationToken()
        withContext(Dispatchers.IO) {
        val path = objectPath
            .split('/')
            .joinToString("/") { encode(it) }
        val connection = (URL("$supabaseUrl/storage/v1/object/${encode(bucket)}/$path").openConnection() as HttpURLConnection)
        connection.requestMethod = "POST"
        connection.connectTimeout = REQUEST_TIMEOUT_MS
        connection.readTimeout = REQUEST_TIMEOUT_MS
        connection.setRequestProperty("apikey", publishableKey)
        connection.setRequestProperty("Authorization", "Bearer $authorizationToken")
        connection.setRequestProperty("Content-Type", contentType)
        connection.setRequestProperty("x-upsert", upsert.toString())
        connection.doOutput = true
        connection.outputStream.use { output ->
            output.write(bytes)
        }

        val status = connection.responseCode
        val response = runCatching {
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        }.getOrDefault("")
        connection.disconnect()

        if (status !in 200..299) {
            throw IOException("Supabase storage upload failed: $bucket/$objectPath returned HTTP $status $response")
        }
        }
    }

    fun publicStorageUrl(bucket: String, objectPath: String): String {
        val path = objectPath
            .split('/')
            .joinToString("/") { encode(it) }
        return "$supabaseUrl/storage/v1/object/public/${encode(bucket)}/$path"
    }

    suspend fun request(
        method: String,
        path: String,
        body: String? = null,
        prefer: String? = null,
    ): String {
        val authorizationToken = authorizationToken()
        return withContext(Dispatchers.IO) {
        val connection = (URL("$supabaseUrl/rest/v1/$path").openConnection() as HttpURLConnection)
        connection.requestMethod = method
        connection.connectTimeout = REQUEST_TIMEOUT_MS
        connection.readTimeout = REQUEST_TIMEOUT_MS
        connection.setRequestProperty("apikey", publishableKey)
        connection.setRequestProperty("Authorization", "Bearer $authorizationToken")
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        if (method != "GET") {
            connection.setRequestProperty("Prefer", prefer ?: "return=minimal")
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
    }

    private suspend fun authorizationToken(): String =
        accessTokenProvider?.invoke()?.takeIf { it.isNotBlank() } ?: publishableKey

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    private companion object {
        private const val REQUEST_TIMEOUT_MS = 20_000
    }
}
