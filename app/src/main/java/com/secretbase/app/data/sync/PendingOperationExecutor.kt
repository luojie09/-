package com.secretbase.app.data.sync

import com.secretbase.app.data.anniversary.Anniversary
import com.secretbase.app.data.message.Message
import com.secretbase.app.data.message.MessageDraft
import com.secretbase.app.data.message.MessageLike
import com.secretbase.app.data.message.MessageReply
import com.secretbase.app.data.supabase.SupabaseImageStorage
import com.secretbase.app.data.supabase.SupabaseRestClient
import com.secretbase.app.data.supabase.millisToIsoInstant
import com.secretbase.app.data.supabase.millisToNullableIsoDate
import com.secretbase.app.data.wish.Wish
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class PendingOperationExecutor(
    private val client: SupabaseRestClient,
    private val imageStorage: SupabaseImageStorage?,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun execute(operation: String, entityId: String, payload: String) {
        when (operation) {
            SyncOperations.MOOD_UPSERT -> client.upsert(MOODS_TABLE, json.decodeFromString<MoodSyncPayload>(payload))
            SyncOperations.QUICK_NOTE_UPSERT -> client.upsert(NOTES_TABLE, json.decodeFromString<QuickNoteSyncPayload>(payload))
            SyncOperations.MESSAGE_UPSERT -> upsertMessage(json.decodeFromString(payload))
            SyncOperations.MESSAGE_DELETE -> client.delete(MESSAGES_TABLE, client.eq("id", entityId))
            SyncOperations.REPLY_UPSERT -> client.upsert(REPLIES_TABLE, json.decodeFromString<MessageReply>(payload).toRow())
            SyncOperations.REPLY_DELETE -> client.delete(REPLIES_TABLE, client.eq("id", entityId))
            SyncOperations.DRAFT_UPSERT -> upsertDraft(json.decodeFromString(payload))
            SyncOperations.DRAFT_DELETE -> client.delete(DRAFTS_TABLE, client.eq("user_id", entityId))
            SyncOperations.LIKE_UPSERT -> client.upsert(LIKES_TABLE, json.decodeFromString<MessageLike>(payload).toRow())
            SyncOperations.LIKE_DELETE -> deleteLike(json.decodeFromString(payload))
            SyncOperations.WISH_UPSERT -> upsertWish(json.decodeFromString(payload))
            SyncOperations.WISH_DELETE -> client.delete(WISHES_TABLE, client.eq("id", entityId))
            SyncOperations.ANNIVERSARY_UPSERT -> upsertAnniversary(json.decodeFromString(payload))
            SyncOperations.ANNIVERSARY_DELETE -> client.delete(ANNIVERSARIES_TABLE, client.eq("id", entityId))
            else -> error("Unsupported pending operation: $operation")
        }
    }

    private suspend fun upsertMessage(message: Message) {
        val imagePaths = imageStorage?.uploadLocalImages(
            imagePaths = message.imagePaths,
            folder = "messages/${message.id}",
        ) ?: message.imagePaths
        client.upsert(MESSAGES_TABLE, message.copy(imagePaths = imagePaths).toRow())
    }

    private suspend fun upsertDraft(payload: DraftSyncPayload) {
        client.upsert(
            DRAFTS_TABLE,
            DraftRow(
                userId = payload.userId,
                content = payload.draft.content,
                imagePaths = payload.draft.imagePaths,
                updatedAt = millisToIsoInstant(payload.draft.updatedAt),
            ),
        )
    }

    private suspend fun deleteLike(like: MessageLike) {
        client.delete(
            LIKES_TABLE,
            client.and(
                client.eq("message_id", like.messageId),
                client.eq("user_id", like.userId),
            ),
        )
    }

    private suspend fun upsertWish(wish: Wish) {
        val cover = wish.coverImagePath?.let { path ->
            imageStorage?.uploadLocalImageIfNeeded(path, "wishes/${wish.id}/cover") ?: path
        }
        val completionImages = wish.completion?.let { completion ->
            imageStorage?.uploadLocalImages(completion.imagePaths, "wishes/${wish.id}/completion")
                ?: completion.imagePaths
        }.orEmpty()
        client.upsert(
            WISHES_TABLE,
            WishRow(
                id = wish.id,
                title = wish.title,
                description = wish.description,
                coverImagePath = cover,
                plannedDate = millisToNullableIsoDate(wish.plannedDate),
                createdAt = millisToIsoInstant(wish.createdAt),
                status = wish.status.name,
                completionText = wish.completion?.text,
                completionImagePaths = completionImages,
                completedAt = wish.completion?.completedAt?.let(::millisToIsoInstant),
            ),
        )
    }

    private suspend fun upsertAnniversary(item: Anniversary) {
        runCatching {
            client.upsert(ANNIVERSARIES_TABLE, item.toRow())
        }.getOrElse { error ->
            if (error.message?.contains("icon_emoji", ignoreCase = true) == true) {
                client.upsert(ANNIVERSARIES_TABLE, item.toLegacyRow())
            } else {
                throw error
            }
        }
    }

    private fun Message.toRow() = MessageRow(
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

    private fun MessageReply.toRow() = ReplyRow(
        id = id,
        messageId = messageId,
        authorId = authorId,
        authorName = authorName,
        content = content,
        createdAt = millisToIsoInstant(createdAt),
        isRead = isRead,
        readAt = readAt?.let(::millisToIsoInstant),
    )

    private fun MessageLike.toRow() = LikeRow(
        messageId = messageId,
        userId = userId,
        createdAt = millisToIsoInstant(createdAt),
    )

    private fun Anniversary.toRow() = AnniversaryRow(
        id = id,
        title = title,
        date = millisToDate(date),
        repeatYearly = repeatYearly,
        reminderType = reminderType.name,
        createdAt = millisToIsoInstant(createdAt),
        iconEmoji = iconEmoji,
    )

    private fun Anniversary.toLegacyRow() = LegacyAnniversaryRow(
        id = id,
        title = title,
        date = millisToDate(date),
        repeatYearly = repeatYearly,
        reminderType = reminderType.name,
        createdAt = millisToIsoInstant(createdAt),
    )

    private fun millisToDate(value: Long): String =
        Instant.ofEpochMilli(value)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE)

    @Serializable
    private data class MessageRow(
        val id: String,
        @SerialName("author_id") val authorId: String,
        @SerialName("author_name") val authorName: String,
        val content: String,
        @SerialName("image_paths") val imagePaths: List<String>,
        @SerialName("created_at") val createdAt: String,
        @SerialName("updated_at") val updatedAt: String?,
        @SerialName("is_read") val isRead: Boolean,
        @SerialName("read_at") val readAt: String?,
        @SerialName("counterpart_read_at") val counterpartReadAt: String?,
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
        @SerialName("read_at") val readAt: String?,
    )

    @Serializable
    private data class DraftRow(
        @SerialName("user_id") val userId: String,
        val content: String,
        @SerialName("image_paths") val imagePaths: List<String>,
        @SerialName("updated_at") val updatedAt: String,
    )

    @Serializable
    private data class LikeRow(
        @SerialName("message_id") val messageId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("created_at") val createdAt: String,
    )

    @Serializable
    private data class WishRow(
        val id: String,
        val title: String,
        val description: String,
        @SerialName("cover_image_path") val coverImagePath: String?,
        @SerialName("planned_date") val plannedDate: String?,
        @SerialName("created_at") val createdAt: String,
        val status: String,
        @SerialName("completion_text") val completionText: String?,
        @SerialName("completion_image_paths") val completionImagePaths: List<String>,
        @SerialName("completed_at") val completedAt: String?,
    )

    @Serializable
    private data class AnniversaryRow(
        val id: String,
        val title: String,
        val date: String,
        @SerialName("repeat_yearly") val repeatYearly: Boolean,
        @SerialName("reminder_type") val reminderType: String,
        @SerialName("created_at") val createdAt: String,
        @SerialName("icon_emoji") val iconEmoji: String?,
    )

    @Serializable
    private data class LegacyAnniversaryRow(
        val id: String,
        val title: String,
        val date: String,
        @SerialName("repeat_yearly") val repeatYearly: Boolean,
        @SerialName("reminder_type") val reminderType: String,
        @SerialName("created_at") val createdAt: String,
    )

    private companion object {
        private const val MESSAGES_TABLE = "messages"
        private const val MOODS_TABLE = "user_moods"
        private const val NOTES_TABLE = "home_quick_notes"
        private const val REPLIES_TABLE = "message_replies"
        private const val DRAFTS_TABLE = "message_drafts"
        private const val LIKES_TABLE = "message_likes"
        private const val WISHES_TABLE = "wishes"
        private const val ANNIVERSARIES_TABLE = "anniversaries"
    }
}

@Serializable
data class DraftSyncPayload(
    val userId: String,
    val draft: MessageDraft,
)

@Serializable
data class MoodSyncPayload(
    @SerialName("user_id") val userId: String,
    @SerialName("mood_label") val moodLabel: String,
    @SerialName("recorded_date") val recordedDate: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class QuickNoteSyncPayload(
    val id: String,
    val title: String,
    @SerialName("icon_slot") val iconSlot: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("click_message") val clickMessage: String,
)
