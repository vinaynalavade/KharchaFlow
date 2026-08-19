package com.vinaynalavade.expensetracker.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape scale for professional financial components.
 * Moderate rounding avoids childish pill-like aesthetics while remaining modern and soft.
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

val CardShape = RoundedCornerShape(16.dp)
val HeroCardShape = RoundedCornerShape(20.dp)
val ButtonShape = RoundedCornerShape(12.dp)
val ChipShape = RoundedCornerShape(8.dp)
val SheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
val PillShape = RoundedCornerShape(percent = 50)
