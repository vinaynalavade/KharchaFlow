package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.FinancialSummary
import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.domain.model.ThemeMode
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import com.vinaynalavade.expensetracker.domain.repository.CategoryRepository
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository
import com.vinaynalavade.expensetracker.domain.usecase.AddTransactionUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetCategoriesUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetTransactionByIdUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetUserPreferencesUseCase
import com.vinaynalavade.expensetracker.domain.usecase.UpdateTransactionUseCase
import com.vinaynalavade.expensetracker.presentation.entry.AddTransactionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests verifying default Income and Expense financial source preferences,
 * ViewModel state initialization, transaction type switching, manual override preservation,
 * and edit-mode source protection.
 */
class TransactionSourcePreferenceTest {

    private val sampleCategory = Category(
        id = 1L,
        name = "Groceries",
        iconName = "shopping_cart",
        colorHex = "#4CAF50",
        type = TransactionType.EXPENSE,
        isDefault = true
    )

    private val incomeCategory = Category(
        id = 2L,
        name = "Salary",
        iconName = "work",
        colorHex = "#2196F3",
        type = TransactionType.INCOME,
        isDefault = true
    )

    @Test
    fun testDefaultSourcePreferences() {
        val defaultPrefs = UserPreferences()
        assertEquals(PaymentMethod.ACCOUNT, defaultPrefs.defaultIncomeSource)
        assertEquals(PaymentMethod.CASH, defaultPrefs.defaultExpenseSource)

        val updatedPrefs = defaultPrefs.copy(
            defaultIncomeSource = PaymentMethod.CASH,
            defaultExpenseSource = PaymentMethod.ACCOUNT
        )
        assertEquals(PaymentMethod.CASH, updatedPrefs.defaultIncomeSource)
        assertEquals(PaymentMethod.ACCOUNT, updatedPrefs.defaultExpenseSource)
    }

    @Test
    fun testDefaultSourceResolutionHelper() {
        val prefsScenarioA = UserPreferences(
            defaultExpenseSource = PaymentMethod.CASH,
            defaultIncomeSource = PaymentMethod.ACCOUNT
        )
        assertEquals(PaymentMethod.CASH, prefsScenarioA.getDefaultSource(TransactionType.EXPENSE))
        assertEquals(PaymentMethod.ACCOUNT, prefsScenarioA.getDefaultSource(TransactionType.INCOME))

        val prefsScenarioB = UserPreferences(
            defaultExpenseSource = PaymentMethod.ACCOUNT,
            defaultIncomeSource = PaymentMethod.CASH
        )
        assertEquals(PaymentMethod.ACCOUNT, prefsScenarioB.getDefaultSource(TransactionType.EXPENSE))
        assertEquals(PaymentMethod.CASH, prefsScenarioB.getDefaultSource(TransactionType.INCOME))

        val prefsBothAccount = UserPreferences(
            defaultExpenseSource = PaymentMethod.ACCOUNT,
            defaultIncomeSource = PaymentMethod.ACCOUNT
        )
        assertEquals(PaymentMethod.ACCOUNT, prefsBothAccount.getDefaultSource(TransactionType.EXPENSE))
        assertEquals(PaymentMethod.ACCOUNT, prefsBothAccount.getDefaultSource(TransactionType.INCOME))

        val prefsBothCash = UserPreferences(
            defaultExpenseSource = PaymentMethod.CASH,
            defaultIncomeSource = PaymentMethod.CASH
        )
        assertEquals(PaymentMethod.CASH, prefsBothCash.getDefaultSource(TransactionType.EXPENSE))
        assertEquals(PaymentMethod.CASH, prefsBothCash.getDefaultSource(TransactionType.INCOME))
    }

    @Test
    fun testAddTransactionViewModelDefaultExpenseSourceCash() = runBlocking {
        val prefsRepo = FakeUserPreferencesRepository(
            UserPreferences(defaultExpenseSource = PaymentMethod.CASH, defaultIncomeSource = PaymentMethod.ACCOUNT)
        )
        val viewModel = createViewModel(
            transactionType = TransactionType.EXPENSE,
            prefsRepo = prefsRepo
        )
        delay(100)

        assertEquals(PaymentMethod.CASH, viewModel.uiState.value.selectedPaymentMethod)
    }

    @Test
    fun testAddTransactionViewModelDefaultExpenseSourceAccount() = runBlocking {
        val prefsRepo = FakeUserPreferencesRepository(
            UserPreferences(defaultExpenseSource = PaymentMethod.ACCOUNT, defaultIncomeSource = PaymentMethod.CASH)
        )
        val viewModel = createViewModel(
            transactionType = TransactionType.EXPENSE,
            prefsRepo = prefsRepo
        )
        delay(100)

        assertEquals(PaymentMethod.ACCOUNT, viewModel.uiState.value.selectedPaymentMethod)
    }

