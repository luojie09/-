package com.secretbase.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

object SecretBaseDesignTokens {
    object FontWeights {
        val Weak = FontWeight.Medium
        val Body = FontWeight.SemiBold
        val Title = FontWeight.Bold
        val Display = FontWeight.Bold
    }

    object Radius {
        val Card = 24.dp
        val HeroCard = 26.dp
        val Sheet = 28.dp
        val Pill = 999.dp

        val CardShape = RoundedCornerShape(Card)
        val HeroCardShape = RoundedCornerShape(HeroCard)
        val SheetShape = RoundedCornerShape(topStart = Sheet, topEnd = Sheet)
        val PillShape = RoundedCornerShape(Pill)
    }

    object Alpha {
        const val ProminentSurface = 0.99f
        const val FloatingSurface = 0.88f
        const val SoftIconSurface = 0.78f
        const val CardBorder = 0.24f
        const val Divider = 0.42f
    }

    object Spacing {
        val ScreenHorizontal = 20.dp
        val CardHorizontal = 16.dp
        val CardVertical = 16.dp
        val ItemGap = 12.dp
        val SectionGap = 22.dp
    }

}
