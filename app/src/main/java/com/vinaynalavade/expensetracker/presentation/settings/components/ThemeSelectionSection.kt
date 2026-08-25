package com.vinaynalavade.expensetracker.presentation.settings.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinaynalavade.expensetracker.R
import com.vinaynalavade.expensetracker.domain.model.ThemeMode
import com.vinaynalavade.expensetracker.presentation.theme.CardShape
import com.vinaynalavade.expensetracker.presentation.theme.IncomeEmerald
import com.vinaynalavade.expensetracker.presentation.theme.Indigo400
import com.vinaynalavade.expensetracker.presentation.theme.Indigo600
import com.vinaynalavade.expensetracker.presentation.theme.Motion
import com.vinaynalavade.expensetracker.presentation.theme.PillShape
import com.vinaynalavade.expensetracker.presentation.theme.Slate100
import com.vinaynalavade.expensetracker.presentation.theme.Slate300
import com.vinaynalavade.expensetracker.presentation.theme.Slate600
import com.vinaynalavade.expensetracker.presentation.theme.Slate700
import com.vinaynalavade.expensetracker.presentation.theme.Slate850
import com.vinaynalavade.expensetracker.presentation.theme.Slate900
import com.vinaynalavade.expensetracker.presentation.theme.Slate950
import com.vinaynalavade.expensetracker.presentation.theme.spacing

/**
 * Premium Theme Selection Component with rich visual preview cards for Light, Dark, and System Default themes.
 */
@Composable
fun ThemeSelectionSection(
    currentThemeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
    ) {
        Text(
            text = "THEME & APPEARANCE",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            ThemePreviewCard(
                title = "Light",
                subtitle = "Crisp & bright",
                icon = Icons.Default.LightMode,
                isSelected = currentThemeMode == ThemeMode.LIGHT,
                onClick = { onThemeModeSelected(ThemeMode.LIGHT) },
                previewType = ThemePreviewType.LIGHT,
                modifier = Modifier.weight(1f)
            )

            ThemePreviewCard(
                title = "Dark",
                subtitle = "Deep & elegant",
                icon = Icons.Default.DarkMode,
                isSelected = currentThemeMode == ThemeMode.DARK,
                onClick = { onThemeModeSelected(ThemeMode.DARK) },
                previewType = ThemePreviewType.DARK,
                modifier = Modifier.weight(1f)
            )

            ThemePreviewCard(
                title = "System",
                subtitle = "Follows device",
                icon = Icons.Default.SettingsBrightness,
                isSelected = currentThemeMode == ThemeMode.SYSTEM,
                onClick = { onThemeModeSelected(ThemeMode.SYSTEM) },
                previewType = ThemePreviewType.SYSTEM,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private enum class ThemePreviewType {
    LIGHT,
    DARK,
    SYSTEM
}

@Composable
private fun ThemePreviewCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    previewType: ThemePreviewType,
    modifier: Modifier = Modifier
) {
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        animationSpec = tween(Motion.DurationFast),
        label = "BorderColor"
    )

    val animatedCardBg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(Motion.DurationFast),
        label = "CardBg"
    )

    val animatedElevation by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 0.dp,
        animationSpec = tween(Motion.DurationFast),
        label = "Elevation"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = animatedCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = animatedBorderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .semantics {
                selected = isSelected
                role = Role.RadioButton
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Miniature UI preview mockup
            ThemeMiniatureMockup(
                previewType = previewType,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            // Header with Icon & Checkmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "Selected" else "Unselected",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Subtitle Description
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Miniature visual UI representation showing light, dark, or system-split theme aesthetics.
 */
@Composable
private fun ThemeMiniatureMockup(
    previewType: ThemePreviewType,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(0.5.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
    ) {
        when (previewType) {
            ThemePreviewType.LIGHT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF1F5F9))
                        .padding(5.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // Mini Hero Card
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White,
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 24.dp, height = 4.dp)
                                        .background(Indigo600, RoundedCornerShape(2.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(IncomeEmerald, CircleShape)
                                )
                            }
                        }

                        // Mini Sub-item 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(14.dp)
                                    .background(Color.White, RoundedCornerShape(3.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(14.dp)
                                    .background(Color.White, RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
            ThemePreviewType.DARK -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Slate950)
                        .padding(5.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // Mini Dark Hero Card
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Slate900,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .border(0.5.dp, Slate700, RoundedCornerShape(4.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 24.dp, height = 4.dp)
                                        .background(Indigo400, RoundedCornerShape(2.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(IncomeEmerald, CircleShape)
                                )
                            }
                        }

                        // Mini Dark Sub-items
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(14.dp)
                                    .background(Slate850, RoundedCornerShape(3.dp))
                                    .border(0.5.dp, Slate700.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(14.dp)
                                    .background(Slate850, RoundedCornerShape(3.dp))
                                    .border(0.5.dp, Slate700.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
            ThemePreviewType.SYSTEM -> {
                // Split-half visual representation
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left half (Light)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(Color(0xFFF1F5F9))
                            .padding(4.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .background(Color.White, RoundedCornerShape(3.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(12.dp)
                                    .background(Indigo600.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                            )
                        }
                    }

                    // Right half (Dark)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(Slate950)
                            .padding(4.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .background(Slate900, RoundedCornerShape(3.dp))
                                    .border(0.5.dp, Slate700, RoundedCornerShape(3.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(12.dp)
                                    .background(Indigo400.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}
