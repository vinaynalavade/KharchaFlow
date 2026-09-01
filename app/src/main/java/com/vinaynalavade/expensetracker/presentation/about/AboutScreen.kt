package com.vinaynalavade.expensetracker.presentation.about

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vinaynalavade.expensetracker.BuildConfig
import com.vinaynalavade.expensetracker.R
import com.vinaynalavade.expensetracker.presentation.theme.ButtonShape
import com.vinaynalavade.expensetracker.presentation.theme.CardShape
import com.vinaynalavade.expensetracker.presentation.theme.pressScale
import com.vinaynalavade.expensetracker.presentation.theme.spacing

import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import com.vinaynalavade.expensetracker.domain.model.UpdateUiState
import com.vinaynalavade.expensetracker.presentation.update.UpdateDialog
import com.vinaynalavade.expensetracker.presentation.update.UpdateViewModel

private enum class AboutDialogType {
    PRIVACY_SECURITY_DETAILS,
    PRIVACY_POLICY,
    TERMS_OF_SERVICE,
    OPEN_SOURCE_LICENSES
}

private const val GITHUB_REPOSITORY_URL = "https://github.com/vinaynalavade/KharchaFlow"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    updateViewModel: UpdateViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeDialog by remember { mutableStateOf<AboutDialogType?>(null) }

    fun openGitHub() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_REPOSITORY_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open browser for $GITHUB_REPOSITORY_URL", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "About KharchaFlow",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg)
        ) {
            // Standalone Update Action Card (ABOVE About section)
            if (updateViewModel != null) {
                val updateState by updateViewModel.uiState.collectAsState()
                val isChecking = updateState is UpdateUiState.Checking
                val isBusy = isChecking || updateState is UpdateUiState.Downloading || updateState is UpdateUiState.Verifying

                AboutSectionCard(
                    icon = Icons.Default.SystemUpdate,
                    title = "App Updates"
                ) {
                    AboutInfoRow(
                        label = "Current Version",
                        value = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
                    )
                    AboutDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Status",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val statusText = when (val state = updateState) {
                                is UpdateUiState.Idle -> "Ready to check"
                                is UpdateUiState.Checking -> "Checking for updates..."
                                is UpdateUiState.UpToDate -> "You're using the latest version"
                                is UpdateUiState.UpdateAvailable -> "Update available: v${state.releaseInfo.latestVersionName}"
                                is UpdateUiState.Downloading -> "Downloading update (${state.progressPercentage}%)..."
                                is UpdateUiState.Verifying -> "Verifying update..."
                                is UpdateUiState.ReadyToInstall -> "Verified & ready to install"
                                is UpdateUiState.InstallPermissionRequired -> "Permission needed to install"
                                is UpdateUiState.VerificationFailed -> "Verification failed"
                                is UpdateUiState.DownloadFailed -> "Update check failed"
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = when (updateState) {
                                    is UpdateUiState.UpdateAvailable, is UpdateUiState.ReadyToInstall -> MaterialTheme.colorScheme.primary
                                    is UpdateUiState.VerificationFailed, is UpdateUiState.DownloadFailed -> MaterialTheme.colorScheme.error
                                    is UpdateUiState.UpToDate -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))

                        Button(
                            onClick = { updateViewModel.checkForUpdates() },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Checking...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check for Updates")
                            }
                        }
                    }
                }
            }

            // About Section — App Header & Logo Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        shape = CardShape
                    ),
                shape = CardShape,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(MaterialTheme.spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier
                            .size(76.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(18.dp)
                            ),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_kharchaflow_logo),
                                contentDescription = stringResource(R.string.app_name),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val versionName = BuildConfig.VERSION_NAME.ifBlank { "1.0.5" }
                    Text(
                        text = "Version $versionName",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

                    Text(
                        text = "A calm, offline-first personal finance manager designed for effortless cash flow tracking, recurring bill management, and smart budget insights.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Section A: About KharchaFlow
            AboutSectionCard(
                icon = Icons.Default.Info,
                title = "About KharchaFlow"
            ) {
                Text(
                    text = "KharchaFlow is a personal finance application designed to help users track expenses, manage income, monitor spending, organize recurring payments, and maintain better control over their financial activity.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                Text(
                    text = "KharchaFlow is an on-device tracking utility and does not offer banking, investment, credit, or financial advisory services.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Section B: Privacy & Security
            AboutSectionCard(
                icon = Icons.Default.Security,
                title = "Privacy & Security"
            ) {
                AboutInfoRow(
                    label = "Storage",
                    value = "100% Local SQLite (Room Database) on-device"
                )
                AboutDivider()
                AboutInfoRow(
                    label = "Tracking",
                    value = "Zero third-party analytics or advertising networks"
                )
                AboutDivider()
                AboutInfoRow(
                    label = "Cloud Backup",
                    value = "Optional OAuth 2.0 to your private Google Drive"
                )
                AboutDivider()
                AboutInfoRow(
                    label = "App Lock",
                    value = "Optional Biometric / PIN protection with FLAG_SECURE"
                )
                AboutDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeDialog = AboutDialogType.PRIVACY_SECURITY_DETAILS }
                        .padding(vertical = MaterialTheme.spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "View Privacy & Security Details",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Section C: Credits
            AboutSectionCard(
                icon = Icons.Default.Person,
                title = "Credits"
            ) {
                AboutInfoRow(
                    label = "Developer",
                    value = "Developed by Vinay Nalavade"
                )
                AboutDivider()
                AboutInfoRow(
                    label = "Copyright",
                    value = "© 2026 KharchaFlow. All rights reserved."
                )
            }

            // Section D: Legal
            AboutSectionCard(
                icon = Icons.Default.Gavel,
                title = "Legal & Licenses"
            ) {
                AboutActionRow(
                    title = "Privacy Policy",
                    onClick = { activeDialog = AboutDialogType.PRIVACY_POLICY }
                )
                AboutDivider()
                AboutActionRow(
                    title = "Terms of Service & Disclaimer",
                    onClick = { activeDialog = AboutDialogType.TERMS_OF_SERVICE }
                )
                AboutDivider()
                AboutActionRow(
                    title = "Open-Source Licenses",
                    onClick = { activeDialog = AboutDialogType.OPEN_SOURCE_LICENSES }
                )
            }

            // Section E: Connect / GitHub
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        shape = CardShape
                    )
                    .clickable { openGitHub() },
                shape = CardShape,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.lg),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.md))
                        Column {
                            Text(
                                text = "KharchaFlow on GitHub",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Explore the open-source repository & contribute",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open GitHub",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
        }
    }

    // Detail & Legal Dialogs
    activeDialog?.let { dialogType ->
        when (dialogType) {
            AboutDialogType.PRIVACY_SECURITY_DETAILS -> {
                AboutLegalDialog(
                    title = "Privacy & Security Architecture",
                    content = """
                        KharchaFlow is architected with strict on-device privacy as its core principle:

                        • 100% Local Storage: All financial records, accounts, transactions, categories, budgets, and recurring schedules are stored exclusively on your device in a local SQLite database (managed by Room).

                        • Zero Telemetry: KharchaFlow does not run proprietary backend servers to collect user financial data. The application contains zero advertising networks and zero third-party analytics SDKs.

                        • Optional Cloud Backup: When you choose to link Google Drive, backups are stored in your personal Google Drive private Application Data folder (appDataFolder). Other apps cannot access KharchaFlow backups, and KharchaFlow cannot access your personal Drive documents.

                        • On-Device App Lock: Protects your financial records using biometric authentication or salted SHA-256 PIN security. Enabling App Lock automatically enables window security (FLAG_SECURE) to prevent unauthorized screenshots and recent-apps preview capture.

                        • Full User Control: You maintain complete ownership of your personal financial records. You can export JSON backups, restore records, or clear all data at any time.
                    """.trimIndent(),
                    onDismiss = { activeDialog = null }
                )
            }
            AboutDialogType.PRIVACY_POLICY -> {
                AboutLegalDialog(
                    title = "Privacy Policy",
                    content = """
                        Effective Date: August 2026

                        1. Information Collection and Handling
                        KharchaFlow is designed as an offline-first personal finance application. All personal financial entries, notes, categories, and account balances remain solely on your local Android device. We do not transmit, collect, monetize, or sell your financial data.

                        2. Device Permissions
                        • Biometric / Fingerprint: Used strictly on-device to verify your identity when App Lock is enabled.
                        • Notifications: Used strictly for local scheduled reminders (such as daily expense check-in, budget threshold alerts, and recurring payment reminders) scheduled via Android AlarmManager/WorkManager on-device.
                        • Google Account (Optional): Used solely for OAuth 2.0 authentication if you opt in to backup your database to your private Google Drive app data storage.

                        3. Third-Party Services
                        KharchaFlow does not embed third-party advertising SDKs, marketing trackers, or commercial telemetry frameworks.

                        4. User Rights and Data Deletion
                        You have complete control over your data. You can delete individual transactions, reset categories, or delete all application data directly through the in-app Settings.
                    """.trimIndent(),
                    onDismiss = { activeDialog = null }
                )
            }
            AboutDialogType.TERMS_OF_SERVICE -> {
                AboutLegalDialog(
                    title = "Terms of Service & Disclaimer",
                    content = """
                        1. Personal Expense Tracking Utility
                        KharchaFlow is an offline-first bookkeeping and expense tracking tool provided solely for personal informational and record-keeping purposes.

                        2. No Financial, Legal, or Tax Advice
                        KharchaFlow is not a bank, deposit-taking institution, credit provider, investment broker, or licensed financial advisor. The calculations, budget indicators, and summaries generated within the app do not constitute financial, investment, accounting, tax, or legal advice.

                        3. User Responsibility
                        Users are solely responsible for verifying the accuracy of transaction records, managing backup archives, and securing access to their physical devices and App Lock credentials.

                        4. Limitation of Liability
                        To the maximum extent permitted by applicable law, KharchaFlow and its developers shall not be liable for any financial decisions, loss of data, calculation discrepancies, or damages resulting from the use of this software.
                    """.trimIndent(),
                    onDismiss = { activeDialog = null }
                )
            }
            AboutDialogType.OPEN_SOURCE_LICENSES -> {
                AboutLegalDialog(
                    title = "Open-Source Licenses",
                    content = """
                        KharchaFlow is free and open-source software licensed under the GNU General Public License v3.0 (GPL-3.0).

                        Copyright (C) 2026 Vinay Nalavade

                        This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

                        This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

                        Open-Source Libraries & Dependencies:
                        • AndroidX Jetpack & Jetpack Compose (AOSP / Google) — Apache License 2.0
                        • Kotlin & Kotlin Coroutines (JetBrains) — Apache License 2.0
                        • AndroidX Room & SQLite (AOSP / Google) — Apache License 2.0
                        • Material Design 3 Components (Google) — Apache License 2.0
                    """.trimIndent(),
                    onDismiss = { activeDialog = null }
                )
            }
        }
    }

    if (updateViewModel != null) {
        UpdateDialog(viewModel = updateViewModel)
    }
}

@Composable
private fun AboutSectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = CardShape
            ),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
            content()
        }
    }
}

@Composable
private fun AboutInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.65f)
        )
    }
}

@Composable
private fun AboutActionRow(
    title: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .pressScale(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun AboutDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
}

@Composable
private fun AboutLegalDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        ),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .widthIn(max = 480.dp),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, shape = ButtonShape) {
                Text(text = "Close", fontWeight = FontWeight.Bold)
            }
        },
        shape = CardShape,
        containerColor = MaterialTheme.colorScheme.surface
    )
}
