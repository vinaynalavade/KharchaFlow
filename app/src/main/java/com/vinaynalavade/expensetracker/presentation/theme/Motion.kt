package com.vinaynalavade.expensetracker.presentation.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

/**
 * Standardized animation duration and easing tokens for KharchaFlow v1.0.5.
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

    fun <T> tactileSpring() = spring<T>(
        dampingRatio = 0.75f,
        stiffness = Spring.StiffnessHigh
    )
}

/**
 * Checks if the system has disabled animations or enabled reduced-motion accessibility.
 */
@Composable
fun rememberIsReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (_: Throwable) {
            false
        }
    }
}

/**
 * Tactile pressed scale modifier for buttons, cards, and interactive settings tiles.
 * Provides subtle tactile visual depression on press (0.98f) with quick spring recovery.
 */
@Composable
fun Modifier.pressScale(
    targetScale: Float = 0.98f,
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true
): Modifier {
    if (!enabled) return this
    val isReducedMotion = rememberIsReducedMotionEnabled()
    if (isReducedMotion) return this

    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1.0f,
        animationSpec = Motion.tactileSpring(),
        label = "PressScale"
    )

    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

