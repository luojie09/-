package com.secretbase.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secretbase.app.R
import com.secretbase.app.data.HeroVisualConfig
import com.secretbase.app.data.HomeVisuals
import com.secretbase.app.data.MoodOption
import com.secretbase.app.ui.theme.CherryPink
import com.secretbase.app.ui.theme.InkBlack
import com.secretbase.app.ui.theme.OutlinePink
import com.secretbase.app.ui.theme.SecretBaseTheme
import com.secretbase.app.ui.theme.SoftPink
import com.secretbase.app.ui.theme.SoftPinkStrong
import com.secretbase.app.ui.theme.SurfaceWhite
import com.secretbase.app.ui.theme.WarmBackground
import com.secretbase.app.ui.theme.WarmGray

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    snackbarHostState: SnackbarHostState,
    onRetry: () -> Unit,
    onMoodCardClick: (String) -> Unit,
    onMoodSelected: (MoodOption) -> Unit,
    onDismissMoodPicker: () -> Unit,
    onQuickNoteChange: (String) -> Unit,
    onQuickNoteSubmit: () -> Unit,
    onPlaceholderAction: (String) -> Unit,
) {
    val payload = uiState.payload
    val backgroundColor = payload?.visuals?.hero?.gradientEndColor() ?: WarmBackground

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 96.dp),
            )
        },
        bottomBar = {
            payload?.let {
                HomeBottomBar(
                    payload = it,
                    onAction = onPlaceholderAction,
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor),
        ) {
            when {
                uiState.isLoading && payload == null -> LoadingContent(
                    modifier = Modifier.padding(innerPadding),
                )

                payload != null -> HomeContent(
                    payload = payload,
                    quickNoteText = uiState.quickNoteText,
                    innerPadding = innerPadding,
                    onMoodCardClick = onMoodCardClick,
                    onQuickNoteChange = onQuickNoteChange,
                    onQuickNoteSubmit = onQuickNoteSubmit,
                    onAction = onPlaceholderAction,
                )

                else -> ErrorContent(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    message = uiState.errorMessage ?: "加载失败，点击重试",
                    onRetry = onRetry,
                )
            }
        }

        if (uiState.editingMoodUserId != null && payload != null) {
            MoodPickerSheet(
                options = payload.moodOptions,
                onDismiss = onDismissMoodPicker,
                onMoodSelected = onMoodSelected,
            )
        }
    }
}

