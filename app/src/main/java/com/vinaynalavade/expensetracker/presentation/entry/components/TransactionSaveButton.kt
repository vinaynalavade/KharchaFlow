package com.vinaynalavade.expensetracker.presentation.entry.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.presentation.theme.ButtonShape
import com.vinaynalavade.expensetracker.presentation.theme.PureWhite
import com.vinaynalavade.expensetracker.presentation.theme.financialColors
import com.vinaynalavade.expensetracker.presentation.theme.spacing

/**
 * Bottom-aligned prominent action button for persisting transaction entries in Create or Edit mode.
 */
@Composable
fun TransactionSaveButton(
    transactionType: TransactionType,
    isEnabled: Boolean,
    isSaving: Boolean,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false
) {
    val buttonText = if (isEditMode) {
        if (transactionType == TransactionType.INCOME) "Update Income" else "Update Expense"
    } else {
        if (transactionType == TransactionType.INCOME) "Save Income" else "Save Expense"
    }

    val buttonColor = if (transactionType == TransactionType.INCOME) {
        MaterialTheme.financialColors.income
    } else {
        MaterialTheme.financialColors.expense
    }

    Button(
        onClick = onSaveClick,
        enabled = isEnabled && !isSaving,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = PureWhite,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = MaterialTheme.spacing.lg)
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                color = PureWhite,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = buttonText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
