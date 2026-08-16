package com.arkulz.arkmoney

import com.arkulz.arkmoney.data.Category
import com.arkulz.arkmoney.data.Expense
import java.math.BigDecimal

fun Expense.matchesQuery(query: String, category: Category?): Boolean {
    val normalized = query.trim().lowercase().replace(',', '.')
    if (normalized.isEmpty()) return true
    val rubles = BigDecimal.valueOf(amountCents, 2)
    val amountVariants = listOf(rubles.toPlainString(), rubles.stripTrailingZeros().toPlainString())
    return listOf(title, comment, category?.name.orEmpty(), this.category).any { normalized in it.lowercase() } ||
        amountVariants.any { it.isNotEmpty() && normalized in it }
}
