package com.vinaynalavade.expensetracker.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand Core Colors (Sophisticated Indigo Palette)
val Indigo950 = Color(0xFF0F0E2A)
val Indigo900 = Color(0xFF1E1B4B)
val Indigo800 = Color(0xFF312E81)
val Indigo700 = Color(0xFF4338CA)
val Indigo600 = Color(0xFF4F46E5)
val Indigo500 = Color(0xFF6366F1)
val Indigo400 = Color(0xFF818CF8)
val Indigo300 = Color(0xFFA5B4FC)
val Indigo200 = Color(0xFFC7D2FE)
val Indigo100 = Color(0xFFE0E7FF)
val Indigo50 = Color(0xFFEEF2FF)

// Financial Semantic - Income (Refined Emerald)
val IncomeEmerald = Color(0xFF10B981)
val IncomeEmeraldDark = Color(0xFF059669)
val IncomeEmeraldLight = Color(0xFF34D399)
val IncomeContainerLight = Color(0xFFECFDF5)
val IncomeOnContainerLight = Color(0xFF065F46)
val IncomeContainerDark = Color(0xFF064E3B)
val IncomeOnContainerDark = Color(0xFFA7F3D0)

// Financial Semantic - Expense (Refined Rose)
val ExpenseRose = Color(0xFFF43F5E)
val ExpenseRoseDark = Color(0xFFE11D48)
val ExpenseRoseLight = Color(0xFFFB7185)
val ExpenseContainerLight = Color(0xFFFFF1F2)
val ExpenseOnContainerLight = Color(0xFF9F1239)
val ExpenseContainerDark = Color(0xFF4C0519)
val ExpenseOnContainerDark = Color(0xFFFECDD3)

// Neutral & Surface Grays (Multi-tiered Slate & Obsidian System)
val Slate950 = Color(0xFF0A0E17) // Deep Obsidian Night Canvas
val Slate900 = Color(0xFF111726) // Elevated Primary Surface / Cards
val Slate850 = Color(0xFF172033) // Secondary Elevated Surface / Sub-containers
val Slate800 = Color(0xFF1E293F) // Tertiary Surface / Sheet Backgrounds
val Slate750 = Color(0xFF243048) // Active / Selected Container Fill
val Slate700 = Color(0xFF2D3B55) // Subtle Borders & Dividers in Dark
val Slate600 = Color(0xFF475569) // Inactive Icon Tints
val Slate500 = Color(0xFF64748B) // Muted Captions & Metadata
val Slate400 = Color(0xFF94A3B8) // Secondary Body Text
val Slate300 = Color(0xFFCBD5E1) // Crisp Light Body
val Slate200 = Color(0xFFE2E8F0) // Subtle Outline in Light / High Contrast Text
val Slate100 = Color(0xFFF1F5F9) // Primary onSurface Text in Dark
val Slate50 = Color(0xFFF8FAFC)  // Pure Crisp Light Canvas
val PureWhite = Color(0xFFFFFFFF)

/**
 * Dedicated semantic colors for financial operations across Light and Dark themes.
 */
@Immutable
data class FinancialColors(
    val income: Color,
    val incomeContainer: Color,
    val onIncomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val onExpenseContainer: Color
)

val LightFinancialColors = FinancialColors(
    income = IncomeEmeraldDark,
    incomeContainer = IncomeContainerLight,
    onIncomeContainer = IncomeOnContainerLight,
    expense = ExpenseRoseDark,
    expenseContainer = ExpenseContainerLight,
    onExpenseContainer = ExpenseOnContainerLight
)

val DarkFinancialColors = FinancialColors(
    income = IncomeEmeraldLight,
    incomeContainer = IncomeContainerDark,
    onIncomeContainer = IncomeOnContainerDark,
    expense = ExpenseRoseLight,
    expenseContainer = ExpenseContainerDark,
    onExpenseContainer = ExpenseOnContainerDark
)

val LocalFinancialColors = staticCompositionLocalOf { LightFinancialColors }

// Material 3 Light Color Scheme (Crisp, High-Trust Financial Banking)
val LightColorScheme = lightColorScheme(
    primary = Indigo600,
    onPrimary = PureWhite,
    primaryContainer = Indigo50,
    onPrimaryContainer = Indigo900,
    secondary = Slate700,
    onSecondary = PureWhite,
    secondaryContainer = Slate100,
    onSecondaryContainer = Slate900,
    tertiary = IncomeEmeraldDark,
    onTertiary = PureWhite,
    tertiaryContainer = IncomeContainerLight,
    onTertiaryContainer = IncomeOnContainerLight,
    error = ExpenseRoseDark,
    onError = PureWhite,
    errorContainer = ExpenseContainerLight,
    onErrorContainer = ExpenseOnContainerLight,
    background = Slate50,
    onBackground = Slate900,
    surface = PureWhite,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate200,
    outlineVariant = Slate300,
    inverseSurface = Slate900,
    inverseOnSurface = Slate50,
    inversePrimary = Indigo300
)

// Material 3 Dark Color Scheme (Deep Obsidian Multi-Tiered Financial Palette)
val DarkColorScheme = darkColorScheme(
    primary = Indigo400,
    onPrimary = Slate950,
    primaryContainer = Color(0xFF24274F),
    onPrimaryContainer = Indigo100,
    secondary = Slate300,
    onSecondary = Slate950,
    secondaryContainer = Slate850,
    onSecondaryContainer = Slate100,
    tertiary = IncomeEmeraldLight,
    onTertiary = Slate950,
    tertiaryContainer = IncomeContainerDark,
    onTertiaryContainer = IncomeOnContainerDark,
    error = ExpenseRoseLight,
    onError = Slate950,
    errorContainer = ExpenseContainerDark,
    onErrorContainer = ExpenseOnContainerDark,
    background = Slate950,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate850,
    onSurfaceVariant = Slate400,
    outline = Slate700,
    outlineVariant = Slate800,
    inverseSurface = Slate100,
    inverseOnSurface = Slate950,
    inversePrimary = Indigo600
)
