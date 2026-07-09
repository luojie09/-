package com.secretbase.app.data.message

data class Message(
    val id: String,
    val authorId: String,
    val authorName: String,
    val content: String,
    val imagePaths: List<String>,
    val createdAt: Long,
    val updatedAt: Long? = null,
    val isRead: Boolean = false,
    val readAt: Long? = null,
    val counterpartReadAt: Long? = null,
    val replies: List<MessageReply> = emptyList(),
)

data class MessageReply(
    val id: String,
    val messageId: String,
    val authorId: String,
    val authorName: String,
    val content: String,
    val createdAt: Long,
    val isRead: Boolean = false,
    val readAt: Long? = null,
)

data class MessageDraft(
    val content: String = "",
    val imagePaths: List<String> = emptyList(),
    val updatedAt: Long = 0L,
)

object SecretBaseUsers {
    const val SHEEP_ID = "sheep"
    const val CHICK_ID = "chick"

    val supportedUserIds = listOf(SHEEP_ID, CHICK_ID)

    fun isSupported(userId: String): Boolean = userId in supportedUserIds

    fun nameFor(userId: String): String =
        when (userId) {
            SHEEP_ID -> "\u5c0f\u7f8a"
            CHICK_ID -> "\u5c0f\u8036"
            else -> "\u6211\u4eec"
        }
}
