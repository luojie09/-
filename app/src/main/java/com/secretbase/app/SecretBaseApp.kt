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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secretbase.app.data.message.SecretBaseUsers
import com.secretbase.app.data.supabase.SupabaseConfig
import com.secretbase.app.data.supabase.SupabaseAuthManager
import com.secretbase.app.data.sync.SyncPhase
import com.secretbase.app.data.sync.SyncStatus
import com.secretbase.app.data.user.UserIdentityStore
import com.secretbase.app.data.wish.WishStatus
import com.secretbase.app.ui.anniversary.AnniversaryScreen
import com.secretbase.app.ui.anniversary.AnniversaryViewModel
import com.secretbase.app.ui.home.HomeScreen
import com.secretbase.app.ui.home.HomeViewModel
import com.secretbase.app.ui.home.RecentActivityScreen
import com.secretbase.app.ui.home.RecentActivityDetailScreen
import com.secretbase.app.ui.messagewall.MessageWallEditorScreen
import com.secretbase.app.ui.messagewall.MessageWallScreen
import com.secretbase.app.ui.messagewall.MessageWallViewModel
import com.secretbase.app.ui.settings.IdentitySettingsScreen
import com.secretbase.app.ui.wishlist.WishCompletionDetailScreen
import com.secretbase.app.ui.wishlist.WishCompletionScreen
import com.secretbase.app.ui.wishlist.WishDetailScreen
import com.secretbase.app.ui.wishlist.WishListScreen
import com.secretbase.app.ui.wishlist.WishListViewModel
import com.secretbase.app.ui.theme.CherryPink
import com.secretbase.app.ui.theme.SurfaceWhite
import com.secretbase.app.ui.theme.WarmGray
import kotlinx.coroutines.launch

