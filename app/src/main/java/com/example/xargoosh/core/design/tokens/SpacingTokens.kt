package com.example.xargoosh.core.design.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class SpacingTokens(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val massive: Dp = 48.dp,
    
    val screenPadding: Dp = 16.dp,
    val listItemPadding: Dp = 12.dp,
    val cardPadding: Dp = 16.dp,
    val sectionSpacing: Dp = 32.dp
)
