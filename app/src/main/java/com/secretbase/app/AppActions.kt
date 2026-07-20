package com.secretbase.app

object AppActions {
    const val OpenMessageWall = "__open_message_wall__"
    const val OpenWishList = "__open_wish_list__"
    const val OpenAnniversary = "__open_anniversary__"
    const val OpenRecentActivities = "__open_recent_activities__"
    const val OpenIdentitySettings = "__open_identity_settings__"

    private const val ActivityDetailPrefix = "__open_activity_detail__:"
    private const val WishCompletionDetailPrefix = "__open_wish_completion_detail__:"

    fun openActivityDetail(activityId: String): String = ActivityDetailPrefix + activityId

    fun activityDetailId(action: String): String? =
        action.takeIf { it.startsWith(ActivityDetailPrefix) }
            ?.removePrefix(ActivityDetailPrefix)
            ?.takeIf(String::isNotBlank)

    fun openWishCompletionDetail(wishId: String): String = WishCompletionDetailPrefix + wishId

    fun wishCompletionDetailId(action: String): String? =
        action.takeIf { it.startsWith(WishCompletionDetailPrefix) }
            ?.removePrefix(WishCompletionDetailPrefix)
            ?.takeIf(String::isNotBlank)
}
