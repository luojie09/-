package com.secretbase.app.data.local

import com.secretbase.app.data.anniversary.Anniversary
import com.secretbase.app.data.message.Message
import com.secretbase.app.data.message.MessageDraft
import com.secretbase.app.data.wish.Wish
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SecretBaseCache(
    database: SecretBaseDatabase,
    private val scopeId: String,
    private val currentUserId: String,
) {
    private val dao = database.cacheEntries()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun observeMessages(): Flow<List<Message>> =
        dao.observe(messagesKey).map { entry ->
            entry?.payload?.decodeList(Message.serializer()).orEmpty()
        }

    fun observeDraft(): Flow<MessageDraft> =
        dao.observe(draftKey).map { entry ->
            entry?.payload?.decodeOrNull<MessageDraft>() ?: MessageDraft()
        }

    fun observeWishes(): Flow<List<Wish>> =
        dao.observe(wishesKey).map { entry ->
            entry?.payload?.decodeList(Wish.serializer()).orEmpty()
        }

    fun observeAnniversaries(): Flow<List<Anniversary>> =
        dao.observe(anniversariesKey).map { entry ->
            entry?.payload?.decodeList(Anniversary.serializer()).orEmpty()
        }

    suspend fun messages(): List<Message> =
        dao.get(messagesKey)?.payload?.decodeList(Message.serializer()).orEmpty()

    suspend fun draft(): MessageDraft =
        dao.get(draftKey)?.payload?.decodeOrNull<MessageDraft>() ?: MessageDraft()

    suspend fun wishes(): List<Wish> =
        dao.get(wishesKey)?.payload?.decodeList(Wish.serializer()).orEmpty()

    suspend fun anniversaries(): List<Anniversary> =
        dao.get(anniversariesKey)?.payload?.decodeList(Anniversary.serializer()).orEmpty()

    suspend fun saveMessages(value: List<Message>) = save(messagesKey, json.encodeToString(value))

    suspend fun saveDraft(value: MessageDraft) = save(draftKey, json.encodeToString(value))

    suspend fun saveWishes(value: List<Wish>) = save(wishesKey, json.encodeToString(value))

    suspend fun saveAnniversaries(value: List<Anniversary>) =
        save(anniversariesKey, json.encodeToString(value))

    private suspend fun save(cacheKey: String, payload: String) {
        dao.upsert(
            CacheEntryEntity(
                cacheKey = cacheKey,
                payload = payload,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    private fun <T> String?.decodeList(serializer: kotlinx.serialization.KSerializer<T>): List<T> =
        if (this.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching { json.decodeFromString(ListSerializer(serializer), this) }.getOrDefault(emptyList())
        }

    private inline fun <reified T> String.decodeOrNull(): T? =
        runCatching { json.decodeFromString<T>(this) }.getOrNull()

    private val messagesKey = "$scopeId:messages"
    private val wishesKey = "$scopeId:wishes"
    private val anniversariesKey = "$scopeId:anniversaries"
    private val draftKey = "$scopeId:draft:$currentUserId"
}
