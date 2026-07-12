package com.secretbase.app

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secretbase.app.data.message.SecretBaseUsers
import com.secretbase.app.data.supabase.SupabaseConfig
import com.secretbase.app.data.user.UserIdentityStore
import com.secretbase.app.data.wish.WishStatus
import com.secretbase.app.ui.anniversary.AnniversaryScreen
import com.secretbase.app.ui.anniversary.AnniversaryViewModel
import com.secretbase.app.ui.home.HomeScreen
import com.secretbase.app.ui.home.HomeViewModel
import com.secretbase.app.ui.home.RecentActivityScreen
import com.secretbase.app.ui.messagewall.MessageWallEditorScreen
import com.secretbase.app.ui.messagewall.MessageWallScreen
import com.secretbase.app.ui.messagewall.MessageWallViewModel
import com.secretbase.app.ui.wishlist.WishCompletionDetailScreen
import com.secretbase.app.ui.wishlist.WishCompletionScreen
import com.secretbase.app.ui.wishlist.WishDetailScreen
import com.secretbase.app.ui.wishlist.WishListScreen
import com.secretbase.app.ui.wishlist.WishListViewModel
import com.secretbase.app.ui.theme.CherryPink
import com.secretbase.app.ui.theme.SurfaceWhite
import com.secretbase.app.ui.theme.WarmGray