@Composable
fun SecretBaseApp(
    initialRoute: String? = null,
    onInitialRouteConsumed: () -> Unit = {},
) {
    val appContext = LocalContext.current.applicationContext
    val userIdentityStore = remember(appContext) { UserIdentityStore(appContext) }
    val authManager = remember(userIdentityStore) { SupabaseAuthManager(userIdentityStore) }
    val initialUserId = remember(userIdentityStore) { userIdentityStore.currentUserId() }
    var currentUserId by rememberSaveable {
        mutableStateOf(
            initialUserId?.takeIf {
                !SupabaseConfig.isConfigured || userIdentityStore.hasAuthenticatedIdentity()
            },
        )
    }
    var isBindingIdentity by rememberSaveable { mutableStateOf(false) }
    var identityError by rememberSaveable { mutableStateOf<String?>(null) }
    var identitySettingsError by rememberSaveable { mutableStateOf<String?>(null) }
    var isSwitchingIdentity by rememberSaveable { mutableStateOf(false) }
    var isValidatingIdentity by rememberSaveable {
        mutableStateOf(SupabaseConfig.isConfigured && userIdentityStore.hasAuthenticatedIdentity())
    }
    val appScope = rememberCoroutineScope()

    LaunchedEffect(isValidatingIdentity) {
        if (!isValidatingIdentity) return@LaunchedEffect
        runCatching { authManager.validAccessToken() }
            .onSuccess {
                isValidatingIdentity = false
            }
            .onFailure {
                userIdentityStore.clearAuthSessionKeepingRole()
                identityError = "身份会话已过期，请使用配对码重新连接"
                currentUserId = null
                isValidatingIdentity = false
            }
    }

    if (isValidatingIdentity) {
        IdentityConnectionProgress()
        return
    }

    if (currentUserId == null) {
        IdentitySelectionGate(
            initialSelection = initialUserId,
            requiresPairingCode = SupabaseConfig.isConfigured,
            isBinding = isBindingIdentity,
            errorMessage = identityError,
            onIdentitySelected = { selectedUserId, pairingCode ->
                if (!SupabaseConfig.isConfigured) {
                    userIdentityStore.saveCurrentUserId(selectedUserId)
                    currentUserId = selectedUserId
                    return@IdentitySelectionGate
                }

                appScope.launch {
                    isBindingIdentity = true
                    identityError = null
                    authManager.bindIdentity(
                        role = selectedUserId,
                        pairingCode = pairingCode,
                    ).onSuccess { identity ->
                        currentUserId = identity.role
                    }.onFailure { error ->
                        identityError = error.message ?: "配对失败，请检查配对码和网络"
                    }
                    isBindingIdentity = false
                }
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
                authManager = authManager,
                coupleId = userIdentityStore.coupleId(),
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
    DisposableEffect(dependencies) {
        onDispose(dependencies::close)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val syncStatus by dependencies.syncStatus.collectAsStateWithLifecycle(initialValue = SyncStatus())
    var backStack by rememberSaveable { mutableStateOf(listOf(AppRoute.Home.route)) }

    LaunchedEffect(initialRoute) {
        initialRoute?.let { route ->
            backStack = listOf(route)
            onInitialRouteConsumed()
        }
    }

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

    Box(modifier = Modifier.fillMaxSize()) {
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

        is AppRoute.ActivityDetail -> {
            val homeViewModel: HomeViewModel = viewModel(
                key = "home-${dependencies.currentUserId}",
                factory = HomeViewModel.factory(
                    homeRepository = dependencies.homeRepository,
                    messageRepository = dependencies.messageRepository,
                    wishRepository = dependencies.wishRepository,
                    anniversaryRepository = dependencies.anniversaryRepository,
                    currentUserId = dependencies.currentUserId,
                    remoteRefreshEvents = dependencies.remoteRefreshEvents,
                ),
            )
            val homeState = homeViewModel.uiState.collectAsStateWithLifecycle()
            CollectSnackbarMessages(homeViewModel, snackbarHostState)
            val activity = homeState.value.payload?.allActivities
                ?.firstOrNull { it.id == currentRoute.activityId }
            if (activity != null) {
                RecentActivityDetailScreen(
                    activity = activity,
                    snackbarHostState = snackbarHostState,
                    onBack = ::popBack,
                    onOpenSource = activity.sourceAction?.let { action ->
                        {
                        val wishId = AppActions.wishCompletionDetailId(action)
                        when {
                            wishId != null -> navigate(AppRoute.WishCompletionDetail(wishId).route)
                            action == AppActions.OpenMessageWall -> navigateTopLevel(AppRoute.MessageWall.route)
                            action == AppActions.OpenWishList -> navigateTopLevel(AppRoute.WishList.route)
                            action == AppActions.OpenAnniversary -> navigateTopLevel(AppRoute.Anniversary.route)
                            else -> homeViewModel.onPlaceholderAction(action)
                        }
                        }
                    },
                )
            } else if (!homeState.value.isLoading) {
                LaunchedEffect(currentRoute.activityId) { popBack() }
            }
        }

        AppRoute.IdentitySettings -> IdentitySettingsScreen(
            currentUserId = selectedUserId,
            coupleId = userIdentityStore.coupleId(),
            sheepAvatarRes = R.drawable.avatar_sheep,
            chickAvatarRes = R.drawable.avatar_chick,
            requiresPairingCode = SupabaseConfig.isConfigured,
            isSaving = isSwitchingIdentity,
            errorMessage = identitySettingsError,
            onBack = ::popBack,
            onSwitchIdentity = { role, pairingCode ->
                identitySettingsError = null
                if (!SupabaseConfig.isConfigured) {
                    userIdentityStore.saveCurrentUserId(role)
                    currentUserId = role
                    backStack = listOf(AppRoute.Home.route)
                } else {
                    appScope.launch {
                        isSwitchingIdentity = true
                        authManager.bindIdentity(role, pairingCode)
                            .onSuccess { identity ->
                                currentUserId = identity.role
                                backStack = listOf(AppRoute.Home.route)
                            }
                            .onFailure { error ->
                                identitySettingsError = error.message ?: "身份切换失败，请检查配对码和网络"
                            }
                        isSwitchingIdentity = false
                    }
                }
            },
        )
    }
        SyncStatusIndicator(
            status = syncStatus,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 76.dp),
        )
    }
}

@Composable
private fun SyncStatusIndicator(
    status: SyncStatus,
    modifier: Modifier = Modifier,
) {
    if (status.phase == SyncPhase.SYNCED) return
    val label = when (status.phase) {
        SyncPhase.SYNCING -> "正在同步"
        SyncPhase.PENDING -> "${status.pendingCount} 项等待同步"
        SyncPhase.ERROR -> "同步失败，将自动重试"
        SyncPhase.SYNCED -> return
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = SurfaceWhite.copy(alpha = 0.96f),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (status.phase == SyncPhase.SYNCING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = CherryPink,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (status.phase == SyncPhase.ERROR) CherryPink else WarmGray,
            )
        }
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
            remoteRefreshEvents = dependencies.remoteRefreshEvents,
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
            val activityId = AppActions.activityDetailId(action)
            when {
                activityId != null -> onNavigate(AppRoute.ActivityDetail(activityId).route)
                action == AppActions.OpenMessageWall -> onSelectTopTab(AppRoute.MessageWall.route)
                action == AppActions.OpenWishList -> onSelectTopTab(AppRoute.WishList.route)
                action == AppActions.OpenAnniversary -> onSelectTopTab(AppRoute.Anniversary.route)
                action == AppActions.OpenRecentActivities -> onNavigate(AppRoute.RecentActivities.route)
                action == AppActions.OpenIdentitySettings -> onNavigate(AppRoute.IdentitySettings.route)
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
            val activityId = AppActions.activityDetailId(action)
            when {
                activityId != null -> onNavigate(AppRoute.ActivityDetail(activityId).route)
                action == AppActions.OpenMessageWall -> onNavigate(AppRoute.MessageWall.route)
                action == AppActions.OpenWishList -> onNavigate(AppRoute.WishList.route)
                action == AppActions.OpenAnniversary -> onNavigate(AppRoute.Anniversary.route)
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
        onToggleLike = messageWallViewModel::toggleLike,
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
            notificationScheduler = dependencies.anniversaryNotificationScheduler,
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
internal fun MainBottomTabBar(
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
private fun RowScope.MainBottomTabItem(
    label: String,
    iconRes: Int,
    active: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 56.dp),
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
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
    data object IdentitySettings : AppRoute("identity-settings")
    data class ActivityDetail(val activityId: String) : AppRoute("activity-detail/$activityId")
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
            route == IdentitySettings.route -> IdentitySettings
            route.startsWith("activity-detail/") -> ActivityDetail(route.substringAfter("activity-detail/"))
            route.startsWith("wish-detail/") -> WishDetail(route.substringAfter("wish-detail/"))
            route.startsWith("wish-complete/") -> WishCompletion(route.substringAfter("wish-complete/"))
            route.startsWith("wish-completion-detail/") -> WishCompletionDetail(route.substringAfter("wish-completion-detail/"))
            else -> Home
        }
    }
}

@Composable
private fun IdentityConnectionProgress() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF3F6),
                        Color(0xFFFFFAFB),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = CherryPink,
                strokeWidth = 2.5.dp,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "正在连接你们的秘密基地",
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray,
            )
        }
    }
}

