package com.vinaynalavade.expensetracker.presentation.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/**
 * Standardized animation duration and easing tokens.
 * Prioritizes fast, purposeful micro-interactions without causing interaction lag.
 */
@Immutable
object Motion {
    const val DurationFast = 150
    const val DurationNormal = 250
    const val DurationEmphasis = 350

    val EasingStandard = FastOutSlowInEasing
    val EasingDecelerate = LinearOutSlowInEasing
    val EasingEmphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    fun <T> fastSpec() = tween<T>(durationMillis = DurationFast, easing = EasingStandard)
    fun <T> normalSpec() = tween<T>(durationMillis = DurationNormal, easing = EasingStandard)
    fun <T> emphasisSpec() = tween<T>(durationMillis = DurationEmphasis, easing = EasingEmphasized)

    fun <T> quickSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}