@Composable
private fun HomeContent(
    payload: HomePayload,
    quickNoteText: String,
    innerPadding: PaddingValues,
    onMoodCardClick: (String) -> Unit,
    onQuickNoteChange: (String) -> Unit,
    onQuickNoteSubmit: () -> Unit,
    onAction: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HomeHeroSection(
                payload = payload,
                onAction = onAction,
            )
        }

        item {
            PaddedSection {
                MoodSection(
                    moods = payload.moodCards,
                    onMoodCardClick = onMoodCardClick,
                )
            }
        }

        item {
            PaddedSection {
                QuickRecordSection(
                    placeholder = payload.quickRecord.placeholder,
                    note = quickNoteText,
                    visuals = payload.visuals,
                    onNoteChange = onQuickNoteChange,
                    onNoteSubmit = onQuickNoteSubmit,
                    onAction = onAction,
                    galleryMessage = payload.quickRecord.galleryMessage,
                    cameraMessage = payload.quickRecord.cameraMessage,
                    calendarMessage = payload.quickRecord.calendarMessage,
                )
            }
        }

        item {
            PaddedSection {
                SectionTitle(title = "核心功能", trailing = null)
            }
        }

        item {
            PaddedSection {
                FeatureGrid(
                    features = payload.featureCards,
                    onAction = onAction,
                )
            }
        }

        item {
            PaddedSection {
                SectionTitle(
                    title = "最近动态",
                    trailing = "查看全部",
                    onTrailingClick = { onAction(payload.recentActivityListMessage) },
                )
            }
        }

        if (payload.activities.isEmpty()) {
            item {
                PaddedSection {
                    EmptyActivityCard(
                        text = payload.recentActivityEmptyText,
                        heroRes = payload.visuals.hero.imageRes,
                    )
                }
            }
        } else {
            item {
                PaddedSection {
                    ActivityCard(
                        activities = payload.activities,
                        onAction = onAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaddedSection(content: @Composable () -> Unit) {
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        content()
    }
}

@Composable
private fun HomeHeroSection(
    payload: HomePayload,
    onAction: (String) -> Unit,
) {
    val overlap = payload.visuals.hero.relationshipCardOverlapDp.dp

    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            HeroBackground(
                payload = payload,
                onAction = onAction,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                RelationshipCard(
                    relationship = payload.relationship,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { measurables, constraints ->
        val heroPlaceable = measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
        val cardPlaceable = measurables[1].measure(constraints.copy(minWidth = 0, minHeight = 0))
        val overlapPx = overlap.roundToPx()
        val layoutHeight = heroPlaceable.height + cardPlaceable.height - overlapPx

        layout(width = constraints.maxWidth, height = layoutHeight) {
            heroPlaceable.placeRelative(0, 0)
            cardPlaceable.placeRelative(0, heroPlaceable.height - overlapPx)
        }
    }
}

@Composable
private fun HeroBackground(
    payload: HomePayload,
    onAction: (String) -> Unit,
) {
    val hero = payload.visuals.hero
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            hero.gradientStartColor(),
            hero.gradientMiddleColor(),
            hero.gradientEndColor(),
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradientBrush),
    ) {
        HomeHeader(
            greeting = payload.greeting,
            coupleDisplayName = payload.coupleDisplayName,
            visuals = payload.visuals,
            messageDotVisible = payload.messageDotVisible,
            topActionMessages = payload.topActionMessages,
            onAction = onAction,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(hero.heightDp.dp),
        ) {
            CoupleIllustration(hero = hero)
            BottomFadeOverlay(hero = hero)
        }
    }
}

@Composable
private fun HomeHeader(
    greeting: String,
    coupleDisplayName: String,
    visuals: HomeVisuals,
    messageDotVisible: Boolean,
    topActionMessages: com.secretbase.app.data.TopActionMessages,
    onAction: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = InkBlack,
            )
            Text(
                text = coupleDisplayName,
                style = MaterialTheme.typography.headlineLarge,
                color = InkBlack,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TopActionButton(
                iconRes = visuals.icon("message"),
                showDot = messageDotVisible,
                onClick = { onAction(topActionMessages.message) },
            )
            TopActionButton(
                iconRes = visuals.icon("settings"),
                showDot = false,
                onClick = { onAction(topActionMessages.settings) },
            )
        }
    }
}

@Composable
private fun CoupleIllustration(hero: HeroVisualConfig) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val horizontalPadding = when {
            maxWidth <= 320.dp -> 12.dp
            maxWidth <= 360.dp -> 18.dp
            maxWidth <= 393.dp -> 22.dp
            else -> 28.dp
        }

        DrawableOrFallback(
            resId = hero.imageRes,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = horizontalPadding, end = horizontalPadding, bottom = 2.dp),
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomCenter,
        )
    }
}

@Composable
private fun BoxScope.BottomFadeOverlay(hero: HeroVisualConfig) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(hero.bottomFadeHeightDp.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        hero.gradientEndColor(),
                    ),
                ),
            ),
    )
}

@Composable
private fun TopActionButton(
    iconRes: Int?,
    showDot: Boolean,
    onClick: () -> Unit,
) {
    Box {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = SurfaceWhite.copy(alpha = 0.92f),
            shadowElevation = 10.dp,
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                DrawableOrFallback(
                    resId = iconRes,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        if (showDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 2.dp)
                    .size(12.dp)
                    .background(CherryPink, CircleShape)
                    .border(2.dp, SurfaceWhite, CircleShape),
            )
        }
    }
}

