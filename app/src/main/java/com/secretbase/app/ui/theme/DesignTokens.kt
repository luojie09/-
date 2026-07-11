package com.secretbase.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

object SecretBaseDesignTokens {
    object FontWeights {
        val Weak = FontWeight.Bold
        val Body = FontWeight.ExtraBold
        val Title = FontWeight.Black
        val Display = FontWeight.Black
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
        const val ProminentSurface = 0.98f
        const val FloatingSurface = 0.80f
        const val SoftIconSurface = 0.78f
        const val CardBorder = 0.38f
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
