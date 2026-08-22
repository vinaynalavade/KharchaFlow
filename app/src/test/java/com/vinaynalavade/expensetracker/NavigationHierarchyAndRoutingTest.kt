package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.notification.NotificationHelper
import com.vinaynalavade.expensetracker.presentation.components.BottomNavItems
import com.vinaynalavade.expensetracker.presentation.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationHierarchyAndRoutingTest {

    @Test
    fun testPrimaryDestinationsIncludedInBottomNav() {
        val routes = BottomNavItems.map { it.route }
        assertTrue(routes.contains(Screen.Dashboard.route))
        assertTrue(routes.contains(Screen.Transactions.route))
        assertTrue(routes.contains(Screen.Analytics.route))
        assertTrue(routes.contains(Screen.Settings.route))
        assertEquals(4, BottomNavItems.size)
    }

    @Test
    fun testPrimaryDestinationsHaveIconsAndTitles() {
        BottomNavItems.forEach { screen ->
            assertNotNull("BottomNav item must have an icon: ${screen.route}", screen.icon)
            assertTrue("BottomNav item must have a valid title resource: ${screen.route}", screen.titleResId != 0)
        }
    }

    @Test
    fun testTransactionsRouteGeneration() {
        // Default route
        val defaultRoute = Screen.Transactions.createRoute()
        assertEquals("transactions?filter=ALL&query=", defaultRoute)

        // Custom filter & query
        val customRoute = Screen.Transactions.createRoute(filter = "EXPENSE", query = "Food")
        assertEquals("transactions?filter=EXPENSE&query=Food", customRoute)

        // Query only
        val queryRoute = Screen.Transactions.createRoute(query = "Rent")
        assertEquals("transactions?filter=ALL&query=Rent", queryRoute)

        // Filter only
        val filterRoute = Screen.Transactions.createRoute(filter = "INCOME")
        assertEquals("transactions?filter=INCOME&query=", filterRoute)
    }

    @Test
    fun testTransactionDetailRouteGeneration() {
        val route = Screen.TransactionDetail.createRoute(42L)
        assertEquals("transaction_detail/42", route)
    }

    @Test
    fun testEditTransactionRouteGeneration() {
        val route = Screen.EditTransaction.createRoute(108L)
        assertEquals("edit_transaction/108", route)
    }

    @Test
    fun testNotificationRoutesMatchScreens() {
        assertEquals("add_expense", Screen.AddExpense.route)
        assertEquals("add_income", Screen.AddIncome.route)
        assertEquals("add_expense", NotificationHelper.ROUTE_ADD_EXPENSE)
        assertEquals("add_income", NotificationHelper.ROUTE_ADD_INCOME)
    }

    @Test
    fun testSecondaryScreenRoutes() {
        assertEquals("monthly_summary", Screen.MonthlySummary.route)
        assertEquals("categories", Screen.Categories.route)
        assertEquals("recurring_transactions", Screen.RecurringTransactions.route)
        assertEquals("statements", Screen.Statements.route)
        assertEquals("calendar", Screen.Calendar.route)
        assertEquals("backup_restore", Screen.BackupRestore.route)
        assertEquals("welcome", Screen.Welcome.route)
        assertEquals("about", Screen.About.route)
    }
}
