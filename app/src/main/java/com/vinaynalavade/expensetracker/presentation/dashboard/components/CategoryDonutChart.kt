package com.vinaynalavade.expensetracker.presentation.dashboard.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.domain.model.CategoryAnalysis
import com.vinaynalavade.expensetracker.domain.model.CategoryAnalysisResult
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Premium, custom Canvas-based interactive Donut Chart for category analysis.
 */
@Composable
fun CategoryDonutChart(
    analysisResult: CategoryAnalysisResult,
    currency: Currency,
    modifier: Modifier = Modifier,
    chartSize: Dp = 220.dp,
    strokeWidth: Dp = 24.dp,
    selectedCategoryId: Long? = null,
    onCategoryClick: ((CategoryAnalysis) -> Unit)? = null
) {
    val animationProgress = remember(analysisResult) { Animatable(0f) }

    LaunchedEffect(analysisResult) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    val emptyColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val defaultCategoryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(chartSize)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(analysisResult, onCategoryClick) {
                    if (onCategoryClick != null && !analysisResult.isEmpty) {
                        detectTapGestures { offset ->
                            val canvasWidth = size.width.toFloat()
                            val canvasHeight = size.height.toFloat()
                            val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                            val chartStrokePx = strokeWidth.toPx()
                            val chartRadius = (minOf(canvasWidth, canvasHeight) - chartStrokePx) / 2f
                            val innerRadius = chartRadius - (chartStrokePx / 1.5f)
                            val outerRadius = chartRadius + (chartStrokePx / 1.5f)

                            if (distance in innerRadius..outerRadius) {
                                var touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (touchAngle < -90f) {
                                    touchAngle += 450f
                                } else {
                                    touchAngle += 90f
                                }

                                var currentAngle = 0f
                                val hasMultiple = analysisResult.categories.size > 1
                                val gap = if (hasMultiple) 2.5f else 0f
                                val totalGaps = if (hasMultiple) gap * analysisResult.categories.size else 0f
                                val availableSweep = 360f - totalGaps

                                for (item in analysisResult.categories) {
                                    val sweep = (item.percentage / 100f) * availableSweep
                                    if (touchAngle >= currentAngle && touchAngle <= currentAngle + sweep + gap) {
                                        onCategoryClick(item)
                                        break
                                    }
                                    currentAngle += sweep + gap
                                }
                            }
                        }
                    }
                }
        ) {
            val strokePx = strokeWidth.toPx()
            val canvasSize = minOf(this.size.width, this.size.height)
            val chartRadius = (canvasSize - strokePx) / 2f
            val topLeft = Offset((this.size.width - 2 * chartRadius) / 2f, (this.size.height - 2 * chartRadius) / 2f)
            val arcSize = Size(chartRadius * 2, chartRadius * 2)

            if (analysisResult.isEmpty) {
                // Draw subtle placeholder ring
                drawArc(
                    color = emptyColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            } else {
                val categories = analysisResult.categories
                val hasMultiple = categories.size > 1
                val gapAngle = if (hasMultiple) 2.5f else 0f
                val totalGaps = if (hasMultiple) gapAngle * categories.size else 0f
                val availableSweep = 360f - totalGaps

                var currentStartAngle = -90f

                categories.forEach { item ->
                    val segmentSweep = (item.percentage / 100f) * availableSweep * animationProgress.value
                    val isSelected = item.categoryId == selectedCategoryId
                    val segmentColor = parseColorSafely(item.categoryColor, defaultCategoryColor)

                    val segmentStroke = if (isSelected) {
                        Stroke(width = strokePx * 1.25f, cap = StrokeCap.Round)
                    } else {
                        Stroke(width = strokePx, cap = StrokeCap.Round)
                    }

                    if (segmentSweep > 0.5f) {
                        drawArc(
                            color = segmentColor,
                            startAngle = currentStartAngle,
                            sweepAngle = segmentSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = segmentStroke
                        )
                    }

                    currentStartAngle += ((item.percentage / 100f) * availableSweep + gapAngle) * animationProgress.value
                }
            }
        }

        // Center Content: Total amount and contextual label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            val totalText = if (analysisResult.isEmpty) {
                "₹0.00"
            } else {
                analysisResult.totalAmount.format(currency)
            }

            val labelText = if (analysisResult.type == TransactionType.INCOME) {
                "Total Income"
            } else {
                "Total Expenses"
            }

            Text(
                text = totalText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = labelText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun parseColorSafely(colorHex: String?, fallback: Color): Color {
    if (colorHex.isNullOrBlank()) return fallback
    return try {
        val cleanHex = if (colorHex.startsWith("#")) colorHex else "#$colorHex"
        Color(android.graphics.Color.parseColor(cleanHex))
    } catch (_: Exception) {
        fallback
    }
}
