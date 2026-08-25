package com.vinaynalavade.expensetracker.presentation.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vinaynalavade.expensetracker.presentation.navigation.Screen
import com.vinaynalavade.expensetracker.presentation.theme.PillShape

val BottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Transactions,
    Screen.Analytics,
    Screen.Settings
)

/**
 * Premium, modern Bottom Navigation Bar for KharchaFlow.
 * Provides clear active tab hierarchy, refined pill indicators, and edge-to-edge window inset support.
 */
@Composable
fun AppBottomBar(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        BottomNavItems.forEach { screen ->
            val isSelected = currentRoute == screen.route ||
                (screen == Screen.Transactions && currentRoute?.startsWith("transactions") == true) ||
                (screen == Screen.Analytics && currentRoute == Screen.MonthlySummary.route)

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        val targetRoute = if (screen == Screen.Transactions) {
                            Screen.Transactions.createRoute()
                        } else {
                            screen.route
                        }
                        onNavigateToRoute(targetRoute)
                    }
                },
                icon = {
                    val icon = screen.icon
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = stringResource(screen.titleResId),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = stringResource(screen.titleResId),
                        style = if (isSelected) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            )
        }
    }
}
