package com.vinaynalavade.expensetracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vinaynalavade.expensetracker.presentation.theme.SheetShape
import com.vinaynalavade.expensetracker.presentation.theme.financialColors
import com.vinaynalavade.expensetracker.presentation.theme.spacing

/**
 * Modal Bottom Sheet providing quick selection between Add Expense and Add Income.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = SheetShape,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MaterialTheme.spacing.lg, bottom = MaterialTheme.spacing.xxl)
        ) {
            // Drag pill handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            Text(
                text = "New Transaction",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xl)
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            // Add Expense Action
            QuickAddOptionRow(
                title = "Add Expense",
                subtitle = "Track money spent, shopping, or bills",
                icon = Icons.Default.ArrowUpward,
                accentColor = MaterialTheme.financialColors.expense,
                containerColor = MaterialTheme.financialColors.expenseContainer,
                onClick = onAddExpenseClick
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xl),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // Add Income Action
            QuickAddOptionRow(
                title = "Add Income",
                subtitle = "Record earnings, salary, or investments",
                icon = Icons.Default.ArrowDownward,
                accentColor = MaterialTheme.financialColors.income,
                containerColor = MaterialTheme.financialColors.incomeContainer,
                onClick = onAddIncomeClick
            )
        }
    }
}

@Composable
private fun QuickAddOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = MaterialTheme.spacing.xl,
                vertical = MaterialTheme.spacing.md
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.lg))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