@Composable
fun SecretBaseApp() {
    val appContext = LocalContext.current.applicationContext
    val userIdentityStore = remember(appContext) { UserIdentityStore(appContext) }
    var currentUserId by rememberSaveable { mutableStateOf(userIdentityStore.currentUserId()) }

    if (currentUserId == null) {
        IdentitySelectionGate(
            onIdentitySelected = { selectedUserId ->
                userIdentityStore.saveCurrentUserId(selectedUserId)
                currentUserId = selectedUserId
            },
        )
        return
    }

    val selectedUserId = currentUserId ?: return
    val dependencyResult = remember(appContext, selectedUserId) {
        runCatching {
            SecretBaseDependencies(
                context = appContext,
                currentUserId = selectedUserId,
            )
        }
            .recoverCatching { error ->
                Log.e(
                    "SecretBaseApp",
                    "Failed to create app dependencies.",
                    error,
                )
                if (SupabaseConfig.isConfigured) {
                    throw error
                }
                SecretBaseDependencies(
                    context = appContext,
                    currentUserId = selectedUserId,
                    enableRemoteModules = false,
                )
            }
    }
    val dependencies = dependencyResult.getOrNull()
    if (dependencies == null) {
        StartupErrorScreen(error = dependencyResult.exceptionOrNull())
        return
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var backStack by rememberSaveable { mutableStateOf(listOf(AppRoute.Home.route)) }

    val currentRoute = remember(backStack) { AppRoute.parse(backStack.last()) }

    fun navigate(route: String) {
        if (backStack.lastOrNull() != route) {
            backStack = backStack + route
        }
    }

    fun navigateTopLevel(route: String) {
        if (backStack.lastOrNull() != route || backStack.size > 1) {
            backStack = listOf(route)
        }
    }

    fun popBack() {
        if (backStack.size > 1) {
            backStack = backStack.dropLast(1)
        }
    }

    fun popToWishList() {
        val index = backStack.indexOfLast { it == AppRoute.WishList.route }
        backStack = if (index >= 0) {
            backStack.take(index + 1)
        } else {
            backStack + AppRoute.WishList.route
        }
    }

    BackHandler(enabled = backStack.size > 1) {
        popBack()
    }

    when (currentRoute) {
        AppRoute.Home -> HomeRoute(
            dependencies = dependencies,
            snackbarHostState = snackbarHostState,
            onNavigate = { route -> navigate(route) },
            onSelectTopTab = { route -> navigateTopLevel(route) },
        )

        AppRoute.MessageWall -> MessageWallRoute(
            dependencies = dependencies,
            snackbarHostState = snackbarHostState,
            onBack = { navigateTopLevel(AppRoute.Home.route) },
            onNavigate = { route -> navigate(route) },
            onSelectTopTab = { route -> navigateTopLevel(route) },
        )

        AppRoute.MessageWallEditor -> MessageWallEditorRoute(
            dependencies = dependencies,
            snackbarHostState = snackbarHostState,
            onBack = ::popBack,
        )

        AppRoute.WishList -> WishListRoute(
            dependencies = dependencies,
            snackbarHostState = snackbarHostState,
            onBack = { navigateTopLevel(AppRoute.Home.route) },
            onNavigate = { route -> navigate(route) },
            onSelectTopTab = { route -> navigateTopLevel(route) },
        )

        is AppRoute.WishDetail -> {
            val wishListViewModel: WishListViewModel = viewModel(
                key = "wish-list-${dependencies.currentUserId}",
                factory = WishListViewModel.factory(
                    homeRepository = dependencies.homeRepository,
                    wishRepository = dependencies.wishRepository,
                ),
            )
            val wishState = wishListViewModel.uiState.collectAsStateWithLifecycle()
            CollectSnackbarMessages(wishListViewModel, snackbarHostState)
            val wish = wishListViewModel.wishById(currentRoute.wishId)
            if (wish != null) {
                WishDetailScreen(
                    wish = wish,
                    illustrationRes = wishState.value.visuals.hero.imageRes,
                    onBack = ::popBack,
                    onEdit = {
                        wishListViewModel.editWish(currentRoute.wishId)
                        popBack()
                    },
                    onComplete = {
                        wishListViewModel.startCompletion(currentRoute.wishId)
                        navigate(AppRoute.WishCompletion(currentRoute.wishId).route)
                    },
                )
            } else {
                LaunchedEffect(currentRoute.wishId) { popBack() }
            }
        }

        is AppRoute.WishCompletion -> {
            val wishListViewModel: WishListViewModel = viewModel(
                key = "wish-list-${dependencies.currentUserId}",
                factory = WishListViewModel.factory(
                    homeRepository = dependencies.homeRepository,
                    wishRepository = dependencies.wishRepository,
                ),
            )
            val wishState = wishListViewModel.uiState.collectAsStateWithLifecycle()
            CollectSnackbarMessages(wishListViewModel, snackbarHostState)
            val wish = wishListViewModel.wishById(currentRoute.wishId)
            if (wish != null) {
                WishCompletionScreen(
                    wish = wish,
                    illustrationRes = wishState.value.visuals.hero.imageRes,
                    completionText = wishState.value.completionText,
                    completionImages = wishState.value.completionImagePaths,
                    completionDate = wishState.value.completionDate,
                    isSaving = wishState.value.isSaving,
                    onBack = {
                        wishListViewModel.resetCompletionDraft()
                        popBack()
                    },
                    onCompletionTextChange = wishListViewModel::updateCompletionText,
                    onAddImages = wishListViewModel::addCompletionImages,
                    onRemoveImage = wishListViewModel::removeCompletionImage,
                    onCompletionDateChange = wishListViewModel::updateCompletionDate,
                    onSave = {
                        wishListViewModel.saveCompletion(currentRoute.wishId) {
                            popToWishList()
                        }
                    },
                )
            } else {
                LaunchedEffect(currentRoute.wishId) { popBack() }
            }
        }

        is AppRoute.WishCompletionDetail -> {
            val wishListViewModel: WishListViewModel = viewModel(
                key = "wish-list-${dependencies.currentUserId}",
                factory = WishListViewModel.factory(
                    homeRepository = dependencies.homeRepository,
                    wishRepository = dependencies.wishRepository,
                ),
            )
            val wishState = wishListViewModel.uiState.collectAsStateWithLifecycle()
            CollectSnackbarMessages(wishListViewModel, snackbarHostState)
            val wish = wishListViewModel.wishById(currentRoute.wishId)
            if (wish != null) {
                WishCompletionDetailScreen(
                    wish = wish,
                    illustrationRes = wishState.value.visuals.hero.imageRes,
                    onBack = ::popBack,
                )
            } else {
                LaunchedEffect(currentRoute.wishId) { popBack() }
            }
        }

        AppRoute.Anniversary -> AnniversaryRoute(
            dependencies = dependencies,
            snackbarHostState = snackbarHostState,
            onBack = { navigateTopLevel(AppRoute.Home.route) },
            onSelectTopTab = { route -> navigateTopLevel(route) },
        )

        AppRoute.RecentActivities -> RecentActivityRoute(
            dependencies = dependencies,
            snackbarHostState = snackbarHostState,
            onBack = ::popBack,
            onNavigate = { route -> navigate(route) },
        )
    }
}

@Composable
private fun HomeRoute(
    dependencies: SecretBaseDependencies,
    snackbarHostState: SnackbarHostState,
    onNavigate: (String) -> Unit,
    onSelectTopTab: (String) -> Unit,
) {
    val homeViewModel: HomeViewModel = viewModel(
        key = "home-${dependencies.currentUserId}",
        factory = HomeViewModel.factory(
            homeRepository = dependencies.homeRepository,
            messageRepository = dependencies.messageRepository,
            wishRepository = dependencies.wishRepository,
            anniversaryRepository = dependencies.anniversaryRepository,
            currentUserId = dependencies.currentUserId,
        ),
    )
    val homeState = homeViewModel.uiState.collectAsStateWithLifecycle()
    CollectSnackbarMessages(homeViewModel, snackbarHostState)

    HomeScreen(
        uiState = homeState.value,
        snackbarHostState = snackbarHostState,
        onRetry = homeViewModel::refresh,
        onMoodCardClick = homeViewModel::onMoodCardClick,
        onMoodSelected = homeViewModel::updateMood,
        onDismissMoodPicker = homeViewModel::dismissMoodPicker,
        onQuickNoteChange = homeViewModel::updateQuickNote,
        onQuickNoteSubmit = homeViewModel::submitQuickNote,
        onPlaceholderAction = { action ->
            when (action) {
                AppActions.OpenMessageWall -> onSelectTopTab(AppRoute.MessageWall.route)
                AppActions.OpenWishList -> onSelectTopTab(AppRoute.WishList.route)
                AppActions.OpenAnniversary -> onSelectTopTab(AppRoute.Anniversary.route)
                AppActions.OpenRecentActivities -> onNavigate(AppRoute.RecentActivities.route)
                else -> homeViewModel.onPlaceholderAction(action)
            }
        },
    )
}

@Composable
private fun RecentActivityRoute(
    dependencies: SecretBaseDependencies,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val homeViewModel: HomeViewModel = viewModel(
        key = "home-${dependencies.currentUserId}",
        factory = HomeViewModel.factory(
            homeRepository = dependencies.homeRepository,
            messageRepository = dependencies.messageRepository,
            wishRepository = dependencies.wishRepository,
            anniversaryRepository = dependencies.anniversaryRepository,
            currentUserId = dependencies.currentUserId,
        ),
    )
    val homeState = homeViewModel.uiState.collectAsStateWithLifecycle()
    CollectSnackbarMessages(homeViewModel, snackbarHostState)

    RecentActivityScreen(
        payload = homeState.value.payload,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onActivityClick = { action ->
            when (action) {
                AppActions.OpenMessageWall -> onNavigate(AppRoute.MessageWall.route)
                AppActions.OpenWishList -> onNavigate(AppRoute.WishList.route)
                AppActions.OpenAnniversary -> onNavigate(AppRoute.Anniversary.route)
                else -> homeViewModel.onPlaceholderAction(action)
            }
        },
    )
}

@Composable
private fun MessageWallRoute(
    dependencies: SecretBaseDependencies,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onSelectTopTab: (String) -> Unit,
) {
    val messageWallViewModel: MessageWallViewModel = viewModel(
        key = "message-wall-${dependencies.currentUserId}",
        factory = MessageWallViewModel.factory(
            homeRepository = dependencies.homeRepository,
            messageRepository = dependencies.messageRepository,
            currentUserId = dependencies.currentUserId,
        ),
    )
    val messageWallState = messageWallViewModel.uiState.collectAsStateWithLifecycle()
    CollectSnackbarMessages(messageWallViewModel, snackbarHostState)

    MessageWallScreen(
        uiState = messageWallState.value,
        snackbarHostState = snackbarHostState,
        bottomBar = {
            MainBottomTabBar(
                activeRoute = AppRoute.MessageWall.route,
                onSelect = onSelectTopTab,
            )
        },
        onBack = onBack,
        onOpenEditor = { onNavigate(AppRoute.MessageWallEditor.route) },
        onReplyClick = messageWallViewModel::openReplyComposer,
        onReplyTextChange = messageWallViewModel::updateReplyText,
        onSendReply = messageWallViewModel::sendReply,
        onCancelReply = messageWallViewModel::closeReplyComposer,
        onToggleReplies = messageWallViewModel::toggleExpandedReplies,
        onDeleteMessage = messageWallViewModel::deleteMessage,
        onDeleteReply = messageWallViewModel::deleteReply,
        onStartEditing = messageWallViewModel::startEditing,
        onMarkMessageRead = messageWallViewModel::markMessageRead,
        onUpdateEditingText = messageWallViewModel::updateEditingText,
        onCancelEditing = messageWallViewModel::cancelEditing,
        onSaveEditing = messageWallViewModel::saveEditing,
    )
}

@Composable
private fun MessageWallEditorRoute(
    dependencies: SecretBaseDependencies,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
) {
    val messageWallViewModel: MessageWallViewModel = viewModel(
        key = "message-wall-${dependencies.currentUserId}",
        factory = MessageWallViewModel.factory(
            homeRepository = dependencies.homeRepository,
            messageRepository = dependencies.messageRepository,
            currentUserId = dependencies.currentUserId,
        ),
    )
    val messageWallState = messageWallViewModel.uiState.collectAsStateWithLifecycle()
    CollectSnackbarMessages(messageWallViewModel, snackbarHostState)

    MessageWallEditorScreen(
        uiState = messageWallState.value,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onDraftTextChange = messageWallViewModel::updateDraftText,
        onAddImages = messageWallViewModel::addSelectedImages,
        onRemoveSelectedImage = messageWallViewModel::removeSelectedImage,
        onPublish = { messageWallViewModel.publishMessage(onSuccess = onBack) },
    )
}

@Composable
private fun WishListRoute(
    dependencies: SecretBaseDependencies,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onSelectTopTab: (String) -> Unit,
) {
    val wishListViewModel: WishListViewModel = viewModel(
        key = "wish-list-${dependencies.currentUserId}",
        factory = WishListViewModel.factory(
            homeRepository = dependencies.homeRepository,
            wishRepository = dependencies.wishRepository,
        ),
    )
    val wishState = wishListViewModel.uiState.collectAsStateWithLifecycle()
    CollectSnackbarMessages(wishListViewModel, snackbarHostState)

    WishListScreen(
        uiState = wishState.value,
        snackbarHostState = snackbarHostState,
        bottomBar = {
            MainBottomTabBar(
                activeRoute = AppRoute.WishList.route,
                onSelect = onSelectTopTab,
            )
        },
        onBack = onBack,
        onSelectStatus = wishListViewModel::selectStatus,
        onAddWish = wishListViewModel::showAddWish,
        onWishClick = { wishId ->
            val wish = wishListViewModel.wishById(wishId) ?: return@WishListScreen
            if (wish.status == WishStatus.REALIZED) {
                onNavigate(AppRoute.WishCompletionDetail(wishId).route)
            } else {
                onNavigate(AppRoute.WishDetail(wishId).route)
            }
        },
        onEditWish = wishListViewModel::editWish,
        onDeleteWish = wishListViewModel::deleteWish,
        onCompleteWish = { wishId ->
            onNavigate(AppRoute.WishDetail(wishId).route)
        },
        onEditorTitleChange = wishListViewModel::updateEditorTitle,
        onEditorDescriptionChange = wishListViewModel::updateEditorDescription,
        onEditorPlannedDateChange = wishListViewModel::updateEditorPlannedDate,
        onEditorCoverChange = wishListViewModel::updateEditorCover,
        onDismissEditor = wishListViewModel::dismissEditor,
        onSaveWish = wishListViewModel::saveWish,
    )
}

@Composable
private fun AnniversaryRoute(
    dependencies: SecretBaseDependencies,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSelectTopTab: (String) -> Unit,
) {
    val anniversaryViewModel: AnniversaryViewModel = viewModel(
        key = "anniversary-${dependencies.currentUserId}",
        factory = AnniversaryViewModel.factory(
            homeRepository = dependencies.homeRepository,
            anniversaryRepository = dependencies.anniversaryRepository,
        ),
    )
    val anniversaryState = anniversaryViewModel.uiState.collectAsStateWithLifecycle()
    CollectSnackbarMessages(anniversaryViewModel, snackbarHostState)

    AnniversaryScreen(
        uiState = anniversaryState.value,
        snackbarHostState = snackbarHostState,
        bottomBar = {
            MainBottomTabBar(
                activeRoute = AppRoute.Anniversary.route,
                onSelect = onSelectTopTab,
            )
        },
        onBack = onBack,
        onAdd = anniversaryViewModel::showAddEditor,
        onEdit = anniversaryViewModel::editItem,
        onDelete = anniversaryViewModel::deleteItem,
        onTitleChange = anniversaryViewModel::updateTitle,
        onDateChange = anniversaryViewModel::updateDate,
        onIconChange = anniversaryViewModel::updateIconEmoji,
        onRepeatChange = anniversaryViewModel::toggleRepeat,
        onReminderChange = anniversaryViewModel::updateReminder,
        onDismissEditor = anniversaryViewModel::dismissEditor,
        onSaveEditor = anniversaryViewModel::saveEditor,
    )
}

@Composable
private fun MainBottomTabBar(
    activeRoute: String,
    onSelect: (String) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = SurfaceWhite,
        shadowElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MainBottomTabItem(
                label = "首页",
                iconRes = R.drawable.ic_home_nav_active,
                active = activeRoute == AppRoute.Home.route,
                onClick = { onSelect(AppRoute.Home.route) },
            )
            MainBottomTabItem(
                label = "留言墙",
                iconRes = R.drawable.ic_message_wall_card,
                active = activeRoute == AppRoute.MessageWall.route,
                onClick = { onSelect(AppRoute.MessageWall.route) },
            )
            MainBottomTabItem(
                label = "愿望清单",
                iconRes = R.drawable.ic_wishlist_card,
                active = activeRoute == AppRoute.WishList.route,
                onClick = { onSelect(AppRoute.WishList.route) },
            )
            MainBottomTabItem(
                label = "纪念日",
                iconRes = R.drawable.ic_calendar_nav,
                active = activeRoute == AppRoute.Anniversary.route,
                onClick = { onSelect(AppRoute.Anniversary.route) },
            )
        }
    }
}

