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
    const val CURRENT_USER_ID = SHEEP_ID

    fun nameFor(userId: String): String =
        when (userId) {
            SHEEP_ID -> "小羊"
            CHICK_ID -> "小耶"
            else -> "我们"
        }
}
