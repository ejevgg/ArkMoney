package com.arkulz.arkmoney

import com.arkulz.arkmoney.data.Category
import com.arkulz.arkmoney.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryOrderingTest {
    @Test fun `moves only inside selected transaction type and normalizes order`() {
        val categories = listOf(
            Category(1, "A", sortOrder = 0), Category(2, "B", sortOrder = 1), Category(3, "C", sortOrder = 2),
            Category(4, "Income", sortOrder = 0, type = TransactionType.INCOME.name),
        )
        val moved = reorderedCategoryType(categories, 2, -1)
        assertEquals(listOf(2L, 1L, 3L), moved.map { it.id })
        assertEquals(listOf(0, 1, 2), moved.map { it.sortOrder })
    }

    @Test fun `does not move beyond edges`() {
        val categories = listOf(Category(1, "A", sortOrder = 0), Category(2, "B", sortOrder = 1))
        assertEquals(listOf(1L, 2L), reorderedCategoryType(categories, 1, -1).map { it.id })
        assertEquals(listOf(1L, 2L), reorderedCategoryType(categories, 2, 1).map { it.id })
    }
}
