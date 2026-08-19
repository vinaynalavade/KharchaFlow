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
import com.vinaynalavade.expensetracker.domain.usecase.GetFinancialSummaryUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetMonthlyLedgerUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetRecurringTransactionsUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetTransactionByIdUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetTransactionsUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetUserPreferencesUseCase
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
    val getMonthlyLedgerUseCase: GetMonthlyLedgerUseCase
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

    override val recurringTransactionRepository: RecurringTransactionRepository by lazy {
        RecurringTransactionRepositoryImpl(database.recurringTransactionDao(), database.transactionDao())
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

    override val getMonthlyLedgerUseCase: GetMonthlyLedgerUseCase by lazy {
        GetMonthlyLedgerUseCase(transactionRepository, userPreferencesRepository)
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
}
