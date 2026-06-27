package com.secretbase.app

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secretbase.app.data.wish.WishStatus
import com.secretbase.app.ui.anniversary.AnniversaryScreen
import com.secretbase.app.ui.anniversary.AnniversaryViewModel
import com.secretbase.app.ui.home.HomeScreen
import com.secretbase.app.ui.home.HomeViewModel
import com.secretbase.app.ui.messagewall.MessageWallScreen
import com.secretbase.app.ui.messagewall.MessageWallViewModel
import com.secretbase.app.ui.wishlist.WishCompletionDetailScreen
import com.secretbase.app.ui.wishlist.WishCompletionScreen
import com.secretbase.app.ui.wishlist.WishDetailScreen
import com.secretbase.app.ui.wishlist.WishListScreen
import com.secretbase.app.ui.wishlist.WishListViewModel

@Composable
fun SecretBaseApp() {
    val appContext = LocalContext.current.applicationContext
    val dependencies = remember(appContext) { SecretBaseDependencies(appContext) }
    val snackbarHostState = remember { SnackbarHostState() }
    var backStack by rememberSaveable { mutableStateOf(listOf(AppRoute.Home.route)) }

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(
            homeRepository = dependencies.homeRepository,
            messageRepository = dependencies.messageRepository,
            wishRepository = dependencies.wishRepository,
        ),
    )
    val messageWallViewModel: MessageWallViewModel = viewModel(
        factory = MessageWallViewModel.factory(
            homeRepository = dependencies.homeRepository,
            messageRepository = dependencies.messageRepository,
        ),
    )
    val wishListViewModel: WishListViewModel = viewModel(
        factory = WishListViewModel.factory(
            homeRepository = dependencies.homeRepository,
            wishRepository = dependencies.wishRepository,
        ),
    )
    val anniversaryViewModel: AnniversaryViewModel = viewModel(
        factory = AnniversaryViewModel.factory(
            homeRepository = dependencies.homeRepository,
            anniversaryRepository = dependencies.anniversaryRepository,
        ),
    )

    val homeState = homeViewModel.uiState.collectAsStateWithLifecycle()
    val messageWallState = messageWallViewModel.uiState.collectAsStateWithLifecycle()
    val wishState = wishListViewModel.uiState.collectAsStateWithLifecycle()
    val anniversaryState = anniversaryViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(homeViewModel) {
        homeViewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(messageWallViewModel) {
        messageWallViewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(wishListViewModel) {
        wishListViewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(anniversaryViewModel) {
        anniversaryViewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val currentRoute = remember(backStack) { AppRoute.parse(backStack.last()) }

    fun navigate(route: String) {
        if (backStack.lastOrNull() != route) {
            backStack = backStack + route
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
        AppRoute.Home -> HomeScreen(
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
                    AppActions.OpenMessageWall -> navigate(AppRoute.MessageWall.route)
                    AppActions.OpenWishList -> navigate(AppRoute.WishList.route)
                    AppActions.OpenAnniversary -> navigate(AppRoute.Anniversary.route)
                    else -> homeViewModel.onPlaceholderAction(action)
                }
            },
        )

        AppRoute.MessageWall -> MessageWallScreen(
            uiState = messageWallState.value,
            snackbarHostState = snackbarHostState,
            onBack = ::popBack,
            onDraftTextChange = messageWallViewModel::updateDraftText,
            onAddImages = messageWallViewModel::addSelectedImages,
            onRemoveSelectedImage = messageWallViewModel::removeSelectedImage,
            onPublish = messageWallViewModel::publishMessage,
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

        AppRoute.WishList -> WishListScreen(
            uiState = wishState.value,
            snackbarHostState = snackbarHostState,
            onBack = ::popBack,
            onSelectStatus = wishListViewModel::selectStatus,
            onAddWish = wishListViewModel::showAddWish,
            onWishClick = { wishId ->
                val wish = wishListViewModel.wishById(wishId) ?: return@WishListScreen
                if (wish.status == WishStatus.REALIZED) {
                    navigate(AppRoute.WishCompletionDetail(wishId).route)
                } else {
                    navigate(AppRoute.WishDetail(wishId).route)
                }
            },
            onEditWish = wishListViewModel::editWish,
            onDeleteWish = wishListViewModel::deleteWish,
            onCompleteWish = { wishId ->
                navigate(AppRoute.WishDetail(wishId).route)
            },
            onEditorTitleChange = wishListViewModel::updateEditorTitle,
            onEditorDescriptionChange = wishListViewModel::updateEditorDescription,
            onEditorPlannedDateChange = wishListViewModel::updateEditorPlannedDate,
            onEditorCoverChange = wishListViewModel::updateEditorCover,
            onDismissEditor = wishListViewModel::dismissEditor,
            onSaveWish = wishListViewModel::saveWish,
        )

        is AppRoute.WishDetail -> {
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

        AppRoute.Anniversary -> AnniversaryScreen(
            uiState = anniversaryState.value,
            snackbarHostState = snackbarHostState,
            onBack = ::popBack,
            onAdd = anniversaryViewModel::showAddEditor,
            onEdit = anniversaryViewModel::editItem,
            onDelete = anniversaryViewModel::deleteItem,
            onTitleChange = anniversaryViewModel::updateTitle,
            onDateChange = anniversaryViewModel::updateDate,
            onRepeatChange = anniversaryViewModel::toggleRepeat,
            onReminderChange = anniversaryViewModel::updateReminder,
            onDismissEditor = anniversaryViewModel::dismissEditor,
            onSaveEditor = anniversaryViewModel::saveEditor,
        )
    }
}

private sealed class AppRoute(val route: String) {
    data object Home : AppRoute("home")
    data object MessageWall : AppRoute("message-wall")
    data object WishList : AppRoute("wish-list")
    data object Anniversary : AppRoute("anniversary")
    data class WishDetail(val wishId: String) : AppRoute("wish-detail/$wishId")
    data class WishCompletion(val wishId: String) : AppRoute("wish-complete/$wishId")
    data class WishCompletionDetail(val wishId: String) : AppRoute("wish-completion-detail/$wishId")

    companion object {
        fun parse(route: String): AppRoute = when {
            route == Home.route -> Home
            route == MessageWall.route -> MessageWall
            route == WishList.route -> WishList
            route == Anniversary.route -> Anniversary
            route.startsWith("wish-detail/") -> WishDetail(route.substringAfter("wish-detail/"))
            route.startsWith("wish-complete/") -> WishCompletion(route.substringAfter("wish-complete/"))
            route.startsWith("wish-completion-detail/") -> WishCompletionDetail(route.substringAfter("wish-completion-detail/"))
            else -> Home
        }
    }
}
