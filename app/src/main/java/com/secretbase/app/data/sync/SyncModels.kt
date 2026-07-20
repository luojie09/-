package com.secretbase.app.data.sync

enum class SyncPhase {
    SYNCED,
    SYNCING,
    PENDING,
    ERROR,
}

data class SyncStatus(
    val phase: SyncPhase = SyncPhase.SYNCED,
    val pendingCount: Int = 0,
    val lastSyncedAt: Long? = null,
    val lastError: String? = null,
    val realtimeConnected: Boolean = false,
)

object SyncModules {
    const val HOME = "home"
    const val MESSAGES = "messages"
    const val WISHES = "wishes"
    const val ANNIVERSARIES = "anniversaries"
}

object SyncOperations {
    const val MOOD_UPSERT = "mood_upsert"
    const val QUICK_NOTE_UPSERT = "quick_note_upsert"
    const val MESSAGE_UPSERT = "message_upsert"
    const val MESSAGE_DELETE = "message_delete"
    const val REPLY_UPSERT = "reply_upsert"
    const val REPLY_DELETE = "reply_delete"
    const val DRAFT_UPSERT = "draft_upsert"
    const val DRAFT_DELETE = "draft_delete"
    const val LIKE_UPSERT = "like_upsert"
    const val LIKE_DELETE = "like_delete"
    const val WISH_UPSERT = "wish_upsert"
    const val WISH_DELETE = "wish_delete"
    const val ANNIVERSARY_UPSERT = "anniversary_upsert"
    const val ANNIVERSARY_DELETE = "anniversary_delete"
}
