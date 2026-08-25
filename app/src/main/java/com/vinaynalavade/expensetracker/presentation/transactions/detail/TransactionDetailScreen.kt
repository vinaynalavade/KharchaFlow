package com.vinaynalavade.expensetracker.presentation.transactions.detail

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.presentation.components.AmountDisplay
import com.vinaynalavade.expensetracker.presentation.components.AppTopBar
import com.vinaynalavade.expensetracker.presentation.components.CategoryIcon
import com.vinaynalavade.expensetracker.presentation.components.LoadingView
import com.vinaynalavade.expensetracker.presentation.theme.ButtonShape
import com.vinaynalavade.expensetracker.presentation.theme.CardShape
import com.vinaynalavade.expensetracker.presentation.theme.spacing

@Composable
fun TransactionDetailScreen(
    viewModel: TransactionDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onTransactionDeleted: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Transaction Details",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            if (uiState is TransactionDetailUiState.Success) {
                val tx = (uiState as TransactionDetailUiState.Success).transaction
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        shape = ButtonShape,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                        Text(text = "Delete", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onNavigateToEdit(tx.id) },
                        shape = ButtonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                        Text(text = "Edit", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        when (val state = uiState) {
            is TransactionDetailUiState.Loading -> {
                LoadingView(modifier = Modifier.padding(innerPadding))
            }
            is TransactionDetailUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(MaterialTheme.spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is TransactionDetailUiState.Success -> {
                val tx = state.transaction
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

                    CategoryIcon(
                        iconName = tx.category.iconName,
                        colorHex = tx.category.colorHex,
                        size = 64.dp,
                        iconSize = 32.dp,
                        cornerRadius = 32.dp
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

                    Text(
                        text = tx.category.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

                    AmountDisplay(
                        amount = tx.amount,
                        type = tx.type,
                        style = MaterialTheme.typography.displayMedium,
                        showPrefix = true
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))

                    // Detail Information Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                shape = CardShape
                            ),
                        shape = CardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(MaterialTheme.spacing.lg)) {
                            DetailRow(label = "Type", value = tx.type.displayName)
                            HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.md), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            DetailRow(label = "Financial Source", value = tx.paymentMethod.displayName)
                            HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.md), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            DetailRow(label = "Date", value = DateTimeUtils.formatDate(tx.timestamp))
                            HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.md), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            DetailRow(label = "Note", value = tx.note?.ifBlank { "None" } ?: "None")
                            HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.md), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            DetailRow(label = "Recorded", value = DateTimeUtils.formatDateTime(tx.createdAt))
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(text = "Delete Transaction?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "This transaction will be removed from your records. You can undo this action immediately after.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteTransaction { deletedTx ->
                            onTransactionDeleted(deletedTx)
                        }
                    }
                ) {
                    Text(text = "Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = "Cancel")
                }
            },
            shape = CardShape,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
