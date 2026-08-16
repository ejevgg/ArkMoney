package com.arkulz.arkmoney

import com.arkulz.arkmoney.data.Category
import com.arkulz.arkmoney.data.DemoDataGenerator
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoDataGeneratorTest {
    @Test fun `creates deterministic demo expenses spanning one year`() {
        val categories = listOf(Category(1, "Другое"), Category(2, "Продукты", "🛒"), Category(3, "Кафе", "☕"))
        val end = LocalDate.of(2026, 8, 16)
        val expenses = DemoDataGenerator.expensesForYear(categories, accountId = 9, today = end)
        assertTrue(expenses.size in 365..1095)
        assertTrue(expenses.all { it.isDemo && it.accountId == 9L && it.amountCents > 0 })
        assertEquals(expenses, DemoDataGenerator.expensesForYear(categories, 9, end))
        assertTrue(expenses.any { it.title.isNotBlank() })
        assertTrue(expenses.all { it.photoPath.isBlank() })
    }

    @Test fun `demo data reaches both ends of requested year`() {
        val end = LocalDate.of(2026, 8, 16)
        val expenses = DemoDataGenerator.expensesForYear(listOf(Category(1, "Другое")), 1, end)
        val dates = expenses.map { java.time.Instant.ofEpochMilli(it.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
        assertEquals(end.minusDays(364), dates.minOrNull())
        assertEquals(end, dates.maxOrNull())
    }
}
