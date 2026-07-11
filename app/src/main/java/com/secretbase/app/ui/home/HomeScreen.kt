package com.secretbase.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.secretbase.app.data.HeroVisualConfig
import com.secretbase.app.data.MoodOption
import com.secretbase.app.ui.theme.CherryPink
import com.secretbase.app.ui.theme.InkBlack
import com.secretbase.app.ui.theme.OutlinePink
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
                modifier = Modifier.padding(bottom = 92.dp),
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
                    innerPadding = innerPadding,
                    onAction = onPlaceholderAction,
                )

                else -> ErrorContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    message = uiState.errorMessage ?: "\u52a0\u8f7d\u5931\u8d25\uff0c\u70b9\u51fb\u91cd\u8bd5",
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
    innerPadding: PaddingValues,
    onAction: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            HomeHeroSection(
                payload = payload,
            )
        }

        item {
            PaddedSection {
                SectionTitle(
                    title = "\u4e00\u8d77\u8bb0\u5f55",
                    trailing = null,
                )
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
                    title = "\u6700\u8fd1\u52a8\u6001",
                    trailing = null,
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
                        onViewAll = { onAction(payload.recentActivityListMessage) },
                        onAction = onAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaddedSection(content: @Composable () -> Unit) {
    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
        content()
    }
}

@Composable
private fun HomeHeroSection(
    payload: HomePayload,
) {
    val overlap = payload.visuals.hero.relationshipCardOverlapDp.dp

    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            HeroBackground(
                payload = payload,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodyMedium.homeToken(HomeTypographyScale.Greeting),
                color = WarmGray,
            )
            CoupleNameTitle(coupleDisplayName = coupleDisplayName)
        }
    }
}

