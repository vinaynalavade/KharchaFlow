package com.vinaynalavade.expensetracker.presentation.backup

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinaynalavade.expensetracker.core.backup.BackupData
import com.vinaynalavade.expensetracker.core.backup.BackupValidationResult
import com.vinaynalavade.expensetracker.core.backup.ImportValidationResult
import com.vinaynalavade.expensetracker.core.backup.JsonBackupParser
import com.vinaynalavade.expensetracker.core.google.GoogleAccountManager
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.GoogleBackupState
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.repository.BackupRepository
import com.vinaynalavade.expensetracker.domain.usecase.CreateBackupUseCase
import com.vinaynalavade.expensetracker.domain.usecase.DisconnectGoogleAccountUseCase
import com.vinaynalavade.expensetracker.domain.usecase.ExportFilterOptions
import com.vinaynalavade.expensetracker.domain.usecase.ExportFormat
import com.vinaynalavade.expensetracker.domain.usecase.ExportTransactionsUseCase
import com.vinaynalavade.expensetracker.domain.usecase.ExportedFileResult
import com.vinaynalavade.expensetracker.domain.usecase.GetCategoriesUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetGoogleBackupStateUseCase
import com.vinaynalavade.expensetracker.domain.usecase.ImportTransactionsUseCase
import com.vinaynalavade.expensetracker.domain.usecase.PerformGoogleDriveBackupUseCase
import com.vinaynalavade.expensetracker.domain.usecase.PrepareGoogleDriveRestoreUseCase
import com.vinaynalavade.expensetracker.domain.usecase.RestoreBackupUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SaveConnectedGoogleAccountUseCase
import com.vinaynalavade.expensetracker.domain.usecase.ValidateBackupUseCase
import com.vinaynalavade.expensetracker.domain.usecase.ValidateImportUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed class BackupOpState {
    data object Idle : BackupOpState()
    data class Loading(val message: String) : BackupOpState()
    data class Success(val message: String) : BackupOpState()
    data class Error(val message: String) : BackupOpState()
}

