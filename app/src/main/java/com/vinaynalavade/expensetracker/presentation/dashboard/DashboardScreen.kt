package com.vinaynalavade.expensetracker.presentation.dashboard

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinaynalavade.expensetracker.R
import com.vinaynalavade.expensetracker.presentation.components.EmptyStateView
import com.vinaynalavade.expensetracker.presentation.components.LoadingView
import com.vinaynalavade.expensetracker.presentation.components.SectionHeader
import com.vinaynalavade.expensetracker.presentation.components.TransactionItem
import com.vinaynalavade.expensetracker.presentation.dashboard.components.BalanceHeroCard
import com.vinaynalavade.expensetracker.presentation.dashboard.components.MonthlyOverviewCard
import com.vinaynalavade.expensetracker.presentation.dashboard.components.GreetingHeader
import com.vinaynalavade.expensetracker.presentation.dashboard.components.QuickActionsSection
import com.vinaynalavade.expensetracker.presentation.theme.spacing

/**
 * Modern, calm financial dashboard screen.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToAddIncome: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onOpenQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenQuickAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_add_transaction)
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingView(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                item {
                    GreetingHeader()
                }

                item {
                    BalanceHeroCard(summary = uiState.summary)
                }

                item {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))
                    QuickActionsSection(
                        onAddExpenseClick = onNavigateToAddExpense,
                        onAddIncomeClick = onNavigateToAddIncome,
                        onViewTransactionsClick = onNavigateToTransactions,
                        onViewCategoriesClick = onNavigateToCategories
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
                    SectionHeader(
                        title = "Recent Activity",
                        actionText = if (uiState.recentTransactions.isNotEmpty()) "View all" else null,
                        onActionClick = onNavigateToTransactions
                    )
                }

                if (uiState.recentTransactions.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = stringResource(R.string.no_transactions_title),
                            description = stringResource(R.string.no_transactions_desc),
                            actionButtonText = "Record First Transaction",
                            onActionClick = onOpenQuickAdd
                        )
                    }
                } else {
                    items(
                        items = uiState.recentTransactions,
                        key = { it.id }
                    ) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            onClick = { /* Transaction details in future phase */ },
                            showDateInSubtitle = false
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.lg),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
                    MonthlyOverviewCard(summary = uiState.summary)
                }
            }
        }
    }
}
