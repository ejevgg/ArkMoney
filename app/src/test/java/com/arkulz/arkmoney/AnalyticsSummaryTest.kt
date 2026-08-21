package com.arkulz.arkmoney

import com.arkulz.arkmoney.data.Expense
import com.arkulz.arkmoney.data.TransactionType
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsSummaryTest {
    private fun operation(date: LocalDate, cents: Long, type: TransactionType) = Expense(amountCents = cents, category = "Test", type = type.name, createdAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())

    @Test fun `summary separates income expense and previous period`() {
        val today = LocalDate.of(2026, 8, 20)
        val range = DateRange(today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()))
        val items = listOf(operation(today, 10_000, TransactionType.EXPENSE), operation(today, 25_000, TransactionType.INCOME), operation(range.start.minusDays(1), 5_000, TransactionType.EXPENSE))
        val result = analyticsSummary(items, range, today)
        assertEquals(10_000, result.expenses)
        assertEquals(25_000, result.income)
        assertEquals(15_000, result.net)
        assertEquals(5_000, result.previousExpenses)
        assertEquals(100, result.expenseChangePercent)
        assertEquals(15_500L, result.projectedExpenses)
    }
}