class BackupViewModel(
    private val backupRepository: BackupRepository,
    private val createBackupUseCase: CreateBackupUseCase,
    private val validateBackupUseCase: ValidateBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val exportTransactionsUseCase: ExportTransactionsUseCase,
    private val validateImportUseCase: ValidateImportUseCase,
    private val importTransactionsUseCase: ImportTransactionsUseCase,
    private val googleAccountManager: GoogleAccountManager,
    private val getGoogleBackupStateUseCase: GetGoogleBackupStateUseCase,
    private val performGoogleDriveBackupUseCase: PerformGoogleDriveBackupUseCase,
    private val prepareGoogleDriveRestoreUseCase: PrepareGoogleDriveRestoreUseCase,
    private val disconnectGoogleAccountUseCase: DisconnectGoogleAccountUseCase,
    private val saveConnectedGoogleAccountUseCase: SaveConnectedGoogleAccountUseCase,
    getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    val lastBackupTimestamp: StateFlow<Long?> = backupRepository.getLastBackupTimestamp()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val googleBackupState: StateFlow<GoogleBackupState> = getGoogleBackupStateUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GoogleBackupState.Disconnected)

    val categories: StateFlow<List<Category>> = getCategoriesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _opState = MutableStateFlow<BackupOpState>(BackupOpState.Idle)
    val opState: StateFlow<BackupOpState> = _opState.asStateFlow()

    private val _restorePreview = MutableStateFlow<BackupValidationResult.Valid?>(null)
    val restorePreview: StateFlow<BackupValidationResult.Valid?> = _restorePreview.asStateFlow()

    private val _cloudRestorePreview = MutableStateFlow<BackupValidationResult.Valid?>(null)
    val cloudRestorePreview: StateFlow<BackupValidationResult.Valid?> = _cloudRestorePreview.asStateFlow()

    private val _importPreview = MutableStateFlow<ImportValidationResult?>(null)
    val importPreview: StateFlow<ImportValidationResult?> = _importPreview.asStateFlow()

    private val _exportOptions = MutableStateFlow(ExportFilterOptions())
    val exportOptions: StateFlow<ExportFilterOptions> = _exportOptions.asStateFlow()

    fun getGoogleSignInIntent(): Intent {
        return googleAccountManager.getSignInIntent()
    }

    fun onGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _opState.value = BackupOpState.Loading("Connecting to Google Account...")
            when (val result = googleAccountManager.parseSignInResult(data)) {
                is AppResult.Success -> {
                    saveConnectedGoogleAccountUseCase(result.data)
                    _opState.value = BackupOpState.Success("Connected as ${result.data.email}")
                }
                is AppResult.Error -> {
                    _opState.value = BackupOpState.Error(result.error.message)
                }
            }
        }
    }

    fun disconnectGoogleAccount() {
        viewModelScope.launch {
            _opState.value = BackupOpState.Loading("Disconnecting Google Account...")
            when (val result = disconnectGoogleAccountUseCase()) {
                is AppResult.Success -> {
                    _opState.value = BackupOpState.Success("Google Account disconnected.")
                }
                is AppResult.Error -> {
                    _opState.value = BackupOpState.Error(result.error.message)
                }
            }
        }
    }

    fun backupToGoogleDrive() {
        viewModelScope.launch {
            _opState.value = BackupOpState.Loading("Backing up your data to Google Drive...")
            when (val result = performGoogleDriveBackupUseCase()) {
                is AppResult.Success -> {
                    _opState.value = BackupOpState.Success("Backup completed successfully to Google Drive!")
                }
                is AppResult.Error -> {
                    _opState.value = BackupOpState.Error(result.error.message)
                }
            }
        }
    }

    fun initiateGoogleRestore() {
        viewModelScope.launch {
            _opState.value = BackupOpState.Loading("Downloading backup from Google Drive...")
            when (val result = prepareGoogleDriveRestoreUseCase()) {
                is AppResult.Success -> {
                    val backupData = result.data
                    val validation = validateBackupUseCase(JsonBackupParser.toJson(backupData))
                    _opState.value = BackupOpState.Idle
                    if (validation is BackupValidationResult.Valid) {
                        _cloudRestorePreview.value = validation
                    } else if (validation is BackupValidationResult.Invalid) {
                        _opState.value = BackupOpState.Error(validation.errorMessage)
                    }
                }
                is AppResult.Error -> {
                    _opState.value = BackupOpState.Error(result.error.message)
                }
            }
        }
    }

    fun confirmCloudRestore(onSuccess: () -> Unit) {
        val validBackup = _cloudRestorePreview.value?.backupData ?: return
        viewModelScope.launch {
            _opState.value = BackupOpState.Loading("Restoring data from Google Drive...")
            when (val result = restoreBackupUseCase(validBackup)) {
                is AppResult.Success -> {
                    _cloudRestorePreview.value = null
                    _opState.value = BackupOpState.Success("Cloud backup restored successfully!")
                    onSuccess()
                }
                is AppResult.Error -> {
                    _opState.value = BackupOpState.Error(result.error.message)
                }
            }
        }
    }

    fun cancelCloudRestore() {
        _cloudRestorePreview.value = null
    }

    fun updateExportFormat(format: ExportFormat) {
        _exportOptions.value = _exportOptions.value.copy(format = format)
    }

    fun updateExportType(type: TransactionType?) {
        _exportOptions.value = _exportOptions.value.copy(type = type)
    }

    fun updateExportCategory(categoryId: Long?) {
        _exportOptions.value = _exportOptions.value.copy(categoryId = categoryId)
    }

    fun updateExportDateRange(startDate: Long?, endDate: Long?) {
        _exportOptions.value = _exportOptions.value.copy(startDate = startDate, endDate = endDate)
    }

    fun createFullBackup(onReadyToSave: (content: String, defaultFileName: String) -> Unit) {
        viewModelScope.launch {
            _opState.value = BackupOpState.Loading("Generating full backup...")
            when (val result = createBackupUseCase()) {
                is AppResult.Success -> {
                    val json = JsonBackupParser.toJson(result.data)
                    val timestampStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"))
                    val fileName = "KharchaFlow_Backup_$timestampStr.json"
                    _opState.value = BackupOpState.Idle
                    onReadyToSave(json, fileName)
                }
                is AppResult.Error -> {
                    _opState.value = BackupOpState.Error(result.error.message)
                }
            }
        }
    }

    fun onBackupFileSelected(jsonContent: String) {
        viewModelScope.launch {
            _opState.value = BackupOpState.Loading("Validating backup file...")
            when (val result = validateBackupUseCase(jsonContent)) {
                is BackupValidationResult.Valid -> {
                    _opState.value = BackupOpState.Idle
                    _restorePreview.value = result
                }
                is BackupValidationResult.Invalid -> {
                    _opState.value = BackupOpState.Error(result.errorMessage)
                }
            }
        }
    }

    fun confirmRestore(onSuccess: () -> Unit) {
        val validBackup = _restorePreview.value?.backupData ?: return
        viewModelScope.launch {
            _opState.value = BackupOpState.Loading("Restoring application data...")
            when (val result = restoreBackupUseCase(validBackup)) {
                is AppResult.Success -> {
                    _restorePreview.value = null
                    _opState.value = BackupOpState.Success("Backup restored successfully!")
                    onSuccess()
                }
                is AppResult.Error -> {
                    _opState.value = BackupOpState.Error(result.error.message)
                }
            }
        }
    }

    fun cancelRestore() {
        _restorePreview.value = null
    }

    fun exportTransactions(onReadyToSave: (ExportedFileResult) -> Unit) {
        viewModelScope.launch {
            _opState.value = BackupOpState.Loading("Exporting transactions...")
            when (val result = exportTransactionsUseCase(_exportOptions.value)) {
                is AppResult.Success -> {
                    _opState.value = BackupOpState.Idle
                    onReadyToSave(result.data)
                }
                is AppResult.Error -> {
                    _opState.value = BackupOpState.Error(result.error.message)
                }
            }
        }
    }

    fun onImportFileSelected(fileContent: String, isCsv: Boolean) {
        viewModelScope.launch {
            _opState.value = BackupOpState.Loading("Validating transaction file...")
            val result = validateImportUseCase(fileContent, isCsv)
            _opState.value = BackupOpState.Idle
            if (result.validTransactions.isEmpty() && result.issues.isNotEmpty()) {
                _opState.value = BackupOpState.Error(result.issues.first())
            } else {
                _importPreview.value = result
            }
        }
    }

    fun confirmImport(onSuccess: () -> Unit) {
        val validTransactions = _importPreview.value?.validTransactions ?: return
        viewModelScope.launch {
            _opState.value = BackupOpState.Loading("Importing ${validTransactions.size} transactions...")
            when (val result = importTransactionsUseCase(validTransactions)) {
                is AppResult.Success -> {
                    _importPreview.value = null
                    _opState.value = BackupOpState.Success("Imported ${result.data} transactions successfully!")
                    onSuccess()
                }
                is AppResult.Error -> {
                    _opState.value = BackupOpState.Error(result.error.message)
                }
            }
        }
    }

    fun cancelImport() {
        _importPreview.value = null
    }

    fun clearOpState() {
        _opState.value = BackupOpState.Idle
    }

    class Factory(
        private val backupRepository: BackupRepository,
        private val createBackupUseCase: CreateBackupUseCase,
        private val validateBackupUseCase: ValidateBackupUseCase,
        private val restoreBackupUseCase: RestoreBackupUseCase,
        private val exportTransactionsUseCase: ExportTransactionsUseCase,
        private val validateImportUseCase: ValidateImportUseCase,
        private val importTransactionsUseCase: ImportTransactionsUseCase,
        private val googleAccountManager: GoogleAccountManager,
        private val getGoogleBackupStateUseCase: GetGoogleBackupStateUseCase,
        private val performGoogleDriveBackupUseCase: PerformGoogleDriveBackupUseCase,
        private val prepareGoogleDriveRestoreUseCase: PrepareGoogleDriveRestoreUseCase,
        private val disconnectGoogleAccountUseCase: DisconnectGoogleAccountUseCase,
        private val saveConnectedGoogleAccountUseCase: SaveConnectedGoogleAccountUseCase,
        private val getCategoriesUseCase: GetCategoriesUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BackupViewModel(
                backupRepository,
                createBackupUseCase,
                validateBackupUseCase,
                restoreBackupUseCase,
                exportTransactionsUseCase,
                validateImportUseCase,
                importTransactionsUseCase,
                googleAccountManager,
                getGoogleBackupStateUseCase,
                performGoogleDriveBackupUseCase,
                prepareGoogleDriveRestoreUseCase,
                disconnectGoogleAccountUseCase,
                saveConnectedGoogleAccountUseCase,
                getCategoriesUseCase
            ) as T
        }
    }
}
