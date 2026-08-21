package com.vinaynalavade.expensetracker.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinaynalavade.expensetracker.R
import com.vinaynalavade.expensetracker.core.constants.AppConstants
import com.vinaynalavade.expensetracker.domain.model.ThemeMode
import com.vinaynalavade.expensetracker.presentation.components.AppTopBar
import com.vinaynalavade.expensetracker.presentation.settings.currency.CurrencySelectionDialog
import com.vinaynalavade.expensetracker.presentation.theme.CardShape
import com.vinaynalavade.expensetracker.presentation.theme.PillShape
import com.vinaynalavade.expensetracker.presentation.theme.spacing
import com.vinaynalavade.expensetracker.presentation.widget.ExpenseTrackerWidgetProvider
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToRecurring: () -> Unit = {},
    onNavigateToStatements: () -> Unit = {},
    onNavigateToMonthlySummary: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showOpeningBalanceDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onDailyReminderToggled(true)
        } else {
            viewModel.onDailyReminderToggled(false)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.nav_settings)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg)
        ) {
            // 1. Appearance Section
            SettingsSection(title = "APPEARANCE") {
                Text(
                    text = "Theme Mode",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
                ) {
                    ThemeOptionChip(
                        label = "System",
                        icon = Icons.Default.SettingsBrightness,
                        isSelected = userPreferences.themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.onThemeModeSelected(ThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionChip(
                        label = "Light",
                        icon = Icons.Default.LightMode,
                        isSelected = userPreferences.themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.onThemeModeSelected(ThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionChip(
                        label = "Dark",
                        icon = Icons.Default.DarkMode,
                        isSelected = userPreferences.themeMode == ThemeMode.DARK,
                        onClick = { viewModel.onThemeModeSelected(ThemeMode.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. Financial Preferences Section
            SettingsSection(title = "FINANCIAL PREFERENCES") {
                SettingsNavigationTile(
                    icon = Icons.Default.AccountBalance,
                    title = "Default Currency",
                    subtitle = userPreferences.currency.name,
                    valueBadge = "${userPreferences.currency.symbol} ${userPreferences.currency.code}",
                    onClick = { showCurrencyDialog = true }
                )

                SettingsDivider()

                SettingsNavigationTile(
                    icon = Icons.Default.Savings,
                    title = "Starting Balance",
                    subtitle = "One-time initial carry-forward balance",
                    valueBadge = userPreferences.openingBalance.format(userPreferences.currency),
                    onClick = { showOpeningBalanceDialog = true }
                )
            }

            // 3. Notifications & Reminders Section (Step 5 Integrated)
            SettingsSection(title = "NOTIFICATIONS & REMINDERS") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = MaterialTheme.spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.md))
                        Column {
                            Text(
                                text = stringResource(R.string.settings_daily_reminder_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_daily_reminder_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))

                    Switch(
                        checked = userPreferences.dailyReminderEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.onDailyReminderToggled(true)
                                }
                            } else {
                                viewModel.onDailyReminderToggled(false)
                            }
                        }
                    )
                }

                AnimatedVisibility(
                    visible = userPreferences.dailyReminderEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        SettingsDivider()

                        val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
                        val formattedTime = remember(userPreferences.dailyReminderHour, userPreferences.dailyReminderMinute) {
                            LocalTime.of(userPreferences.dailyReminderHour, userPreferences.dailyReminderMinute).format(timeFormatter)
                        }

                        SettingsNavigationTile(
                            icon = Icons.Default.Schedule,
                            title = stringResource(R.string.settings_reminder_time_title),
                            subtitle = "Time of day to check today's activity",
                            valueBadge = formattedTime,
                            onClick = { showTimePickerDialog = true }
                        )
                    }
                }
            }

            // 4. Categories & Customization Section (Step 6 Integrated)
            SettingsSection(title = "CATEGORIES & CUSTOMIZATION") {
                SettingsNavigationTile(
                    icon = Icons.Default.Category,
                    title = "Manage Categories",
                    subtitle = "Customize expense & income categories, icons, and colors",
                    onClick = onNavigateToCategories
                )
            }

            // 5. Financial Tools & Reports Section
            SettingsSection(title = "FINANCIAL TOOLS & REPORTS") {
                SettingsNavigationTile(
                    icon = Icons.Default.CalendarMonth,
                    title = "Monthly Summary",
                    subtitle = "Opening balance, carry-forward, and category breakdowns",
                    onClick = onNavigateToMonthlySummary
                )
                SettingsDivider()
                SettingsNavigationTile(
                    icon = Icons.Default.Repeat,
                    title = "Recurring & EMI",
                    subtitle = "Automated salary, subscriptions, and EMI due alerts",
                    onClick = onNavigateToRecurring
                )
                SettingsDivider()
                SettingsNavigationTile(
                    icon = Icons.Default.Description,
                    title = "Financial Statements",
                    subtitle = "Generate bank-quality PDF statements with running balance",
                    onClick = onNavigateToStatements
                )
            }

            // 6. Data & Privacy Section
            SettingsSection(title = "DATA & PRIVACY") {
                SettingsNavigationTile(
                    icon = Icons.Default.Backup,
                    title = "Backup & Restore",
                    subtitle = "Export your data and create secure local backups",
                    onClick = onNavigateToBackup
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = MaterialTheme.spacing.xs),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = MaterialTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.md))
                    Column {
                        Text(
                            text = "100% On-Device & Private",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "All financial records and settings are stored locally on your device. No cloud sync, tracking, or ads.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 7. About Section
            SettingsSection(title = "ABOUT") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_kharchaflow_logo),
                        contentDescription = "KharchaFlow Logo",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Column {
                        Text(
                            text = AppConstants.APP_NAME,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${AppConstants.APP_DESCRIPTOR} • Version 1.0.1",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Developed by ${AppConstants.APP_CREATOR}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showCurrencyDialog) {
        CurrencySelectionDialog(
            currentCurrency = userPreferences.currency,
            onCurrencySelected = { newCurrency ->
                viewModel.onCurrencySelected(newCurrency)
                ExpenseTrackerWidgetProvider.updateAll(context)
            },
            onDismiss = { showCurrencyDialog = false }
        )
    }

    if (showOpeningBalanceDialog) {
        OpeningBalanceDialog(
            currentAmount = userPreferences.openingBalance,
            currency = userPreferences.currency,
            onDismiss = { showOpeningBalanceDialog = false },
            onSave = { newSubunits ->
                viewModel.onOpeningBalanceChanged(newSubunits)
                ExpenseTrackerWidgetProvider.updateAll(context)
            }
        )
    }

    if (showTimePickerDialog) {
        ReminderTimePickerDialog(
            initialHour = userPreferences.dailyReminderHour,
            initialMinute = userPreferences.dailyReminderMinute,
            onTimeSelected = { hour, minute ->
                viewModel.onReminderTimeSelected(hour, minute)
            },
            onDismiss = { showTimePickerDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_select_time),
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
                    onTimeSelected(timePickerState.hour, timePickerState.minute)
                    onDismiss()
                }
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = CardShape,
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = CardShape
                ),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsNavigationTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    valueBadge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = MaterialTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (valueBadge != null) {
            Surface(
                shape = PillShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xs)
            ) {
                Text(
                    text = valueBadge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = MaterialTheme.spacing.xs),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    )
}

@Composable
private fun ThemeOptionChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label = "ThemeChipBg"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
        animationSpec = tween(200),
        label = "ThemeChipBorder"
    )

    Surface(
        shape = PillShape,
        color = animatedBg,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = animatedBorderColor
        ),
        modifier = modifier
            .clip(PillShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.sm, vertical = MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
