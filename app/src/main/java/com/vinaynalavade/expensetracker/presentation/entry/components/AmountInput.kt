package com.vinaynalavade.expensetracker.presentation.entry.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.presentation.theme.LocalCurrency
import com.vinaynalavade.expensetracker.presentation.theme.PillShape
import com.vinaynalavade.expensetracker.presentation.theme.financialColors
import com.vinaynalavade.expensetracker.presentation.theme.spacing

/**
 * Calculator-inspired prominent amount input component.
 * Allows easy numeric entry with currency symbol, automatic decimal validation, and clear error states.
 */
@Composable
fun AmountInput(
    amountText: String,
    onAmountChange: (String) -> Unit,
    transactionType: TransactionType,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    focusRequester: FocusRequester? = null,
    onOpenCalculator: (() -> Unit)? = null,
    onImeAction: () -> Unit = {},
    currency: Currency = LocalCurrency.current
) {
    val interactionSource = remember { MutableInteractionSource() }

    val accentColor = if (transactionType == TransactionType.INCOME) {
        MaterialTheme.financialColors.income
    } else {
        MaterialTheme.financialColors.expense
    }

    val displayColor = if (amountText.isNotBlank()) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AMOUNT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            if (onOpenCalculator != null) {
                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(PillShape)
                        .clickable(onClick = onOpenCalculator)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Open in-app calculator",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Calculator",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

        // Center Calculator-style Amount Display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    focusRequester?.requestFocus()
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Currency Symbol
            Text(
                text = currency.symbol,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                modifier = Modifier.padding(end = MaterialTheme.spacing.xs)
            )

            // Hidden / Integrated BasicTextField with large styling
            BasicTextField(
                value = amountText,
                onValueChange = { input ->
                    val filtered = sanitizeAmountInput(input, currency.decimalDigits)
                    onAmountChange(filtered)
                },
                modifier = Modifier
                    .then(
                        if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
                    )
                    .semantics {
                        contentDescription = "Transaction amount in ${currency.name}"
                    },
                textStyle = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = displayColor,
                    textAlign = TextAlign.Start
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { onImeAction() }
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (amountText.isEmpty()) {
                            Text(
                                text = "0",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        // Error message transition
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Sanitizes and restricts input string to valid decimal representations.
 */
private fun sanitizeAmountInput(input: String, maxDecimalDigits: Int): String {
    val clean = input.filter { it.isDigit() || it == '.' }

    val parts = clean.split('.')
    if (parts.size > 2) {
        // Disallow multiple decimal points
        return parts[0] + "." + parts[1]
    }

    if (parts.size == 2 && parts[1].length > maxDecimalDigits) {
        // Restrict to max decimal digits
        return parts[0] + "." + parts[1].take(maxDecimalDigits)
    }

    // Handle leading zeros: if "05" -> "5", but allow "0" or "0."
    if (clean.length > 1 && clean.startsWith("0") && clean[1] != '.') {
        return clean.dropWhile { it == '0' }
    }

    return clean
}
