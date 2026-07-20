package com.secretbase.app.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.secretbase.app.R

@OptIn(ExperimentalTextApi::class)
val SecretBaseSansFontFamily = FontFamily(
    Font(R.font.noto_sans_sc_variable, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.noto_sans_sc_variable, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.noto_sans_sc_variable, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.noto_sans_sc_variable, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.noto_sans_sc_variable, weight = FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
    Font(R.font.noto_sans_sc_variable, weight = FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(900))),
)

@OptIn(ExperimentalTextApi::class)
val SecretBaseSerifFontFamily = FontFamily(
    Font(R.font.noto_serif_sc_variable, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.noto_serif_sc_variable, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.noto_serif_sc_variable, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.noto_serif_sc_variable, weight = FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
    Font(R.font.noto_serif_sc_variable, weight = FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(900))),
)

val SecretBaseTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = SecretBaseSerifFontFamily,
        fontSize = 38.sp,
        lineHeight = 42.sp,
        fontWeight = SecretBaseDesignTokens.FontWeights.Display,
        letterSpacing = (-0.9).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = SecretBaseSerifFontFamily,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = SecretBaseDesignTokens.FontWeights.Title,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = SecretBaseSerifFontFamily,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        fontWeight = SecretBaseDesignTokens.FontWeights.Title,
        letterSpacing = (-0.1).sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = SecretBaseSansFontFamily,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = SecretBaseDesignTokens.FontWeights.Body,
    ),
    bodyMedium = TextStyle(
        fontFamily = SecretBaseSansFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = SecretBaseDesignTokens.FontWeights.Body,
    ),
    bodySmall = TextStyle(
        fontFamily = SecretBaseSansFontFamily,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = SecretBaseDesignTokens.FontWeights.Weak,
    ),
    labelLarge = TextStyle(
        fontFamily = SecretBaseSansFontFamily,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = SecretBaseDesignTokens.FontWeights.Body,
    ),
    labelMedium = TextStyle(
        fontFamily = SecretBaseSansFontFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = SecretBaseDesignTokens.FontWeights.Body,
    ),
    labelSmall = TextStyle(
        fontFamily = SecretBaseSansFontFamily,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = SecretBaseDesignTokens.FontWeights.Weak,
    ),
)

// The alpha screenshot renderer cannot load variable font resources. This
// inspection-only typography preserves our sizes and weights with platform fonts.
val SecretBasePreviewTypography = Typography(
    headlineLarge = SecretBaseTypography.headlineLarge.copy(fontFamily = FontFamily.Serif),
    titleLarge = SecretBaseTypography.titleLarge.copy(fontFamily = FontFamily.Serif),
    titleMedium = SecretBaseTypography.titleMedium.copy(fontFamily = FontFamily.Serif),
    bodyLarge = SecretBaseTypography.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
    bodyMedium = SecretBaseTypography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
    bodySmall = SecretBaseTypography.bodySmall.copy(fontFamily = FontFamily.SansSerif),
    labelLarge = SecretBaseTypography.labelLarge.copy(fontFamily = FontFamily.SansSerif),
    labelMedium = SecretBaseTypography.labelMedium.copy(fontFamily = FontFamily.SansSerif),
    labelSmall = SecretBaseTypography.labelSmall.copy(fontFamily = FontFamily.SansSerif),
)
