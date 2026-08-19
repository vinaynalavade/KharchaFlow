package com.vinaynalavade.expensetracker.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand Core Colors
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

// Neutral & Surface Grays (Modern Slate Family)
val Slate950 = Color(0xFF0B0F17)
val Slate900 = Color(0xFF111827)
val Slate850 = Color(0xFF182234)
val Slate800 = Color(0xFF1F2937)
val Slate700 = Color(0xFF374151)
val Slate600 = Color(0xFF4B5563)
val Slate500 = Color(0xFF6B7280)
val Slate400 = Color(0xFF9CA3AF)
val Slate300 = Color(0xFFD1D5DB)
val Slate200 = Color(0xFFE5E7EB)
val Slate100 = Color(0xFFF3F4F6)
val Slate50 = Color(0xFFF8FAFC)
val PureWhite = Color(0xFFFFFFFF)

/**
 * Dedicated semantic colors for financial operations.
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

// Material 3 Light Color Scheme
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
    outlineVariant = Slate300
)

// Material 3 Dark Color Scheme
val DarkColorScheme = darkColorScheme(
    primary = Indigo400,
    onPrimary = Indigo950,
    primaryContainer = Indigo900,
    onPrimaryContainer = Indigo100,
    secondary = Slate400,
    onSecondary = Slate950,
    secondaryContainer = Slate850,
    onSecondaryContainer = Slate200,
    tertiary = IncomeEmeraldLight,
    onTertiary = Slate950,
    tertiaryContainer = IncomeContainerDark,
    onTertiaryContainer = IncomeOnContainerDark,
    error = ExpenseRoseLight,
    onError = Slate950,
    errorContainer = ExpenseContainerDark,
    onErrorContainer = ExpenseOnContainerDark,
    background = Slate950,
    onBackground = Slate50,
    surface = Slate900,
    onSurface = Slate50,
    surfaceVariant = Slate850,
    onSurfaceVariant = Slate400,
    outline = Slate700,
    outlineVariant = Slate800
)
