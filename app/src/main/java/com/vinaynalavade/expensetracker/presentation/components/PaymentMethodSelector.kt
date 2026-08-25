package com.vinaynalavade.expensetracker.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.presentation.theme.PillShape
import com.vinaynalavade.expensetracker.presentation.theme.spacing

/**
 * Reusable, premium Financial Source selector supporting Cash and Account.
 * Features subtle animations, clear selected states, and accessible semantics.
 */
@Composable
fun PaymentMethodSelector(
    selectedMethod: PaymentMethod,
    onMethodSelect: (PaymentMethod) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    horizontalPadding: Dp = if (isCompact) 0.dp else MaterialTheme.spacing.lg,
    showLabel: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
    ) {
        if (showLabel) {
            Text(
                text = "FINANCIAL SOURCE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) MaterialTheme.spacing.xs else MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PaymentMethod.entries.forEach { method ->
                val isSelected = selectedMethod == method
                PaymentMethodOption(
                    method = method,
                    isSelected = isSelected,
                    isCompact = isCompact,
                    onClick = { onMethodSelect(method) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodOption(
    method: PaymentMethod,
    isSelected: Boolean,
    isCompact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (method) {
        PaymentMethod.CASH -> Icons.Default.Payments
        PaymentMethod.ACCOUNT -> Icons.Default.AccountBalance
    }

    val animatedBgColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "bgColor"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        },
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "borderColor"
    )

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val iconTint = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val displayLabel = when (method) {
        PaymentMethod.CASH -> "Cash"
        PaymentMethod.ACCOUNT -> "Account"
    }

    Row(
        modifier = modifier
            .clip(PillShape)
            .background(animatedBgColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = animatedBorderColor,
                shape = PillShape
            )
            .clickable(onClick = onClick)
            .semantics {
                role = Role.RadioButton
                selected = isSelected
            }
            .padding(
                horizontal = if (isCompact) MaterialTheme.spacing.xs else MaterialTheme.spacing.sm,
                vertical = if (isCompact) 8.dp else 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = method.displayName,
            tint = iconTint,
            modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
        )

        Spacer(modifier = Modifier.width(if (isCompact) 4.dp else 6.dp))

        Text(
            text = displayLabel,
            style = if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1
        )
    }
}

