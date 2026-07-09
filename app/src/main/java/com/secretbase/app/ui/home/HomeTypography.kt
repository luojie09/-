package com.secretbase.app.ui.home

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Immutable
internal data class HomeTextToken(
    val fontSize: TextUnit,
    val lineHeight: TextUnit,
    val fontWeight: FontWeight,
    val fontFamily: FontFamily = FontFamily.SansSerif,
    val letterSpacing: TextUnit = 0.sp,
)

internal object HomeTypographyScale {
    val Greeting = HomeTextToken(14.sp, 18.sp, FontWeight.Medium)
    val HeroName = HomeTextToken(32.sp, 36.sp, FontWeight.SemiBold, FontFamily.Serif, letterSpacing = (-0.4).sp)
    val HeroHeart = HomeTextToken(19.sp, 20.sp, FontWeight.Medium)
    val RelationshipLabel = HomeTextToken(13.sp, 18.sp, FontWeight.Medium, letterSpacing = 0.1.sp)
    val DaysValue = HomeTextToken(52.sp, 52.sp, FontWeight.SemiBold, FontFamily.SansSerif, letterSpacing = (-1.1).sp)
    val DaysUnit = HomeTextToken(18.sp, 22.sp, FontWeight.SemiBold, letterSpacing = (-0.05).sp)
    val Meta = HomeTextToken(14.sp, 20.sp, FontWeight.Medium)
    val Eyebrow = HomeTextToken(12.sp, 16.sp, FontWeight.Medium)
    val AnniversaryTitle = HomeTextToken(17.sp, 22.sp, FontWeight.SemiBold, FontFamily.Serif, letterSpacing = (-0.15).sp)
    val Capsule = HomeTextToken(12.sp, 16.sp, FontWeight.SemiBold)
    val SectionTitle = HomeTextToken(22.sp, 28.sp, FontWeight.Bold, FontFamily.Serif, letterSpacing = (-0.25).sp)
    val SectionAction = HomeTextToken(13.sp, 18.sp, FontWeight.Medium)
    val FeatureTitle = HomeTextToken(17.sp, 22.sp, FontWeight.SemiBold, FontFamily.Serif, letterSpacing = (-0.15).sp)
    val FeatureSummary = HomeTextToken(14.sp, 20.sp, FontWeight.Medium)
    val ActivityTitle = HomeTextToken(16.sp, 22.sp, FontWeight.SemiBold, FontFamily.Serif, letterSpacing = (-0.1).sp)
    val ActivityMeta = HomeTextToken(12.sp, 18.sp, FontWeight.Medium)
    val NavLabel = HomeTextToken(11.sp, 14.sp, FontWeight.Medium)
    val EmptyState = HomeTextToken(14.sp, 20.sp, FontWeight.Medium)
    val PickerTitle = HomeTextToken(22.sp, 28.sp, FontWeight.SemiBold, FontFamily.Serif, letterSpacing = (-0.25).sp)
}

internal fun TextStyle.homeToken(token: HomeTextToken): TextStyle =
    copy(
        fontFamily = token.fontFamily,
        fontSize = token.fontSize,
        lineHeight = token.lineHeight,
        fontWeight = token.fontWeight,
        letterSpacing = token.letterSpacing,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