    @Test
    fun testAddTransactionViewModelDefaultIncomeSourceCash() = runBlocking {
        val prefsRepo = FakeUserPreferencesRepository(
            UserPreferences(defaultExpenseSource = PaymentMethod.ACCOUNT, defaultIncomeSource = PaymentMethod.CASH)
        )
        val viewModel = createViewModel(
            transactionType = TransactionType.INCOME,
            prefsRepo = prefsRepo
        )
        delay(100)

        assertEquals(PaymentMethod.CASH, viewModel.uiState.value.selectedPaymentMethod)
    }

    @Test
    fun testAddTransactionViewModelDefaultIncomeSourceAccount() = runBlocking {
        val prefsRepo = FakeUserPreferencesRepository(
            UserPreferences(defaultExpenseSource = PaymentMethod.CASH, defaultIncomeSource = PaymentMethod.ACCOUNT)
        )
        val viewModel = createViewModel(
            transactionType = TransactionType.INCOME,
            prefsRepo = prefsRepo
        )
        delay(100)

        assertEquals(PaymentMethod.ACCOUNT, viewModel.uiState.value.selectedPaymentMethod)
    }

    @Test
    fun testAddTransactionViewModelTypeSwitchingResolvesDefaults() = runBlocking {
        val prefsRepo = FakeUserPreferencesRepository(
            UserPreferences(defaultExpenseSource = PaymentMethod.CASH, defaultIncomeSource = PaymentMethod.ACCOUNT)
        )
        val viewModel = createViewModel(
            transactionType = TransactionType.EXPENSE,
            prefsRepo = prefsRepo
        )
        delay(100)

        // Initial: Expense -> Cash
        assertEquals(TransactionType.EXPENSE, viewModel.uiState.value.transactionType)
        assertEquals(PaymentMethod.CASH, viewModel.uiState.value.selectedPaymentMethod)

        // Switch to Income -> Account
        viewModel.onTransactionTypeChange(TransactionType.INCOME)
        delay(100)
        assertEquals(TransactionType.INCOME, viewModel.uiState.value.transactionType)
        assertEquals(PaymentMethod.ACCOUNT, viewModel.uiState.value.selectedPaymentMethod)

        // Switch back to Expense -> Cash
        viewModel.onTransactionTypeChange(TransactionType.EXPENSE)
        delay(100)
        assertEquals(TransactionType.EXPENSE, viewModel.uiState.value.transactionType)
        assertEquals(PaymentMethod.CASH, viewModel.uiState.value.selectedPaymentMethod)
    }

    @Test
    fun testAddTransactionViewModelTypeSwitchingReverseDefaults() = runBlocking {
        val prefsRepo = FakeUserPreferencesRepository(
            UserPreferences(defaultExpenseSource = PaymentMethod.ACCOUNT, defaultIncomeSource = PaymentMethod.CASH)
        )
        val viewModel = createViewModel(
            transactionType = TransactionType.EXPENSE,
            prefsRepo = prefsRepo
        )
        delay(100)

        // Initial: Expense -> Account
        assertEquals(TransactionType.EXPENSE, viewModel.uiState.value.transactionType)
        assertEquals(PaymentMethod.ACCOUNT, viewModel.uiState.value.selectedPaymentMethod)

        // Switch to Income -> Cash
        viewModel.onTransactionTypeChange(TransactionType.INCOME)
        delay(100)
        assertEquals(TransactionType.INCOME, viewModel.uiState.value.transactionType)
        assertEquals(PaymentMethod.CASH, viewModel.uiState.value.selectedPaymentMethod)

        // Switch back to Expense -> Account
        viewModel.onTransactionTypeChange(TransactionType.EXPENSE)
        delay(100)
        assertEquals(TransactionType.EXPENSE, viewModel.uiState.value.transactionType)
        assertEquals(PaymentMethod.ACCOUNT, viewModel.uiState.value.selectedPaymentMethod)
    }

