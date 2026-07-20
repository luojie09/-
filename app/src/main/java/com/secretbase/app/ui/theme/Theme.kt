package com.secretbase.app.ui.theme

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.platform.LocalInspectionMode

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
    val typography = if (LocalInspectionMode.current) {
        SecretBasePreviewTypography
    } else {
        SecretBaseTypography
    }
    CompositionLocalProvider(LocalIndication provides NoGrayPressIndication) {
        MaterialTheme(
            colorScheme = SecretBaseColorScheme,
            typography = typography,
            content = content,
        )
    }
}

private object NoGrayPressIndication : Indication {
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance =
        remember { NoGrayPressIndicationInstance }
}

private object NoGrayPressIndicationInstance : IndicationInstance {
    override fun ContentDrawScope.drawIndication() {
        drawContent()
    }
}
