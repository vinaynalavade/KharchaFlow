package com.vinaynalavade.expensetracker.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.vinaynalavade.expensetracker.R

sealed class Screen(
    val route: String,
    @StringRes val titleResId: Int = R.string.app_name,
    val icon: ImageVector? = null
) {
    data object Dashboard : Screen("dashboard", R.string.nav_dashboard, Icons.Default.Dashboard)
    data object Transactions : Screen("transactions?filter={filter}&query={query}", R.string.nav_transactions, Icons.AutoMirrored.Filled.ReceiptLong) {
        fun createRoute(filter: String? = null, query: String? = null): String {
            val f = filter ?: "ALL"
            val q = query ?: ""
            return "transactions?filter=$f&query=$q"
        }
    }
    data object Analytics : Screen("monthly_summary", R.string.nav_analytics, Icons.Default.PieChart)
    data object Categories : Screen("categories", R.string.nav_categories, Icons.Default.Category)
    data object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)

    data object Welcome : Screen("welcome")
    data object About : Screen("about")

    data object AddExpense : Screen("add_expense")
    data object AddIncome : Screen("add_income")

    data object TransactionDetail : Screen("transaction_detail/{transactionId}") {
        fun createRoute(transactionId: Long) = "transaction_detail/$transactionId"
    }

    data object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: Long) = "edit_transaction/$transactionId"
    }

    data object MonthlySummary : Screen("monthly_summary")
    data object RecurringTransactions : Screen("recurring_transactions")
    data object Statements : Screen("statements")
    data object Calendar : Screen("calendar")
    data object BackupRestore : Screen("backup_restore")
    data object AppLockSetup : Screen("app_lock_setup")
    data object ChangePin : Screen("change_pin")
}
