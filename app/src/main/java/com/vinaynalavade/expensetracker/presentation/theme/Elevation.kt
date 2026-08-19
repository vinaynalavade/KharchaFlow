package com.vinaynalavade.expensetracker.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standardized elevation tokens for KharchaFlow.
 * Emphasizes restrained elevation with subtle tonal differences.
 */
@Immutable
data class Elevation(
    val none: Dp = 0.dp,
    val low: Dp = 1.dp,
    val medium: Dp = 2.dp,
    val high: Dp = 4.dp,
    val raised: Dp = 8.dp
)

val LocalElevation = staticCompositionLocalOf { Elevation() }
