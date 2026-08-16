package com.arkulz.arkmoney

import com.arkulz.arkmoney.data.Expense
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsLogicTest {
    @Test fun `month is selected from first through last day`() {
        assertEquals(DateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)), AnalyticsPeriod.MONTH.range(LocalDate.of(2026, 2, 12)))
    }

    @Test fun `quarter starts at calendar quarter boundary`() {
        assertEquals(DateRange(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30)), AnalyticsPeriod.QUARTER.range(LocalDate.of(2026, 5, 20)))
    }

    @Test fun `range filtering includes both boundary dates`() {
        val start = LocalDate.of(2026, 8, 1)
        val expenses = listOf(expenseOn(start), expenseOn(start.plusDays(1)), expenseOn(start.plusDays(2)))
        assertEquals(2, expenses.inRange(DateRange(start, start.plusDays(1))).size)
    }

    @Test fun `daily totals include missing days as zero`() {
        val start = LocalDate.of(2026, 8, 1)
        val values = listOf(expenseOn(start, 100), expenseOn(start, 250), expenseOn(start.plusDays(2), 500)).dailyTotals(listOf(start, start.plusDays(1), start.plusDays(2)))
        assertEquals(listOf(350L, 0L, 500L), values)
    }

    @Test fun `week begins on monday and ends on sunday`() {
        assertEquals(DateRange(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 16)), AnalyticsPeriod.WEEK.range(LocalDate.of(2026, 8, 16)))
    }

    @Test fun `period navigation preserves calendar units`() {
        assertEquals(LocalDate.of(2026, 7, 16), AnalyticsPeriod.MONTH.shift(LocalDate.of(2026, 8, 16), -1))
        assertEquals(LocalDate.of(2027, 8, 16), AnalyticsPeriod.YEAR.shift(LocalDate.of(2026, 8, 16), 1))
    }

    private fun expenseOn(date: LocalDate, cents: Long = 100) = Expense(amountCents = cents, category = "Тест", createdAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
}
