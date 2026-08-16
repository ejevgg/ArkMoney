package com.arkulz.arkmoney

import com.arkulz.arkmoney.data.Category
import com.arkulz.arkmoney.data.Expense
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseSearchTest {
    private val expense = Expense(amountCents = 12550, category = "Кафе", title = "Завтрак с Анной", comment = "Капучино")

    @Test fun `finds expense by custom title and description`() {
        assertTrue(expense.matchesQuery("анн", Category(1, "Кафе")))
        assertTrue(expense.matchesQuery("капучино", Category(1, "Кафе")))
    }

    @Test fun `finds expense by amount with comma or whole rubles`() {
        assertTrue(expense.matchesQuery("125,5", null))
        assertTrue(expense.matchesQuery("125,50", null))
        assertTrue(Expense(amountCents = 12000, category = "Дом").matchesQuery("120", null))
    }

    @Test fun `does not match unrelated query`() {
        assertFalse(expense.matchesQuery("такси", Category(1, "Кафе")))
    }
}
