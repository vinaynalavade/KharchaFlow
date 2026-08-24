package com.vinaynalavade.expensetracker.presentation.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinaynalavade.expensetracker.BuildConfig
import com.vinaynalavade.expensetracker.R
import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.core.security.BiometricAuthHelper
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import com.vinaynalavade.expensetracker.domain.model.GoogleAccountInfo
import com.vinaynalavade.expensetracker.domain.model.GoogleBackupState
import com.vinaynalavade.expensetracker.domain.model.RecurringReminderAdvance
import com.vinaynalavade.expensetracker.domain.model.ThemeMode
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import com.vinaynalavade.expensetracker.presentation.backup.components.ReplaceDataConfirmationDialog
import com.vinaynalavade.expensetracker.presentation.backup.components.RestorePromptDialog
import com.vinaynalavade.expensetracker.presentation.components.AppTopBar
import com.vinaynalavade.expensetracker.presentation.settings.components.EditProfileDialog
import com.vinaynalavade.expensetracker.presentation.settings.components.ImageCropDialog
import com.vinaynalavade.expensetracker.presentation.settings.components.ProfileCard
import com.vinaynalavade.expensetracker.presentation.settings.components.ProfilePhotoOptionsDialog
import com.vinaynalavade.expensetracker.presentation.theme.ButtonShape
import com.vinaynalavade.expensetracker.presentation.theme.CardShape
import com.vinaynalavade.expensetracker.presentation.theme.PillShape
import com.vinaynalavade.expensetracker.presentation.theme.spacing
import com.vinaynalavade.expensetracker.presentation.widget.WidgetUpdateManager
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToRecurring: () -> Unit = {},
    onNavigateToStatements: () -> Unit = {},
    onNavigateToMonthlySummary: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToAppLockSetup: () -> Unit = {},
    onNavigateToChangePin: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val googleBackupState by viewModel.googleBackupState.collectAsStateWithLifecycle()
    val accountActionState by viewModel.accountActionState.collectAsStateWithLifecycle()
    val restorePromptEligibility by viewModel.restorePromptEligibility.collectAsStateWithLifecycle()
    val showReplaceConfirmation by viewModel.showReplaceConfirmation.collectAsStateWithLifecycle()
    val isManualBackupRunning by viewModel.isManualBackupRunning.collectAsStateWithLifecycle()

    val isBiometricAvailable = remember(context) { BiometricAuthHelper.isBiometricAvailable(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showOpeningBalanceDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showBudgetLimitDialog by remember { mutableStateOf(false) }
    var showRecurringAdvanceDialog by remember { mutableStateOf(false) }
    var showAutoLockDialog by remember { mutableStateOf(false) }
    var showUnlockMethodDialog by remember { mutableStateOf(false) }
    var showDisableAppLockDialog by remember { mutableStateOf(false) }
    var showDisconnectGoogleDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showProfilePhotoOptionsDialog by remember { mutableStateOf(false) }
    var pendingCropImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pendingCropImageUri = uri
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onNotificationsMasterToggled(true)
        } else {
            viewModel.onNotificationsMasterToggled(false)
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onGoogleSignInResult(result.data)
    }

    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onConsentResult(result.resultCode)
    }

    LaunchedEffect(accountActionState) {
        when (val state = accountActionState) {
            is AccountActionState.ConsentRequired -> {
                consentLauncher.launch(state.consentIntent)
            }
            is AccountActionState.Message -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearAccountActionMessage()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.nav_settings)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
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
            // 1. Profile Section
            SettingsSection(title = stringResource(R.string.settings_section_profile)) {
                ProfileCard(
                    userPreferences = userPreferences,
                    googleBackupState = googleBackupState,
                    onAvatarClick = { showProfilePhotoOptionsDialog = true },
                    onEditNameClick = { showEditProfileDialog = true },
                    onConnectGoogleClick = { googleSignInLauncher.launch(viewModel.getGoogleSignInIntent()) },
                    onDisconnectGoogleClick = { showDisconnectGoogleDialog = true }
                )
            }

            // 2. Personalization Section
            SettingsSection(title = "PERSONALIZATION") {
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
                    ThemeOptionChip(
                        label = "System",
                        icon = Icons.Default.SettingsBrightness,
                        isSelected = userPreferences.themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.onThemeModeSelected(ThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f)
                    )
                }

                SettingsDivider()

                SettingsNavigationTile(
                    icon = Icons.Default.AccountBalance,
                    title = "Default Currency",
                    subtitle = userPreferences.currency.name,
                    valueBadge = "${userPreferences.currency.symbol} ${userPreferences.currency.code}",
                    onClick = { showCurrencyDialog = true }
                )

                SettingsDivider()

                SettingsNavigationTile(
                    icon = Icons.Default.AccountBalance,
                    title = "Starting Balance",
                    subtitle = "Opening balance for net worth calculation",
                    valueBadge = userPreferences.openingBalance.format(userPreferences.currency),
                    onClick = { showOpeningBalanceDialog = true }
                )

                SettingsDivider()

                val currentLanguage = AppLanguage.fromCode(userPreferences.appLanguage)
                SettingsNavigationTile(
                    icon = Icons.Default.Translate,
                    title = stringResource(R.string.settings_language),
                    subtitle = stringResource(R.string.settings_language_desc),
                    valueBadge = currentLanguage.nativeName,
                    onClick = { showLanguageDialog = true }
                )
            }

            // 3. Categories Management Section (Moved from Bottom Navigation)
            SettingsSection(title = "CATEGORIES") {
                SettingsNavigationTile(
                    icon = Icons.Default.Category,
                    title = "Manage Categories",
                    subtitle = "Customize expense and income categories, icons, and colors",
                    onClick = onNavigateToCategories
                )
            }

            // 4. Security & Privacy Section
            SettingsSection(title = "SECURITY & PRIVACY") {
                SettingsSwitchTile(
                    icon = Icons.Default.Lock,
                    title = stringResource(R.string.settings_app_lock_title),
                    subtitle = stringResource(R.string.settings_app_lock_desc),
                    checked = userPreferences.appLockEnabled,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            onNavigateToAppLockSetup()
                        } else {
                            showDisableAppLockDialog = true
                        }
                    }
                )

                AnimatedVisibility(
                    visible = userPreferences.appLockEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        SettingsDivider()

                        if (isBiometricAvailable) {
                            SettingsNavigationTile(
                                icon = Icons.Default.Fingerprint,
                                title = stringResource(R.string.settings_unlock_method_title),
                                subtitle = "Choose between Biometric or PIN authentication",
                                valueBadge = if (userPreferences.biometricEnabled) "Biometric + PIN" else "PIN Only",
                                onClick = { showUnlockMethodDialog = true }
                            )
                            SettingsDivider()
                        }

                        val autoLockBadge = when (userPreferences.autoLockDurationSeconds) {
                            0L -> "Immediately"
                            30L -> "After 30 seconds"
                            60L -> "After 1 minute"
                            300L -> "After 5 minutes"
                            else -> "${userPreferences.autoLockDurationSeconds}s"
                        }

                        SettingsNavigationTile(
                            icon = Icons.Default.Timer,
                            title = stringResource(R.string.settings_auto_lock_title),
                            subtitle = "How quickly the app locks when minimized",
                            valueBadge = autoLockBadge,
                            onClick = { showAutoLockDialog = true }
                        )

                        SettingsDivider()

                        SettingsNavigationTile(
                            icon = Icons.Default.LockReset,
                            title = stringResource(R.string.settings_change_pin),
                            subtitle = "Update your 4-digit security PIN",
                            onClick = onNavigateToChangePin
                        )

                        SettingsDivider()

                        SettingsSwitchTile(
                            icon = Icons.Default.VisibilityOff,
                            title = stringResource(R.string.settings_hide_recents_title),
                            subtitle = stringResource(R.string.settings_hide_recents_desc),
                            checked = userPreferences.hideContentInRecents,
                            onCheckedChange = { viewModel.onHideContentInRecentsToggled(it) }
                        )
                    }
                }
            }

            // 5. Notifications & Reminders Section
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
                                text = stringResource(R.string.settings_notifications_master_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_notifications_master_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))

                    Switch(
                        checked = userPreferences.notificationsMasterEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.onNotificationsMasterToggled(true)
                                }
                            } else {
                                viewModel.onNotificationsMasterToggled(false)
                            }
                        }
                    )
                }

                AnimatedVisibility(
                    visible = userPreferences.notificationsMasterEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        SettingsDivider()

                        // Daily Reminder
                        SettingsSwitchTile(
                            icon = Icons.Default.Schedule,
                            title = stringResource(R.string.settings_daily_reminder_title),
                            subtitle = stringResource(R.string.settings_daily_reminder_desc),
                            checked = userPreferences.dailyReminderEnabled,
                            onCheckedChange = { viewModel.onDailyReminderToggled(it) }
                        )

                        if (userPreferences.dailyReminderEnabled) {
                            val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
                            val formattedTime = remember(userPreferences.dailyReminderHour, userPreferences.dailyReminderMinute) {
                                LocalTime.of(userPreferences.dailyReminderHour, userPreferences.dailyReminderMinute).format(timeFormatter)
                            }

                            SettingsNavigationTile(
                                icon = Icons.Default.Schedule,
                                title = stringResource(R.string.settings_reminder_time_title),
                                subtitle = "Daily reminder time",
                                valueBadge = formattedTime,
                                onClick = { showTimePickerDialog = true }
                            )
                        }

                        SettingsDivider()

                        // Budget Alerts
                        SettingsSwitchTile(
                            icon = Icons.Default.PieChart,
                            title = stringResource(R.string.settings_budget_alerts_title),
                            subtitle = stringResource(R.string.settings_budget_alerts_desc),
                            checked = userPreferences.budgetAlertsEnabled,
                            onCheckedChange = { viewModel.onBudgetAlertsToggled(it) }
                        )

                        if (userPreferences.budgetAlertsEnabled) {
                            val budgetLimitBadge = if (userPreferences.monthlyBudgetLimitSubunits > 0L) {
                                userPreferences.monthlyBudgetLimit.format(userPreferences.currency)
                            } else {
                                "Not Set"
                            }

                            SettingsNavigationTile(
                                icon = Icons.Default.PieChart,
                                title = stringResource(R.string.settings_budget_limit_title),
                                subtitle = "Monthly spending target threshold",
                                valueBadge = budgetLimitBadge,
                                onClick = { showBudgetLimitDialog = true }
                            )
                        }

                        SettingsDivider()

                        // Bill & Payment Reminders
                        SettingsSwitchTile(
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            title = stringResource(R.string.settings_recurring_reminders_title),
                            subtitle = stringResource(R.string.settings_recurring_reminders_desc),
                            checked = userPreferences.recurringRemindersEnabled,
                            onCheckedChange = { viewModel.onRecurringRemindersToggled(it) }
                        )

                        if (userPreferences.recurringRemindersEnabled) {
                            val advanceLabel = RecurringReminderAdvance.fromDays(userPreferences.recurringReminderAdvanceDays).label

                            SettingsNavigationTile(
                                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                title = stringResource(R.string.settings_recurring_advance_title),
                                subtitle = "When to receive payment due alerts",
                                valueBadge = advanceLabel,
                                onClick = { showRecurringAdvanceDialog = true }
                            )
                        }

                        SettingsDivider()

                        // Savings Goal Milestones
                        SettingsSwitchTile(
                            icon = Icons.Default.Flag,
                            title = stringResource(R.string.settings_savings_goals_title),
                            subtitle = stringResource(R.string.settings_savings_goals_desc),
                            checked = userPreferences.savingsGoalNotificationsEnabled,
                            onCheckedChange = { viewModel.onSavingsGoalNotificationsToggled(it) }
                        )
                    }
                }
            }

            // 6. Data & Backup Section
            SettingsSection(title = "DATA & BACKUP") {
                // Automatic Backup Toggle
                SettingsSwitchTile(
                    icon = Icons.Default.CloudSync,
                    title = stringResource(R.string.settings_auto_backup_title),
                    subtitle = stringResource(R.string.settings_auto_backup_desc),
                    checked = userPreferences.automaticBackupEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && googleBackupState !is GoogleBackupState.Connected) {
                            googleSignInLauncher.launch(viewModel.getGoogleSignInIntent())
                        } else {
                            viewModel.onAutomaticBackupToggled(enabled)
                        }
                    }
                )

                // Backup Status & Manual Action
                val lastTimestamp = (googleBackupState as? GoogleBackupState.Connected)?.lastBackupTimestamp
                    ?: userPreferences.lastDismissedRestoreBackupTimestamp
                val statusText = when {
                    userPreferences.lastBackupStatus == "FAILED" -> {
                        stringResource(R.string.backup_status_failed)
                    }
                    lastTimestamp != null && lastTimestamp > 0L -> {
                        val instant = java.time.Instant.ofEpochMilli(lastTimestamp)
                        val formatted = java.time.format.DateTimeFormatter.ofLocalizedDateTime(
                            java.time.format.FormatStyle.MEDIUM,
                            java.time.format.FormatStyle.SHORT
                        ).format(instant.atZone(java.time.ZoneId.systemDefault()))
                        stringResource(R.string.backup_status_last_success, formatted)
                    }
                    else -> stringResource(R.string.backup_status_no_backup)
                }

                SettingsNavigationTile(
                    icon = Icons.Default.CloudUpload,
                    title = stringResource(R.string.backup_btn_now),
                    subtitle = statusText,
                    valueBadge = if (isManualBackupRunning) "Backing up..." else null,
                    onClick = {
                        if (googleBackupState !is GoogleBackupState.Connected) {
                            googleSignInLauncher.launch(viewModel.getGoogleSignInIntent())
                        } else {
                            viewModel.onBackupNowClick()
                        }
                    }
                )

                SettingsDivider()

                SettingsNavigationTile(
                    icon = Icons.Default.Sync,
                    title = "Backup & Restore",
                    subtitle = "Google Drive cloud backup and local file export/import",
                    onClick = onNavigateToBackup
                )

                SettingsDivider()

                SettingsNavigationTile(
                    icon = Icons.Default.Description,
                    title = "Export & Reports",
                    subtitle = "Export transactions to CSV or generate financial statements",
                    onClick = onNavigateToStatements
                )

                SettingsDivider()

                SettingsNavigationTile(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "Recurring & EMIs",
                    subtitle = "Manage recurring payments, subscriptions, and reminders",
                    onClick = onNavigateToRecurring
                )
            }

            // 7. About Section
            SettingsSection(title = "ABOUT") {
                SettingsNavigationTile(
                    icon = Icons.Default.Info,
                    title = "About KharchaFlow",
                    subtitle = "Version ${BuildConfig.VERSION_NAME}, privacy & security, open source",
                    valueBadge = "v${BuildConfig.VERSION_NAME}",
                    onClick = onNavigateToAbout
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
        }
    }

    // --- Dialogs ---

    if (showEditProfileDialog) {
        val currentName = userPreferences.userName ?: (googleBackupState as? GoogleBackupState.Connected)?.account?.displayName
        EditProfileDialog(
            currentName = currentName,
            onSave = { newName ->
                viewModel.onProfileNameChanged(newName)
            },
            onDismiss = { showEditProfileDialog = false }
        )
    }

    if (showProfilePhotoOptionsDialog) {
        ProfilePhotoOptionsDialog(
            hasCustomPhoto = !userPreferences.profileImageUri.isNullOrBlank(),
            onChoosePhoto = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRemovePhoto = {
                viewModel.onRemoveProfileImage()
            },
            onDismiss = { showProfilePhotoOptionsDialog = false }
        )
    }

    if (showCurrencyDialog) {
        CurrencySelectionDialog(
            selectedCurrency = userPreferences.currency,
            onCurrencySelected = { currency ->
                viewModel.onCurrencySelected(currency)
                WidgetUpdateManager.refreshAllWidgets(context)
                showCurrencyDialog = false
            },
            onDismiss = { showCurrencyDialog = false }
        )
    }

    if (showOpeningBalanceDialog) {
        OpeningBalanceDialog(
            initialSubunits = userPreferences.openingBalanceSubunits,
            currency = userPreferences.currency,
            onDismiss = { showOpeningBalanceDialog = false },
            onSave = { newSubunits ->
                viewModel.onOpeningBalanceChanged(newSubunits)
                WidgetUpdateManager.refreshAllWidgets(context)
            }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguageCode = userPreferences.appLanguage,
            onLanguageSelected = { languageCode ->
                viewModel.onLanguageSelected(languageCode)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
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

    if (showBudgetLimitDialog) {
        MonthlyBudgetLimitDialog(
            currentLimit = userPreferences.monthlyBudgetLimit,
            currency = userPreferences.currency,
            onSave = { newLimitSubunits ->
                viewModel.onMonthlyBudgetLimitChanged(newLimitSubunits)
            },
            onDismiss = { showBudgetLimitDialog = false }
        )
    }

    if (showRecurringAdvanceDialog) {
        RecurringAdvanceSelectionDialog(
            currentAdvanceDays = userPreferences.recurringReminderAdvanceDays,
            onDaysSelected = { days ->
                viewModel.onRecurringReminderAdvanceDaysSelected(days)
            },
            onDismiss = { showRecurringAdvanceDialog = false }
        )
    }

    if (showAutoLockDialog) {
        AutoLockSelectionDialog(
            currentDurationSeconds = userPreferences.autoLockDurationSeconds,
            onDurationSelected = { seconds ->
                viewModel.onAutoLockDurationSelected(seconds)
                showAutoLockDialog = false
            },
            onDismiss = { showAutoLockDialog = false }
        )
    }

    if (showUnlockMethodDialog) {
        UnlockMethodDialog(
            isBiometricEnabled = userPreferences.biometricEnabled,
            onMethodSelected = { enableBiometric ->
                viewModel.onBiometricToggled(enableBiometric)
                showUnlockMethodDialog = false
            },
            onDismiss = { showUnlockMethodDialog = false }
        )
    }

    if (showDisableAppLockDialog) {
        val disabledMessage = stringResource(R.string.app_lock_disabled_success)
        val coroutineScope = rememberCoroutineScope()
        DisableAppLockVerificationDialog(
            onDismiss = { showDisableAppLockDialog = false },
            onVerifyAndDisable = { pin, onSuccess, onError ->
                viewModel.verifyAndDisableAppLock(pin, onSuccess, onError)
            },
            getLockoutSeconds = { viewModel.getLockoutSecondsRemaining() },
            onDisabledSuccess = {
                showDisableAppLockDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(disabledMessage)
                }
            }
        )
    }

    if (showDisconnectGoogleDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectGoogleDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Disconnect Google Account?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Your local transaction data will NOT be deleted. Google Drive cloud backup will be disabled until you reconnect.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisconnectGoogleDialog = false
                        viewModel.disconnectGoogleAccount()
                    }
                ) {
                    Text(
                        text = "Disconnect",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectGoogleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    restorePromptEligibility?.let { eligible ->
        RestorePromptDialog(
            backupDate = eligible.formattedDate,
            backupSizeBytes = eligible.metadata.sizeBytes,
            onRestoreClick = { viewModel.onRestorePromptAccepted() },
            onNotNowClick = { viewModel.onDismissRestorePrompt(dontAskAgain = false) },
            onDontAskAgainClick = { viewModel.onDismissRestorePrompt(dontAskAgain = true) }
        )
    }

    if (showReplaceConfirmation) {
        ReplaceDataConfirmationDialog(
            onConfirmReplace = { viewModel.onConfirmReplaceAndRestore() },
            onDismiss = { viewModel.onCancelReplaceConfirmation() }
        )
    }

    val cropUri = pendingCropImageUri
    if (cropUri != null) {
        ImageCropDialog(
            imageUri = cropUri,
            onCropConfirmed = { croppedUriString ->
                viewModel.onProfileImageSelected(croppedUriString)
                pendingCropImageUri = null
            },
            onDismiss = {
                pendingCropImageUri = null
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = MaterialTheme.spacing.xs, bottom = MaterialTheme.spacing.xs)
        )
        Surface(
            shape = CardShape,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.md),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsNavigationTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    valueBadge: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
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
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
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
private fun SettingsSwitchTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
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

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
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

@Composable
private fun CurrencySelectionDialog(
    selectedCurrency: Currency,
    onCurrencySelected: (Currency) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Default Currency",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Currency.SUPPORTED_CURRENCIES.forEach { currency ->
                    val isSelected = currency.code == selectedCurrency.code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onCurrencySelected(currency) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${currency.symbol} ${currency.name} (${currency.code})",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun OpeningBalanceDialog(
    initialSubunits: Long,
    currency: Currency,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    var rawInput by remember {
        mutableStateOf(if (initialSubunits == 0L) "" else (initialSubunits / 100.0).toString().removeSuffix(".0"))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Set Starting Balance",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter your starting account balance. This will be added to your net total calculations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = rawInput,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("""^-?\d*\.?\d{0,2}$"""))) {
                            rawInput = newValue
                        }
                    },
                    label = { Text("Opening Balance") },
                    prefix = { Text(currency.symbol) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amountSubunits = Amount.fromStringOrNull(rawInput, currency)?.subunits ?: 0L
                    onSave(amountSubunits)
                    onDismiss()
                }
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
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
                Text("OK", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MonthlyBudgetLimitDialog(
    currentLimit: Amount,
    currency: Currency,
    onSave: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var rawInput by remember {
        mutableStateOf(if (currentLimit.isZero) "" else (currentLimit.subunits / 100.0).toString().removeSuffix(".0"))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_budget_limit_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Set your monthly spending target. You will receive alerts when reaching 50%, 75%, 90%, 100%, and when over budget.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = rawInput,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                            rawInput = newValue
                        }
                    },
                    label = { Text("Monthly Budget") },
                    prefix = { Text(currency.symbol) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amountSubunits = Amount.fromStringOrNull(rawInput, currency)?.subunits ?: 0L
                    onSave(amountSubunits)
                    onDismiss()
                }
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RecurringAdvanceSelectionDialog(
    currentAdvanceDays: Int,
    onDaysSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        RecurringReminderAdvance.ON_DUE_DATE,
        RecurringReminderAdvance.ONE_DAY_BEFORE,
        RecurringReminderAdvance.THREE_DAYS_BEFORE,
        RecurringReminderAdvance.SEVEN_DAYS_BEFORE
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_recurring_advance_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { advance ->
                    val isSelected = currentAdvanceDays == advance.days
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onDaysSelected(advance.days)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = advance.label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AutoLockSelectionDialog(
    currentDurationSeconds: Long,
    onDurationSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        0L to "Immediately",
        30L to "After 30 seconds",
        60L to "After 1 minute",
        300L to "After 5 minutes"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_auto_lock_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (seconds, label) ->
                    val isSelected = currentDurationSeconds == seconds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDurationSelected(seconds) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun UnlockMethodDialog(
    isBiometricEnabled: Boolean,
    onMethodSelected: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_unlock_method_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onMethodSelected(true) }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isBiometricEnabled) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isBiometricEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Biometric + PIN (Recommended)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isBiometricEnabled) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isBiometricEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Use fingerprint or face unlock, with PIN fallback",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onMethodSelected(false) }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (!isBiometricEnabled) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (!isBiometricEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "PIN Only",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (!isBiometricEnabled) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (!isBiometricEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Always enter your 4-digit PIN manually",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
