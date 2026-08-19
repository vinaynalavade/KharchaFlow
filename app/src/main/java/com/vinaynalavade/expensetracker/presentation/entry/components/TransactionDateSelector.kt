package com.vinaynalavade.expensetracker.presentation.entry.components

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import com.vinaynalavade.expensetracker.presentation.theme.PillShape
import com.vinaynalavade.expensetracker.presentation.theme.spacing
import java.time.LocalDate

/**
 * Intuitive date selector with quick "Today" / "Yesterday" chips and a full Material 3 Date Picker dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDateSelector(
    selectedDateEpoch: Long,
    onDateSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = MaterialTheme.spacing.lg
) {
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val formattedSelectedDate = DateTimeUtils.formatDate(selectedDateEpoch)
    val isToday = formattedSelectedDate == "Today"
    val isYesterday = formattedSelectedDate == "Yesterday"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
    ) {
        Text(
            text = "DATE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quick Today Chip
            QuickDateChip(
                label = "Today",
                isSelected = isToday,
                onClick = {
                    onDateSelect(System.currentTimeMillis())
                }
            )

            // Quick Yesterday Chip
            QuickDateChip(
                label = "Yesterday",
                isSelected = isYesterday,
                onClick = {
                    val yesterdayEpoch = DateTimeUtils.getStartOfDayEpoch(LocalDate.now().minusDays(1))
                    onDateSelect(yesterdayEpoch)
                }
            )

            // Custom Date Picker Chip
            val customLabel = if (!isToday && !isYesterday) formattedSelectedDate else "Other date"
            val isCustomSelected = !isToday && !isYesterday

            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(
                        if (isCustomSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    )
                    .border(
                        width = if (isCustomSelected) 1.5.dp else 1.dp,
                        color = if (isCustomSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = PillShape
                    )
                    .clickable { showDatePickerDialog = true }
                    .padding(
                        horizontal = MaterialTheme.spacing.md,
                        vertical = MaterialTheme.spacing.sm
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = if (isCustomSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                Text(
                    text = customLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isCustomSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateEpoch,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // Allow today and past dates across all global timezones
                    val tomorrowBuffer = System.currentTimeMillis() + 86_400_000L
                    return utcTimeMillis <= tomorrowBuffer
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { pickedUtcMillis ->
                            // Convert UTC date to local date time to prevent timezone shift
                            val pickedLocalDate = java.time.Instant.ofEpochMilli(pickedUtcMillis)
                                .atZone(java.time.ZoneId.of("UTC"))
                                .toLocalDate()
                            val localEpoch = pickedLocalDate
                                .atTime(java.time.LocalTime.now())
                                .atZone(java.time.ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                            onDateSelect(localEpoch)
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun QuickDateChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = PillShape
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = MaterialTheme.spacing.md,
                vertical = MaterialTheme.spacing.sm
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    }
}
