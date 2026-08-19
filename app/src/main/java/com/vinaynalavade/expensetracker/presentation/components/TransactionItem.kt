package com.vinaynalavade.expensetracker.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.presentation.theme.spacing

/**
 * Clean, scannable transaction list item with category icon, title, contextual metadata, and amount.
 */
@Composable
fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDateInSubtitle: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = MaterialTheme.spacing.lg,
                vertical = MaterialTheme.spacing.md
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(
            iconName = transaction.category.iconName,
            colorHex = transaction.category.colorHex
        )

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.md))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = transaction.category.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val subtitle = when {
                !transaction.note.isNullOrBlank() && showDateInSubtitle -> {
                    "${transaction.note} • ${DateTimeUtils.formatDate(transaction.timestamp)}"
                }
                !transaction.note.isNullOrBlank() -> {
                    "${transaction.note} • ${DateTimeUtils.formatTime(transaction.timestamp)}"
                }
                showDateInSubtitle -> {
                    DateTimeUtils.formatDate(transaction.timestamp)
                }
                else -> {
                    DateTimeUtils.formatTime(transaction.timestamp)
                }
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))

        AmountDisplay(
            amount = transaction.amount,
            type = transaction.type,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
