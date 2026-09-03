package com.vinaynalavade.expensetracker.presentation.entry.components

import android.text.format.DateFormat
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import com.vinaynalavade.expensetracker.presentation.theme.CardShape
import com.vinaynalavade.expensetracker.presentation.theme.PillShape
import com.vinaynalavade.expensetracker.presentation.theme.spacing
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Intuitive date & time selector with quick chips ("Today" / "Yesterday" / "Now")
 * and full Material 3 Date and Time Picker dialogs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDateSelector(
    selectedDateEpoch: Long,
    onDateSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = MaterialTheme.spacing.lg
) {
    val context = LocalContext.current
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    val formattedSelectedDate = DateTimeUtils.formatDate(selectedDateEpoch)
    val formattedSelectedTime = DateTimeUtils.formatTime(selectedDateEpoch, context)
    val isToday = formattedSelectedDate == "Today"
    val isYesterday = formattedSelectedDate == "Yesterday"

    val currentLocalDateTime = DateTimeUtils.epochToLocalDateTime(selectedDateEpoch)
    val now = LocalTime.now()
    val isNow = isToday && currentLocalDateTime.hour == now.hour && currentLocalDateTime.minute == now.minute

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
    ) {
        // --- 1. DATE SECTION ---
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
            // Quick Today Chip (preserves selected time)
            QuickDateChip(
                label = "Today",
                isSelected = isToday,
                onClick = {
                    val preservedTime = currentLocalDateTime.toLocalTime()
                    val todayEpoch = LocalDate.now()
                        .atTime(preservedTime)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    onDateSelect(todayEpoch)
                }
            )

            // Quick Yesterday Chip (preserves selected time)
            QuickDateChip(
                label = "Yesterday",
                isSelected = isYesterday,
                onClick = {
                    val preservedTime = currentLocalDateTime.toLocalTime()
                    val yesterdayEpoch = LocalDate.now().minusDays(1)
                        .atTime(preservedTime)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
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

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

        // --- 2. TIME SECTION ---
        Text(
            text = "TIME",
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
            // Quick Now Chip (sets current time with seconds/millis reset to 0, preserves selected date)
            QuickTimeChip(
                label = "Now",
                isSelected = isNow,
                onClick = {
                    val currentLocalDate = currentLocalDateTime.toLocalDate()
                    val currentTime = LocalTime.now()
                    val newTime = LocalTime.of(currentTime.hour, currentTime.minute, 0, 0)
                    val newEpoch = currentLocalDate
                        .atTime(newTime)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    onDateSelect(newEpoch)
                }
            )

            // Custom Time Picker Chip
            val isTimeCustomSelected = !isNow

            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(
                        if (isTimeCustomSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    )
                    .border(
                        width = if (isTimeCustomSelected) 1.5.dp else 1.dp,
                        color = if (isTimeCustomSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = PillShape
                    )
                    .clickable { showTimePickerDialog = true }
                    .padding(
                        horizontal = MaterialTheme.spacing.md,
                        vertical = MaterialTheme.spacing.sm
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (isTimeCustomSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                Text(
                    text = formattedSelectedTime,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isTimeCustomSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isTimeCustomSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    // --- Date Picker Dialog ---
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
                            // Convert UTC date to local date time while preserving the selected time
                            val pickedLocalDate = Instant.ofEpochMilli(pickedUtcMillis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            val preservedTime = DateTimeUtils.epochToLocalDateTime(selectedDateEpoch).toLocalTime()
                            val localEpoch = pickedLocalDate
                                .atTime(preservedTime)
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                            onDateSelect(localEpoch)
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
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

    // --- Time Picker Dialog ---
    if (showTimePickerDialog) {
        val is24Hour = DateFormat.is24HourFormat(context)
        val timePickerState = rememberTimePickerState(
            initialHour = currentLocalDateTime.hour,
            initialMinute = currentLocalDateTime.minute,
            is24Hour = is24Hour
        )

        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = {
                Text(
                    text = "Select Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val currentLocalDate = DateTimeUtils.epochToLocalDate(selectedDateEpoch)
                        val newTime = LocalTime.of(timePickerState.hour, timePickerState.minute, 0, 0)
                        val newEpoch = currentLocalDate
                            .atTime(newTime)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                        onDateSelect(newEpoch)
                        showTimePickerDialog = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = CardShape,
            containerColor = MaterialTheme.colorScheme.surface
        )
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

@Composable
private fun QuickTimeChip(
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
            imageVector = Icons.Default.AccessTime,
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
