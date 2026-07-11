package com.secretbase.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.secretbase.app.R

val SecretBaseSansFontFamily = FontFamily(
    Font(R.font.noto_sans_sc_variable, weight = FontWeight.Normal),
    Font(R.font.noto_sans_sc_variable, weight = FontWeight.Medium),
    Font(R.font.noto_sans_sc_variable, weight = FontWeight.SemiBold),
    Font(R.font.noto_sans_sc_variable, weight = FontWeight.Bold),
    Font(R.font.noto_sans_sc_variable, weight = FontWeight.ExtraBold),
)

val SecretBaseSerifFontFamily = FontFamily(
    Font(R.font.noto_serif_sc_variable, weight = FontWeight.Medium),
    Font(R.font.noto_serif_sc_variable, weight = FontWeight.SemiBold),
    Font(R.font.noto_serif_sc_variable, weight = FontWeight.Bold),
    Font(R.font.noto_serif_sc_variable, weight = FontWeight.Black),
)

val SecretBaseTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = SecretBaseSerifFontFamily,
        fontSize = 38.sp,
        lineHeight = 42.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.9).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = SecretBaseSerifFontFamily,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = SecretBaseSerifFontFamily,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.1).sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = SecretBaseSansFontFamily,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Bold,
    ),
    bodyMedium = TextStyle(
        fontFamily = SecretBaseSansFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
    ),
    bodySmall = TextStyle(
        fontFamily = SecretBaseSansFontFamily,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    labelLarge = TextStyle(
        fontFamily = SecretBaseSansFontFamily,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Bold,
    ),
    labelMedium = TextStyle(
        fontFamily = SecretBaseSansFontFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
    ),
    labelSmall = TextStyle(
        fontFamily = SecretBaseSansFontFamily,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.SemiBold,
    ),
)
