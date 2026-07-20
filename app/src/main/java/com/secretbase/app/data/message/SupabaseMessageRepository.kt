package com.secretbase.app.data.message

import android.util.Log
import com.secretbase.app.data.local.SecretBaseCache
import com.secretbase.app.data.supabase.SupabaseRestClient
import com.secretbase.app.data.supabase.isoInstantToMillis
import com.secretbase.app.data.sync.DraftSyncPayload
import com.secretbase.app.data.sync.SecretBaseSyncManager
import com.secretbase.app.data.sync.SyncModules
import com.secretbase.app.data.sync.SyncOperations
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SupabaseMessageRepository(
    private val client: SupabaseRestClient,
    private val currentUserId: String,
    private val cache: SecretBaseCache,
    private val syncManager: SecretBaseSyncManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : MessageRepository {
    private val refreshMutex = Mutex()
    private val json = Json { encodeDefaults = true }

    init {
        scope.launch { refreshAllSafely() }
        scope.launch {
            syncManager.refreshEvents
                .filter { it == SyncModules.MESSAGES }
                .collect { refreshAllSafely() }
        }
        scope.launch {
            while (isActive) {
                delay(FALLBACK_REFRESH_INTERVAL_MS)
                refreshAllSafely()
            }
        }
    }

    override fun observeMessages(): Flow<List<Message>> = cache.observeMessages()

    override fun observeDraft(): Flow<MessageDraft> = cache.observeDraft()

    override suspend fun updateDraft(content: String, imagePaths: List<String>) {
        val draft = MessageDraft(
            content = content.take(MAX_MESSAGE_LENGTH),
            imagePaths = imagePaths.take(MAX_IMAGES),
            updatedAt = now(),
        )
        cache.saveDraft(draft)
        syncManager.enqueue(
            module = SyncModules.MESSAGES,
            operation = SyncOperations.DRAFT_UPSERT,
            entityId = currentUserId,
            payload = json.encodeToString(DraftSyncPayload(currentUserId, draft)),
            dedupeKey = "draft:$currentUserId",
        )
    }

    override suspend fun clearDraft() {
        cache.saveDraft(MessageDraft())
        syncManager.enqueue(
            module = SyncModules.MESSAGES,
            operation = SyncOperations.DRAFT_DELETE,
            entityId = currentUserId,
            dedupeKey = "draft:$currentUserId",
        )
    }

    override suspend fun publishMessage(content: String, imagePaths: List<String>): Result<Message> = runCatching {
        val safeContent = content.trim().take(MAX_MESSAGE_LENGTH)
        val safeImages = imagePaths.take(MAX_IMAGES)
        require(safeContent.isNotBlank() || safeImages.isNotEmpty()) { "文字和图片不能同时为空" }

        val timestamp = now()
        val message = Message(
            id = "message-${UUID.randomUUID()}",
            authorId = currentUserId,
            authorName = SecretBaseUsers.nameFor(currentUserId),
            content = safeContent,
            imagePaths = safeImages,
            createdAt = timestamp,
            isRead = true,
            readAt = timestamp,
        )
        cache.saveMessages(listOf(message) + cache.messages())
        cache.saveDraft(MessageDraft())
        syncManager.enqueue(
            SyncModules.MESSAGES,
            SyncOperations.MESSAGE_UPSERT,
            message.id,
            json.encodeToString(message),
        )
        syncManager.enqueue(
            SyncModules.MESSAGES,
            SyncOperations.DRAFT_DELETE,
            currentUserId,
            dedupeKey = "draft:$currentUserId",
        )
        message
    }

    override suspend fun updateMessage(messageId: String, content: String): Result<Unit> = runCatching {
        val safeContent = content.trim().take(MAX_MESSAGE_LENGTH)
        require(safeContent.isNotBlank()) { "留言内容不能为空" }
        var updated: Message? = null
        val messages = cache.messages().map { message ->
            if (message.id == messageId && message.authorId == currentUserId) {
                message.copy(content = safeContent, updatedAt = now()).also { updated = it }
            } else {
                message
            }
        }
        val value = updated ?: error("Message not found")
        cache.saveMessages(messages)
        syncManager.enqueue(
            SyncModules.MESSAGES,
            SyncOperations.MESSAGE_UPSERT,
            messageId,
            json.encodeToString(value),
        )
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> = runCatching {
        val current = cache.messages()
        check(current.any { it.id == messageId && it.authorId == currentUserId }) { "Message not found" }
        cache.saveMessages(current.filterNot { it.id == messageId })
        syncManager.enqueue(SyncModules.MESSAGES, SyncOperations.MESSAGE_DELETE, messageId)
    }

    override suspend fun addReply(messageId: String, content: String): Result<MessageReply> = runCatching {
        val safeContent = content.trim().take(MAX_REPLY_LENGTH)
        require(safeContent.isNotBlank()) { "回复内容不能为空" }
        val timestamp = now()
        val reply = MessageReply(
            id = "reply-${UUID.randomUUID()}",
            messageId = messageId,
            authorId = currentUserId,
            authorName = SecretBaseUsers.nameFor(currentUserId),
            content = safeContent,
            createdAt = timestamp,
            isRead = true,
            readAt = timestamp,
        )
        var found = false
        val messages = cache.messages().map { message ->
            if (message.id == messageId) {
                found = true
                message.copy(replies = message.replies + reply)
            } else {
                message
            }
        }
        check(found) { "留言不存在" }
        cache.saveMessages(messages)
        syncManager.enqueue(
            SyncModules.MESSAGES,
            SyncOperations.REPLY_UPSERT,
            reply.id,
            json.encodeToString(reply),
        )
        reply
    }

    override suspend fun deleteReply(replyId: String): Result<Unit> = runCatching {
        var found = false
        val messages = cache.messages().map { message ->
            val target = message.replies.firstOrNull { it.id == replyId && it.authorId == currentUserId }
            if (target != null) {
                found = true
                message.copy(replies = message.replies.filterNot { it.id == replyId })
            } else {
                message
            }
        }
        check(found) { "回复不存在" }
        cache.saveMessages(messages)
        syncManager.enqueue(SyncModules.MESSAGES, SyncOperations.REPLY_DELETE, replyId)
    }

    override suspend fun markMessageRead(messageId: String): Result<Unit> = runCatching {
        val readAt = now()
        val changedReplies = mutableListOf<MessageReply>()
        var changedMessage: Message? = null
        val messages = cache.messages().map { message ->
            if (message.id != messageId) return@map message
            val replies = message.replies.map { reply ->
                if (reply.authorId != currentUserId && !reply.isRead) {
                    reply.copy(isRead = true, readAt = readAt).also(changedReplies::add)
                } else {
                    reply
                }
            }
            message.copy(
                isRead = message.isRead || message.authorId != currentUserId,
                readAt = if (message.authorId != currentUserId) readAt else message.readAt,
                replies = replies,
            ).also { updated ->
                if (updated != message) changedMessage = updated
            }
        }
        cache.saveMessages(messages)
        changedMessage?.let { message ->
            syncManager.enqueue(
                SyncModules.MESSAGES,
                SyncOperations.MESSAGE_UPSERT,
                message.id,
                json.encodeToString(message),
            )
        }
        changedReplies.forEach { reply ->
            syncManager.enqueue(
                SyncModules.MESSAGES,
                SyncOperations.REPLY_UPSERT,
                reply.id,
                json.encodeToString(reply),
            )
        }
    }

    override suspend fun toggleLike(messageId: String): Result<Boolean> = runCatching {
        val timestamp = now()
        var found = false
        var liked = false
        val messages = cache.messages().map { message ->
            if (message.id != messageId) return@map message
            found = true
            liked = currentUserId !in message.likedByUserIds
            message.copy(
                likedByUserIds = if (liked) {
                    (message.likedByUserIds + currentUserId).distinct()
                } else {
                    message.likedByUserIds.filterNot { it == currentUserId }
                },
            )
        }
        check(found) { "留言不存在" }
        cache.saveMessages(messages)

        val like = MessageLike(
            messageId = messageId,
            userId = currentUserId,
            createdAt = timestamp,
        )
        syncManager.enqueue(
            module = SyncModules.MESSAGES,
            operation = if (liked) SyncOperations.LIKE_UPSERT else SyncOperations.LIKE_DELETE,
            entityId = "$messageId:$currentUserId",
            payload = json.encodeToString(like),
            dedupeKey = "like:$messageId:$currentUserId",
        )
        liked
    }

    private suspend fun refreshAllSafely() {
        if (syncManager.hasPending(SyncModules.MESSAGES)) return
        runCatching {
            refreshMessages()
            refreshDraft()
        }.onFailure { error ->
            Log.w(TAG, "Remote refresh failed; keeping Room cache", error)
        }
    }

    private suspend fun refreshMessages() = refreshMutex.withLock {
        val messageRows = client.select<MessageRow>(
            MESSAGES_TABLE,
            client.and("select=*", client.order("created_at", descending = true)),
        )
        val replyRows = client.select<ReplyRow>(
            REPLIES_TABLE,
            client.and("select=*", client.order("created_at")),
        )
        val likeRows = runCatching {
            client.select<LikeRow>(
                LIKES_TABLE,
                "select=message_id,user_id",
            )
        }.getOrDefault(emptyList())
        val replies = replyRows.groupBy { it.messageId }
        val likes = likeRows.groupBy(LikeRow::messageId)
        cache.saveMessages(
            messageRows.map { row ->
                row.toDomain(
                    replies = replies[row.id].orEmpty().map { it.toDomain() },
                    likedByUserIds = likes[row.id].orEmpty().map(LikeRow::userId),
                )
            },
        )
    }

    private suspend fun refreshDraft() {
        val rows = client.select<DraftRow>(
            DRAFTS_TABLE,
            client.and("select=*", client.eq("user_id", currentUserId)),
        )
        cache.saveDraft(rows.firstOrNull()?.toDomain() ?: MessageDraft())
    }

    private fun MessageRow.toDomain(
        replies: List<MessageReply>,
        likedByUserIds: List<String>,
    ) = Message(
        id = id,
        authorId = authorId,
        authorName = authorName,
        content = content,
        imagePaths = imagePaths,
        createdAt = isoInstantToMillis(createdAt),
        updatedAt = updatedAt?.let(::isoInstantToMillis),
        isRead = isRead,
        readAt = readAt?.let(::isoInstantToMillis),
        counterpartReadAt = counterpartReadAt?.let(::isoInstantToMillis),
        replies = replies,
        likedByUserIds = likedByUserIds,
    )

    private fun ReplyRow.toDomain() = MessageReply(
        id = id,
        messageId = messageId,
        authorId = authorId,
        authorName = authorName,
        content = content,
        createdAt = isoInstantToMillis(createdAt),
        isRead = isRead,
        readAt = readAt?.let(::isoInstantToMillis),
    )

    private fun DraftRow.toDomain() = MessageDraft(content, imagePaths, isoInstantToMillis(updatedAt))

    private fun now() = System.currentTimeMillis()

    @Serializable
    private data class MessageRow(
        val id: String,
        @SerialName("author_id") val authorId: String,
        @SerialName("author_name") val authorName: String,
        val content: String,
        @SerialName("image_paths") val imagePaths: List<String> = emptyList(),
        @SerialName("created_at") val createdAt: String,
        @SerialName("updated_at") val updatedAt: String? = null,
        @SerialName("is_read") val isRead: Boolean,
        @SerialName("read_at") val readAt: String? = null,
        @SerialName("counterpart_read_at") val counterpartReadAt: String? = null,
    )

    @Serializable
    private data class ReplyRow(
        val id: String,
        @SerialName("message_id") val messageId: String,
        @SerialName("author_id") val authorId: String,
        @SerialName("author_name") val authorName: String,
        val content: String,
        @SerialName("created_at") val createdAt: String,
        @SerialName("is_read") val isRead: Boolean,
        @SerialName("read_at") val readAt: String? = null,
    )

    @Serializable
    private data class DraftRow(
        @SerialName("user_id") val userId: String,
        val content: String,
        @SerialName("image_paths") val imagePaths: List<String> = emptyList(),
        @SerialName("updated_at") val updatedAt: String,
    )

    @Serializable
    private data class LikeRow(
        @SerialName("message_id") val messageId: String,
        @SerialName("user_id") val userId: String,
    )

    private companion object {
        private const val MESSAGES_TABLE = "messages"
        private const val REPLIES_TABLE = "message_replies"
        private const val DRAFTS_TABLE = "message_drafts"
        private const val LIKES_TABLE = "message_likes"
        private const val MAX_IMAGES = 9
        private const val MAX_MESSAGE_LENGTH = 500
        private const val MAX_REPLY_LENGTH = 300
        private const val FALLBACK_REFRESH_INTERVAL_MS = 60_000L
        private const val TAG = "SupabaseMessageRepo"
    }
}
