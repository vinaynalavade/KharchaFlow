package com.vinaynalavade.expensetracker.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vinaynalavade.expensetracker.di.AppContainer
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import com.vinaynalavade.expensetracker.presentation.transactions.TransactionFilter
import com.vinaynalavade.expensetracker.presentation.backup.BackupScreen
import com.vinaynalavade.expensetracker.presentation.backup.BackupViewModel
import com.vinaynalavade.expensetracker.presentation.calendar.CalendarScreen
import com.vinaynalavade.expensetracker.presentation.calendar.CalendarViewModel
import com.vinaynalavade.expensetracker.presentation.categories.CategoriesScreen
import com.vinaynalavade.expensetracker.presentation.categories.CategoriesViewModel
import com.vinaynalavade.expensetracker.presentation.dashboard.DashboardScreen
import com.vinaynalavade.expensetracker.presentation.dashboard.DashboardViewModel
import com.vinaynalavade.expensetracker.presentation.entry.AddTransactionScreen
import com.vinaynalavade.expensetracker.presentation.entry.AddTransactionViewModel
import com.vinaynalavade.expensetracker.presentation.recurring.RecurringTransactionsScreen
import com.vinaynalavade.expensetracker.presentation.recurring.RecurringViewModel
import com.vinaynalavade.expensetracker.presentation.settings.SettingsScreen
import com.vinaynalavade.expensetracker.presentation.settings.SettingsViewModel
import com.vinaynalavade.expensetracker.presentation.statements.StatementsScreen
import com.vinaynalavade.expensetracker.presentation.statements.StatementsViewModel
import com.vinaynalavade.expensetracker.presentation.summary.MonthlySummaryScreen
import com.vinaynalavade.expensetracker.presentation.summary.MonthlySummaryViewModel
import com.vinaynalavade.expensetracker.presentation.theme.Motion
import com.vinaynalavade.expensetracker.presentation.transactions.TransactionsScreen
import com.vinaynalavade.expensetracker.presentation.transactions.TransactionsViewModel
import com.vinaynalavade.expensetracker.presentation.transactions.detail.TransactionDetailScreen
import com.vinaynalavade.expensetracker.presentation.transactions.detail.TransactionDetailViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun NavGraph(
    navController: NavHostController,
    container: AppContainer,
    onOpenQuickAdd: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    onShowUndoSnackbar: (String, () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        enterTransition = {
            fadeIn(animationSpec = tween(Motion.DurationNormal)) +
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(Motion.DurationNormal))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(Motion.DurationFast))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(Motion.DurationNormal))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(Motion.DurationFast)) +
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(Motion.DurationFast))
        },
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            val viewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModel.Factory(
                    container.getFinancialSummaryUseCase,
                    container.getTransactionsUseCase,
                    container.getCategoryAnalysisUseCase
                )
            )
            val userPrefs by container.getUserPreferencesUseCase()
                .collectAsStateWithLifecycle(initialValue = UserPreferences())

            DashboardScreen(
                viewModel = viewModel,
                currency = userPrefs.currency,
                onNavigateToAddExpense = {
                    navController.navigate(Screen.AddExpense.route)
                },
                onNavigateToAddIncome = {
                    navController.navigate(Screen.AddIncome.route)
                },
                onNavigateToTransactions = {
                    navController.navigate(Screen.Transactions.createRoute())
                },
                onNavigateToCategories = {
                    navController.navigate(Screen.Categories.route)
                },
                onNavigateToCategoryTransactions = { month, categoryName, type ->
                    navController.navigate(
                        Screen.Transactions.createRoute(
                            filter = type.name,
                            query = categoryName
                        )
                    )
                },
                onNavigateToTransactionDetail = { id ->
                    navController.navigate(Screen.TransactionDetail.createRoute(id))
                },
                onOpenQuickAdd = onOpenQuickAdd
            )
        }

        composable(
            route = Screen.Transactions.route,
            arguments = listOf(
                navArgument("filter") {
                    type = NavType.StringType
                    defaultValue = "ALL"
                    nullable = true
                },
                navArgument("query") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val filterParam = backStackEntry.arguments?.getString("filter")
            val queryParam = backStackEntry.arguments?.getString("query") ?: ""
            val initialFilter = when (filterParam?.uppercase()) {
                "EXPENSE" -> TransactionFilter.EXPENSE
                "INCOME" -> TransactionFilter.INCOME
                else -> TransactionFilter.ALL
            }

            val viewModel: TransactionsViewModel = viewModel(
                key = "tx_${filterParam}_${queryParam}",
                factory = TransactionsViewModel.Factory(
                    container.getTransactionsUseCase,
                    container.addTransactionUseCase,
                    initialFilter = initialFilter,
                    initialSearchQuery = queryParam
                )
            )
            TransactionsScreen(
                viewModel = viewModel,
                onOpenQuickAdd = onOpenQuickAdd,
                onNavigateToTransactionDetail = { transactionId ->
                    navController.navigate(Screen.TransactionDetail.createRoute(transactionId))
                },
                onNavigateToCalendar = {
                    navController.navigate(Screen.Calendar.route)
                }
            )
        }

        composable(Screen.Calendar.route) {
            val viewModel: CalendarViewModel = viewModel(
                factory = CalendarViewModel.Factory(
                    container.getTransactionsUseCase
                )
            )
            CalendarScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTransactionDetail = { id ->
                    navController.navigate(Screen.TransactionDetail.createRoute(id))
                },
                onOpenAddTransaction = onOpenQuickAdd
            )
        }

        composable(
            route = Screen.TransactionDetail.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
            val viewModel: TransactionDetailViewModel = viewModel(
                factory = TransactionDetailViewModel.Factory(
                    transactionId = transactionId,
                    getTransactionByIdUseCase = container.getTransactionByIdUseCase,
                    deleteTransactionUseCase = container.deleteTransactionUseCase
                )
            )
            TransactionDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->
                    navController.navigate(Screen.EditTransaction.createRoute(id))
                },
                onTransactionDeleted = { deletedTx ->
                    navController.popBackStack()
                    onShowUndoSnackbar("Transaction deleted") {
                        CoroutineScope(Dispatchers.IO).launch {
                            container.addTransactionUseCase(deletedTx)
                        }
                    }
                }
            )
        }

        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
            val viewModel: AddTransactionViewModel = viewModel(
                factory = AddTransactionViewModel.Factory(
                    editTransactionId = transactionId,
                    addTransactionUseCase = container.addTransactionUseCase,
                    updateTransactionUseCase = container.updateTransactionUseCase,
                    getTransactionByIdUseCase = container.getTransactionByIdUseCase,
                    getCategoriesUseCase = container.getCategoriesUseCase
                )
            )
            AddTransactionScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onTransactionSaved = { msg ->
                    navController.popBackStack()
                    onShowSnackbar(msg)
                }
            )
        }

        composable(Screen.Categories.route) {
            val viewModel: CategoriesViewModel = viewModel(
                factory = CategoriesViewModel.Factory(
                    container.getCategoriesUseCase,
                    container.saveCategoryUseCase,
                    container.deleteCategoryUseCase
                )
            )
            CategoriesScreen(viewModel = viewModel)
        }

        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(
                    container.getUserPreferencesUseCase,
                    container.setThemeModeUseCase,
                    container.setCurrencyUseCase,
                    container.setOpeningBalanceUseCase,
                    container.setDailyReminderUseCase,
                    container.dailyReminderScheduler
                )
            )
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                onNavigateToRecurring = { navController.navigate(Screen.RecurringTransactions.route) },
                onNavigateToStatements = { navController.navigate(Screen.Statements.route) },
                onNavigateToMonthlySummary = { navController.navigate(Screen.MonthlySummary.route) },
                onNavigateToBackup = { navController.navigate(Screen.BackupRestore.route) }
            )
        }

        composable(Screen.MonthlySummary.route) {
            val viewModel: MonthlySummaryViewModel = viewModel(
                factory = MonthlySummaryViewModel.Factory(
                    container.getMonthlyLedgerUseCase
                )
            )
            MonthlySummaryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTransactionDetail = { id ->
                    navController.navigate(Screen.TransactionDetail.createRoute(id))
                }
            )
        }

        composable(Screen.RecurringTransactions.route) {
            val viewModel: RecurringViewModel = viewModel(
                factory = RecurringViewModel.Factory(
                    container.getRecurringTransactionsUseCase,
                    container.saveRecurringTransactionUseCase,
                    container.deleteRecurringTransactionUseCase,
                    container.getCategoriesUseCase
                )
            )
            RecurringTransactionsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Statements.route) {
            val viewModel: StatementsViewModel = viewModel(
                factory = StatementsViewModel.Factory(
                    container.generateStatementUseCase
                )
            )
            StatementsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddExpense.route) {
            val viewModel: AddTransactionViewModel = viewModel(
                factory = AddTransactionViewModel.Factory(
                    transactionType = TransactionType.EXPENSE,
                    addTransactionUseCase = container.addTransactionUseCase,
                    updateTransactionUseCase = container.updateTransactionUseCase,
                    getTransactionByIdUseCase = container.getTransactionByIdUseCase,
                    getCategoriesUseCase = container.getCategoriesUseCase
                )
            )
            AddTransactionScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onTransactionSaved = { confirmationMessage ->
                    navController.popBackStack()
                    onShowSnackbar(confirmationMessage)
                }
            )
        }

        composable(Screen.AddIncome.route) {
            val viewModel: AddTransactionViewModel = viewModel(
                factory = AddTransactionViewModel.Factory(
                    transactionType = TransactionType.INCOME,
                    addTransactionUseCase = container.addTransactionUseCase,
                    updateTransactionUseCase = container.updateTransactionUseCase,
                    getTransactionByIdUseCase = container.getTransactionByIdUseCase,
                    getCategoriesUseCase = container.getCategoriesUseCase
                )
            )
            AddTransactionScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onTransactionSaved = { confirmationMessage ->
                    navController.popBackStack()
                    onShowSnackbar(confirmationMessage)
                }
            )
        }

        composable(Screen.BackupRestore.route) {
            val viewModel: BackupViewModel = viewModel(
                factory = BackupViewModel.Factory(
                    backupRepository = container.backupRepository,
                    createBackupUseCase = container.createBackupUseCase,
                    validateBackupUseCase = container.validateBackupUseCase,
                    restoreBackupUseCase = container.restoreBackupUseCase,
                    exportTransactionsUseCase = container.exportTransactionsUseCase,
                    validateImportUseCase = container.validateImportUseCase,
                    importTransactionsUseCase = container.importTransactionsUseCase,
                    getCategoriesUseCase = container.getCategoriesUseCase
                )
            )
            BackupScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