@Composable
private fun MainBottomTabItem(
    label: String,
    iconRes: Int,
    active: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.height(52.dp),
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                ),
                color = if (active) CherryPink else WarmGray,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CollectSnackbarMessages(
    viewModel: HomeViewModel,
    snackbarHostState: SnackbarHostState,
) {
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
}

@Composable
private fun CollectSnackbarMessages(
    viewModel: MessageWallViewModel,
    snackbarHostState: SnackbarHostState,
) {
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
}

@Composable
private fun CollectSnackbarMessages(
    viewModel: WishListViewModel,
    snackbarHostState: SnackbarHostState,
) {
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
}

@Composable
private fun CollectSnackbarMessages(
    viewModel: AnniversaryViewModel,
    snackbarHostState: SnackbarHostState,
) {
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
}

private sealed class AppRoute(val route: String) {
    data object Home : AppRoute("home")
    data object MessageWall : AppRoute("message-wall")
    data object MessageWallEditor : AppRoute("message-wall/editor")
    data object WishList : AppRoute("wish-list")
    data object Anniversary : AppRoute("anniversary")
    data object RecentActivities : AppRoute("recent-activities")
    data class WishDetail(val wishId: String) : AppRoute("wish-detail/$wishId")
    data class WishCompletion(val wishId: String) : AppRoute("wish-complete/$wishId")
    data class WishCompletionDetail(val wishId: String) : AppRoute("wish-completion-detail/$wishId")

