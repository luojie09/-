package com.secretbase.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SecretBaseColorScheme = lightColorScheme(
    primary = CherryPink,
    background = WarmBackground,
    surface = SurfaceWhite,
    onPrimary = SurfaceWhite,
    onBackground = InkBlack,
    onSurface = InkBlack,
)

@Composable
fun SecretBaseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SecretBaseColorScheme,
        typography = SecretBaseTypography,
        content = content,
    )
}

