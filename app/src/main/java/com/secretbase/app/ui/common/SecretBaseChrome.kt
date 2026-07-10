package com.secretbase.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
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
import com.secretbase.app.ui.theme.CherryPink
import com.secretbase.app.ui.theme.InkBlack
import com.secretbase.app.ui.theme.OutlinePink
import com.secretbase.app.ui.theme.SurfaceWhite
import com.secretbase.app.ui.theme.WarmBackground
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
                        Color(0xFFFFF7F8),
                        WarmBackground,
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 18.dp, top = 72.dp)
                .size(132.dp)
                .background(Color(0x10FFD9E4), CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 88.dp)
                .size(112.dp)
                .background(Color(0x0CFFF1F5), CircleShape),
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .height(44.dp),
    ) {
        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.15).sp,
            ),
            color = InkBlack,
        )
        SecretBaseTopBarButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "返回",
            tint = InkBlack,
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        if (actionIcon != null && actionDescription != null && onActionClick != null) {
            SecretBaseTopBarButton(
                icon = actionIcon,
                contentDescription = actionDescription,
                tint = actionTint,
                onClick = onActionClick,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (eyebrow.isNotBlank()) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.1.sp,
                ),
                color = CherryPink.copy(alpha = 0.84f),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.25).sp,
                ),
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

@Composable
fun SecretBaseCardSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = SurfaceWhite.copy(alpha = 0.94f),
        shadowElevation = 2.dp,
        tonalElevation = 0.dp,
        shape = shape,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlinePink.copy(alpha = 0.72f)),
        content = content,
    )
}

@Composable
fun SecretBasePrimaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        color = if (enabled) CherryPink else Color(0xFFECE6E8),
        shadowElevation = if (enabled) 1.dp else 0.dp,
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (enabled) CherryPink.copy(alpha = 0.14f) else OutlinePink.copy(alpha = 0.7f),
        ),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (enabled) SurfaceWhite else WarmGray,
        )
    }
}

@Composable
fun SecretBaseSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        color = SurfaceWhite.copy(alpha = 0.8f),
        shadowElevation = 0.dp,
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlinePink.copy(alpha = 0.82f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = InkBlack,
        )
    }
}

@Composable
fun SecretBaseInputSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = Color(0xFFFDF9FA),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        shape = shape,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlinePink.copy(alpha = 0.78f)),
        content = content,
    )
}

@Composable
fun SecretBaseTopBarButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(40.dp),
        color = SurfaceWhite.copy(alpha = 0.82f),
        shape = CircleShape,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlinePink.copy(alpha = 0.62f)),
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun SecretBaseMiniActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = WarmGray,
    showLabel: Boolean = true,
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (showLabel) 8.dp else 7.dp,
                vertical = if (showLabel) 6.dp else 7.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(14.dp),
            )
            if (showLabel) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = tint,
                )
            }
        }
    }
}
