package com.vinaynalavade.expensetracker.di

import android.content.Context
import com.vinaynalavade.expensetracker.data.local.database.ExpenseTrackerDatabase
import com.vinaynalavade.expensetracker.data.preferences.UserPreferencesDataStore
import com.vinaynalavade.expensetracker.data.repository.CategoryRepositoryImpl
import com.vinaynalavade.expensetracker.data.repository.RecurringTransactionRepositoryImpl
import com.vinaynalavade.expensetracker.data.repository.TransactionRepositoryImpl
import com.vinaynalavade.expensetracker.data.repository.UserPreferencesRepositoryImpl
import com.vinaynalavade.expensetracker.domain.repository.CategoryRepository
import com.vinaynalavade.expensetracker.domain.repository.RecurringTransactionRepository
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository
import com.vinaynalavade.expensetracker.domain.usecase.AddTransactionUseCase
import com.vinaynalavade.expensetracker.domain.usecase.DeleteCategoryUseCase
import com.vinaynalavade.expensetracker.domain.usecase.DeleteRecurringTransactionUseCase
import com.vinaynalavade.expensetracker.domain.usecase.DeleteTransactionUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GenerateStatementUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetCategoriesUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetCategoryAnalysisUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetFinancialSummaryUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetMonthlyLedgerUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetRecurringTransactionsUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetTransactionByIdUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetTransactionsUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetUserPreferencesUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetWidgetFinancialSummaryUseCase
import com.vinaynalavade.expensetracker.domain.usecase.ProcessDueRecurringTransactionsUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SaveCategoryUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SaveRecurringTransactionUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SetCurrencyUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SetDailyReminderUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SetOpeningBalanceUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SetThemeModeUseCase
import com.vinaynalavade.expensetracker.domain.usecase.UpdateTransactionUseCase

interface AppContainer {
    val transactionRepository: TransactionRepository
    val categoryRepository: CategoryRepository
    val userPreferencesRepository: UserPreferencesRepository
    val recurringTransactionRepository: RecurringTransactionRepository

    val getTransactionsUseCase: GetTransactionsUseCase
    val getTransactionByIdUseCase: GetTransactionByIdUseCase
    val addTransactionUseCase: AddTransactionUseCase
    val updateTransactionUseCase: UpdateTransactionUseCase
    val deleteTransactionUseCase: DeleteTransactionUseCase

    val getCategoriesUseCase: GetCategoriesUseCase
    val saveCategoryUseCase: SaveCategoryUseCase
    val deleteCategoryUseCase: DeleteCategoryUseCase

    val getFinancialSummaryUseCase: GetFinancialSummaryUseCase
    val getWidgetFinancialSummaryUseCase: GetWidgetFinancialSummaryUseCase
    val getMonthlyLedgerUseCase: GetMonthlyLedgerUseCase
    val getCategoryAnalysisUseCase: GetCategoryAnalysisUseCase
    val generateStatementUseCase: GenerateStatementUseCase

    val getRecurringTransactionsUseCase: GetRecurringTransactionsUseCase
    val saveRecurringTransactionUseCase: SaveRecurringTransactionUseCase
    val deleteRecurringTransactionUseCase: DeleteRecurringTransactionUseCase
    val processDueRecurringTransactionsUseCase: ProcessDueRecurringTransactionsUseCase

    val getUserPreferencesUseCase: GetUserPreferencesUseCase
    val setThemeModeUseCase: SetThemeModeUseCase
    val setCurrencyUseCase: SetCurrencyUseCase
    val setOpeningBalanceUseCase: SetOpeningBalanceUseCase
    val setDailyReminderUseCase: SetDailyReminderUseCase
    val dailyReminderScheduler: com.vinaynalavade.expensetracker.core.notification.DailyReminderScheduler