    companion object {
        fun parse(route: String): AppRoute = when {
            route == Home.route -> Home
            route == MessageWall.route -> MessageWall
            route == MessageWallEditor.route -> MessageWallEditor
            route == WishList.route -> WishList
            route == Anniversary.route -> Anniversary
            route == RecentActivities.route -> RecentActivities
            route.startsWith("wish-detail/") -> WishDetail(route.substringAfter("wish-detail/"))
            route.startsWith("wish-complete/") -> WishCompletion(route.substringAfter("wish-complete/"))
            route.startsWith("wish-completion-detail/") -> WishCompletionDetail(route.substringAfter("wish-completion-detail/"))
            else -> Home
        }
    }
}

@Composable
private fun IdentitySelectionGate(
    onIdentitySelected: (String) -> Unit,
) {
    var pendingSelection by rememberSaveable { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF3F6),
                        Color(0xFFFFFAFB),
                    ),
                ),
            ),
    )

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = Color.White,
            shadowElevation = 22.dp,
        ) {
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .padding(horizontal = 24.dp, vertical = 28.dp),
            ) {
                Text(
                    text = "\u8bf7\u9009\u62e9\u5f53\u524d\u8eab\u4efd",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "\u8fd9\u6837\u7559\u8a00\u3001\u56de\u590d\u548c\u5fc3\u60c5\u624d\u4f1a\u5199\u5165\u6b63\u786e\u7684\u6570\u636e\u5e93\u8eab\u4efd\u3002",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8C8690),
                )
                Spacer(modifier = Modifier.height(20.dp))
                IdentityOptionCard(
                    userId = SecretBaseUsers.SHEEP_ID,
                    selected = pendingSelection == SecretBaseUsers.SHEEP_ID,
                    onClick = { pendingSelection = SecretBaseUsers.SHEEP_ID },
                )
                Spacer(modifier = Modifier.height(12.dp))
                IdentityOptionCard(
                    userId = SecretBaseUsers.CHICK_ID,
                    selected = pendingSelection == SecretBaseUsers.CHICK_ID,
                    onClick = { pendingSelection = SecretBaseUsers.CHICK_ID },
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { pendingSelection?.let(onIdentitySelected) },
                    enabled = pendingSelection != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "\u8fdb\u5165\u79d8\u5bc6\u57fa\u5730")
                }
            }
        }
    }
}

