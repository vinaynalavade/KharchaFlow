package com.vinaynalavade.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.vinaynalavade.expensetracker.presentation.theme.ButtonShape
import com.vinaynalavade.expensetracker.presentation.theme.ExpenseTrackerTheme
import com.vinaynalavade.expensetracker.presentation.widget.ExpenseTrackerWidgetProvider
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val pendingNavRoute = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val route = intent.getStringExtra(NotificationHelper.EXTRA_START_ROUTE)
        if (route != null) {
            pendingNavRoute.value = route
        }
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

            ExpenseTrackerTheme(
                themeMode = userPreferences.themeMode,
                dynamicColor = userPreferences.useDynamicColors,
                currency = userPreferences.currency
            ) {
                val navController = rememberNavController()

                LaunchedEffect(initialStartRoute, pendingNavRoute.value) {
                    val route = pendingNavRoute.value ?: initialStartRoute
                    if (route == NotificationHelper.ROUTE_ADD_EXPENSE) {
                        navController.navigate(Screen.AddExpense.route)
                        pendingNavRoute.value = null
                    } else if (route == NotificationHelper.ROUTE_ADD_INCOME) {
                        navController.navigate(Screen.AddIncome.route)
                        pendingNavRoute.value = null
                    }
                }

                MainAppScaffold(
                    navController = navController,
                    app = app
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    navController: NavHostController,
    app: ExpenseTrackerApp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val shouldShowBottomBar = BottomNavItems.any { it.route == currentRoute }

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
                        if (route == Screen.Dashboard.route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        } else {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
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
            onOpenQuickAdd = {
                showQuickAddSheet = true
            },
            onShowSnackbar = { message ->
                ExpenseTrackerWidgetProvider.updateAll(context)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short
                    )
                }
            },
            onShowUndoSnackbar = { message, onUndo ->
                ExpenseTrackerWidgetProvider.updateAll(context)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onUndo()
                        ExpenseTrackerWidgetProvider.updateAll(context)
                    }
                }
            },
            modifier = Modifier.padding(
                bottom = if (shouldShowBottomBar) innerPadding.calculateBottomPadding() else innerPadding.calculateBottomPadding()
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
