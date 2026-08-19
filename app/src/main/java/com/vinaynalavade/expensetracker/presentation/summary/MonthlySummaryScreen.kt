package com.vinaynalavade.expensetracker.presentation.summary

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.domain.model.CategorySpending
import com.vinaynalavade.expensetracker.domain.model.MonthlyLedgerSummary
import com.vinaynalavade.expensetracker.presentation.components.AmountDisplay
import com.vinaynalavade.expensetracker.presentation.components.AppTopBar
import com.vinaynalavade.expensetracker.presentation.components.CategoryIcon
import com.vinaynalavade.expensetracker.presentation.components.LoadingView
import com.vinaynalavade.expensetracker.presentation.components.TransactionItem
import com.vinaynalavade.expensetracker.presentation.components.UiState
import com.vinaynalavade.expensetracker.presentation.theme.CardShape
import com.vinaynalavade.expensetracker.presentation.theme.HeroCardShape
import com.vinaynalavade.expensetracker.presentation.theme.financialColors
import com.vinaynalavade.expensetracker.presentation.theme.spacing
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MonthlySummaryScreen(
    viewModel: MonthlySummaryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTransactionDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Monthly Summary",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm)
        ) {
            // Month Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MaterialTheme.spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = viewModel::onPreviousMonth) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                }

                Text(
                    text = selectedMonth.format(monthFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(onClick = viewModel::onNextMonth) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            when (val state = uiState) {
                is UiState.Loading -> {
                    LoadingView()
                }
                is UiState.Empty -> {
                    Text(text = "No records for this month.")
                }
                is UiState.Error -> {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    val summary = state.data
                    MonthlySummaryContent(
                        summary = summary,
                        onTransactionClick = onNavigateToTransactionDetail
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xxl))
        }
    }
}

@Composable
private fun MonthlySummaryContent(
    summary: MonthlyLedgerSummary,
    onTransactionClick: (Long) -> Unit
) {
    // 1. Hero Balance Overview Card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = HeroCardShape
            ),
        shape = HeroCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.lg)) {
            Text(
                text = "CLOSING BALANCE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
            AmountDisplay(
                amount = summary.closingBalance,
                style = MaterialTheme.typography.displaySmall
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = MaterialTheme.spacing.md),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryStat(label = "Opening Balance", amount = summary.openingBalance)
                SummaryStat(label = "Net Change", amount = summary.netChange, isHighlight = true)
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryStat(label = "Total Income", amount = summary.totalIncome, color = MaterialTheme.financialColors.income)
                SummaryStat(label = "Total Expenses", amount = summary.totalExpense, color = MaterialTheme.financialColors.expense)
            }
        }
    }

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))

    // 2. Expense Category Breakdown
    if (summary.expenseBreakdown.isNotEmpty()) {
        Text(
            text = "TOP EXPENSE CATEGORIES",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    shape = CardShape
                ),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
                summary.expenseBreakdown.take(5).forEachIndexed { idx, item ->
                    if (idx > 0) HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.sm), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    CategorySpendingRow(item = item)
                }
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
    }

    // 3. Transactions in Month
    Text(
        text = "MONTH ACTIVITY (${summary.transactions.size})",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

    if (summary.transactions.isEmpty()) {
        Text(
            text = "No transactions recorded in this month.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    } else {
        summary.transactions.take(10).forEach { tx ->
            TransactionItem(
                transaction = tx,
                onClick = { onTransactionClick(tx.id) },
                modifier = Modifier.padding(vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun SummaryStat(
    label: String,
    amount: Amount,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    isHighlight: Boolean = false
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = amount.format(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun CategorySpendingRow(item: CategorySpending) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(iconName = item.category.iconName, colorHex = item.category.colorHex, size = 30.dp, iconSize = 16.dp)
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                Text(text = item.category.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            Text(text = item.totalAmount.format(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { item.percentageOfTotal },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = MaterialTheme.financialColors.expense,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
    }
}
