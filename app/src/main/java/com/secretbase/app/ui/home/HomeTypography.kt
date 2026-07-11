package com.secretbase.app.ui.home

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.secretbase.app.ui.theme.SecretBaseSansFontFamily
import com.secretbase.app.ui.theme.SecretBaseSerifFontFamily

@Immutable
internal data class HomeTextToken(
    val fontSize: TextUnit,
    val lineHeight: TextUnit,
    val fontWeight: FontWeight,
    val fontFamily: FontFamily = SecretBaseSansFontFamily,
    val letterSpacing: TextUnit = 0.sp,
)

internal object HomeTypographyScale {
    val Greeting = HomeTextToken(14.sp, 18.sp, FontWeight.SemiBold)
    val HeroName = HomeTextToken(32.sp, 36.sp, FontWeight.Bold, SecretBaseSerifFontFamily, letterSpacing = (-0.4).sp)
    val HeroHeart = HomeTextToken(19.sp, 20.sp, FontWeight.SemiBold)
    val RelationshipLabel = HomeTextToken(13.sp, 18.sp, FontWeight.SemiBold, letterSpacing = 0.1.sp)
    val DaysValue = HomeTextToken(52.sp, 52.sp, FontWeight.Bold, SecretBaseSansFontFamily, letterSpacing = (-1.1).sp)
    val DaysUnit = HomeTextToken(18.sp, 22.sp, FontWeight.Bold, letterSpacing = (-0.05).sp)
    val Meta = HomeTextToken(14.sp, 20.sp, FontWeight.SemiBold)
    val Eyebrow = HomeTextToken(12.sp, 16.sp, FontWeight.SemiBold)
    val AnniversaryTitle = HomeTextToken(17.sp, 22.sp, FontWeight.Bold, SecretBaseSerifFontFamily, letterSpacing = (-0.15).sp)
    val Capsule = HomeTextToken(12.sp, 16.sp, FontWeight.Bold)
    val SectionTitle = HomeTextToken(22.sp, 28.sp, FontWeight.Black, SecretBaseSerifFontFamily, letterSpacing = (-0.25).sp)
    val SectionAction = HomeTextToken(13.sp, 18.sp, FontWeight.SemiBold)
    val FeatureTitle = HomeTextToken(17.sp, 22.sp, FontWeight.Bold, SecretBaseSerifFontFamily, letterSpacing = (-0.15).sp)
    val FeatureSummary = HomeTextToken(14.sp, 20.sp, FontWeight.SemiBold)
    val ActivityTitle = HomeTextToken(16.sp, 22.sp, FontWeight.Bold, SecretBaseSerifFontFamily, letterSpacing = (-0.1).sp)
    val ActivityMeta = HomeTextToken(12.sp, 18.sp, FontWeight.SemiBold)
    val NavLabel = HomeTextToken(11.sp, 14.sp, FontWeight.SemiBold)
    val EmptyState = HomeTextToken(14.sp, 20.sp, FontWeight.SemiBold)
    val PickerTitle = HomeTextToken(22.sp, 28.sp, FontWeight.Bold, SecretBaseSerifFontFamily, letterSpacing = (-0.25).sp)
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
