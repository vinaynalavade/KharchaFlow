package com.vinaynalavade.expensetracker.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vinaynalavade.expensetracker.domain.model.FinancialSummary
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.presentation.components.AmountDisplay
import com.vinaynalavade.expensetracker.presentation.theme.HeroCardShape
import com.vinaynalavade.expensetracker.presentation.theme.financialColors
import com.vinaynalavade.expensetracker.presentation.theme.spacing

/**
 * Premium Hero Balance Card displaying Total Balance prominently with breakdown for Income & Expense.
 */
@Composable
fun BalanceHeroCard(
    summary: FinancialSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.lg)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = HeroCardShape
            ),
        shape = HeroCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.xl)
        ) {
            Text(
                text = "Total Balance",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

            AmountDisplay(
                amount = summary.currentBalance,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                showPrefix = false
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Income Summary Pill
                FinancialSummaryPill(
                    title = "Income",
                    amount = summary.totalIncome,
                    type = TransactionType.INCOME,
                    icon = Icons.Default.ArrowDownward,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(MaterialTheme.spacing.md))

                // Expense Summary Pill
                FinancialSummaryPill(
                    title = "Expense",
                    amount = summary.totalExpense,
                    type = TransactionType.EXPENSE,
                    icon = Icons.Default.ArrowUpward,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FinancialSummaryPill(
    title: String,
    amount: com.vinaynalavade.expensetracker.core.model.Amount,
    type: TransactionType,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val accentColor = if (type == TransactionType.INCOME) {
        MaterialTheme.financialColors.income
    } else {
        MaterialTheme.financialColors.expense
    }

    val containerColor = if (type == TransactionType.INCOME) {
        MaterialTheme.financialColors.incomeContainer
    } else {
        MaterialTheme.financialColors.expenseContainer
    }

    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(containerColor)
            .padding(MaterialTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AmountDisplay(
                amount = amount,
                type = type,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                showPrefix = false
            )
        }
    }
}
