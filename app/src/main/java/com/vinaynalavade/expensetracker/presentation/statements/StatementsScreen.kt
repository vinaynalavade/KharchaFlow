package com.vinaynalavade.expensetracker.presentation.statements

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinaynalavade.expensetracker.core.utils.PdfUtils
import com.vinaynalavade.expensetracker.domain.model.StatementReport
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.presentation.components.AmountDisplay
import com.vinaynalavade.expensetracker.presentation.components.AppTopBar
import com.vinaynalavade.expensetracker.presentation.components.LoadingView
import com.vinaynalavade.expensetracker.presentation.components.UiState
import com.vinaynalavade.expensetracker.presentation.theme.ButtonShape
import com.vinaynalavade.expensetracker.presentation.theme.CardShape
import com.vinaynalavade.expensetracker.presentation.theme.PillShape
import com.vinaynalavade.expensetracker.presentation.theme.financialColors
import com.vinaynalavade.expensetracker.presentation.theme.spacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatementsScreen(
    viewModel: StatementsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isGeneratingPdf by viewModel.isGeneratingPdf.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Financial Statements",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            if (uiState is UiState.Success) {
                Button(
                    onClick = {
                        viewModel.generateAndSharePdf(context) { pdfFile ->
                            val shareIntent = PdfUtils.createShareIntent(context, pdfFile)
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Financial Statement"))
                        }
                    },
                    enabled = !isGeneratingPdf,
                    shape = ButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.md)
                        .height(52.dp)
                ) {
                    if (isGeneratingPdf) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                        Text(text = "Generate & Share PDF Statement", fontWeight = FontWeight.Bold)
                    }
                }
            }
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
            Text(
                text = "SELECT PERIOD",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
            ) {
                StatementPeriodOption.entries.forEach { period ->
                    val isSelected = selectedPeriod == period
                    Surface(
                        shape = PillShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .clip(PillShape)
                            .clickable { viewModel.onPeriodSelected(period) }
                    ) {
                        Text(
                            text = period.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.xs)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            when (val state = uiState) {
                is UiState.Loading -> {
                    LoadingView()
                }
                is UiState.Empty -> {
                    Text(text = "No data available for this statement period.")
                }
                is UiState.Error -> {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    StatementPreviewContent(report = state.data)
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun StatementPreviewContent(report: StatementReport) {
    // Statement Summary Box
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), shape = CardShape),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.lg)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Statement Period", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = report.currency.code, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Text(text = report.periodTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.md), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Opening Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = report.openingBalance.format(report.currency), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(text = "Closing Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = report.closingBalance.format(report.currency), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Total Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "+ " + report.totalIncome.format(report.currency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.financialColors.income)
                }
                Column {
                    Text(text = "Total Expenses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "- " + report.totalExpense.format(report.currency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.financialColors.expense)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

    // Running Balance Ledger Preview
    Text(
        text = "RUNNING BALANCE LEDGER (${report.ledgerItems.size} entries)",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), shape = CardShape),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
            report.ledgerItems.take(8).forEachIndexed { idx, item ->
                if (idx > 0) HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.xs), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(text = "${item.dateString} • ${item.categoryName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        if (item.amount != null && item.type != null) {
                            AmountDisplay(
                                amount = item.amount,
                                type = item.type,
                                style = MaterialTheme.typography.bodyMedium,
                                showPrefix = true
                            )
                        }
                        Text(
                            text = "Bal: ${item.runningBalance.format(report.currency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (report.ledgerItems.size > 8) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                Text(
                    text = "+ ${report.ledgerItems.size - 8} more transactions included in PDF",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
