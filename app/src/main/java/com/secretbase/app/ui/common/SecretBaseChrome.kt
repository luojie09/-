package com.secretbase.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secretbase.app.ui.messagewall.WallCircleButton
import com.secretbase.app.ui.theme.CherryPink
import com.secretbase.app.ui.theme.InkBlack
import com.secretbase.app.ui.theme.OutlinePink
import com.secretbase.app.ui.theme.SurfaceWhite
import com.secretbase.app.ui.theme.WarmBackground
import com.secretbase.app.ui.theme.WarmBackgroundTop
import com.secretbase.app.ui.theme.WarmGray

@Composable
fun SecretBasePageBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        WarmBackgroundTop,
                        WarmBackground,
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 18.dp, top = 54.dp)
                .size(168.dp)
                .background(Color(0x22FFD8E4), CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp, top = 132.dp)
                .size(124.dp)
                .background(Color(0x18FFF0F5), CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 72.dp)
                .size(140.dp)
                .background(Color(0x16FFEAF1), CircleShape),
        )

        content()
    }
}

@Composable
fun SecretBasePageTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actionIcon: ImageVector? = null,
    actionDescription: String? = null,
    onActionClick: (() -> Unit)? = null,
    actionTint: Color = CherryPink,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 10.dp, end = 10.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WallCircleButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "返回",
            tint = InkBlack,
            onClick = onBack,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp,
            ),
            color = InkBlack,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (actionIcon != null && actionDescription != null && onActionClick != null) {
            WallCircleButton(
                icon = actionIcon,
                contentDescription = actionDescription,
                tint = actionTint,
                onClick = onActionClick,
            )
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
fun SecretBaseSectionIntro(
    eyebrow: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = SurfaceWhite.copy(alpha = 0.78f),
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlinePink.copy(alpha = 0.72f)),
        ) {
            Text(
                text = eyebrow,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = CherryPink,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = InkBlack,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray,
            )
        }
    }
}