@Composable
private fun RelationshipCard(
    relationship: RelationshipUiModel,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.shadow(20.dp, RoundedCornerShape(28.dp), clip = false),
        shape = RoundedCornerShape(28.dp),
        color = SurfaceWhite.copy(alpha = 0.98f),
        border = androidx.compose.foundation.BorderStroke(1.dp, SoftPink),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = relationship.label,
                style = MaterialTheme.typography.bodyLarge,
                color = InkBlack,
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = relationship.daysTogether.toString(),
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 64.sp, lineHeight = 64.sp),
                    color = InkBlack,
                )
                Text(
                    text = "天",
                    style = MaterialTheme.typography.titleLarge,
                    color = InkBlack,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Text(
                text = "从 ${relationship.startDateText} 开始",
                style = MaterialTheme.typography.bodyLarge,
                color = WarmGray,
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = SurfaceWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlinePink),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "下个纪念日：${relationship.anniversaryTitle}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = InkBlack,
                        )
                        Text(
                            text = relationship.anniversaryCountdownLabel,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = InkBlack,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(SoftPink.copy(alpha = 0.55f), RoundedCornerShape(100.dp)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(relationship.anniversaryProgress)
                                .height(10.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(CherryPink, SoftPinkStrong),
                                    ),
                                    RoundedCornerShape(100.dp),
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodSection(
    moods: List<MoodCardUiModel>,
    onMoodCardClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        moods.forEach { mood ->
            Surface(
                onClick = { onMoodCardClick(mood.userId) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                color = SurfaceWhite.copy(alpha = 0.96f),
                shadowElevation = 10.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlinePink),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AvatarImage(
                        resId = mood.avatarRes,
                        modifier = Modifier.size(54.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "${mood.displayName} · ${mood.moodLabel}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = InkBlack,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = mood.moodEmoji,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickRecordSection(
    placeholder: String,
    note: String,
    visuals: HomeVisuals,
    onNoteChange: (String) -> Unit,
    onNoteSubmit: () -> Unit,
    onAction: (String) -> Unit,
    galleryMessage: String,
    cameraMessage: String,
    calendarMessage: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceWhite.copy(alpha = 0.96f),
        shadowElevation = 10.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlinePink),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BasicTextField(
                value = note,
                onValueChange = onNoteChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = InkBlack),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onNoteSubmit() },
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (note.isBlank()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = WarmGray,
                            )
                        }
                        innerTextField()
                    }
                },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                QuickActionIcon(
                    iconRes = visuals.icon("quickGallery"),
                    onClick = { onAction(galleryMessage) },
                )
                QuickActionIcon(
                    iconRes = visuals.icon("quickCamera"),
                    onClick = { onAction(cameraMessage) },
                )
                QuickActionIcon(
                    iconRes = visuals.icon("quickCalendar"),
                    onClick = { onAction(calendarMessage) },
                )
            }
        }
    }
}

@Composable
private fun QuickActionIcon(
    iconRes: Int?,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            DrawableOrFallback(
                resId = iconRes,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    trailing: String?,
    onTrailingClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = InkBlack,
        )
        if (trailing != null && onTrailingClick != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray,
                modifier = Modifier.clickable(onClick = onTrailingClick),
            )
        }
    }
}

@Composable
private fun FeatureGrid(
    features: List<FeatureCardUiModel>,
    onAction: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        features.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { feature ->
                    Surface(
                        onClick = { onAction(feature.clickMessage) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        color = SurfaceWhite.copy(alpha = 0.98f),
                        shadowElevation = 12.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, OutlinePink),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            DrawableOrFallback(
                                resId = feature.iconRes,
                                modifier = Modifier.size(48.dp),
                            )
                            Text(
                                text = feature.title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = InkBlack,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = feature.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarmGray,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityCard(
    activities: List<ActivityUiModel>,
    onAction: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceWhite.copy(alpha = 0.98f),
        shadowElevation = 12.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlinePink),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            activities.forEachIndexed { index, activity ->
                ActivityRow(
                    activity = activity,
                    showDivider = index < activities.lastIndex,
                    onClick = { onAction(activity.clickMessage) },
                )
            }
        }
    }
}

@Composable
private fun ActivityRow(
    activity: ActivityUiModel,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(CherryPink, CircleShape),
            )
            Surface(
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceWhite,
            ) {
                DrawableOrFallback(
                    resId = activity.iconRes,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = InkBlack,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = activity.relativeTime,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGray,
                )
            }
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = WarmGray,
            )
        }

        if (showDivider) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OutlinePink),
            )
        }
    }
}