    @Test
    fun testAddTransactionViewModelExplicitUserOverridePreserved() = runBlocking {
        val prefsRepo = FakeUserPreferencesRepository(
            UserPreferences(defaultExpenseSource = PaymentMethod.CASH, defaultIncomeSource = PaymentMethod.ACCOUNT)
        )
        val viewModel = createViewModel(
            transactionType = TransactionType.EXPENSE,
            prefsRepo = prefsRepo
        )
        delay(100)

        // Initial: Cash
        assertEquals(PaymentMethod.CASH, viewModel.uiState.value.selectedPaymentMethod)

        // User explicitly selects Account
        viewModel.onPaymentMethodSelect(PaymentMethod.ACCOUNT)
        assertEquals(PaymentMethod.ACCOUNT, viewModel.uiState.value.selectedPaymentMethod)

        // Preference change emitted in background should NOT clobber explicit user choice
        prefsRepo.setDefaultExpenseSource(PaymentMethod.CASH)
        delay(100)
        assertEquals(PaymentMethod.ACCOUNT, viewModel.uiState.value.selectedPaymentMethod)
    }

    @Test
    fun testAddTransactionViewModelEditModePreservesExistingSource() = runBlocking {
        val txRepo = FakeTransactionRepository()
        val existingTx = Transaction(
            id = 42L,
            amount = Amount(25000L),
            type = TransactionType.EXPENSE,
            category = sampleCategory,
            paymentMethod = PaymentMethod.ACCOUNT, // Existing source is ACCOUNT
            note = "Client Dinner",
            timestamp = 1000L
        )
        txRepo.insertTransaction(existingTx)

        // Settings default is CASH
        val prefsRepo = FakeUserPreferencesRepository(
            UserPreferences(defaultExpenseSource = PaymentMethod.CASH, defaultIncomeSource = PaymentMethod.CASH)
        )

        val viewModel = createViewModel(
            transactionType = TransactionType.EXPENSE,
            editTransactionId = 42L,
            txRepo = txRepo,
            prefsRepo = prefsRepo
        )
        delay(100)

        // Verify edit mode retained ACCOUNT despite default being CASH
        assertEquals(true, viewModel.uiState.value.isEditMode)
        assertEquals(PaymentMethod.ACCOUNT, viewModel.uiState.value.selectedPaymentMethod)
    }