    val backupRepository: com.vinaynalavade.expensetracker.domain.repository.BackupRepository
    val createBackupUseCase: com.vinaynalavade.expensetracker.domain.usecase.CreateBackupUseCase
    val validateBackupUseCase: com.vinaynalavade.expensetracker.domain.usecase.ValidateBackupUseCase
    val restoreBackupUseCase: com.vinaynalavade.expensetracker.domain.usecase.RestoreBackupUseCase
    val exportTransactionsUseCase: com.vinaynalavade.expensetracker.domain.usecase.ExportTransactionsUseCase
    val validateImportUseCase: com.vinaynalavade.expensetracker.domain.usecase.ValidateImportUseCase
    val importTransactionsUseCase: com.vinaynalavade.expensetracker.domain.usecase.ImportTransactionsUseCase

    val googleAccountManager: com.vinaynalavade.expensetracker.core.google.GoogleAccountManager
    val googleDriveBackupRepository: com.vinaynalavade.expensetracker.domain.repository.GoogleDriveBackupRepository
    val getGoogleBackupStateUseCase: com.vinaynalavade.expensetracker.domain.usecase.GetGoogleBackupStateUseCase
    val performGoogleDriveBackupUseCase: com.vinaynalavade.expensetracker.domain.usecase.PerformGoogleDriveBackupUseCase
    val prepareGoogleDriveRestoreUseCase: com.vinaynalavade.expensetracker.domain.usecase.PrepareGoogleDriveRestoreUseCase
    val disconnectGoogleAccountUseCase: com.vinaynalavade.expensetracker.domain.usecase.DisconnectGoogleAccountUseCase
    val saveConnectedGoogleAccountUseCase: com.vinaynalavade.expensetracker.domain.usecase.SaveConnectedGoogleAccountUseCase

    val securePinManager: com.vinaynalavade.expensetracker.core.security.SecurePinManager
    val appLockManager: com.vinaynalavade.expensetracker.core.security.AppLockManager
    val setAppLockEnabledUseCase: com.vinaynalavade.expensetracker.domain.usecase.SetAppLockEnabledUseCase
    val setBiometricEnabledUseCase: com.vinaynalavade.expensetracker.domain.usecase.SetBiometricEnabledUseCase
    val setAutoLockDurationUseCase: com.vinaynalavade.expensetracker.domain.usecase.SetAutoLockDurationUseCase
    val setHideContentInRecentsUseCase: com.vinaynalavade.expensetracker.domain.usecase.SetHideContentInRecentsUseCase
    val verifyPinUseCase: com.vinaynalavade.expensetracker.domain.usecase.VerifyPinUseCase
    val savePinUseCase: com.vinaynalavade.expensetracker.domain.usecase.SavePinUseCase
    val changePinUseCase: com.vinaynalavade.expensetracker.domain.usecase.ChangePinUseCase
    val disableAppLockUseCase: com.vinaynalavade.expensetracker.domain.usecase.DisableAppLockUseCase

    val notificationStateRepository: com.vinaynalavade.expensetracker.domain.repository.NotificationStateRepository
    val checkBudgetThresholdsUseCase: com.vinaynalavade.expensetracker.domain.usecase.CheckBudgetThresholdsUseCase
    val checkUpcomingRecurringPaymentsUseCase: com.vinaynalavade.expensetracker.domain.usecase.CheckUpcomingRecurringPaymentsUseCase
    val processFinancialRemindersUseCase: com.vinaynalavade.expensetracker.domain.usecase.ProcessFinancialRemindersUseCase
    val rescheduleAllRemindersUseCase: com.vinaynalavade.expensetracker.domain.usecase.RescheduleAllRemindersUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val database: ExpenseTrackerDatabase by lazy {
        ExpenseTrackerDatabase.getInstance(context)
    }

    private val dataStore: UserPreferencesDataStore by lazy {
        UserPreferencesDataStore(context)
    }

