package com.arkulz.arkmoney

import com.arkulz.arkmoney.data.Account
import com.arkulz.arkmoney.data.Expense
import com.arkulz.arkmoney.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialLogicTest {
    @Test fun `balance subtracts only expenses from matching account`() {
        val account = Account(7, "Карта", 100_000)
        val expenses = listOf(Expense(amountCents = 12_000, category = "A", accountId = 7), Expense(amountCents = 50_000, category = "B", accountId = 8))
        assertEquals(88_000L, account.currentBalance(expenses))
    }

    @Test fun `income increases only matching account balance`() {
        val income = Expense(amountCents = 25_000, category = "Зарплата", accountId = 7, type = TransactionType.INCOME.name)
        assertEquals(125_000L, Account(7, "Карта", 100_000).currentBalance(listOf(income)))
        assertEquals(100_000L, Account(8, "Другая", 100_000).currentBalance(listOf(income)))
    }

    @Test fun `transfer decreases source and increases destination without creating money`() {
        val transfer = Expense(amountCents = 30_000, category = "", accountId = 7, transferAccountId = 8, type = TransactionType.TRANSFER.name)
        val source = Account(7, "Карта", 100_000).currentBalance(listOf(transfer))
        val destination = Account(8, "Наличные", 20_000).currentBalance(listOf(transfer))
        assertEquals(70_000L, source)
        assertEquals(50_000L, destination)
        assertEquals(120_000L, source + destination)
    }

    @Test fun `testing unlocks only on tenth version tap`() {
        var count = 0
        repeat(9) { val next = nextVersionTap(count); count = next.first; assertFalse(next.second) }
        val tenth = nextVersionTap(count)
        assertEquals(0, tenth.first)
        assertTrue(tenth.second)
    }
}
