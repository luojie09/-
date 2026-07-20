package com.secretbase.app.data.message

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class FakeMessageRepository(
    private val currentUserId: String,
) : MessageRepository {

    private val messages = MutableStateFlow(seedMessages())
    private val draft = MutableStateFlow(seedDraft())

    override fun observeMessages(): Flow<List<Message>> = messages.asStateFlow()

    override fun observeDraft(): Flow<MessageDraft> = draft.asStateFlow()

    override suspend fun updateDraft(
        content: String,
        imagePaths: List<String>,
    ) {
        draft.value = MessageDraft(
            content = content.take(MAX_MESSAGE_LENGTH),
            imagePaths = imagePaths.take(MAX_IMAGES),
            updatedAt = now(),
        )
    }

    override suspend fun clearDraft() {
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
        messages.update { current -> listOf(message) + current }
        draft.value = MessageDraft()
        message
    }

    override suspend fun updateMessage(
        messageId: String,
        content: String,
    ): Result<Unit> = runCatching {
        val safeContent = content.trim().take(MAX_MESSAGE_LENGTH)
        require(safeContent.isNotBlank()) { "留言内容不能为空" }

        messages.update { current ->
            current.map { message ->
                if (message.id == messageId && message.authorId == currentUserId) {
                    message.copy(
                        content = safeContent,
                        updatedAt = now(),
                    )
                } else {
                    message
                }
            }
        }
    }

    override suspend fun deleteMessage(
        messageId: String,
    ): Result<Unit> = runCatching {
        messages.update { current ->
            current.filterNot { message ->
                message.id == messageId && message.authorId == currentUserId
            }
        }
    }

    override suspend fun addReply(
        messageId: String,
        content: String,
    ): Result<MessageReply> = runCatching {
        val safeContent = content.trim().take(MAX_REPLY_LENGTH)
        require(safeContent.isNotBlank()) { "回复内容不能为空" }

        val reply = MessageReply(
            id = "reply-${UUID.randomUUID()}",
            messageId = messageId,
            authorId = currentUserId,
            authorName = SecretBaseUsers.nameFor(currentUserId),
            content = safeContent,
            createdAt = now(),
            isRead = true,
            readAt = now(),
        )

        var updated = false
        messages.update { current ->
            current.map { message ->
                if (message.id == messageId) {
                    updated = true
                    message.copy(replies = message.replies + reply)
                } else {
                    message
                }
            }
        }
        check(updated) { "留言不存在" }
        reply
    }

    override suspend fun deleteReply(
        replyId: String,
    ): Result<Unit> = runCatching {
        messages.update { current ->
            current.map { message ->
                message.copy(
                    replies = message.replies.filterNot { reply ->
                        reply.id == replyId && reply.authorId == currentUserId
                    },
                )
            }
        }
    }

    override suspend fun markMessageRead(
        messageId: String,
    ): Result<Unit> = runCatching {
        val readAt = now()
        messages.update { current ->
            current.map { message ->
                if (message.id == messageId) {
                    message.copy(
                        isRead = if (message.authorId == currentUserId) {
                            message.isRead
                        } else {
                            true
                        },
                        readAt = if (message.authorId == currentUserId) {
                            message.readAt
                        } else {
                            readAt
                        },
                        replies = message.replies.map { reply ->
                            if (reply.authorId == currentUserId || reply.isRead) {
                                reply
                            } else {
                                reply.copy(
                                    isRead = true,
                                    readAt = readAt,
                                )
                            }
                        },
                    )
                } else {
                    message
                }
            }
        }
    }

    override suspend fun toggleLike(messageId: String): Result<Boolean> = runCatching {
        var liked = false
        var found = false
        messages.update { current ->
            current.map { message ->
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
        }
        check(found) { "留言不存在" }
        liked
    }

    private fun seedDraft(): MessageDraft =
        MessageDraft(
            content = "",
            imagePaths = emptyList(),
            updatedAt = now(),
        )

    private fun seedMessages(): List<Message> {
        val current = now()
        return listOf(
            Message(
                id = "message-sheep-blossom",
                authorId = SecretBaseUsers.SHEEP_ID,
                authorName = SecretBaseUsers.nameFor(SecretBaseUsers.SHEEP_ID),
                content = "今天看到一朵超美的樱花，第一时间就想分享给你。",
                imagePaths = listOf("mock://blossom-sky"),
                createdAt = current - 10 * MINUTE,
                isRead = true,
                readAt = current - 10 * MINUTE,
                counterpartReadAt = null,
                replies = listOf(
                    MessageReply(
                        id = "reply-chick-blossom",
                        messageId = "message-sheep-blossom",
                        authorId = SecretBaseUsers.CHICK_ID,
                        authorName = SecretBaseUsers.nameFor(SecretBaseUsers.CHICK_ID),
                        content = "好想和你一起去看，光是照片就已经心动啦。",
                        createdAt = current - 5 * MINUTE,
                        isRead = false,
                        readAt = null,
                    ),
                ),
            ),
            Message(
                id = "message-chick-sunset",
                authorId = SecretBaseUsers.CHICK_ID,
                authorName = SecretBaseUsers.nameFor(SecretBaseUsers.CHICK_ID),
                content = "一起去看了日落和海，好治愈呀～",
                imagePaths = listOf(
                    "mock://sunset-gold",
                    "mock://sunset-shore",
                ),
                createdAt = current - 16 * HOUR,
                isRead = false,
                readAt = null,
                counterpartReadAt = current - 15 * HOUR,
                replies = emptyList(),
            ),
            Message(
                id = "message-sheep-morning",
                authorId = SecretBaseUsers.SHEEP_ID,
                authorName = SecretBaseUsers.nameFor(SecretBaseUsers.SHEEP_ID),
                content = "早安～新的一天也要元气满满哦！",
                imagePaths = emptyList(),
                createdAt = current - 32 * HOUR,
                isRead = true,
                readAt = current - 32 * HOUR,
                counterpartReadAt = current - 28 * HOUR,
                replies = listOf(
                    MessageReply(
                        id = "reply-sheep-morning-1",
                        messageId = "message-sheep-morning",
                        authorId = SecretBaseUsers.SHEEP_ID,
                        authorName = SecretBaseUsers.nameFor(SecretBaseUsers.SHEEP_ID),
                        content = "记得喝温水，别空着肚子出门。",
                        createdAt = current - 31 * HOUR,
                        isRead = true,
                        readAt = current - 31 * HOUR,
                    ),
                    MessageReply(
                        id = "reply-chick-morning-2",
                        messageId = "message-sheep-morning",
                        authorId = SecretBaseUsers.CHICK_ID,
                        authorName = SecretBaseUsers.nameFor(SecretBaseUsers.CHICK_ID),
                        content = "收到收到，今天也会很想你。",
                        createdAt = current - 30 * HOUR,
                        isRead = true,
                        readAt = current - 29 * HOUR,
                    ),
                    MessageReply(
                        id = "reply-sheep-morning-3",
                        messageId = "message-sheep-morning",
                        authorId = SecretBaseUsers.SHEEP_ID,
                        authorName = SecretBaseUsers.nameFor(SecretBaseUsers.SHEEP_ID),
                        content = "那我等你下班，晚上一起吃小蛋糕。",
                        createdAt = current - 29 * HOUR,
                        isRead = true,
                        readAt = current - 29 * HOUR,
                    ),
                    MessageReply(
                        id = "reply-chick-morning-4",
                        messageId = "message-sheep-morning",
                        authorId = SecretBaseUsers.CHICK_ID,
                        authorName = SecretBaseUsers.nameFor(SecretBaseUsers.CHICK_ID),
                        content = "好呀，想吃草莓奶油味的！",
                        createdAt = current - 28 * HOUR,
                        isRead = true,
                        readAt = current - 28 * HOUR,
                    ),
                ),
            ),
            Message(
                id = "message-chick-text",
                authorId = SecretBaseUsers.CHICK_ID,
                authorName = SecretBaseUsers.nameFor(SecretBaseUsers.CHICK_ID),
                content = "回家路上给你带了热乎乎的小甜点，等会记得开门收惊喜。",
                imagePaths = emptyList(),
                createdAt = current - 46 * HOUR,
                isRead = false,
                readAt = null,
                counterpartReadAt = current - 45 * HOUR,
                replies = emptyList(),
            ),
        )
    }

    private fun now(): Long = System.currentTimeMillis()

    companion object {
        private const val MAX_IMAGES = 9
        private const val MAX_MESSAGE_LENGTH = 500
        private const val MAX_REPLY_LENGTH = 300
        private const val MINUTE = 60_000L
        private const val HOUR = 3_600_000L
    }
}
