package com.arkulz.arkmoney

import com.arkulz.arkmoney.data.Account
import com.arkulz.arkmoney.data.Expense
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

    @Test fun `testing unlocks only on tenth version tap`() {
        var count = 0
        repeat(9) { val next = nextVersionTap(count); count = next.first; assertFalse(next.second) }
        val tenth = nextVersionTap(count)
        assertEquals(0, tenth.first)
        assertTrue(tenth.second)
    }
}
