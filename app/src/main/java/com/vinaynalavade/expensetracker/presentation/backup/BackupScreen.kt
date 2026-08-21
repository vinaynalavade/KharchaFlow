package com.vinaynalavade.expensetracker.presentation.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinaynalavade.expensetracker.core.backup.BackupValidationResult
import com.vinaynalavade.expensetracker.core.backup.ImportValidationResult
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.usecase.ExportFormat
import com.vinaynalavade.expensetracker.domain.usecase.ExportedFileResult
import com.vinaynalavade.expensetracker.presentation.components.AppTopBar
import com.vinaynalavade.expensetracker.presentation.theme.ButtonShape
import com.vinaynalavade.expensetracker.presentation.theme.CardShape
import com.vinaynalavade.expensetracker.presentation.theme.PillShape
import com.vinaynalavade.expensetracker.presentation.theme.spacing
import com.vinaynalavade.expensetracker.presentation.widget.ExpenseTrackerWidgetProvider
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val lastBackupTimestamp by viewModel.lastBackupTimestamp.collectAsStateWithLifecycle()
    val opState by viewModel.opState.collectAsStateWithLifecycle()
    val restorePreview by viewModel.restorePreview.collectAsStateWithLifecycle()
    val importPreview by viewModel.importPreview.collectAsStateWithLifecycle()
    val exportOptions by viewModel.exportOptions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var pendingSaveContent by remember { mutableStateOf<String?>(null) }
    var pendingSaveMimeType by remember { mutableStateOf("application/json") }

    // SAF Launchers
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(pendingSaveMimeType)
    ) { uri: Uri? ->
        if (uri != null && pendingSaveContent != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    OutputStreamWriter(output).use { writer ->
                        writer.write(pendingSaveContent)
                    }
                }
                scope.launch {
                    snackbarHostState.showSnackbar("File saved successfully!")
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Failed to write file: ${e.message}")
                }
            } finally {
                pendingSaveContent = null
            }
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input)).readText()
                }
                if (!content.isNullOrBlank()) {
                    viewModel.onBackupFileSelected(content)
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Failed to read file: ${e.message}")
                }
            }
        }
    }

    val openImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val isCsv = uri.toString().endsWith(".csv", ignoreCase = true) ||
                        context.contentResolver.getType(uri)?.contains("csv") == true
                val content = context.contentResolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input)).readText()
                }
                if (!content.isNullOrBlank()) {
                    viewModel.onImportFileSelected(content, isCsv)
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Failed to read file: ${e.message}")
                }
            }
        }
    }

    LaunchedEffect(opState) {
        when (val state = opState) {
            is BackupOpState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearOpState()
            }
            is BackupOpState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearOpState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Backup & Restore",
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg)
            ) {
                // Last Backup Status Card
                val lastBackupText = if (lastBackupTimestamp != null && lastBackupTimestamp!! > 0L) {
                    val instant = Instant.ofEpochMilli(lastBackupTimestamp!!)
                    val formatted = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                        .withZone(ZoneId.systemDefault())
                        .format(instant)
                    "Last backup: $formatted"
                } else {
                    "No backup created yet on this device"
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (lastBackupTimestamp != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (lastBackupTimestamp != null) Icons.Default.CheckCircle else Icons.Default.Backup,
                            contentDescription = null,
                            tint = if (lastBackupTimestamp != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.md))
                        Column {
                            Text(
                                text = "Full Local Backup Status",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = lastBackupText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 1. Full Backup & Restore Section
                BackupSection(title = "FULL APPLICATION BACKUP & RESTORE") {
                    BackupActionTile(
                        icon = Icons.Default.Backup,
                        title = "Create Full Backup",
                        subtitle = "Export all transactions, categories, reminders, and preferences into a structured JSON backup",
                        onClick = {
                            viewModel.createFullBackup { content, fileName ->
                                pendingSaveContent = content
                                pendingSaveMimeType = "application/json"
                                createDocLauncher.launch(fileName)
                            }
                        }
                    )

                    BackupDivider()

                    BackupActionTile(
                        icon = Icons.Default.Restore,
                        title = "Restore from Backup",
                        subtitle = "Restore a full KharchaFlow JSON backup to replace and restore application data",
                        onClick = {
                            openBackupLauncher.launch(arrayOf("application/json", "text/*"))
                        }
                    )
                }

                // 2. Export Transactions Section
                BackupSection(title = "EXPORT TRANSACTIONS") {
                    Text(
                        text = "Export Format",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
                    ) {
                        ExportOptionChip(
                            label = "CSV (Excel / Sheets)",
                            isSelected = exportOptions.format == ExportFormat.CSV,
                            onClick = { viewModel.updateExportFormat(ExportFormat.CSV) },
                            modifier = Modifier.weight(1f)
                        )
                        ExportOptionChip(
                            label = "JSON (Structured)",
                            isSelected = exportOptions.format == ExportFormat.JSON,
                            onClick = { viewModel.updateExportFormat(ExportFormat.JSON) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

                    Text(
                        text = "Transaction Type",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
                    ) {
                        ExportOptionChip(
                            label = "All",
                            isSelected = exportOptions.type == null,
                            onClick = { viewModel.updateExportType(null) },
                            modifier = Modifier.weight(1f)
                        )
                        ExportOptionChip(
                            label = "Expenses",
                            isSelected = exportOptions.type == TransactionType.EXPENSE,
                            onClick = { viewModel.updateExportType(TransactionType.EXPENSE) },
                            modifier = Modifier.weight(1f)
                        )
                        ExportOptionChip(
                            label = "Income",
                            isSelected = exportOptions.type == TransactionType.INCOME,
                            onClick = { viewModel.updateExportType(TransactionType.INCOME) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

                    Text(
                        text = "Date Scope",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
                    ) {
                        ExportOptionChip(
                            label = "All Time",
                            isSelected = exportOptions.startDate == null,
                            onClick = { viewModel.updateExportDateRange(null, null) },
                            modifier = Modifier.weight(1f)
                        )
                        ExportOptionChip(
                            label = "This Month",
                            isSelected = exportOptions.startDate != null,
                            onClick = {
                                val currentMonth = YearMonth.now()
                                val start = DateTimeUtils.getStartOfDayEpoch(currentMonth.atDay(1))
                                val end = DateTimeUtils.getEndOfDayEpoch(currentMonth.atEndOfMonth())
                                viewModel.updateExportDateRange(start, end)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

                    Button(
                        onClick = {
                            viewModel.exportTransactions { result ->
                                pendingSaveContent = result.content
                                pendingSaveMimeType = result.mimeType
                                createDocLauncher.launch(result.fileName)
                            }
                        },
                        shape = ButtonShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                        Text(text = "Export Transactions (${exportOptions.format.name})", fontWeight = FontWeight.Bold)
                    }
                }

                // 3. Import Transactions Section
                BackupSection(title = "IMPORT TRANSACTIONS") {
                    BackupActionTile(
                        icon = Icons.Default.FileUpload,
                        title = "Import from CSV or JSON",
                        subtitle = "Select a valid KharchaFlow CSV spreadsheet or JSON transaction file to import records",
                        onClick = {
                            openImportLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "application/json", "text/*"))
                        }
                    )
                }

                // 4. Data & Privacy Guarantee Section
                BackupSection(title = "DATA & PRIVACY GUARANTEE") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                text = "100% Local & Private",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "All backups and exports remain entirely in your control on your device. No cloud sync, no tracking, and no external servers.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }

            // Loading overlay
            if (opState is BackupOpState.Loading) {
                Surface(
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Card(
                            shape = CardShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(MaterialTheme.spacing.lg),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = (opState as BackupOpState.Loading).message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Restore Confirmation Dialog
    if (restorePreview != null) {
        val preview = restorePreview!!
        val dateStr = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(preview.createdAt))

        AlertDialog(
            onDismissRequest = { viewModel.cancelRestore() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Restore Full Backup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                ) {
                    Text(
                        text = "Backup Details:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("• Transactions: ${preview.transactionCount}", style = MaterialTheme.typography.bodySmall)
                    Text("• Categories: ${preview.categoryCount}", style = MaterialTheme.typography.bodySmall)
                    Text("• Recurring / EMIs: ${preview.recurringCount}", style = MaterialTheme.typography.bodySmall)
                    Text("• Created: $dateStr", style = MaterialTheme.typography.bodySmall)
                    Text("• Backup Version: ${preview.backupData.backupVersion} (App v${preview.appVersion})", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

                    Text(
                        text = "⚠️ Warning: Restoring this backup will replace all current transactions, categories, and settings on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmRestore {
                            ExpenseTrackerWidgetProvider.updateAll(context)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Replace & Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelRestore() }) {
                    Text("Cancel")
                }
            },
            shape = CardShape,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Import Preview Dialog
    if (importPreview != null) {
        val preview = importPreview!!

        AlertDialog(
            onDismissRequest = { viewModel.cancelImport() },
            title = {
                Text(
                    text = "Import Transactions Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                ) {
                    Text(
                        text = "File Summary:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("• Total rows evaluated: ${preview.totalRows}", style = MaterialTheme.typography.bodySmall)
                    Text("• Valid transactions ready to import: ${preview.validTransactions.size}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

                    if (preview.issues.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                        Text(
                            text = "Issues detected (${preview.issues.size} invalid rows):",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        preview.issues.take(4).forEach { issue ->
                            Text("• $issue", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                        if (preview.issues.size > 4) {
                            Text("... and ${preview.issues.size - 4} more invalid rows", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmImport {
                            ExpenseTrackerWidgetProvider.updateAll(context)
                        }
                    },
                    enabled = preview.validTransactions.isNotEmpty()
                ) {
                    Text("Import ${preview.validTransactions.size} Transactions", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelImport() }) {
                    Text("Cancel")
                }
            },
            shape = CardShape,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun BackupSection(
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
private fun BackupActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
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

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun BackupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = MaterialTheme.spacing.xs),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    )
}

@Composable
private fun ExportOptionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = PillShape,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
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
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
