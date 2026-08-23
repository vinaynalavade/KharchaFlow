package com.vinaynalavade.expensetracker.domain.usecase

import android.content.Context
import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.notification.DailyReminderScheduler
import com.vinaynalavade.expensetracker.core.notification.NotificationHelper
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import com.vinaynalavade.expensetracker.domain.model.BudgetThreshold
import com.vinaynalavade.expensetracker.domain.model.RecurrenceFrequency
import com.vinaynalavade.expensetracker.domain.model.RecurringTransaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.repository.NotificationStateRepository
import com.vinaynalavade.expensetracker.domain.repository.RecurringTransactionRepository
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class CheckBudgetThresholdsUseCase(
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val notificationStateRepository: NotificationStateRepository,
    private val context: Context? = null
) {
    suspend operator fun invoke(currentDate: LocalDate = LocalDate.now()): List<BudgetThreshold> {
        val prefs = userPreferencesRepository.getUserPreferences().firstOrNull() ?: return emptyList()

        if (!prefs.notificationsMasterEnabled || !prefs.budgetAlertsEnabled || prefs.monthlyBudgetLimitSubunits <= 0L) {
            return emptyList()
        }

        val yearMonth = java.time.YearMonth.from(currentDate)
        val monthStart = DateTimeUtils.getStartOfMonthEpoch(yearMonth)
        val monthEnd = DateTimeUtils.getEndOfMonthEpoch(yearMonth)
        val monthKey = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))

        val transactions = transactionRepository.getTransactionsBetween(monthStart, monthEnd).firstOrNull() ?: emptyList()
        val totalExpenseSubunits = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount.subunits }

        val budgetLimitSubunits = prefs.monthlyBudgetLimitSubunits
        val percent = if (budgetLimitSubunits > 0) (totalExpenseSubunits * 100) / budgetLimitSubunits else 0

        val thresholdsToCheck = listOf(
            BudgetThreshold.FIFTY to (percent >= 50),
            BudgetThreshold.SEVENTY_FIVE to (percent >= 75),
            BudgetThreshold.NINETY to (percent >= 90),
            BudgetThreshold.HUNDRED to (percent >= 100 && totalExpenseSubunits == budgetLimitSubunits),
            BudgetThreshold.OVER_BUDGET to (totalExpenseSubunits > budgetLimitSubunits)
        )

        val triggeredNow = mutableListOf<BudgetThreshold>()

        for ((threshold, isCrossed) in thresholdsToCheck) {
            if (isCrossed && !notificationStateRepository.hasBudgetThresholdFired(monthKey, threshold)) {
                notificationStateRepository.markBudgetThresholdFired(monthKey, threshold)
                triggeredNow.add(threshold)

                if (context != null) {
                    NotificationHelper.showBudgetAlertNotification(
                        context = context,
                        threshold = threshold,
                        spentAmount = Amount(totalExpenseSubunits),
                        budgetLimit = Amount(budgetLimitSubunits),
                        currency = prefs.currency
                    )
                }
            }
        }

        return triggeredNow
    }
}

