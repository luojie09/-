package com.secretbase.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.secretbase.app.ui.common.SecretBaseCardSurface
import com.secretbase.app.ui.common.SecretBasePageBackground
import com.secretbase.app.ui.common.SecretBasePageTopBar
import com.secretbase.app.ui.common.SecretBasePrimaryButton
import com.secretbase.app.ui.common.SecretBaseSnackbarHost
import com.secretbase.app.ui.theme.InkBlack
import com.secretbase.app.ui.theme.WarmGray

@Composable
fun RecentActivityDetailScreen(
    activity: ActivityUiModel,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpenSource: (() -> Unit)?,
) {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        snackbarHost = { SecretBaseSnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        SecretBasePageBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
            ) {
                SecretBasePageTopBar(
                    title = "动态详情",
                    onBack = onBack,
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        SecretBaseCardSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(26.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    activity.iconRes?.let { iconRes ->
                                        Image(
                                            painter = painterResource(iconRes),
                                            contentDescription = null,
                                            modifier = Modifier.height(38.dp),
                                            contentScale = ContentScale.Fit,
                                        )
                                    }
                                }
                                Text(
                                    text = activity.title,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = InkBlack,
                                )
                                Text(
                                    text = activity.relativeTime,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WarmGray,
                                )
                                Text(
                                    text = activity.detail,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2f,
                                    ),
                                    color = InkBlack,
                                )
                            }
                        }
                    }
                    activity.imagePaths.forEach { imagePath ->
                        item(key = imagePath) {
                            AsyncImage(
                                model = imagePath,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                    if (onOpenSource != null) {
                        item {
                            SecretBasePrimaryButton(
                                text = "查看原内容",
                                enabled = true,
                                onClick = onOpenSource,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}