@Composable
private fun EmptyActivityCard(
    text: String,
    heroRes: Int?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceWhite.copy(alpha = 0.98f),
        shadowElevation = 12.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlinePink),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DrawableOrFallback(
                resId = heroRes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = WarmGray,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HomeBottomBar(
    payload: HomePayload,
    onAction: (String) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = SurfaceWhite.copy(alpha = 0.96f),
        shadowElevation = 18.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomNavItem(
                label = "首页",
                iconRes = payload.visuals.icon("navHome"),
                active = true,
                onClick = {},
            )
            BottomNavItem(
                label = "纪念日",
                iconRes = payload.visuals.icon("navAnniversary"),
                active = false,
                onClick = { onAction(payload.bottomNavMessages.anniversary) },
            )
            BottomNavItem(
                label = "相册",
                iconRes = payload.visuals.icon("navAlbum"),
                active = false,
                onClick = { onAction(payload.bottomNavMessages.album) },
            )
            BottomNavItem(
                label = "我的",
                iconRes = payload.visuals.icon("navProfile"),
                active = false,
                onClick = { onAction(payload.bottomNavMessages.profile) },
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    iconRes: Int?,
    active: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DrawableOrFallback(
            resId = iconRes,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            ),
            color = if (active) CherryPink else WarmGray,
        )
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SkeletonBlock(height = 36.dp, width = 72.dp)
        SkeletonBlock(height = 48.dp, width = 220.dp)
        SkeletonBlock(height = 420.dp, modifier = Modifier.fillMaxWidth())
        repeat(4) {
            SkeletonBlock(height = 110.dp, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ErrorContent(
    modifier: Modifier,
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = InkBlack,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = CherryPink),
        ) {
            Text(text = "点击重试")
        }
    }
}

@Composable
private fun SkeletonBlock(
    height: androidx.compose.ui.unit.Dp,
    width: androidx.compose.ui.unit.Dp? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(RoundedCornerShape(24.dp))
            .background(SoftPink.copy(alpha = 0.45f)),
    )
}

