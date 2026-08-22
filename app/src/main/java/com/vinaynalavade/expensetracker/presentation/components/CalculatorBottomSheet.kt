package com.vinaynalavade.expensetracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinaynalavade.expensetracker.R
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.domain.usecase.CalculatorEvaluationResult
import com.vinaynalavade.expensetracker.domain.usecase.EvaluateCalculatorExpressionUseCase
import com.vinaynalavade.expensetracker.presentation.theme.ButtonShape
import com.vinaynalavade.expensetracker.presentation.theme.CardShape
import com.vinaynalavade.expensetracker.presentation.theme.LocalCurrency
import com.vinaynalavade.expensetracker.presentation.theme.SheetShape
import com.vinaynalavade.expensetracker.presentation.theme.financialColors
import com.vinaynalavade.expensetracker.presentation.theme.spacing

/**
 * Material 3 in-app calculator bottom sheet for financial amount calculations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorBottomSheet(
    sheetState: SheetState,
    initialAmount: String = "",
    onDismissRequest: () -> Unit,
    onUseResult: (String) -> Unit,
    currency: Currency = LocalCurrency.current,
    modifier: Modifier = Modifier
) {
    val evaluator = remember { EvaluateCalculatorExpressionUseCase() }

    var expression by remember {
        mutableStateOf(
            if (initialAmount.isNotBlank() && initialAmount != "0") initialAmount else ""
        )
    }

    val evaluationResult = remember(expression) {
        evaluator(expression, currency.decimalDigits)
    }

    val scrollState = rememberScrollState()

    // Auto-scroll expression display to the end as user types
    LaunchedEffect(expression) {
        if (expression.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

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
                .navigationBarsPadding()
                .padding(top = MaterialTheme.spacing.md, bottom = MaterialTheme.spacing.lg)
                .padding(horizontal = MaterialTheme.spacing.lg)
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                    Text(
                        text = "Amount Calculator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Calculator",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            // 1. Expression Display Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = CardShape
                    ),
                shape = CardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.sm),
                    horizontalAlignment = Alignment.End
                ) {
                    // Active Expression String
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = if (expression.isEmpty()) "0" else expression,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (expression.isEmpty()) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Live Evaluation Result Line
                    when (val res = evaluationResult) {
                        is CalculatorEvaluationResult.Success -> {
                            Text(
                                text = "= ${currency.symbol} ${res.displayString}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (res.isPositive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                        is CalculatorEvaluationResult.Error -> {
                            Text(
                                text = res.message,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        is CalculatorEvaluationResult.Incomplete,
                        is CalculatorEvaluationResult.Empty -> {
                            Text(
                                text = "= ${currency.symbol} 0",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            // 2. Keypad Layout (4 columns x 5 rows)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Clear, Backspace, Divide, Multiply
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalculatorButton(
                        text = "C",
                        onClick = { expression = "" },
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    CalculatorIconButton(
                        icon = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        onClick = {
                            if (expression.isNotEmpty()) {
                                expression = if (expression.endsWith(" ")) {
                                    expression.dropLast(3)
                                } else {
                                    expression.dropLast(1)
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    CalculatorButton(
                        text = "÷",
                        onClick = { expression = appendOperator(expression, "÷") },
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    CalculatorButton(
                        text = "×",
                        onClick = { expression = appendOperator(expression, "×") },
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: 7, 8, 9, Minus
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalculatorButton(text = "7", onClick = { expression = appendDigit(expression, "7") }, modifier = Modifier.weight(1f))
                    CalculatorButton(text = "8", onClick = { expression = appendDigit(expression, "8") }, modifier = Modifier.weight(1f))
                    CalculatorButton(text = "9", onClick = { expression = appendDigit(expression, "9") }, modifier = Modifier.weight(1f))
                    CalculatorButton(
                        text = "-",
                        onClick = { expression = appendOperator(expression, "-") },
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3: 4, 5, 6, Plus
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalculatorButton(text = "4", onClick = { expression = appendDigit(expression, "4") }, modifier = Modifier.weight(1f))
                    CalculatorButton(text = "5", onClick = { expression = appendDigit(expression, "5") }, modifier = Modifier.weight(1f))
                    CalculatorButton(text = "6", onClick = { expression = appendDigit(expression, "6") }, modifier = Modifier.weight(1f))
                    CalculatorButton(
                        text = "+",
                        onClick = { expression = appendOperator(expression, "+") },
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 4: 1, 2, 3, Equals
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalculatorButton(text = "1", onClick = { expression = appendDigit(expression, "1") }, modifier = Modifier.weight(1f))
                    CalculatorButton(text = "2", onClick = { expression = appendDigit(expression, "2") }, modifier = Modifier.weight(1f))
                    CalculatorButton(text = "3", onClick = { expression = appendDigit(expression, "3") }, modifier = Modifier.weight(1f))
                    CalculatorButton(
                        text = "=",
                        onClick = {
                            if (evaluationResult is CalculatorEvaluationResult.Success) {
                                expression = evaluationResult.formattedAmount
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 5: 0 (span 2), Decimal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalculatorButton(
                        text = "0",
                        onClick = { expression = appendDigit(expression, "0") },
                        modifier = Modifier.weight(2f)
                    )
                    CalculatorButton(
                        text = ".",
                        onClick = { expression = appendDecimal(expression) },
                        modifier = Modifier.weight(1f)
                    )
                    CalculatorButton(
                        text = "00",
                        onClick = { expression = appendDoubleZero(expression) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            // 3. Action Buttons (Cancel / Use Result)
            val isResultApplicable = evaluationResult is CalculatorEvaluationResult.Success && evaluationResult.isPositive
            val applicableAmount = if (evaluationResult is CalculatorEvaluationResult.Success) evaluationResult.formattedAmount else ""

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
            ) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    shape = ButtonShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text(text = "Cancel", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        if (isResultApplicable) {
                            onUseResult(applicableAmount)
                            onDismissRequest()
                        }
                    },
                    enabled = isResultApplicable,
                    shape = ButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(50.dp)
                ) {
                    Text(
                        text = if (isResultApplicable) "Use Result (${currency.symbol}$applicableAmount)" else "Use Result",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun appendDigit(current: String, digit: String): String {
    return if (current == "0") digit else current + digit
}

private fun appendDoubleZero(current: String): String {
    if (current.isEmpty() || current == "0") return "0"
    if (current.endsWith(" + ") || current.endsWith(" - ") || current.endsWith(" × ") || current.endsWith(" ÷ ")) {
        return current + "0"
    }
    return current + "00"
}

private fun appendDecimal(current: String): String {
    if (current.isEmpty() || current.endsWith(" ")) {
        return current + "0."
    }

    // Check if the current last number segment already contains a decimal point
    val lastSegment = current.substringAfterLast(' ')
    if (!lastSegment.contains('.')) {
        return current + "."
    }
    return current
}

private fun appendOperator(current: String, op: String): String {
    if (current.isEmpty()) {
        return if (op == "-") "-" else ""
    }

    if (current == "-") {
        return if (op == "-") "-" else ""
    }

    if (current.endsWith(" ")) {
        // Replace previous operator
        val trimmed = current.trimEnd()
        val beforeOp = trimmed.substringBeforeLast(' ')
        return if (beforeOp.isEmpty() || beforeOp == trimmed) {
            op + " "
        } else {
            "$beforeOp $op "
        }
    }

    return "$current $op "
}

@Composable
private fun CalculatorButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        shape = ButtonShape,
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        modifier = modifier
            .height(52.dp)
            .clip(ButtonShape)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun CalculatorIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        shape = ButtonShape,
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        modifier = modifier
            .height(52.dp)
            .clip(ButtonShape)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
