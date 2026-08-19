package com.vinaynalavade.expensetracker.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinaynalavade.expensetracker.R
import com.vinaynalavade.expensetracker.core.constants.AppConstants
import com.vinaynalavade.expensetracker.core.notification.NotificationHelper
import com.vinaynalavade.expensetracker.domain.model.ThemeMode
import com.vinaynalavade.expensetracker.presentation.components.AppTopBar
import com.vinaynalavade.expensetracker.presentation.settings.currency.CurrencySelectionDialog
import com.vinaynalavade.expensetracker.presentation.theme.CardShape
import com.vinaynalavade.expensetracker.presentation.theme.PillShape
import com.vinaynalavade.expensetracker.presentation.theme.spacing

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToRecurring: () -> Unit = {},
    onNavigateToStatements: () -> Unit = {},
    onNavigateToMonthlySummary: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showOpeningBalanceDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onDailyReminderToggled(true)
            NotificationHelper.scheduleDailyReminder(context, 21, 0)
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
            SettingsGroup(title = "APPEARANCE") {
                Text(
                    text = "Theme Mode",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
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

            // 2. Financial Configuration (Currency & Opening Balance)
            SettingsGroup(title = "FINANCIAL CONFIGURATION") {
                // Currency Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCurrencyDialog = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Default Currency", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${userPreferences.currency.name} (${userPreferences.currency.symbol})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showCurrencyDialog = true }) {
                        Text("Change", fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.sm), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Starting Balance Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showOpeningBalanceDialog = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Starting Balance", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "One-time initial balance: ${userPreferences.openingBalance.format(userPreferences.currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showOpeningBalanceDialog = true }) {
                        Text("Edit", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 3. Reminders & Alerts Section
            SettingsGroup(title = "NOTIFICATIONS & REMINDERS") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Daily Evening Reminder", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Notifies at 9:00 PM only if no transactions were recorded today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                                    NotificationHelper.scheduleDailyReminder(context, 21, 0)
                                }
                            } else {
                                viewModel.onDailyReminderToggled(false)
                                NotificationHelper.cancelDailyReminder(context)
                            }
                        }
                    )
                }
            }

            // 4. Tools & Reports Navigation Section
            SettingsGroup(title = "FINANCIAL TOOLS & REPORTS") {
                NavigationTile(
                    icon = Icons.Default.CalendarMonth,
                    title = "Monthly Summary",
                    subtitle = "Opening balance, carry-forward, and category breakdowns",
                    onClick = onNavigateToMonthlySummary
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.xs), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                NavigationTile(
                    icon = Icons.Default.Repeat,
                    title = "Recurring & EMI",
                    subtitle = "Automated salary, subscriptions, and EMI due alerts",
                    onClick = onNavigateToRecurring
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.xs), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                NavigationTile(
                    icon = Icons.Default.Description,
                    title = "Financial Statements",
                    subtitle = "Generate bank-quality PDF statements with running balance",
                    onClick = onNavigateToStatements
                )
            }



            // 6. About Section
            SettingsGroup(title = "ABOUT") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_kharchaflow_logo),
                        contentDescription = "KharchaFlow Logo",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Column {
                        Text(text = AppConstants.APP_NAME, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "${AppConstants.APP_DESCRIPTOR} • Version 1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Created by ${AppConstants.APP_CREATOR}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showCurrencyDialog) {
        CurrencySelectionDialog(
            currentCurrency = userPreferences.currency,
            onCurrencySelected = { newCurrency -> viewModel.onCurrencySelected(newCurrency) },
            onDismiss = { showCurrencyDialog = false }
        )
    }

    if (showOpeningBalanceDialog) {
        OpeningBalanceDialog(
            currentAmount = userPreferences.openingBalance,
            currency = userPreferences.currency,
            onDismiss = { showOpeningBalanceDialog = false },
            onSave = { newSubunits -> viewModel.onOpeningBalanceChanged(newSubunits) }
        )
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
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
private fun NavigationTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = MaterialTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun ThemeOptionChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = PillShape,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = modifier
            .clip(PillShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.sm, vertical = MaterialTheme.spacing.xs),
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
