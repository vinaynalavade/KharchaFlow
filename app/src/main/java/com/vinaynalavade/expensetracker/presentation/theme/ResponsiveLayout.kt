package com.vinaynalavade.expensetracker.presentation.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable responsive layout modifiers and safe inset utilities for KharchaFlow.
 * Ensures consistent, accessible layout behavior across standard phones, compact screens,
 * 3-button/gesture navigation bars, edge-to-edge system bars, and software keyboard (IME).
 */

import androidx.compose.runtime.Composable

/**
 * Applies safe bottom insets combining the navigation bar and IME insets seamlessly
 * without creating double-padding gaps or pushing actions off screen.
 */
@Composable
fun Modifier.safeBottomInsets(): Modifier = this.windowInsetsPadding(
    WindowInsets.navigationBars.union(WindowInsets.ime)
)

/**
 * Applies safe screen insets for edge-to-edge full-screen dialogs and surfaces
 * (respects status bar, navigation bar, and display cutouts).
 */
@Composable
fun Modifier.safeScreenPadding(): Modifier = this.windowInsetsPadding(
    WindowInsets.safeDrawing
)

/**
 * Applies safe top status bar insets.
 */
@Composable
fun Modifier.safeStatusBarsPadding(): Modifier = this.windowInsetsPadding(
    WindowInsets.statusBars
)

/**
 * Applies responsive dialog width: 92% on small/standard phones, max 440dp on tablets/foldables.
 */
fun Modifier.responsiveDialogWidth(maxWidth: Dp = 440.dp): Modifier = this
    .fillMaxWidth(0.92f)
    .widthIn(max = maxWidth)

/**
 * Constrains dialog / modal content height to a safe viewport proportion to prevent off-screen overflow.
 */
fun Modifier.adaptiveDialogHeight(maxFraction: Float = 0.88f): Modifier = this
    .fillMaxHeight(maxFraction)
