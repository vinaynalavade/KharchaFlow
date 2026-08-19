package com.vinaynalavade.expensetracker.presentation.recurring

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinaynalavade.expensetracker.domain.model.RecurringTransaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.presentation.components.AmountDisplay
import com.vinaynalavade.expensetracker.presentation.components.AppTopBar
import com.vinaynalavade.expensetracker.presentation.components.CategoryIcon
import com.vinaynalavade.expensetracker.presentation.components.EmptyStateView
import com.vinaynalavade.expensetracker.presentation.components.LoadingView
import com.vinaynalavade.expensetracker.presentation.components.UiState
import com.vinaynalavade.expensetracker.presentation.theme.CardShape
import com.vinaynalavade.expensetracker.presentation.theme.PureWhite
import com.vinaynalavade.expensetracker.presentation.theme.spacing

@Composable
fun RecurringTransactionsScreen(
    viewModel: RecurringViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<RecurringTransaction?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Recurring & EMI",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = {
                        itemToEdit = null
                        showAddDialog = true
                    }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Recurring")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    itemToEdit = null
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = PureWhite,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Recurring")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        when (val state = uiState) {
            is UiState.Loading -> {
                LoadingView(modifier = Modifier.padding(innerPadding))
            }
            is UiState.Empty -> {
                EmptyStateView(
                    title = "No Recurring Transactions",
                    description = "Schedule recurring income (e.g. Salary) or expenses (e.g. Rent, EMI, Subscriptions).",
                    actionButtonText = "Schedule Recurring",
                    onActionClick = {
                        itemToEdit = null
                        showAddDialog = true
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            is UiState.Error -> {
                EmptyStateView(
                    title = "Error",
                    description = state.message,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            is UiState.Success -> {
                val data = state.data
                if (data.items.isEmpty()) {
                    EmptyStateView(
                        title = "No Recurring Transactions",
                        description = "Schedule recurring income (e.g. Salary) or expenses (e.g. Rent, EMI, Subscriptions).",
                        actionButtonText = "Schedule Recurring",
                        onActionClick = {
                            itemToEdit = null
                            showAddDialog = true
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(
                            horizontal = MaterialTheme.spacing.lg,
                            vertical = MaterialTheme.spacing.md
                        ),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
                    ) {
                        items(items = data.items, key = { it.id }) { item ->
                            RecurringCard(
                                item = item,
                                onToggle = { isEnabled -> viewModel.toggleEnabled(item, isEnabled) },
                                onEdit = {
                                    itemToEdit = item
                                    showAddDialog = true
                                },
                                onDelete = { viewModel.deleteRecurring(item.id) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }

                if (showAddDialog) {
                    AddEditRecurringDialog(
                        itemToEdit = itemToEdit,
                        availableCategories = data.categories,
                        onDismiss = { showAddDialog = false },
                        onSave = { newItem ->
                            viewModel.saveRecurring(newItem) {
                                showAddDialog = false
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurringCard(
    item: RecurringTransaction,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIcon(
                        iconName = item.category.iconName,
                        colorHex = item.category.colorHex,
                        size = 38.dp,
                        iconSize = 20.dp
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.md))
                    Column {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${item.frequency.displayName} on day ${item.dayOfMonth}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(checked = item.isEnabled, onCheckedChange = onToggle)
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AmountDisplay(
                    amount = item.amount,
                    type = item.type,
                    style = MaterialTheme.typography.titleMedium,
                    showPrefix = true
                )

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
