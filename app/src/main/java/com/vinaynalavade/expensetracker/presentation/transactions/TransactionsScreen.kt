package com.vinaynalavade.expensetracker.presentation.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
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
import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import com.vinaynalavade.expensetracker.presentation.components.AppTopBar
import com.vinaynalavade.expensetracker.presentation.components.EmptyStateView
import com.vinaynalavade.expensetracker.presentation.components.LoadingView
import com.vinaynalavade.expensetracker.presentation.components.TransactionItem
import com.vinaynalavade.expensetracker.presentation.theme.CardShape
import com.vinaynalavade.expensetracker.presentation.theme.PillShape
import com.vinaynalavade.expensetracker.presentation.theme.financialColors
import com.vinaynalavade.expensetracker.presentation.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onOpenQuickAdd: () -> Unit,
    onNavigateToTransactionDetail: (Long) -> Unit,
    onNavigateToCalendar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isSearchExpanded by remember { mutableStateOf(uiState.searchQuery.isNotBlank()) }
    var showCustomDatePicker by remember { mutableStateOf(false) }

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
            // 1. Expandable Search Bar
            AnimatedVisibility(
                visible = isSearchExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text("Search by category, note, or amount...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
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

            // 2. Filter Controls (Type + Date Range)
            FilterBar(
                selectedFilter = uiState.selectedFilter,
                selectedDateRange = uiState.selectedDateRange,
                customStartDate = uiState.customStartDate,
                customEndDate = uiState.customEndDate,
                isFilterActive = uiState.isFilterActive,
                onFilterSelected = viewModel::onFilterSelected,
                onDateRangeSelected = { range ->
                    if (range == DateRangeFilter.CUSTOM) {
                        showCustomDatePicker = true
                    } else {
                        viewModel.onDateRangeSelected(range)
                    }
                },
                onCustomDateClick = { showCustomDatePicker = true },
                onResetFilters = viewModel::resetFilters
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

            // 3. Main Content
            if (uiState.isLoading) {
                LoadingView()
            } else if (uiState.totalTransactionsCount == 0) {
                // Completely empty database
                EmptyStateView(
                    title = stringResource(R.string.no_transactions_title),
                    description = stringResource(R.string.no_transactions_desc),
                    actionButtonText = stringResource(R.string.action_add_transaction),
                    onActionClick = onOpenQuickAdd
                )
            } else if (uiState.groups.isEmpty()) {
                // Filters or search returned no matches
                EmptyStateView(
                    title = "No Matching Transactions",
                    description = "No transactions found for the selected filters or keyword.",
                    actionButtonText = "Reset All Filters",
                    onActionClick = viewModel::resetFilters
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    // Summary Banner Card
                    item(key = "summary_header_card") {
                        TransactionSummaryCard(
                            summary = uiState.summary,
                            isFilterActive = uiState.isFilterActive,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = MaterialTheme.spacing.lg,
                                    vertical = MaterialTheme.spacing.xs
                                )
                        )
                    }

                    // Month and Day Grouped List
                    uiState.groups.forEach { monthGroup ->
                        // Month Banner Header
                        item(key = "month_${monthGroup.monthHeader}") {
                            MonthHeaderBanner(monthGroup = monthGroup)
                        }

                        monthGroup.dailyGroups.forEach { dayGroup ->
                            // Day Header
                            item(key = "day_${monthGroup.monthHeader}_${dayGroup.dateHeader}") {
                                DayHeaderBanner(dayGroup = dayGroup)
                            }

                            items(
                                items = dayGroup.transactions,
                                key = { it.id }
                            ) { transaction ->
                                TransactionItem(
                                    transaction = transaction,
                                    onClick = { onNavigateToTransactionDetail(transaction.id) },
                                    showDateInSubtitle = false
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.lg),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Custom Date Range Picker Dialog
    if (showCustomDatePicker) {
        CustomDateRangePickerDialog(
            initialStartDate = uiState.customStartDate,
            initialEndDate = uiState.customEndDate,
            onDismiss = { showCustomDatePicker = false },
            onDateRangeConfirmed = { startMillis, endMillis ->
                showCustomDatePicker = false
                viewModel.onCustomDateRangeSet(startMillis, endMillis)
            }
        )
    }
}

/**
 * Filter Bar with horizontally scrollable pills for Type, Date Range, and Reset.
 */
@Composable
private fun FilterBar(
    selectedFilter: TransactionFilter,
    selectedDateRange: DateRangeFilter,
    customStartDate: Long?,
    customEndDate: Long?,
    isFilterActive: Boolean,
    onFilterSelected: (TransactionFilter) -> Unit,
    onDateRangeSelected: (DateRangeFilter) -> Unit,
    onCustomDateClick: () -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.xxs)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type Filter Chips
            TransactionFilter.entries.forEach { filter ->
                val isSelected = selectedFilter == filter
                FilterChipItem(
                    label = filter.displayName,
                    isSelected = isSelected,
                    onClick = { onFilterSelected(filter) }
                )
            }

            Box(
                modifier = Modifier
                    .height(20.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            )

            // Date Range Filter Chips
            DateRangeFilter.entries.forEach { range ->
                val isSelected = selectedDateRange == range
                val label = if (range == DateRangeFilter.CUSTOM && customStartDate != null && customEndDate != null) {
                    val s = DateTimeUtils.formatDate(customStartDate)
                    val e = DateTimeUtils.formatDate(customEndDate)
                    "$s – $e"
                } else {
                    range.displayName
                }

                FilterChipItem(
                    label = label,
                    isSelected = isSelected,
                    icon = if (range == DateRangeFilter.CUSTOM) Icons.Default.DateRange else null,
                    onClick = {
                        if (range == DateRangeFilter.CUSTOM && isSelected) {
                            onCustomDateClick()
                        } else {
                            onDateRangeSelected(range)
                        }
                    }
                )
            }

            // Reset action pill
            if (isFilterActive) {
                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clip(PillShape)
                        .clickable(onClick = onResetFilters)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAltOff,
                            contentDescription = "Reset Filters",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = PillShape,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (!isSelected) {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
        } else null,
        modifier = Modifier
            .clip(PillShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Summary Card displaying total income, total expense, net change, and count for current filtered view.
 */
@Composable
private fun TransactionSummaryCard(
    summary: TransactionSummaryHeader,
    isFilterActive: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            shape = CardShape
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isFilterActive) "FILTERED OVERVIEW" else "FINANCIAL OVERVIEW",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )

                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = "${summary.transactionCount} ${if (summary.transactionCount == 1) "entry" else "entries"}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                modifier = Modifier.padding(vertical = 2.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Income
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "+${Amount(summary.totalIncome).format()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.financialColors.income
                    )
                }

                // Expense
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Expense",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Amount(summary.totalExpense).format(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.financialColors.expense
                    )
                }

                // Net Balance
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Net",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val isPositive = summary.netBalance >= 0
                    val prefix = if (isPositive && summary.netBalance > 0) "+" else ""
                    Text(
                        text = "$prefix${Amount(summary.netBalance).format()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) MaterialTheme.financialColors.income else MaterialTheme.financialColors.expense
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthHeaderBanner(monthGroup: MonthlyTransactionGroup) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = MaterialTheme.spacing.lg,
                end = MaterialTheme.spacing.lg,
                top = MaterialTheme.spacing.md,
                bottom = MaterialTheme.spacing.xs
            )
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = monthGroup.monthHeader.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            if (monthGroup.totalExpense > 0) {
                Text(
                    text = "Out: ${Amount(monthGroup.totalExpense).format()}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DayHeaderBanner(dayGroup: DailyTransactionGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = MaterialTheme.spacing.lg,
                end = MaterialTheme.spacing.lg,
                top = MaterialTheme.spacing.sm,
                bottom = MaterialTheme.spacing.xxs
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dayGroup.dateHeader,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val totalDayOut = dayGroup.totalExpense
        if (totalDayOut > 0) {
            Text(
                text = "-${Amount(totalDayOut).format()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDateRangePickerDialog(
    initialStartDate: Long?,
    initialEndDate: Long?,
    onDismiss: () -> Unit,
    onDateRangeConfirmed: (Long, Long) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartDate ?: System.currentTimeMillis(),
        initialSelectedEndDateMillis = initialEndDate ?: System.currentTimeMillis()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis ?: start
                    if (start != null && end != null) {
                        onDateRangeConfirmed(start, end)
                    }
                },
                enabled = dateRangePickerState.selectedStartDateMillis != null
            ) {
                Text("Apply Range", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = CardShape
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Text(
                    text = "Select Transaction Date Range",
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            headline = {
                val start = dateRangePickerState.selectedStartDateMillis
                val end = dateRangePickerState.selectedEndDateMillis
                val headlineText = if (start != null && end != null) {
                    "${DateTimeUtils.formatDate(start)} – ${DateTimeUtils.formatDate(end)}"
                } else if (start != null) {
                    "${DateTimeUtils.formatDate(start)} – Select end"
                } else {
                    "Choose start and end dates"
                }
                Text(
                    text = headlineText,
                    modifier = Modifier.padding(start = 24.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            showModeToggle = false,
            modifier = Modifier.weight(1f)
        )
    }
}
