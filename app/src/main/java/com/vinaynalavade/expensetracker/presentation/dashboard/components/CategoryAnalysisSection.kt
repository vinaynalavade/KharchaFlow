package com.vinaynalavade.expensetracker.presentation.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.domain.model.CategoryAnalysis
import com.vinaynalavade.expensetracker.domain.model.CategoryAnalysisResult
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.presentation.components.CategoryIcon
import com.vinaynalavade.expensetracker.presentation.theme.CardShape
import com.vinaynalavade.expensetracker.presentation.theme.PillShape
import com.vinaynalavade.expensetracker.presentation.theme.spacing
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Premium Category Analysis section on the dashboard with segmented control,
 * donut chart, and interactive category breakdown list.
 */
@Composable
fun CategoryAnalysisSection(
    analysisResult: CategoryAnalysisResult?,
    selectedMonth: YearMonth,
    selectedType: TransactionType,
    currency: Currency,
    onTypeChange: (TransactionType) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCategoryClick: (CategoryAnalysis) -> Unit,
    modifier: Modifier = Modifier
) {
    var highlightedCategoryId by remember { mutableStateOf<Long?>(null) }

    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
    val formattedMonth = selectedMonth.format(monthFormatter)
    val isCurrentMonth = selectedMonth == YearMonth.now()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.lg)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = CardShape
            ),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.lg)
        ) {
            // 1. Header with Month navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                    Text(
                        text = "Category Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Month Switcher (< August 2026 >)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPreviousMonth,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Month",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = formattedMonth,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrentMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Month",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            // 2. Segmented Tab Control [ Expenses | Income ]
            TabRow(
                selectedTabIndex = if (selectedType == TransactionType.EXPENSE) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .clip(PillShape)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = PillShape
                    )
            ) {
                Tab(
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = {
                        highlightedCategoryId = null
                        onTypeChange(TransactionType.EXPENSE)
                    },
                    text = {
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedType == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
                Tab(
                    selected = selectedType == TransactionType.INCOME,
                    onClick = {
                        highlightedCategoryId = null
                        onTypeChange(TransactionType.INCOME)
                    },
                    text = {
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedType == TransactionType.INCOME) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            // 3. Custom Canvas Donut Chart
            val safeResult = analysisResult ?: CategoryAnalysisResult.empty(selectedType)

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CategoryDonutChart(
                    analysisResult = safeResult,
                    currency = currency,
                    selectedCategoryId = highlightedCategoryId,
                    onCategoryClick = { clickedCategory ->
                        highlightedCategoryId = if (highlightedCategoryId == clickedCategory.categoryId) null else clickedCategory.categoryId
                    }
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            // 4. Category Breakdown Rows or Empty State
            if (safeResult.isEmpty) {
                val emptyMessage = if (selectedType == TransactionType.EXPENSE) {
                    "No expenses recorded for $formattedMonth"
                } else {
                    "No income recorded for $formattedMonth"
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = MaterialTheme.spacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    modifier = Modifier.padding(bottom = MaterialTheme.spacing.sm)
                )

                safeResult.categories.forEach { category ->
                    val isHighlighted = category.categoryId == highlightedCategoryId

                    CategoryBreakdownRow(
                        category = category,
                        currency = currency,
                        isHighlighted = isHighlighted,
                        onClick = { onCategoryClick(category) }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownRow(
    category: CategoryAnalysis,
    currency: Currency,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = MaterialTheme.spacing.xs, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(
            iconName = category.categoryIcon,
            colorHex = category.categoryColor,
            size = 32.dp,
            iconSize = 16.dp,
            cornerRadius = 8.dp
        )

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.categoryName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val countText = if (category.transactionCount == 1) "1 transaction" else "${category.transactionCount} transactions"
            Text(
                text = countText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = category.amount.format(currency),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val formattedPercentage = if (category.percentage >= 10f || category.percentage % 1f == 0f) {
                "${Math.round(category.percentage)}%"
            } else {
                String.format(Locale.getDefault(), "%.1f%%", category.percentage)
            }

            Text(
                text = formattedPercentage,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}
