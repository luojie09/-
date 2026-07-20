package com.secretbase.app.ui.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.secretbase.app.AppActions
import com.secretbase.app.R
import com.secretbase.app.data.BottomNavMessages
import com.secretbase.app.data.HeroVisualConfig
import com.secretbase.app.data.HomeVisuals
import com.secretbase.app.data.MoodOption
import com.secretbase.app.data.QuickRecordConfig
import com.secretbase.app.data.TopActionMessages
import com.secretbase.app.ui.theme.SecretBaseTheme

enum class HomePreviewScenario {
    Normal,
    Empty,
}

@Composable
fun HomeScreenPreviewContent(
    scenario: HomePreviewScenario,
) {
    SecretBaseTheme {
        HomeScreen(
            uiState = buildHomePreviewUiState(scenario),
            snackbarHostState = remember { SnackbarHostState() },
            onRetry = {},
            onMoodCardClick = {},
            onMoodSelected = {},
            onDismissMoodPicker = {},
            onQuickNoteChange = {},
            onQuickNoteSubmit = {},
            onPlaceholderAction = {},
        )
    }
}

fun buildHomePreviewUiState(
    scenario: HomePreviewScenario = HomePreviewScenario.Normal,
): HomeUiState {
    val payload = HomePayload(
        greeting = "Good morning",
        coupleDisplayName = "\u5c0f\u7f8a\u2764\uFE0F\u5c0f\u8036",
        relationship = RelationshipUiModel(
            label = "\u6211\u4eec\u5df2\u7ecf\u5728\u4e00\u8d77",
            daysTogether = 48,
            startDateText = "2026.05.06 \u661f\u671f\u4e09",
            anniversaryTitle = "\u604b\u7231\u4e00\u5468\u5e74",
            daysUntilAnniversary = 318,
            anniversaryCountdownLabel = "\u8fd8\u6709 318 \u5929",
            anniversaryProgress = 0.14f,
        ),
        visuals = buildPreviewHomeVisuals(),
        quickRecord = QuickRecordConfig(
            placeholder = "\u4eca\u5929\u60f3\u8bf4\u70b9\u4ec0\u4e48\u2026",
            entryMessage = "\u5f85\u63a5\u5165\u5feb\u6377\u53d1\u5e03\u9875",
            galleryMessage = "\u5f85\u63a5\u5165\u76f8\u518c\u9009\u62e9",
            cameraMessage = "\u5f85\u63a5\u5165\u62cd\u7167",
            calendarMessage = "\u5f85\u63a5\u5165\u65e5\u671f\u8bb0\u5f55",
        ),
        topActionMessages = TopActionMessages(
            message = "\u5f85\u63a5\u5165\u6d88\u606f\u9875",
            settings = "\u5f85\u63a5\u5165\u8bbe\u7f6e\u9875",
        ),
        bottomNavMessages = BottomNavMessages(
            messageWall = AppActions.OpenMessageWall,
            wishlist = AppActions.OpenWishList,
            anniversary = "\u5f85\u63a5\u5165\u7eaa\u5ff5\u65e5\u9875",
            album = "\u5f85\u63a5\u5165\u76f8\u518c\u9875",
            profile = "\u5f85\u63a5\u5165\u6211\u7684\u9875",
        ),
        moodOptions = MoodOption.defaults,
        moodCards = when (scenario) {
            HomePreviewScenario.Normal -> listOf(
                MoodCardUiModel(
                    userId = "sheep",
                    displayName = "\u5c0f\u7f8a",
                    moodLabel = "\u5f00\u5fc3",
                    moodEmoji = "\uD83D\uDE0A",
                    editable = true,
                    avatarRes = R.drawable.avatar_sheep,
                ),
                MoodCardUiModel(
                    userId = "chick",
                    displayName = "\u5c0f\u8036",
                    moodLabel = "\u5e73\u9759",
                    moodEmoji = "\uD83D\uDE0C",
                    editable = false,
                    avatarRes = R.drawable.avatar_chick,
                ),
            )

            HomePreviewScenario.Empty -> listOf(
                MoodCardUiModel(
                    userId = "sheep",
                    displayName = "\u5c0f\u7f8a",
                    moodLabel = "\u4eca\u5929\u8fd8\u672a\u8bb0\u5f55",
                    moodEmoji = "...",
                    editable = true,
                    avatarRes = R.drawable.avatar_sheep,
                ),
                MoodCardUiModel(
                    userId = "chick",
                    displayName = "\u5c0f\u8036",
                    moodLabel = "\u4eca\u5929\u8fd8\u672a\u8bb0\u5f55",
                    moodEmoji = "...",
                    editable = false,
                    avatarRes = R.drawable.avatar_chick,
                ),
            )
        },
        featureCards = listOf(
            FeatureCardUiModel(
                id = "messageWall",
                title = "\u7559\u8a00\u5899",
                summary = "9\u6761\u65b0\u6d88\u606f",
                clickMessage = "\u5f85\u63a5\u5165\u7559\u8a00\u5899",
                iconRes = R.drawable.ic_message_wall_card,
            ),
            FeatureCardUiModel(
                id = "wishlist",
                title = "\u613f\u671b\u6e05\u5355",
                summary = "3 / 8 \u5b8c\u6210",
                clickMessage = "\u5f85\u63a5\u5165\u613f\u671b\u6e05\u5355",
                iconRes = R.drawable.ic_wishlist_card,
            ),
            FeatureCardUiModel(
                id = "anniversary",
                title = "\u7eaa\u5ff5\u65e5",
                summary = "318\u5929\u540e",
                clickMessage = "\u5f85\u63a5\u5165\u7eaa\u5ff5\u65e5",
                iconRes = R.drawable.ic_anniversary_card,
            ),
            FeatureCardUiModel(
                id = "tasks",
                title = "\u6311\u6218\u4efb\u52a1",
                summary = "2\u9879\u8fdb\u884c\u4e2d",
                clickMessage = "\u5f85\u63a5\u5165\u6311\u6218\u4efb\u52a1",
                iconRes = R.drawable.ic_task_card,
            ),
        ),
        activities = when (scenario) {
            HomePreviewScenario.Normal -> listOf(
                ActivityUiModel(
                    id = "photo",
                    title = "\u5c0f\u8036\u4e0a\u4f20\u4e86 3 \u5f20\u65b0\u7167",
                    relativeTime = "2\u5c0f\u65f6\u524d",
                    clickMessage = "\u5f85\u63a5\u5165\u76f8\u518c\u8be6\u60c5",
                    iconRes = R.drawable.ic_activity_photo,
                ),
                ActivityUiModel(
                    id = "wish",
                    title = "\u5b8c\u6210\u4e86\u613f\u671b\u6e05\u5355\u4e2d\u7684\u300c\u770b\u6d77\u300d",
                    relativeTime = "\u6628\u5929",
                    clickMessage = "\u5f85\u63a5\u5165\u613f\u671b\u8be6\u60c5",
                    iconRes = R.drawable.ic_activity_checklist,
                ),
            )

            HomePreviewScenario.Empty -> emptyList()
        },
        allActivities = when (scenario) {
            HomePreviewScenario.Normal -> listOf(
                ActivityUiModel(
                    id = "photo",
                    title = "\u5c0f\u8036\u4e0a\u4f20\u4e86 3 \u5f20\u65b0\u7167",
                    relativeTime = "2\u5c0f\u65f6\u524d",
                    clickMessage = "\u5f85\u63a5\u5165\u76f8\u518c\u8be6\u60c5",
                    iconRes = R.drawable.ic_activity_photo,
                ),
                ActivityUiModel(
                    id = "wish",
                    title = "\u5b8c\u6210\u4e86\u613f\u671b\u6e05\u5355\u4e2d\u7684\u300c\u770b\u6d77\u300d",
                    relativeTime = "\u6628\u5929",
                    clickMessage = "\u5f85\u63a5\u5165\u613f\u671b\u8be6\u60c5",
                    iconRes = R.drawable.ic_activity_checklist,
                ),
                ActivityUiModel(
                    id = "message",
                    title = "\u5c0f\u8036\u53d1\u5e03\u4e86\u4e00\u6761\u65b0\u7559\u8a00",
                    relativeTime = "\u521a\u521a",
                    clickMessage = AppActions.OpenMessageWall,
                    iconRes = R.drawable.ic_message_wall_card,
                ),
                ActivityUiModel(
                    id = "anniversary",
                    title = "\u65b0\u589e\u4e86\u7eaa\u5ff5\u65e5\u300c\u751f\u65e5 - \u5c0f\u7f8a\u300d",
                    relativeTime = "3\u5929\u524d",
                    clickMessage = AppActions.OpenAnniversary,
                    iconRes = R.drawable.ic_anniversary_card,
                ),
            )

            HomePreviewScenario.Empty -> emptyList()
        },
        recentActivityEmptyText = "\u8fd8\u6ca1\u6709\u65b0\u7684\u8bb0\u5f55\uff0c\u53bb\u7559\u4e0b\u5c5e\u4e8e\u6211\u4eec\u7684\u56de\u5fc6\u5427",
        recentActivityListMessage = AppActions.OpenRecentActivities,
        messageDotVisible = scenario == HomePreviewScenario.Normal,
    )

    return HomeUiState(
        isLoading = false,
        quickNoteText = "",
        payload = payload,
    )
}

