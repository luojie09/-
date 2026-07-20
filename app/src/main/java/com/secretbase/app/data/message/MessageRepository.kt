package com.secretbase.app.data.message

import kotlinx.coroutines.flow.Flow

interface MessageRepository {

    fun observeMessages(): Flow<List<Message>>

    fun observeDraft(): Flow<MessageDraft>

    suspend fun updateDraft(
        content: String,
        imagePaths: List<String>,
    )

    suspend fun clearDraft()

    suspend fun publishMessage(
        content: String,
        imagePaths: List<String>,
    ): Result<Message>

    suspend fun updateMessage(
        messageId: String,
        content: String,
    ): Result<Unit>

    suspend fun deleteMessage(
        messageId: String,
    ): Result<Unit>

    suspend fun addReply(
        messageId: String,
        content: String,
    ): Result<MessageReply>

    suspend fun deleteReply(
        replyId: String,
    ): Result<Unit>

    suspend fun markMessageRead(
        messageId: String,
    ): Result<Unit>

    suspend fun toggleLike(
        messageId: String,
    ): Result<Boolean>
}
