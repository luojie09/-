package com.secretbase.app.data.wish

enum class WishStatus {
    UNREALIZED,
    REALIZED,
}

data class Wish(
    val id: String,
    val title: String,
    val description: String,
    val coverImagePath: String?,
    val plannedDate: Long?,
    val createdAt: Long,
    val status: WishStatus,
    val completion: WishCompletion?,
)

data class WishCompletion(
    val text: String,
    val imagePaths: List<String>,
    val completedAt: Long,
)