@Composable
private fun CoupleIllustration(hero: HeroVisualConfig) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = when {
            maxWidth <= 320.dp -> 10.dp
            maxWidth <= 360.dp -> 14.dp
            maxWidth <= 393.dp -> 18.dp
            else -> 24.dp
        }

        DrawableOrFallback(
            resId = hero.imageRes,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.16f
                    scaleY = 1.16f
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = 28.dp,
                    bottom = 0.dp,
                ),
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
private fun RelationshipCard(
    relationship: RelationshipUiModel,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        color = SurfaceWhite.copy(alpha = 0.80f),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = relationship.label,
                    style = MaterialTheme.typography.bodySmall.homeToken(HomeTypographyScale.RelationshipLabel),
                    color = WarmGray,
                )

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = relationship.daysTogether.toString(),
                        style = MaterialTheme.typography.displaySmall.homeToken(HomeTypographyScale.DaysValue),
                        color = InkBlack,
                    )
                    Text(
                        text = "\u5929",
                        style = MaterialTheme.typography.titleMedium.homeToken(HomeTypographyScale.DaysUnit),
                        color = InkBlack,
                        modifier = Modifier.padding(bottom = 7.dp),
                    )
                }

                Text(
                    text = relationship.startDateText,
                    style = MaterialTheme.typography.bodyMedium.homeToken(HomeTypographyScale.Meta),
                    color = WarmGray,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OutlinePink.copy(alpha = 0.46f)),
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = "\u4e0b\u4e2a\u7eaa\u5ff5\u65e5",
                            style = MaterialTheme.typography.bodySmall.homeToken(HomeTypographyScale.Eyebrow),
                            color = WarmGray,
                        )
                        Text(
                            text = relationship.anniversaryTitle,
                            style = MaterialTheme.typography.bodyLarge.homeToken(HomeTypographyScale.AnniversaryTitle),
                            color = InkBlack,
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = SoftPink.copy(alpha = 0.22f),
                    ) {
                        Text(
                            text = relationship.anniversaryCountdownLabel,
                            style = MaterialTheme.typography.bodySmall.homeToken(HomeTypographyScale.Capsule),
                            color = InkBlack,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(SoftPink.copy(alpha = 0.22f), RoundedCornerShape(999.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(relationship.anniversaryProgress)
                            .height(6.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        CherryPink.copy(alpha = 0.72f),
                                        SoftPinkStrong.copy(alpha = 0.66f),
                                    ),
                                ),
                                RoundedCornerShape(999.dp),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun CoupleNameTitle(coupleDisplayName: String) {
    val heartToken = "\u2764\uFE0F"
    val names = when {
        coupleDisplayName.contains(heartToken) -> coupleDisplayName.split(heartToken)
        coupleDisplayName.contains(" & ") -> coupleDisplayName.split(" & ")
        else -> emptyList()
    }.map { it.trim() }

    if (names.size == 2) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = names[0],
                style = MaterialTheme.typography.headlineMedium.homeToken(HomeTypographyScale.HeroName),
                color = InkBlack,
            )
            Text(
                text = heartToken,
                style = MaterialTheme.typography.titleMedium.homeToken(HomeTypographyScale.HeroHeart),
                color = CherryPink.copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = 5.dp),
            )
            Text(
                text = names[1],
                style = MaterialTheme.typography.headlineMedium.homeToken(HomeTypographyScale.HeroName),
                color = InkBlack,
            )
        }
    } else {
        Text(
            text = coupleDisplayName,
            style = MaterialTheme.typography.headlineMedium.homeToken(HomeTypographyScale.HeroName),
            color = InkBlack,
            maxLines = 2,
        )
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
            style = MaterialTheme.typography.titleLarge.homeToken(HomeTypographyScale.SectionTitle),
            color = InkBlack,
        )

        if (trailing != null && onTrailingClick != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.bodyMedium.homeToken(HomeTypographyScale.SectionAction),
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
        features.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { feature ->
                    FeatureGridCard(
                        feature = feature,
                        modifier = Modifier.weight(1f),
                        onAction = onAction,
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FeatureGridCard(
    feature: FeatureCardUiModel,
    modifier: Modifier = Modifier,
    onAction: (String) -> Unit,
) {
    Surface(
        onClick = { onAction(feature.clickMessage) },
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = SurfaceWhite,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 104.dp)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceWhite,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        DrawableOrFallback(
                            resId = feature.iconRes,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleMedium.homeToken(HomeTypographyScale.FeatureTitle),
                    color = InkBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = feature.summary,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodyMedium.homeToken(HomeTypographyScale.FeatureSummary),
                color = WarmGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ActivityCard(
    activities: List<ActivityUiModel>,
    onViewAll: () -> Unit,
    onAction: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceWhite,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            activities.forEachIndexed { index, activity ->
                ActivityRow(
                    activity = activity,
                    showDivider = true,
                    onClick = { onAction(activity.clickMessage) },
                )
            }
            Text(
                text = "\u67e5\u770b\u5168\u90e8",
                style = MaterialTheme.typography.bodyMedium.homeToken(HomeTypographyScale.SectionAction),
                color = WarmGray,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onViewAll)
                    .padding(vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun ActivityRow(
    activity: ActivityUiModel,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceWhite,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    DrawableOrFallback(
                        resId = activity.iconRes,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.bodyLarge.homeToken(HomeTypographyScale.ActivityTitle),
                    color = InkBlack,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = activity.relativeTime,
                    style = MaterialTheme.typography.bodySmall.homeToken(HomeTypographyScale.ActivityMeta),
                    color = WarmGray,
                )
            }

            Text(
                text = "\u203a",
                style = MaterialTheme.typography.titleLarge.homeToken(HomeTypographyScale.FeatureTitle),
                color = WarmGray,
            )
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OutlinePink.copy(alpha = 0.42f)),
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
        color = SurfaceWhite,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DrawableOrFallback(
                resId = heroRes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.homeToken(HomeTypographyScale.EmptyState),
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
        color = SurfaceWhite,
        shadowElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomNavItem(
                label = "\u9996\u9875",
                iconRes = payload.visuals.icon("navHome"),
                active = true,
                onClick = {},
            )
            BottomNavItem(
                label = "\u7eaa\u5ff5\u65e5",
                iconRes = payload.visuals.icon("navAnniversary"),
                active = false,
                onClick = { onAction(payload.bottomNavMessages.anniversary) },
            )
            BottomNavItem(
                label = "\u76f8\u518c",
                iconRes = payload.visuals.icon("navAlbum"),
                active = false,
                onClick = { onAction(payload.bottomNavMessages.album) },
            )
            BottomNavItem(
                label = "\u6211\u7684",
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
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DrawableOrFallback(
                resId = iconRes,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.homeToken(
                    if (active) HomeTypographyScale.SectionAction else HomeTypographyScale.NavLabel,
                ),
                color = if (active) CherryPink else WarmGray,
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SkeletonBlock(height = 28.dp, width = 84.dp)
        SkeletonBlock(height = 42.dp, width = 220.dp)
        SkeletonBlock(height = 360.dp, modifier = Modifier.fillMaxWidth())
        repeat(3) {
            SkeletonBlock(height = 120.dp, modifier = Modifier.fillMaxWidth())
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
            Text(text = "\u70b9\u51fb\u91cd\u8bd5")
        }
    }
}

@Composable
private fun SkeletonBlock(
    height: Dp,
    width: Dp? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(RoundedCornerShape(24.dp))
            .background(SoftPink.copy(alpha = 0.36f)),
    )
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
                    .background(SoftPink.copy(alpha = 0.42f), RoundedCornerShape(16.dp)),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
                text = "\u9009\u62e9\u4eca\u5929\u7684\u5fc3\u60c5",
                style = MaterialTheme.typography.titleLarge.homeToken(HomeTypographyScale.PickerTitle),
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
                            shadowElevation = 0.dp,
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
                                    style = MaterialTheme.typography.bodyLarge.homeToken(HomeTypographyScale.FeatureTitle),
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
