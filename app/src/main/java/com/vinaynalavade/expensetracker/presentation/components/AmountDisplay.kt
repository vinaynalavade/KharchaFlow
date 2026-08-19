package com.vinaynalavade.expensetracker.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.presentation.theme.LocalCurrency
import com.vinaynalavade.expensetracker.presentation.theme.financialColors

/**
 * Reusable accessible financial amount display component.
 * Ensures consistent currency symbol placement, decimal formatting, sign indicators, and semantic colors.
 */
@Composable
fun AmountDisplay(
    amount: Amount,
    modifier: Modifier = Modifier,
    type: TransactionType? = null,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    fontWeight: FontWeight = FontWeight.SemiBold,
    overrideColor: Color? = null,
    showPrefix: Boolean = true,
    showSign: Boolean = true,
    currency: Currency = LocalCurrency.current
) {
    val shouldPrefix = showPrefix && showSign
    val formattedNumber = amount.format(currency, includeSymbol = true)

    val displayText = when {
        !shouldPrefix -> formattedNumber
        type == TransactionType.INCOME -> "+ $formattedNumber"
        type == TransactionType.EXPENSE -> "- $formattedNumber"
        else -> formattedNumber
    }

    val textColor = overrideColor ?: when (type) {
        TransactionType.INCOME -> MaterialTheme.financialColors.income
        TransactionType.EXPENSE -> MaterialTheme.financialColors.expense
        null -> MaterialTheme.colorScheme.onSurface
    }

    val accessibilityDesc = when (type) {
        TransactionType.INCOME -> "Income amount $formattedNumber"
        TransactionType.EXPENSE -> "Expense amount $formattedNumber"
        null -> "Amount $formattedNumber"
    }

    Text(
        text = displayText,
        style = style,
        fontWeight = fontWeight,
        color = textColor,
        modifier = modifier.semantics {
            contentDescription = accessibilityDesc
        }
    )
}
