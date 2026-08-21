package com.vinaynalavade.expensetracker.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vinaynalavade.expensetracker.core.constants.AppConstants
import com.vinaynalavade.expensetracker.data.local.dao.CategoryDao
import com.vinaynalavade.expensetracker.data.local.dao.RecurringTransactionDao
import com.vinaynalavade.expensetracker.data.local.dao.TransactionDao
import com.vinaynalavade.expensetracker.data.local.entity.CategoryEntity
import com.vinaynalavade.expensetracker.data.local.entity.RecurringTransactionEntity
import com.vinaynalavade.expensetracker.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        RecurringTransactionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ExpenseTrackerDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseTrackerDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recurring_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `amount_subunits` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `category_id` INTEGER NOT NULL,
                        `note` TEXT,
                        `frequency` TEXT NOT NULL,
                        `day_of_month` INTEGER NOT NULL,
                        `day_of_week` INTEGER NOT NULL,
                        `start_date` INTEGER NOT NULL,
                        `end_date` INTEGER,
                        `is_enabled` INTEGER NOT NULL,
                        `is_auto_generated` INTEGER NOT NULL,
                        `reminder_days_before` INTEGER,
                        `last_generated_date` INTEGER,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_category_id` ON `recurring_transactions` (`category_id`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `payment_method` TEXT NOT NULL DEFAULT 'CASH'")
                db.execSQL("ALTER TABLE `recurring_transactions` ADD COLUMN `payment_method` TEXT NOT NULL DEFAULT 'CASH'")
            }
        }

        fun getInstance(context: Context): ExpenseTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseTrackerDatabase::class.java,
                    AppConstants.DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                seedDefaultCategoriesSql(db)
            }
        }

        private fun seedDefaultCategoriesSql(db: SupportSQLiteDatabase) {
            val defaultCategories = listOf(
                // Expense Categories
                Triple("Food & Dining", "restaurant", "#EF4444") to "EXPENSE",
                Triple("Transportation", "directions_car", "#F59E0B") to "EXPENSE",
                Triple("Shopping", "shopping_bag", "#8B5CF6") to "EXPENSE",
                Triple("Bills & Utilities", "receipt_long", "#3B82F6") to "EXPENSE",
                Triple("Entertainment", "movie", "#EC4899") to "EXPENSE",
                Triple("Health & Medical", "medical_services", "#10B981") to "EXPENSE",
                Triple("Education", "school", "#6366F1") to "EXPENSE",
                Triple("Personal Care", "spa", "#14B8A6") to "EXPENSE",
                Triple("Housing & Rent", "home", "#F97316") to "EXPENSE",
                Triple("EMI & Loans", "account_balance", "#DC2626") to "EXPENSE",
                Triple("Subscriptions", "subscriptions", "#7C3AED") to "EXPENSE",
                Triple("Other Expense", "more_horiz", "#64748B") to "EXPENSE",

                // Income Categories
                Triple("Salary", "payments", "#10B981") to "INCOME",
                Triple("Freelance & Consulting", "work", "#3B82F6") to "INCOME",
                Triple("Investments & Dividends", "trending_up", "#8B5CF6") to "INCOME",
                Triple("Rental Income", "real_estate_agent", "#F59E0B") to "INCOME",
                Triple("Business Revenue", "storefront", "#06B6D4") to "INCOME",
                Triple("Gifts & Grants", "card_giftcard", "#EC4899") to "INCOME",
                Triple("Refunds & Cashbacks", "replay", "#14B8A6") to "INCOME",
                Triple("Other Income", "attach_money", "#64748B") to "INCOME"
            )

            for ((item, type) in defaultCategories) {
                val (name, icon, color) = item
                db.execSQL(
                    "INSERT INTO categories (name, icon_name, color_hex, type, is_default) VALUES (?, ?, ?, ?, 1)",
                    arrayOf(name, icon, color, type)
                )
            }
        }
    }
}
