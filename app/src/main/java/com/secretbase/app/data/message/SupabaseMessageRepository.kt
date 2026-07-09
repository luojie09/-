package com.secretbase.app.data.message

import android.util.Log
import com.secretbase.app.data.supabase.isoInstantToMillis
import com.secretbase.app.data.supabase.millisToIsoInstant
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

class SupabaseMessageRepository(
    private val client: SupabaseClient,
    private val currentUserId: String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : MessageRepository {

    private val messages = MutableStateFlow<List<Message>>(emptyList())
    private val draft = MutableStateFlow(MessageDraft())

    init {
        scope.launch {
            runCatching { refreshAll() }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load message wall from Supabase", error)
                }
        }
    }

    override fun observeMessages(): Flow<List<Message>> = messages.asStateFlow()

    override fun observeDraft(): Flow<MessageDraft> = draft.asStateFlow()

    override suspend fun updateDraft(
        content: String,
        imagePaths: List<String>,
    ) {
        val row = DraftRow(
            userId = currentUserId,
            content = content.take(MAX_MESSAGE_LENGTH),
            imagePaths = imagePaths.take(MAX_IMAGES),
            updatedAt = millisToIsoInstant(now()),
        )
        runCatching {
            client.from(DRAFTS_TABLE).delete {
                filter {
                    eq("user_id", currentUserId)
                }
            }
            client.from(DRAFTS_TABLE).insert(row)
            draft.value = row.toDomain()
        }.onFailure { error ->
            Log.e(TAG, "Failed to update draft in Supabase", error)
            draft.value = row.toDomain()
        }
    }

    override suspend fun clearDraft() {
        runCatching {
            client.from(DRAFTS_TABLE).delete {
                filter {
                    eq("user_id", currentUserId)
                }
            }
        }.onFailure { error ->
            Log.e(TAG, "Failed to clear draft in Supabase", error)
        }
        draft.value = MessageDraft()
    }

    override suspend fun publishMessage(
        content: String,
        imagePaths: List<String>,
    ): Result<Message> = runCatching {
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
            updatedAt = null,
            isRead = true,
            readAt = timestamp,
            counterpartReadAt = null,
        )
        client.from(MESSAGES_TABLE).insert(message.toRow())
        refreshMessages()
        clearDraft()
        message
    }

    override suspend fun updateMessage(
        messageId: String,
        content: String,
    ): Result<Unit> = runCatching {
        val safeContent = content.trim().take(MAX_MESSAGE_LENGTH)
        require(safeContent.isNotBlank()) { "留言内容不能为空" }

        val message = messages.value.firstOrNull { it.id == messageId }
            ?: error("Message not found")
        client.from(MESSAGES_TABLE).update(
            message.copy(content = safeContent, updatedAt = now()).toRow(),
        ) {
            filter {
                eq("id", messageId)
                eq("author_id", currentUserId)
            }
        }
        refreshMessages()
    }

    override suspend fun deleteMessage(
        messageId: String,
    ): Result<Unit> = runCatching {
        client.from(MESSAGES_TABLE).delete {
            filter {
                eq("id", messageId)
                eq("author_id", currentUserId)
            }
        }
        refreshMessages()
    }

    override suspend fun addReply(
        messageId: String,
        content: String,
    ): Result<MessageReply> = runCatching {
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
        client.from(REPLIES_TABLE).insert(reply.toRow())
        refreshMessages()
        reply
    }

    override suspend fun deleteReply(
        replyId: String,
    ): Result<Unit> = runCatching {
        client.from(REPLIES_TABLE).delete {
            filter {
                eq("id", replyId)
                eq("author_id", currentUserId)
            }
        }
        refreshMessages()
    }

    override suspend fun markMessageRead(
        messageId: String,
    ): Result<Unit> = runCatching {
        val readAt = now()
        val message = messages.value.firstOrNull { it.id == messageId } ?: return@runCatching

        if (message.authorId != currentUserId && !message.isRead) {
            client.from(MESSAGES_TABLE).update(
                message.copy(
                    isRead = true,
                    readAt = readAt,
                ).toRow(),
            ) {
                filter {
                    eq("id", messageId)
                }
            }
        }

        message.replies
            .filter { reply -> reply.authorId != currentUserId && !reply.isRead }
            .forEach { reply ->
                client.from(REPLIES_TABLE).update(
                    reply.copy(
                        isRead = true,
                        readAt = readAt,
                    ).toRow(),
                ) {
                    filter {
                        eq("id", reply.id)
                    }
                }
            }
        refreshMessages()
    }

    private suspend fun refreshAll() {
        refreshMessages()
        refreshDraft()
    }

    private suspend fun refreshMessages() {
        val messageRows = client.from(MESSAGES_TABLE)
            .select {
                order("created_at", order = Order.DESCENDING)
            }
            .decodeList<MessageRow>()

        val replyRows = client.from(REPLIES_TABLE)
            .select {
                order("created_at", order = Order.ASCENDING)
            }
            .decodeList<ReplyRow>()

        val repliesByMessageId = replyRows
            .groupBy { it.messageId }
            .mapValues { (_, rows) -> rows.map { row -> row.toDomain() } }

        messages.value = messageRows.map { row ->
            row.toDomain(repliesByMessageId[row.id].orEmpty())
        }
    }

    private suspend fun refreshDraft() {
        val rows = client.from(DRAFTS_TABLE)
            .select {
                filter {
                    eq("user_id", currentUserId)
                }
            }
            .decodeList<DraftRow>()
        draft.value = rows.firstOrNull()?.toDomain() ?: MessageDraft()
    }

    private fun Message.toRow(): MessageRow =
        MessageRow(
            id = id,
            authorId = authorId,
            authorName = authorName,
            content = content,
            imagePaths = imagePaths,
            createdAt = millisToIsoInstant(createdAt),
            updatedAt = updatedAt?.let(::millisToIsoInstant),
            isRead = isRead,
            readAt = readAt?.let(::millisToIsoInstant),
            counterpartReadAt = counterpartReadAt?.let(::millisToIsoInstant),
        )

    private fun MessageReply.toRow(): ReplyRow =
        ReplyRow(
            id = id,
            messageId = messageId,
            authorId = authorId,
            authorName = authorName,
            content = content,
            createdAt = millisToIsoInstant(createdAt),
            isRead = isRead,
            readAt = readAt?.let(::millisToIsoInstant),
        )

    private fun MessageRow.toDomain(replies: List<MessageReply>): Message =
        Message(
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
        )

    private fun ReplyRow.toDomain(): MessageReply =
        MessageReply(
            id = id,
            messageId = messageId,
            authorId = authorId,
            authorName = authorName,
            content = content,
            createdAt = isoInstantToMillis(createdAt),
            isRead = isRead,
            readAt = readAt?.let(::isoInstantToMillis),
        )

    private fun DraftRow.toDomain(): MessageDraft =
        MessageDraft(
            content = content,
            imagePaths = imagePaths,
            updatedAt = isoInstantToMillis(updatedAt),
        )

    private fun now(): Long = System.currentTimeMillis()

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

    private companion object {
        private const val MESSAGES_TABLE = "messages"
        private const val REPLIES_TABLE = "message_replies"
        private const val DRAFTS_TABLE = "message_drafts"
        private const val MAX_IMAGES = 9
        private const val MAX_MESSAGE_LENGTH = 500
        private const val MAX_REPLY_LENGTH = 300
        private const val TAG = "SupabaseMessageRepo"
    }
}