    override val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(database.transactionDao())
    }

    override val categoryRepository: CategoryRepository by lazy {
        CategoryRepositoryImpl(database.categoryDao())
    }

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepositoryImpl(dataStore)
    }

    override val dailyReminderScheduler: com.vinaynalavade.expensetracker.core.notification.DailyReminderScheduler by lazy {
        com.vinaynalavade.expensetracker.core.notification.AlarmDailyReminderScheduler(context, userPreferencesRepository)
    }

    override val recurringTransactionRepository: RecurringTransactionRepository by lazy {
        RecurringTransactionRepositoryImpl(database.recurringTransactionDao(), database.transactionDao())
    }

    override val backupRepository: com.vinaynalavade.expensetracker.domain.repository.BackupRepository by lazy {
        com.vinaynalavade.expensetracker.data.repository.BackupRepositoryImpl(
            database,
            transactionRepository,
            categoryRepository,
            recurringTransactionRepository,
            userPreferencesRepository
        )
    }

    override val getTransactionsUseCase: GetTransactionsUseCase by lazy {
        GetTransactionsUseCase(transactionRepository)
    }

    override val getTransactionByIdUseCase: GetTransactionByIdUseCase by lazy {
        GetTransactionByIdUseCase(transactionRepository)
    }

    override val addTransactionUseCase: AddTransactionUseCase by lazy {
        AddTransactionUseCase(transactionRepository)
    }

    override val updateTransactionUseCase: UpdateTransactionUseCase by lazy {
        UpdateTransactionUseCase(transactionRepository)
    }

    override val deleteTransactionUseCase: DeleteTransactionUseCase by lazy {
        DeleteTransactionUseCase(transactionRepository)
    }

    override val getCategoriesUseCase: GetCategoriesUseCase by lazy {
        GetCategoriesUseCase(categoryRepository)
    }

    override val saveCategoryUseCase: SaveCategoryUseCase by lazy {
        SaveCategoryUseCase(categoryRepository)
    }

    override val deleteCategoryUseCase: DeleteCategoryUseCase by lazy {
        DeleteCategoryUseCase(categoryRepository, transactionRepository)
    }

    override val getFinancialSummaryUseCase: GetFinancialSummaryUseCase by lazy {
        GetFinancialSummaryUseCase(transactionRepository, userPreferencesRepository)
    }

    override val getWidgetFinancialSummaryUseCase: GetWidgetFinancialSummaryUseCase by lazy {
        GetWidgetFinancialSummaryUseCase(transactionRepository, userPreferencesRepository)
    }

    override val getMonthlyLedgerUseCase: GetMonthlyLedgerUseCase by lazy {
        GetMonthlyLedgerUseCase(transactionRepository, userPreferencesRepository)
    }

    override val getCategoryAnalysisUseCase: GetCategoryAnalysisUseCase by lazy {
        GetCategoryAnalysisUseCase(transactionRepository)
    }

    override val generateStatementUseCase: GenerateStatementUseCase by lazy {
        GenerateStatementUseCase(transactionRepository, userPreferencesRepository)
    }

    override val getRecurringTransactionsUseCase: GetRecurringTransactionsUseCase by lazy {
        GetRecurringTransactionsUseCase(recurringTransactionRepository)
    }

    override val saveRecurringTransactionUseCase: SaveRecurringTransactionUseCase by lazy {
        SaveRecurringTransactionUseCase(recurringTransactionRepository)
    }

    override val deleteRecurringTransactionUseCase: DeleteRecurringTransactionUseCase by lazy {
        DeleteRecurringTransactionUseCase(recurringTransactionRepository)
    }

    override val processDueRecurringTransactionsUseCase: ProcessDueRecurringTransactionsUseCase by lazy {
        ProcessDueRecurringTransactionsUseCase(recurringTransactionRepository)
    }

    override val getUserPreferencesUseCase: GetUserPreferencesUseCase by lazy {
        GetUserPreferencesUseCase(userPreferencesRepository)
    }

    override val setThemeModeUseCase: SetThemeModeUseCase by lazy {
        SetThemeModeUseCase(userPreferencesRepository)
    }

    override val setCurrencyUseCase: SetCurrencyUseCase by lazy {
        SetCurrencyUseCase(userPreferencesRepository)
    }

    override val setOpeningBalanceUseCase: SetOpeningBalanceUseCase by lazy {
        SetOpeningBalanceUseCase(userPreferencesRepository)
    }

    override val setDailyReminderUseCase: SetDailyReminderUseCase by lazy {
        SetDailyReminderUseCase(userPreferencesRepository)
    }

    override val createBackupUseCase: com.vinaynalavade.expensetracker.domain.usecase.CreateBackupUseCase by lazy {
        com.vinaynalavade.expensetracker.domain.usecase.CreateBackupUseCase(backupRepository)
    }

    override val validateBackupUseCase: com.vinaynalavade.expensetracker.domain.usecase.ValidateBackupUseCase by lazy {
        com.vinaynalavade.expensetracker.domain.usecase.ValidateBackupUseCase(backupRepository)
    }

    override val restoreBackupUseCase: com.vinaynalavade.expensetracker.domain.usecase.RestoreBackupUseCase by lazy {
        com.vinaynalavade.expensetracker.domain.usecase.RestoreBackupUseCase(backupRepository, rescheduleAllRemindersUseCase)
    }

    override val exportTransactionsUseCase: com.vinaynalavade.expensetracker.domain.usecase.ExportTransactionsUseCase by lazy {
        com.vinaynalavade.expensetracker.domain.usecase.ExportTransactionsUseCase(
            backupRepository,
            categoryRepository,
            userPreferencesRepository
        )
    }

    override val validateImportUseCase: com.vinaynalavade.expensetracker.domain.usecase.ValidateImportUseCase by lazy {
        com.vinaynalavade.expensetracker.domain.usecase.ValidateImportUseCase(
            backupRepository,
            userPreferencesRepository
        )
    }

    override val importTransactionsUseCase: com.vinaynalavade.expensetracker.domain.usecase.ImportTransactionsUseCase by lazy {
        com.vinaynalavade.expensetracker.domain.usecase.ImportTransactionsUseCase(backupRepository)
    }

    override val googleAccountManager: com.vinaynalavade.expensetracker.core.google.GoogleAccountManager by lazy {
        com.vinaynalavade.expensetracker.core.google.GoogleAccountManager(context)
    }

    private val googleDriveRestService: com.vinaynalavade.expensetracker.core.google.GoogleDriveRestService by lazy {
        com.vinaynalavade.expensetracker.core.google.GoogleDriveRestService()
    }

    override val googleDriveBackupRepository: com.vinaynalavade.expensetracker.domain.repository.GoogleDriveBackupRepository by lazy {
        com.vinaynalavade.expensetracker.data.repository.GoogleDriveBackupRepositoryImpl(
            googleAccountManager,
            googleDriveRestService,
            dataStore,
            backupRepository
        )
    }

    override val getGoogleBackupStateUseCase: com.vinaynalavade.expensetracker.domain.usecase.GetGoogleBackupStateUseCase by lazy {
        com.vinaynalavade.expensetracker.domain.usecase.GetGoogleBackupStateUseCase(googleDriveBackupRepository)
    }

    override val performGoogleDriveBackupUseCase: com.vinaynalavade.expensetracker.domain.usecase.PerformGoogleDriveBackupUseCase by lazy {
        com.vinaynalavade.expensetracker.domain.usecase.PerformGoogleDriveBackupUseCase(backupRepository, googleDriveBackupRepository)
    }

    override val prepareGoogleDriveRestoreUseCase: com.vinaynalavade.expensetracker.domain.usecase.PrepareGoogleDriveRestoreUseCase by lazy {
        com.vinaynalavade.expensetracker.domain.usecase.PrepareGoogleDriveRestoreUseCase(googleDriveBackupRepository)
    }

    override val disconnectGoogleAccountUseCase: com.vinaynalavade.expensetracker.domain.usecase.DisconnectGoogleAccountUseCase by lazy {
        com.vinaynalavade.expensetracker.domain.usecase.DisconnectGoogleAccountUseCase(googleDriveBackupRepository)
    }

    override val saveConnectedGoogleAccountUseCase: com.vinaynalavade.expensetracker.domain.usecase.SaveConnectedGoogleAccountUseCase by lazy {
        com.vinaynalavade.expensetracker.domain.usecase.SaveConnectedGoogleAccountUseCase(googleDriveBackupRepository)
    }

    override val securePinManager: com.vinaynalavade.expensetracker.core.security.SecurePinManager by lazy {
        com.vinaynalavade.expensetracker.core.security.SecurePinManager(context)
    }

    override val appLockManager: com.vinaynalavade.expensetracker.core.security.AppLockManager by lazy {
        com.vinaynalavade.expensetracker.core.security.AppLockManager()
    }

    override val setAppLockEnabledUseCase: com.vinaynalavade.expensetracker.domain.usecase.SetAppLockEnabledUseCase by lazy {
        com.vinaynalavade.expensetracker.domain.usecase.SetAppLockEnabledUseCase(userPreferencesRepository, appLockManager)
    }

    override val setBiometricEnabledUseCase: com.vinaynalavade.expensetracker.domain.usecase.SetBiometricEnabledUseCase by lazy {
        com.vinaynalavade.expensetracker.domain.usecase.SetBiometricEnabledUseCase(userPreferencesRepository)
    }

    override val setAutoLockDurationUseCase: com.vinaynalavade.expensetracker.domain.usecase.SetAutoLockDurationUseCase by lazy {
        com.vinaynalavade.expensetracker.domain.usecase.SetAutoLockDurationUseCase(userPreferencesRepository)
    }

    override val setHideContentInRecentsUseCase: com.vinaynalavade.expensetracker.domain.usecase.SetHideContentInRecentsUseCase by lazy {
        com.vinaynalavade.expensetracker.domain.usecase.SetHideContentInRecentsUseCase(userPreferencesRepository)
    }

    override val verifyPinUseCase: com.vinaynalavade.expensetracker.domain.usecase.VerifyPinUseCase by lazy {
        com.vinaynalavade.expensetracker.domain.usecase.VerifyPinUseCase(securePinManager, appLockManager)
    }

    override val savePinUseCase: com.vinaynalavade.expensetracker.domain.usecase.SavePinUseCase by lazy {
        com.vinaynalavade.expensetracker.domain.usecase.SavePinUseCase(securePinManager)
    }

    override val changePinUseCase: com.vinaynalavade.expensetracker.domain.usecase.ChangePinUseCase by lazy {
        com.vinaynalavade.expensetracker.domain.usecase.ChangePinUseCase(securePinManager)
    }

    override val disableAppLockUseCase: com.vinaynalavade.expensetracker.domain.usecase.DisableAppLockUseCase by lazy {
        com.vinaynalavade.expensetracker.domain.usecase.DisableAppLockUseCase(securePinManager, userPreferencesRepository, appLockManager)
    }

    override val notificationStateRepository: com.vinaynalavade.expensetracker.domain.repository.NotificationStateRepository by lazy {
        com.vinaynalavade.expensetracker.data.repository.NotificationStateRepositoryImpl(context)
    }

    override val checkBudgetThresholdsUseCase: com.vinaynalavade.expensetracker.domain.usecase.CheckBudgetThresholdsUseCase by lazy {
        com.vinaynalavade.expensetracker.domain.usecase.CheckBudgetThresholdsUseCase(
            transactionRepository,
            userPreferencesRepository,
            notificationStateRepository,
            context
        )
    }

    override val checkUpcomingRecurringPaymentsUseCase: com.vinaynalavade.expensetracker.domain.usecase.CheckUpcomingRecurringPaymentsUseCase by lazy {
        com.vinaynalavade.expensetracker.domain.usecase.CheckUpcomingRecurringPaymentsUseCase(
            recurringTransactionRepository,
            userPreferencesRepository,
            notificationStateRepository,
            context
        )
    }

    override val processFinancialRemindersUseCase: com.vinaynalavade.expensetracker.domain.usecase.ProcessFinancialRemindersUseCase by lazy {
        com.vinaynalavade.expensetracker.domain.usecase.ProcessFinancialRemindersUseCase(
            processDueRecurringTransactionsUseCase,
            checkBudgetThresholdsUseCase,
            checkUpcomingRecurringPaymentsUseCase,
            notificationStateRepository
        )
    }

    override val rescheduleAllRemindersUseCase: com.vinaynalavade.expensetracker.domain.usecase.RescheduleAllRemindersUseCase by lazy {
        com.vinaynalavade.expensetracker.domain.usecase.RescheduleAllRemindersUseCase(
            userPreferencesRepository,
            dailyReminderScheduler
        )
    }
}
