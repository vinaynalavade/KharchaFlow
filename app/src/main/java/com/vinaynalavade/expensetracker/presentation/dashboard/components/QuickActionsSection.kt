package com.vinaynalavade.expensetracker.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vinaynalavade.expensetracker.presentation.theme.ButtonShape
import com.vinaynalavade.expensetracker.presentation.theme.financialColors
import com.vinaynalavade.expensetracker.presentation.theme.spacing

/**
 * Quick action shortcuts on the dashboard for essential daily operations.
 */
@Composable
fun QuickActionsSection(
    onAddExpenseClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onViewTransactionsClick: () -> Unit,
    onViewCategoriesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
    ) {
        QuickActionButton(
            label = "Expense",
            icon = Icons.Default.ArrowUpward,
            iconTint = MaterialTheme.financialColors.expense,
            containerColor = MaterialTheme.financialColors.expenseContainer,
            onClick = onAddExpenseClick,
            modifier = Modifier.weight(1f)
        )

        QuickActionButton(
            label = "Income",
            icon = Icons.Default.ArrowDownward,
            iconTint = MaterialTheme.financialColors.income,
            containerColor = MaterialTheme.financialColors.incomeContainer,
            onClick = onAddIncomeClick,
            modifier = Modifier.weight(1f)
        )

        QuickActionButton(
            label = "History",
            icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ReceiptLong,
            iconTint = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            onClick = onViewTransactionsClick,
            modifier = Modifier.weight(1f)
        )

        QuickActionButton(
            label = "Categories",
            icon = Icons.Default.Category,
            iconTint = MaterialTheme.colorScheme.secondary,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            onClick = onViewCategoriesClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(ButtonShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = ButtonShape
            )
            .clickable(onClick = onClick)
            .padding(vertical = MaterialTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