@Composable
private fun AvatarImage(
    resId: Int?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = SurfaceWhite,
    ) {
        DrawableOrFallback(
            resId = resId,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun DrawableOrFallback(
    resId: Int?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
) {
    Box {
        if (resId != null) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                modifier = modifier.align(alignment),
                contentScale = contentScale,
            )
        } else {
            Box(
                modifier = modifier
                    .align(alignment)
                    .background(SoftPink.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
            )
        }
    }
}

@Composable
private fun MoodPickerSheet(
    options: List<MoodOption>,
    onDismiss: () -> Unit,
    onMoodSelected: (MoodOption) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceWhite,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "选择今天的心情",
                style = MaterialTheme.typography.titleLarge,
                color = InkBlack,
            )

            options.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { option ->
                        Surface(
                            onClick = { onMoodSelected(option) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            color = SurfaceWhite,
                            shadowElevation = 8.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, OutlinePink),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = option.emoji,
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = InkBlack,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun HeroVisualConfig.gradientStartColor(): Color = gradientStartHex.toColorOrDefault(Color(0xFFFFEEF3))

private fun HeroVisualConfig.gradientMiddleColor(): Color = gradientMiddleHex.toColorOrDefault(Color(0xFFFFF7F8))

private fun HeroVisualConfig.gradientEndColor(): Color = gradientEndHex.toColorOrDefault(Color(0xFFFFFBFA))

private fun String.toColorOrDefault(default: Color): Color =
    runCatching {
        val hex = removePrefix("#")
        val argb = when (hex.length) {
            6 -> 0xFF000000 or hex.toLong(16)
            8 -> hex.toLong(16)
            else -> error("Unsupported color format")
        }
        Color(argb)
    }.getOrDefault(default)

@Preview(showBackground = true, widthDp = 320, heightDp = 852, name = "Home 320dp")
@Composable
private fun HomeScreenPreview320() {
    PreviewHomeScreen()
}

@Preview(showBackground = true, widthDp = 360, heightDp = 852, name = "Home 360dp")
@Composable
private fun HomeScreenPreview360() {
    PreviewHomeScreen()
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Home 393dp")
@Composable
private fun HomeScreenPreview393() {
    PreviewHomeScreen()
}

@Preview(showBackground = true, widthDp = 430, heightDp = 852, name = "Home 430dp")
@Composable
private fun HomeScreenPreview430() {
    PreviewHomeScreen()
}

@Composable
private fun PreviewHomeScreen() {
    SecretBaseTheme {
        HomeScreen(
            uiState = previewHomeUiState(),
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

private fun previewHomeUiState(): HomeUiState {
    val visuals = HomeVisuals(
        hero = HeroVisualConfig(
            imageRes = R.drawable.home_couple_hero,
            gradientStartHex = "#FFEEF3",
            gradientMiddleHex = "#FFF7F8",
            gradientEndHex = "#FFFBFA",
            heightDp = 250,
            bottomFadeHeightDp = 72,
            relationshipCardOverlapDp = 24,
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

    return HomeUiState(
        isLoading = false,
        payload = HomePayload(
            greeting = "早安",
            coupleDisplayName = "小羊 & 小耶",
            relationship = RelationshipUiModel(
                label = "我们已经一起走过的日子",
                daysTogether = 48,
                startDateText = "2026.05.06",
                anniversaryTitle = "恋爱一周年",
                daysUntilAnniversary = 318,
                anniversaryCountdownLabel = "还有 318 天",
                anniversaryProgress = 0.14f,
            ),
            visuals = visuals,
            quickRecord = com.secretbase.app.data.QuickRecordConfig(
                placeholder = "今天想说点什么……",
                entryMessage = "待接入快捷发布页",
                galleryMessage = "待接入从相册选择图片",
                cameraMessage = "待接入拍照",
                calendarMessage = "待接入日期 / 纪念记录",
            ),
            topActionMessages = com.secretbase.app.data.TopActionMessages(
                message = "待接入消息页",
                settings = "待接入设置页",
            ),
            bottomNavMessages = com.secretbase.app.data.BottomNavMessages(
                anniversary = "待接入纪念日页",
                album = "待接入相册页",
                profile = "待接入我的页",
            ),
            moodOptions = MoodOption.defaults,
            moodCards = listOf(
                MoodCardUiModel(
                    userId = "sheep",
                    displayName = "小羊",
                    moodLabel = "开心",
                    moodEmoji = "😊",
                    editable = true,
                    avatarRes = R.drawable.avatar_sheep,
                ),
                MoodCardUiModel(
                    userId = "chick",
                    displayName = "小耶",
                    moodLabel = "平静",
                    moodEmoji = "🙂",
                    editable = false,
                    avatarRes = R.drawable.avatar_chick,
                ),
            ),
            featureCards = listOf(
                FeatureCardUiModel("anniversary", "纪念日", "318天后", "待接入纪念日", R.drawable.ic_anniversary_card),
                FeatureCardUiModel("album", "甜蜜相册", "128张", "待接入相册", R.drawable.ic_album_card),
                FeatureCardUiModel("wishlist", "愿望清单", "3 / 8 完成", "待接入愿望清单", R.drawable.ic_wishlist_card),
                FeatureCardUiModel("messageWall", "留言墙", "9条新消息", "待接入留言墙", R.drawable.ic_message_wall_card),
                FeatureCardUiModel("diary", "心情日记", "已连续12天", "待接入心情日记", R.drawable.ic_diary_card),
                FeatureCardUiModel("tasks", "情侣任务", "2项进行中", "待接入情侣任务", R.drawable.ic_task_card),
            ),
            activities = listOf(
                ActivityUiModel(
                    id = "photo",
                    title = "小耶上传了 3 张新照",
                    relativeTime = "2小时前",
                    clickMessage = "待接入相册详情",
                    iconRes = R.drawable.ic_activity_photo,
                ),
                ActivityUiModel(
                    id = "wish",
                    title = "完成了愿望清单中的「看海」",
                    relativeTime = "昨天",
                    clickMessage = "待接入愿望详情",
                    iconRes = R.drawable.ic_activity_checklist,
                ),
            ),
            recentActivityEmptyText = "还没有新的记录，去留下属于我们的回忆吧",
            recentActivityListMessage = "待接入完整动态列表",
            messageDotVisible = true,
        ),
    )
}
