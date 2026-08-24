package com.vinaynalavade.expensetracker

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vinaynalavade.expensetracker.core.notification.NotificationHelper
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import com.vinaynalavade.expensetracker.presentation.components.AppBottomBar
import com.vinaynalavade.expensetracker.presentation.components.BottomNavItems
import com.vinaynalavade.expensetracker.presentation.components.QuickAddBottomSheet
import com.vinaynalavade.expensetracker.presentation.navigation.NavGraph
import com.vinaynalavade.expensetracker.presentation.navigation.Screen
import com.vinaynalavade.expensetracker.presentation.security.AppLockViewModel
import com.vinaynalavade.expensetracker.presentation.security.UnlockScreen
import com.vinaynalavade.expensetracker.presentation.theme.ButtonShape
import com.vinaynalavade.expensetracker.presentation.theme.ExpenseTrackerTheme
import com.vinaynalavade.expensetracker.presentation.widget.WidgetUpdateManager
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val pendingNavRoute = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val route = intent.getStringExtra(NotificationHelper.EXTRA_START_ROUTE)
        if (route != null) {
            pendingNavRoute.value = route
        }
    }

    override fun onStart() {
        super.onStart()
        val app = application as ExpenseTrackerApp
        val prefs = app.container.userPreferencesRepository
        // Note: auto-lock check is evaluated in Compose lifecycle observer with latest preferences
    }

    override fun onStop() {
        super.onStop()
        val app = application as ExpenseTrackerApp
        app.container.appLockManager.onAppBackgrounded()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ExpenseTrackerApp
        val container = app.container

        val initialStartRoute = intent?.getStringExtra(NotificationHelper.EXTRA_START_ROUTE)

        setContent {
            val userPreferences by container.getUserPreferencesUseCase()
                .collectAsStateWithLifecycle(initialValue = UserPreferences())
            val isSessionUnlocked by container.appLockManager.isSessionUnlocked
                .collectAsStateWithLifecycle()

            val isLocked = userPreferences.appLockEnabled && !isSessionUnlocked

            // Apply Window Privacy Flag (FLAG_SECURE)
            LaunchedEffect(userPreferences.appLockEnabled, userPreferences.hideContentInRecents) {
                if (userPreferences.appLockEnabled && userPreferences.hideContentInRecents) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            // Lifecycle Observer for Auto-Lock
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, userPreferences.appLockEnabled, userPreferences.autoLockDurationSeconds) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_START) {
                        container.appLockManager.onAppForegrounded(
                            appLockEnabled = userPreferences.appLockEnabled,
                            autoLockDurationSeconds = userPreferences.autoLockDurationSeconds
                        )
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            ExpenseTrackerTheme(
                themeMode = userPreferences.themeMode,
                dynamicColor = userPreferences.useDynamicColors,
                currency = userPreferences.currency
            ) {
                if (isLocked) {
                    BackHandler {
                        moveTaskToBack(true)
                    }

                    val unlockViewModel: AppLockViewModel = viewModel(
                        factory = AppLockViewModel.Factory(
                            container.getUserPreferencesUseCase,
                            container.appLockManager,
                            container.securePinManager,
                            container.verifyPinUseCase,
                            container.savePinUseCase,
                            container.changePinUseCase,
                            container.setAppLockEnabledUseCase,
                            container.setBiometricEnabledUseCase,
                            container.disableAppLockUseCase
                        )
                    )

                    UnlockScreen(
                        viewModel = unlockViewModel,
                        onUnlockSuccess = {
                            container.appLockManager.unlock()
                        }
                    )
                } else {
                    val navController = rememberNavController()
                    var handledInitialRoute by rememberSaveable { mutableStateOf(false) }

                    LaunchedEffect(initialStartRoute, pendingNavRoute.value) {
                        val route = pendingNavRoute.value ?: if (!handledInitialRoute) initialStartRoute else null
                        if (route != null) {
                            handledInitialRoute = true
                            pendingNavRoute.value = null
                            when (route) {
                                NotificationHelper.ROUTE_ADD_EXPENSE -> {
                                    navController.navigate(Screen.AddExpense.route)
                                }
                                NotificationHelper.ROUTE_ADD_INCOME -> {
                                    navController.navigate(Screen.AddIncome.route)
                                }
                                NotificationHelper.ROUTE_TRANSACTIONS, "transactions" -> {
                                    navController.navigate(Screen.Transactions.createRoute()) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                NotificationHelper.ROUTE_RECURRING -> {
                                    navController.navigate(Screen.RecurringTransactions.route)
                                }
                                NotificationHelper.ROUTE_DASHBOARD -> {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        }
                    }

                    MainAppScaffold(
                        navController = navController,
                        app = app,
                        isFirstLaunch = userPreferences.isFirstLaunch,
                        isAppTourCompleted = userPreferences.isAppTourCompleted
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    navController: NavHostController,
    app: ExpenseTrackerApp,
    isFirstLaunch: Boolean,
    isAppTourCompleted: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val shouldShowBottomBar = BottomNavItems.any { it.route == currentRoute } ||
        currentRoute?.startsWith("transactions") == true ||
        currentRoute == Screen.MonthlySummary.route

    // Quick Add Bottom Sheet State
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showQuickAddSheet by remember { mutableStateOf(false) }

    // Snackbar Host State
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = ButtonShape,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.primary
                )
            }
        },
        bottomBar = {
            if (shouldShowBottomBar) {
                AppBottomBar(
                    currentRoute = currentRoute,
                    onNavigateToRoute = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            container = app.container,
            isFirstLaunch = isFirstLaunch,
            isAppTourCompleted = isAppTourCompleted,
            onOpenQuickAdd = {
                showQuickAddSheet = true
            },
            onShowSnackbar = { message ->
                WidgetUpdateManager.refreshAllWidgets(context)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short
                    )
                }
            },
            onShowUndoSnackbar = { message, onUndo ->
                WidgetUpdateManager.refreshAllWidgets(context)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onUndo()
                        WidgetUpdateManager.refreshAllWidgets(context)
                    }
                }
            },
            modifier = Modifier.padding(
                bottom = innerPadding.calculateBottomPadding()
            )
        )

        if (showQuickAddSheet) {
            QuickAddBottomSheet(
                sheetState = sheetState,
                onDismissRequest = {
                    showQuickAddSheet = false
                },
                onAddExpenseClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showQuickAddSheet = false
                        navController.navigate(Screen.AddExpense.route)
                    }
                },
                onAddIncomeClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showQuickAddSheet = false
                        navController.navigate(Screen.AddIncome.route)
                    }
                }
            )
        }
    }
}
