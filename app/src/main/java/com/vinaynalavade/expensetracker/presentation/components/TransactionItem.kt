package com.vinaynalavade.expensetracker.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.presentation.theme.pressScale
import com.vinaynalavade.expensetracker.presentation.theme.spacing

/**
 * Clean, scannable transaction list item with category icon, title, contextual metadata, payment method, and amount.
 */
@Composable
fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDateInSubtitle: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
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

            val timeOrDate = if (showDateInSubtitle) {
                DateTimeUtils.formatDate(transaction.timestamp)
            } else {
                DateTimeUtils.formatTime(transaction.timestamp, context)
            }

            val notePrefix = if (!transaction.note.isNullOrBlank()) {
                "${transaction.note} • "
            } else {
                ""
            }

            val methodIcon = when (transaction.paymentMethod) {
                PaymentMethod.CASH -> Icons.Default.Payments
                PaymentMethod.ACCOUNT -> Icons.Default.AccountBalance
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$notePrefix$timeOrDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Text(
                    text = " • ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                Icon(
                    imageVector = methodIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier.size(12.dp)
                )

                Spacer(modifier = Modifier.width(3.dp))

                Text(
                    text = transaction.paymentMethod.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
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
