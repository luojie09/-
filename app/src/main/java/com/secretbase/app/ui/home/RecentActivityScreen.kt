package com.secretbase.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.secretbase.app.ui.common.SecretBaseCardSurface
import com.secretbase.app.ui.common.SecretBasePageBackground
import com.secretbase.app.ui.common.SecretBasePageTopBar
import com.secretbase.app.ui.common.SecretBaseSnackbarHost
import com.secretbase.app.ui.theme.InkBlack
import com.secretbase.app.ui.theme.SurfaceWhite
import com.secretbase.app.ui.theme.WarmGray

@Composable
fun RecentActivityScreen(
    payload: HomePayload?,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onActivityClick: (String) -> Unit,
) {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        snackbarHost = { SecretBaseSnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        SecretBasePageBackground {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SecretBasePageTopBar(
                        title = "最近动态",
                        onBack = onBack,
                    )
                }

                val activities = payload?.allActivities.orEmpty()
                if (activities.isEmpty()) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            RecentActivityEmptyState(
                                text = payload?.recentActivityEmptyText ?: "还没有新的动态",
                            )
                        }
                    }
                } else {
                    items(activities, key = ActivityUiModel::id) { activity ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            RecentActivityListItem(
                                activity = activity,
                                onClick = { onActivityClick(activity.clickMessage) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentActivityListItem(
    activity: ActivityUiModel,
    onClick: () -> Unit,
) {
    SecretBaseCardSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
    ) {
        Surface(
            onClick = onClick,
            color = SurfaceWhite.copy(alpha = 0f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceWhite,
                ) {
                    if (activity.iconRes != null) {
                        Image(
                            painter = painterResource(id = activity.iconRes),
                            contentDescription = null,
                            modifier = Modifier.padding(9.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = activity.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = InkBlack,
                    )
                    Text(
                        text = activity.relativeTime,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = WarmGray,
                    )
                }
                Text(
                    text = "›",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = WarmGray,
                )
            }
        }
    }
}

@Composable
private fun RecentActivityEmptyState(text: String) {
    SecretBaseCardSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "这里还没有动态",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = InkBlack,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray,
            )
        }
    }
}