@Composable
private fun IdentitySelectionGate(
    initialSelection: String?,
    requiresPairingCode: Boolean,
    isBinding: Boolean,
    errorMessage: String?,
    onIdentitySelected: (String, String) -> Unit,
) {
    var pendingSelection by rememberSaveable { mutableStateOf(initialSelection) }
    var pairingCode by rememberSaveable { mutableStateOf("") }

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
                    text = if (requiresPairingCode) {
                        "选择身份并输入一次配对码，这台设备就会安全连接到你们的秘密基地。"
                    } else {
                        "这样留言、回复和心情才会写入正确的身份。"
                    },
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
                if (requiresPairingCode) {
                    Spacer(modifier = Modifier.height(18.dp))
                    OutlinedTextField(
                        value = pairingCode,
                        onValueChange = {
                            pairingCode = it.take(MAX_PAIRING_CODE_LENGTH)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBinding,
                        singleLine = true,
                        label = { Text("配对码") },
                        placeholder = { Text("只需首次绑定时输入") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = errorMessage != null,
                        supportingText = errorMessage?.let { message ->
                            { Text(message) }
                        },
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        pendingSelection?.let { selection ->
                            onIdentitySelected(selection, pairingCode)
                        }
                    },
                    enabled = pendingSelection != null &&
                        !isBinding &&
                        (!requiresPairingCode || pairingCode.isNotBlank()),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isBinding) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "正在连接")
                    } else {
                        Text(text = "进入秘密基地")
                    }
                }
            }
        }
    }
}

private const val MAX_PAIRING_CODE_LENGTH = 64

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