    @Test
    fun testLegacyPaymentMethodNormalization() {
        assertEquals(PaymentMethod.ACCOUNT, PaymentMethod.fromString("UPI"))
        assertEquals(PaymentMethod.ACCOUNT, PaymentMethod.fromString("BANK_ACCOUNT"))
        assertEquals(PaymentMethod.ACCOUNT, PaymentMethod.fromString("BANK"))
        assertEquals(PaymentMethod.ACCOUNT, PaymentMethod.fromString("CARD"))
        assertEquals(PaymentMethod.ACCOUNT, PaymentMethod.fromString("NET_BANKING"))
        assertEquals(PaymentMethod.ACCOUNT, PaymentMethod.fromString("account"))
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromString("CASH"))
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromString("cash"))
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromString(null))
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromString("UNKNOWN_VALUE"))
    }

    private fun createViewModel(
        transactionType: TransactionType = TransactionType.EXPENSE,
        editTransactionId: Long? = null,
        txRepo: FakeTransactionRepository = FakeTransactionRepository(),
        catRepo: FakeCategoryRepository = FakeCategoryRepository(mutableListOf(sampleCategory, incomeCategory)),
        prefsRepo: FakeUserPreferencesRepository = FakeUserPreferencesRepository()
    ): AddTransactionViewModel {
        return AddTransactionViewModel(
            transactionType = transactionType,
            editTransactionId = editTransactionId,
            addTransactionUseCase = AddTransactionUseCase(txRepo),
            updateTransactionUseCase = UpdateTransactionUseCase(txRepo),
            getTransactionByIdUseCase = GetTransactionByIdUseCase(txRepo),
            getCategoriesUseCase = GetCategoriesUseCase(catRepo),
            getUserPreferencesUseCase = GetUserPreferencesUseCase(prefsRepo),
            coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
        )
    }

    private class FakeTransactionRepository : TransactionRepository {
        val savedTransactions = mutableListOf<Transaction>()
        private val flow = MutableStateFlow<List<Transaction>>(emptyList())

        override fun getTransactions(): Flow<List<Transaction>> = flow

        override fun getTransactionById(id: Long): Flow<Transaction?> =
            flow.map { list -> list.find { it.id == id } }

        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> =
            flow.map { list -> list.filter { it.timestamp in startDate..endDate } }

        override fun getFinancialSummary(): Flow<FinancialSummary> = flowOf(FinancialSummary.EMPTY)

        override fun getFinancialSummaryByDateRange(startDate: Long, endDate: Long): Flow<FinancialSummary> =
            flowOf(FinancialSummary.EMPTY)

        override suspend fun insertTransaction(transaction: Transaction): AppResult<Long> {
            val id = if (transaction.id == 0L) (savedTransactions.size + 1).toLong() else transaction.id
            val txWithId = transaction.copy(id = id)
            savedTransactions.add(txWithId)
            flow.value = savedTransactions.toList()
            return AppResult.Success(id)
        }

        override suspend fun updateTransaction(transaction: Transaction): AppResult<Unit> {
            val index = savedTransactions.indexOfFirst { it.id == transaction.id }
            if (index != -1) {
                savedTransactions[index] = transaction
            } else {
                savedTransactions.add(transaction)
            }
            flow.value = savedTransactions.toList()
            return AppResult.Success(Unit)
        }

        override suspend fun deleteTransaction(id: Long): AppResult<Unit> {
            savedTransactions.removeAll { it.id == id }
            flow.value = savedTransactions.toList()
            return AppResult.Success(Unit)
        }
    }

    private class FakeCategoryRepository(
        private val list: MutableList<Category> = mutableListOf()
    ) : CategoryRepository {
        private val flow = MutableStateFlow(list.toList())

        override fun getCategories(): Flow<List<Category>> = flow

        override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> =
            flow.map { cats -> cats.filter { it.type == type } }

        override fun getCategoryById(id: Long): Flow<Category?> =
            flow.map { cats -> cats.find { it.id == id } }

        override suspend fun insertCategory(category: Category): AppResult<Long> {
            list.add(category)
            flow.value = list.toList()
            return AppResult.Success(category.id)
        }

        override suspend fun updateCategory(category: Category): AppResult<Unit> {
            val idx = list.indexOfFirst { it.id == category.id }
            if (idx != -1) list[idx] = category
            flow.value = list.toList()
            return AppResult.Success(Unit)
        }

        override suspend fun deleteCategory(id: Long): AppResult<Unit> {
            list.removeAll { it.id == id }
            flow.value = list.toList()
            return AppResult.Success(Unit)
        }
    }

    private class FakeUserPreferencesRepository(
        initialPrefs: UserPreferences = UserPreferences()
    ) : UserPreferencesRepository {
        private val flow = MutableStateFlow(initialPrefs)
        var currentPrefs: UserPreferences
            get() = flow.value
            set(value) {
                flow.value = value
            }

        override fun getUserPreferences(): Flow<UserPreferences> = flow.asStateFlow()

        override suspend fun setThemeMode(themeMode: ThemeMode): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(themeMode = themeMode)
            return AppResult.Success(Unit)
        }

        override suspend fun setCurrencyCode(currencyCode: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setDynamicColors(useDynamicColors: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setFirstLaunchCompleted(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setOpeningBalance(subunits: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setEmiReminders(enabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override fun getLastBackupTimestamp(): Flow<Long?> = flowOf(null)
        override suspend fun setLastBackupTimestamp(timestamp: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setAppLockEnabled(enabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setBiometricEnabled(enabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setAutoLockDurationSeconds(seconds: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setHideContentInRecents(hide: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setNotificationsMasterEnabled(enabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setBudgetAlertsEnabled(enabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setMonthlyBudgetLimit(subunits: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setRecurringRemindersEnabled(enabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setRecurringReminderAdvanceDays(days: Int): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setSavingsGoalNotificationsEnabled(enabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setAppLanguage(languageCode: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setProfileName(name: String?): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(userName = name)
            return AppResult.Success(Unit)
        }
        override suspend fun setProfileImageUri(uri: String?): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(profileImageUri = uri)
            return AppResult.Success(Unit)
        }
        override suspend fun setAutomaticBackupEnabled(enabled: Boolean): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(automaticBackupEnabled = enabled)
            return AppResult.Success(Unit)
        }
        override suspend fun setLastBackupStatus(status: String?): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(lastBackupStatus = status)
            return AppResult.Success(Unit)
        }
        override suspend fun setLastBackupError(error: String?): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(lastBackupError = error)
            return AppResult.Success(Unit)
        }
        override suspend fun setLastDismissedRestoreBackupTimestamp(timestamp: Long?): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(lastDismissedRestoreBackupTimestamp = timestamp)
            return AppResult.Success(Unit)
        }
        override suspend fun setAppTourCompleted(completed: Boolean): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(isAppTourCompleted = completed)
            return AppResult.Success(Unit)
        }

        override suspend fun setDefaultIncomeSource(source: PaymentMethod): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(defaultIncomeSource = source)
            return AppResult.Success(Unit)
        }

        override suspend fun setDefaultExpenseSource(source: PaymentMethod): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(defaultExpenseSource = source)
            return AppResult.Success(Unit)
        }
    }
}
