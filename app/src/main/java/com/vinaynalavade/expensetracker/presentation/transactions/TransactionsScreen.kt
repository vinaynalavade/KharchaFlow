package com.vinaynalavade.expensetracker.presentation.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinaynalavade.expensetracker.R
import com.vinaynalavade.expensetracker.presentation.components.AppTopBar
import com.vinaynalavade.expensetracker.presentation.components.EmptyStateView
import com.vinaynalavade.expensetracker.presentation.components.LoadingView
import com.vinaynalavade.expensetracker.presentation.components.TransactionItem
import com.vinaynalavade.expensetracker.presentation.components.UiState
import com.vinaynalavade.expensetracker.presentation.theme.PillShape
import com.vinaynalavade.expensetracker.presentation.theme.spacing

import androidx.compose.material.icons.filled.CalendarMonth

@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onOpenQuickAdd: () -> Unit,
    onNavigateToTransactionDetail: (Long) -> Unit,
    onNavigateToCalendar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var isSearchExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.nav_transactions),
                actions = {
                    IconButton(onClick = onNavigateToCalendar) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Calendar View",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = {
                        isSearchExpanded = !isSearchExpanded
                        if (!isSearchExpanded) viewModel.onSearchQueryChanged("")
                    }) {
                        Icon(
                            imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search Transactions"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenQuickAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.action_add_transaction)
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar (Expandable)
            AnimatedVisibility(visible = isSearchExpanded) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text("Search by category or note...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = PillShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.xs)
                )
            }

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
            ) {
                TransactionFilter.entries.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        shape = PillShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .clip(PillShape)
                            .clickable { viewModel.onFilterSelected(filter) }
                    ) {
                        Text(
                            text = filter.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.xs)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

            // Main Content
            when (val state = uiState) {
                is UiState.Loading -> {
                    LoadingView()
                }
                is UiState.Empty -> {
                    val emptyTitle = if (searchQuery.isNotBlank()) "No Matching Transactions" else stringResource(R.string.no_transactions_title)
                    val emptyDesc = if (searchQuery.isNotBlank()) "Try searching for a different keyword." else stringResource(R.string.no_transactions_desc)
                    EmptyStateView(
                        title = emptyTitle,
                        description = emptyDesc,
                        actionButtonText = stringResource(R.string.action_add_transaction),
                        onActionClick = onOpenQuickAdd
                    )
                }
                is UiState.Error -> {
                    EmptyStateView(
                        title = "Error Loading Transactions",
                        description = state.message,
                        actionButtonText = "Retry",
                        onActionClick = { viewModel.onFilterSelected(selectedFilter) }
                    )
                }
                is UiState.Success -> {
                    val monthGroups = state.data
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = MaterialTheme.spacing.lg,
                            vertical = MaterialTheme.spacing.xs
                        )
                    ) {
                        monthGroups.forEach { monthGroup ->
                            // Month Banner Header
                            item(key = "month_${monthGroup.monthHeader}") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = MaterialTheme.spacing.md, bottom = MaterialTheme.spacing.xs)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.xs)
                                ) {
                                    Text(
                                        text = monthGroup.monthHeader.uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            monthGroup.dailyGroups.forEach { dayGroup ->
                                // Day Header
                                item(key = "day_${monthGroup.monthHeader}_${dayGroup.dateHeader}") {
                                    Text(
                                        text = dayGroup.dateHeader,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(
                                            start = MaterialTheme.spacing.xs,
                                            top = MaterialTheme.spacing.sm,
                                            bottom = MaterialTheme.spacing.xxs
                                        )
                                    )
                                }

                                items(
                                    items = dayGroup.transactions,
                                    key = { it.id }
                                ) { transaction ->
                                    TransactionItem(
                                        transaction = transaction,
                                        onClick = { onNavigateToTransactionDetail(transaction.id) },
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}