@Composable
private fun StartupErrorScreen(error: Throwable?) {
    val rootCause = remember(error) { error?.rootCause() }
    val diagnosticText = remember(rootCause) {
        buildString {
            append("\u7248\u672c\uff1a")
            append(BuildConfig.VERSION_NAME)
            append(" (")
            append(BuildConfig.VERSION_CODE)
            appendLine(")")
            append("\u4e91\u7aef\uff1a")
            appendLine(if (SupabaseConfig.isConfigured) "\u5df2\u914d\u7f6e" else "\u672a\u914d\u7f6e")
            if (SupabaseConfig.url.isNotBlank()) {
                append("URL\uff1a")
                appendLine(SupabaseConfig.url)
            }
            if (rootCause != null) {
                append("\u9519\u8bef\uff1a")
                appendLine(rootCause.javaClass.simpleName)
                rootCause.message
                    ?.take(260)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { message ->
                        append("\u4fe1\u606f\uff1a")
                        append(message)
                    }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8FA))
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 14.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "\u4e91\u7aef\u8fde\u63a5\u5931\u8d25",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF242128),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "\u8fd9\u4e2a\u5b89\u88c5\u5305\u5df2\u914d\u7f6e Supabase\uff0c\u4f46\u542f\u52a8\u65f6\u6ca1\u80fd\u521d\u59cb\u5316\u4e91\u7aef\u8fde\u63a5\u3002\u8bf7\u786e\u8ba4\u7f51\u7edc\u540e\u91cd\u65b0\u6253\u5f00\u5e94\u7528\u3002",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF7B747E),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = diagnosticText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9A929B),
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFFF7F9))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
        }
    }
}

private fun Throwable.rootCause(): Throwable {
    var current = this
    while (current.cause != null && current.cause !== current) {
        current = current.cause ?: current
    }
    return current
}

@Composable
private fun IdentityOptionCard(
    userId: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) Color(0xFFFF91AF) else Color(0xFFEAE5E9)
    val backgroundColor = if (selected) Color(0xFFFFF4F7) else Color.White
    val accentColor = if (selected) Color(0xFFFF7FA2) else Color(0xFFB9B2BA)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (selected) Color(0xFFFFE6EE) else Color(0xFFF6F4F6)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = SecretBaseUsers.nameFor(userId).take(1),
                style = MaterialTheme.typography.titleMedium,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = SecretBaseUsers.nameFor(userId),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2B2630),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (userId == SecretBaseUsers.SHEEP_ID) {
                    "\u4ee5\u5c0f\u7f8a\u7684\u8eab\u4efd\u8fdb\u5165"
                } else {
                    "\u4ee5\u5c0f\u8036\u7684\u8eab\u4efd\u8fdb\u5165"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8C8690),
            )
        }
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (selected) Color(0xFFFF86A8) else Color(0xFFF2EDF0)),
        )
    }
}