internal fun buildPreviewHomeVisuals(): HomeVisuals =
    HomeVisuals(
        hero = HeroVisualConfig(
            imageRes = R.drawable.home_couple_hero,
            gradientStartHex = "#FFE4EC",
            gradientMiddleHex = "#FFF0F4",
            gradientEndHex = "#FFF4F7",
            heightDp = 250,
            bottomFadeHeightDp = 0,
            relationshipCardOverlapDp = 0,
        ),
        backgroundOverlayRes = null,
        avatarResByUserId = mapOf(
            "sheep" to R.drawable.avatar_sheep,
            "chick" to R.drawable.avatar_chick,
        ),
        iconResBySlot = mapOf(
            "message" to R.drawable.ic_message_bubble,
            "settings" to R.drawable.ic_settings_gear,
            "quickGallery" to R.drawable.ic_gallery_stack,
            "quickCamera" to R.drawable.ic_camera,
            "quickCalendar" to R.drawable.ic_calendar_outline_pink,
            "anniversaryFeature" to R.drawable.ic_anniversary_card,
            "albumFeature" to R.drawable.ic_album_card,
            "wishlistFeature" to R.drawable.ic_wishlist_card,
            "messageWallFeature" to R.drawable.ic_message_wall_card,
            "diaryFeature" to R.drawable.ic_diary_card,
            "taskFeature" to R.drawable.ic_task_card,
            "navHome" to R.drawable.ic_home_nav_active,
            "navAnniversary" to R.drawable.ic_calendar_nav,
            "navAlbum" to R.drawable.ic_album_nav,
            "navProfile" to R.drawable.ic_profile_nav,
            "activityPhoto" to R.drawable.ic_activity_photo,
            "activityChecklist" to R.drawable.ic_activity_checklist,
            "activityNote" to R.drawable.ic_message_bubble,
        ),
    )

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Home Normal 393x852")
@Composable
private fun HomeScreenPreviewNormal393() {
    HomeScreenPreviewContent(HomePreviewScenario.Normal)
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "Home Normal 360x800")
@Composable
private fun HomeScreenPreviewNormal360() {
    HomeScreenPreviewContent(HomePreviewScenario.Normal)
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Home Empty 393x852")
@Composable
private fun HomeScreenPreviewEmpty393() {
    HomeScreenPreviewContent(HomePreviewScenario.Empty)
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "Home Empty 360x800")
@Composable
private fun HomeScreenPreviewEmpty360() {
    HomeScreenPreviewContent(HomePreviewScenario.Empty)
}
