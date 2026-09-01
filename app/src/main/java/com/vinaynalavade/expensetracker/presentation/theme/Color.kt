package com.vinaynalavade.expensetracker.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ==========================================================================
// KharchaFlow Monochrome Core Palette (Multinational Fintech Quality)
// ==========================================================================

// Charcoal & Near-Black (Authoritative Main Actions, FABs, Primary Controls)
val Charcoal950 = Color(0xFF111111) // Near-Black Primary Action / Night Canvas
val Charcoal900 = Color(0xFF18181B) // Elevated Dark Surface / Card
val Charcoal850 = Color(0xFF202024) // Secondary Elevated Surface in Dark
val Charcoal800 = Color(0xFF27272A) // Subtle Dark Borders & Outlines
val Charcoal700 = Color(0xFF3F3F46) // Active Dark Chip Fill

// Pure White & Canvas Neutrals
val PureWhite = Color(0xFFFFFFFF)
val CanvasOffWhite = Color(0xFFF7F7F5) // Soft Warm Surface Canvas

// Neutral Grays (Multi-tiered Hierarchy for Secondary Elements, Text, Dividers)
val NeutralGray900 = Color(0xFF111827)
val NeutralGray800 = Color(0xFF1F2937)
val NeutralGray700 = Color(0xFF374151)
val NeutralGray600 = Color(0xFF4B5563)
val NeutralGray500 = Color(0xFF6B7280) // Secondary Gray - Captions, Metadata & Subtitles
val NeutralGray400 = Color(0xFF9CA3AF) // Muted Icons & Disabled States
val NeutralGray300 = Color(0xFFD1D5DB) // Subtle Dividers
val NeutralGray200 = Color(0xFFE5E7EB) // Card Outlines & Borders
val NeutralGray100 = Color(0xFFF3F4F6) // Level 2 Surface / Inactive Pill Background
val NeutralGray50 = Color(0xFFFAFAFA)

// ==========================================================================
// KharchaFlow Brand Accent (Logo Green #028166 - Used Subtly & Strategically)
// ==========================================================================
val BrandGreen = Color(0xFF028166)          // Primary Brand Accent
val BrandGreenDark = Color(0xFF014D3D)      // Deep Pressed Green
val BrandGreenLight = Color(0xFF03A37F)     // Dark Theme Accent Highlight
val BrandGreenContainer = Color(0xFFEDF7F4) // Light Mode Subtle Tint Container
val BrandGreenOnContainer = Color(0xFF024B3B)
val BrandGreenDarkContainer = Color(0xFF0B2E24)
val BrandGreenDarkOnContainer = Color(0xFFA7F3D0)

// ==========================================================================
// Financial Semantic Colors (Emerald Income, Rose Expense)
// ==========================================================================
val IncomeEmerald = Color(0xFF10B981)
val IncomeEmeraldDark = Color(0xFF059669)
val IncomeEmeraldLight = Color(0xFF34D399)
val IncomeContainerLight = Color(0xFFECFDF5)
val IncomeOnContainerLight = Color(0xFF065F46)
val IncomeContainerDark = Color(0xFF064E3B)
val IncomeOnContainerDark = Color(0xFFA7F3D0)

val ExpenseRose = Color(0xFFF43F5E)
val ExpenseRoseDark = Color(0xFFE11D48)
val ExpenseRoseLight = Color(0xFFFB7185)
val ExpenseContainerLight = Color(0xFFFFF1F2)
val ExpenseOnContainerLight = Color(0xFF9F1239)
val ExpenseContainerDark = Color(0xFF4C0519)
val ExpenseOnContainerDark = Color(0xFFFECDD3)

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

// Material 3 Light Color Scheme (Monochrome-First, Charcoal #111111, Pure White, Soft Canvas #F7F7F5)
val LightColorScheme = lightColorScheme(
    primary = Charcoal950,
    onPrimary = PureWhite,
    primaryContainer = NeutralGray100,
    onPrimaryContainer = Charcoal950,
    secondary = BrandGreen,
    onSecondary = PureWhite,
    secondaryContainer = BrandGreenContainer,
    onSecondaryContainer = BrandGreenOnContainer,
    tertiary = IncomeEmeraldDark,
    onTertiary = PureWhite,
    tertiaryContainer = IncomeContainerLight,
    onTertiaryContainer = IncomeOnContainerLight,
    error = ExpenseRoseDark,
    onError = PureWhite,
    errorContainer = ExpenseContainerLight,
    onErrorContainer = ExpenseOnContainerLight,
    background = CanvasOffWhite,
    onBackground = Charcoal950,
    surface = PureWhite,
    onSurface = Charcoal950,
    surfaceVariant = NeutralGray100,
    onSurfaceVariant = NeutralGray500,
    outline = NeutralGray200,
    outlineVariant = NeutralGray300,
    inverseSurface = Charcoal950,
    inverseOnSurface = CanvasOffWhite,
    inversePrimary = PureWhite
)

// Material 3 Dark Color Scheme (Deep Obsidian / Charcoal with Crisp Monochrome Contrast & Subtle Green Highlights)
val DarkColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = Charcoal950,
    primaryContainer = Charcoal850,
    onPrimaryContainer = PureWhite,
    secondary = BrandGreenLight,
    onSecondary = Charcoal950,
    secondaryContainer = BrandGreenDarkContainer,
    onSecondaryContainer = BrandGreenDarkOnContainer,
    tertiary = IncomeEmeraldLight,
    onTertiary = Charcoal950,
    tertiaryContainer = IncomeContainerDark,
    onTertiaryContainer = IncomeOnContainerDark,
    error = ExpenseRoseLight,
    onError = Charcoal950,
    errorContainer = ExpenseContainerDark,
    onErrorContainer = ExpenseOnContainerDark,
    background = Charcoal950,
    onBackground = PureWhite,
    surface = Charcoal900,
    onSurface = PureWhite,
    surfaceVariant = Charcoal850,
    onSurfaceVariant = NeutralGray400,
    outline = Charcoal800,
    outlineVariant = Charcoal700,
    inverseSurface = PureWhite,
    inverseOnSurface = Charcoal950,
    inversePrimary = Charcoal950
)