class CheckUpcomingRecurringPaymentsUseCase(
    private val recurringTransactionRepository: RecurringTransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val notificationStateRepository: NotificationStateRepository,
    private val context: Context? = null
) {
    suspend operator fun invoke(currentDate: LocalDate = LocalDate.now()): List<Pair<RecurringTransaction, Int>> {
        val prefs = userPreferencesRepository.getUserPreferences().firstOrNull() ?: return emptyList()

        if (!prefs.notificationsMasterEnabled || !prefs.recurringRemindersEnabled) {
            return emptyList()
        }

        val dateKey = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val allRecurring = recurringTransactionRepository.getRecurringTransactions().firstOrNull() ?: emptyList()
        val activeExpenses = allRecurring.filter { it.isEnabled && it.type == TransactionType.EXPENSE }

        val remindersToSend = mutableListOf<Pair<RecurringTransaction, Int>>()

        for (item in activeExpenses) {
            val nextDueDate = calculateNextDueDate(item, currentDate)
            val daysRemaining = ChronoUnit.DAYS.between(currentDate, nextDueDate).toInt()

            val targetAdvanceDays = item.reminderDaysBefore ?: prefs.recurringReminderAdvanceDays

            if (daysRemaining >= 0 && daysRemaining == targetAdvanceDays) {
                if (!notificationStateRepository.hasRecurringReminderFired(dateKey, item.id)) {
                    notificationStateRepository.markRecurringReminderFired(dateKey, item.id)
                    remindersToSend.add(item to daysRemaining)

                    if (context != null) {
                        NotificationHelper.showRecurringPaymentNotification(
                            context = context,
                            title = item.title,
                            amountString = item.amount.format(prefs.currency),
                            daysRemaining = daysRemaining,
                            recurringId = item.id
                        )
                    }
                }
            }
        }

        return remindersToSend
    }

    private fun calculateNextDueDate(item: RecurringTransaction, fromDate: LocalDate): LocalDate {
        return when (item.frequency) {
            RecurrenceFrequency.DAILY -> fromDate
            RecurrenceFrequency.WEEKLY -> {
                var candidate = fromDate
                while (candidate.dayOfWeek.value != item.dayOfWeek) {
                    candidate = candidate.plusDays(1)
                }
                candidate
            }
            RecurrenceFrequency.MONTHLY -> {
                val thisMonth = java.time.YearMonth.from(fromDate)
                val dayThisMonth = minOf(item.dayOfMonth.coerceIn(1, 31), thisMonth.lengthOfMonth())
                val thisMonthCandidate = thisMonth.atDay(dayThisMonth)
                if (!thisMonthCandidate.isBefore(fromDate)) {
                    thisMonthCandidate
                } else {
                    val nextMonth = thisMonth.plusMonths(1)
                    val dayNextMonth = minOf(item.dayOfMonth.coerceIn(1, 31), nextMonth.lengthOfMonth())
                    nextMonth.atDay(dayNextMonth)
                }
            }
            RecurrenceFrequency.YEARLY -> {
                val thisYearMonth = java.time.YearMonth.of(fromDate.year, fromDate.month)
                val dayThisYear = minOf(item.dayOfMonth.coerceIn(1, 31), thisYearMonth.lengthOfMonth())
                val thisYearCandidate = thisYearMonth.atDay(dayThisYear)
                if (!thisYearCandidate.isBefore(fromDate)) {
                    thisYearCandidate
                } else {
                    val nextYearMonth = thisYearMonth.plusYears(1)
                    val dayNextYear = minOf(item.dayOfMonth.coerceIn(1, 31), nextYearMonth.lengthOfMonth())
                    nextYearMonth.atDay(dayNextYear)
                }
            }
        }
    }
}

class ProcessFinancialRemindersUseCase(
    private val processDueRecurringTransactionsUseCase: ProcessDueRecurringTransactionsUseCase,
    private val checkBudgetThresholdsUseCase: CheckBudgetThresholdsUseCase,
    private val checkUpcomingRecurringPaymentsUseCase: CheckUpcomingRecurringPaymentsUseCase,
    private val notificationStateRepository: NotificationStateRepository
) {
    suspend operator fun invoke(): AppResult<Unit> {
        return try {
            val today = LocalDate.now()
            val monthKey = today.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val dateKey = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

            // 1. Process due recurring transactions / salary
            processDueRecurringTransactionsUseCase()

            // 2. Check budget threshold notifications
            checkBudgetThresholdsUseCase(today)

            // 3. Check upcoming recurring payment alerts
            checkUpcomingRecurringPaymentsUseCase(today)

            // 4. Clean old notification states
            notificationStateRepository.clearOldNotificationState(monthKey, dateKey)

            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(com.vinaynalavade.expensetracker.core.result.AppError.DatabaseError("Failed to process financial reminders.", e))
        }
    }
}

class RescheduleAllRemindersUseCase(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dailyReminderScheduler: DailyReminderScheduler
) {
    suspend operator fun invoke(): AppResult<Unit> {
        return try {
            val prefs = userPreferencesRepository.getUserPreferences().firstOrNull()
            if (prefs != null && prefs.notificationsMasterEnabled && prefs.dailyReminderEnabled) {
                dailyReminderScheduler.schedule(prefs.dailyReminderHour, prefs.dailyReminderMinute)
            } else {
                dailyReminderScheduler.cancel()
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(com.vinaynalavade.expensetracker.core.result.AppError.PreferencesError("Failed to reschedule reminders.", e))
        }
    }
}
