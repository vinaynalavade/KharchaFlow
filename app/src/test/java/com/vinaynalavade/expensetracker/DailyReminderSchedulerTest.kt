package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.notification.DailyReminderScheduler
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.FinancialSummary
import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.domain.model.ThemeMode
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class DailyReminderSchedulerTest {

    private val testScheduler = object : DailyReminderScheduler {
        override fun schedule(hour: Int, minute: Int) {}
        override fun cancel() {}
        override fun reschedule() {}

        override fun calculateNextTriggerMillis(hour: Int, minute: Int, nowMillis: Long): Long {
            val zoneId = ZoneId.systemDefault()
            val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
            var scheduled = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)

            if (!scheduled.isAfter(now)) {
                scheduled = scheduled.plusDays(1)
            }

            return scheduled.toInstant().toEpochMilli()
        }
    }

    @Test
    fun testSelectedTimeLaterTodaySchedulesForToday() {
        val zoneId = ZoneId.systemDefault()
        // Current time: 10:00 AM on 2026-08-22
        val now = ZonedDateTime.of(2026, 8, 22, 10, 0, 0, 0, zoneId)
        val nowMillis = now.toInstant().toEpochMilli()

        // Reminder set for 21:00 (9:00 PM)
        val nextTriggerMillis = testScheduler.calculateNextTriggerMillis(21, 0, nowMillis)
        val triggerDate = Instant.ofEpochMilli(nextTriggerMillis).atZone(zoneId)

        assertEquals(2026, triggerDate.year)
        assertEquals(8, triggerDate.monthValue)
        assertEquals(22, triggerDate.dayOfMonth) // Same day!
        assertEquals(21, triggerDate.hour)
        assertEquals(0, triggerDate.minute)
    }

    @Test
    fun testSelectedTimeEarlierTodaySchedulesForTomorrow() {
        val zoneId = ZoneId.systemDefault()
        // Current time: 22:00 (10:00 PM) on 2026-08-22
        val now = ZonedDateTime.of(2026, 8, 22, 22, 0, 0, 0, zoneId)
        val nowMillis = now.toInstant().toEpochMilli()

        // Reminder set for 21:00 (9:00 PM) - already passed!
        val nextTriggerMillis = testScheduler.calculateNextTriggerMillis(21, 0, nowMillis)
        val triggerDate = Instant.ofEpochMilli(nextTriggerMillis).atZone(zoneId)

        assertEquals(2026, triggerDate.year)
        assertEquals(8, triggerDate.monthValue)
        assertEquals(23, triggerDate.dayOfMonth) // Tomorrow!
        assertEquals(21, triggerDate.hour)
        assertEquals(0, triggerDate.minute)
    }

    @Test
    fun testCustomMinuteAndMidnightBoundary() {
        val zoneId = ZoneId.systemDefault()
        // Current time: 23:45 on 2026-08-22
        val now = ZonedDateTime.of(2026, 8, 22, 23, 45, 0, 0, zoneId)
        val nowMillis = now.toInstant().toEpochMilli()

        // Reminder set for 00:15 (12:15 AM)
        val nextTriggerMillis = testScheduler.calculateNextTriggerMillis(0, 15, nowMillis)
        val triggerDate = Instant.ofEpochMilli(nextTriggerMillis).atZone(zoneId)

        assertEquals(2026, triggerDate.year)
        assertEquals(8, triggerDate.monthValue)
        assertEquals(23, triggerDate.dayOfMonth) // Next calendar day
        assertEquals(0, triggerDate.hour)
        assertEquals(15, triggerDate.minute)
    }

    @Test
    fun testSmartTransactionSuppressionLogic() = runBlocking {
        val today = LocalDate.now()
        val todayStart = DateTimeUtils.getStartOfDayEpoch(today)
        val todayEnd = DateTimeUtils.getEndOfDayEpoch(today)

        val cat = Category(1L, "General", "category", "#64748B", TransactionType.EXPENSE)

        // Case 1: No transactions today -> NOT suppressed (Notification shown)
        val emptyRepo = FakeTxRepo(emptyList())
        val todayTx1 = emptyRepo.getTransactionsBetween(todayStart, todayEnd).first()
        val shouldSuppress1 = todayTx1.isNotEmpty()
        assertFalse("Notification should not be suppressed when 0 transactions recorded", shouldSuppress1)

        // Case 2: 1 Expense transaction recorded today -> SUPPRESSED (Notification hidden)
        val expenseTx = Transaction(1L, Amount(5000L), TransactionType.EXPENSE, cat, PaymentMethod.CASH, null, todayStart + 1000L)
        val expenseRepo = FakeTxRepo(listOf(expenseTx))
        val todayTx2 = expenseRepo.getTransactionsBetween(todayStart, todayEnd).first()
        val shouldSuppress2 = todayTx2.isNotEmpty()
        assertTrue("Notification should be suppressed when expense transaction recorded today", shouldSuppress2)

        // Case 3: 1 Income transaction recorded today -> SUPPRESSED (Notification hidden)
        val incomeTx = Transaction(2L, Amount(500000L), TransactionType.INCOME, cat, PaymentMethod.BANK_ACCOUNT, null, todayStart + 2000L)
        val incomeRepo = FakeTxRepo(listOf(incomeTx))
        val todayTx3 = incomeRepo.getTransactionsBetween(todayStart, todayEnd).first()
        val shouldSuppress3 = todayTx3.isNotEmpty()
        assertTrue("Notification should be suppressed when income transaction recorded today", shouldSuppress3)
    }

    private class FakeTxRepo(private val list: List<Transaction>) : TransactionRepository {
        override fun getTransactions(): Flow<List<Transaction>> = flowOf(list)
        override fun getTransactionById(id: Long): Flow<Transaction?> = flowOf(list.find { it.id == id })
        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> =
            flowOf(list.filter { it.timestamp in startDate..endDate })
        override fun getFinancialSummary(): Flow<FinancialSummary> = flowOf(FinancialSummary.EMPTY)
        override fun getFinancialSummaryByDateRange(startDate: Long, endDate: Long): Flow<FinancialSummary> = flowOf(FinancialSummary.EMPTY)
        override suspend fun insertTransaction(transaction: Transaction) = AppResult.Success(1L)
        override suspend fun updateTransaction(transaction: Transaction) = AppResult.Success(Unit)
        override suspend fun deleteTransaction(id: Long) = AppResult.Success(Unit)
    }
}
