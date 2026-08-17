package com.arkulz.arkmoney

import com.arkulz.arkmoney.data.Category

fun reorderedCategoryType(categories: List<Category>, categoryId: Long, direction: Int): List<Category> {
    val selected = categories.firstOrNull { it.id == categoryId } ?: return emptyList()
    val typed = categories.filter { it.type == selected.type }.sortedWith(compareBy(Category::sortOrder, Category::id))
    val from = typed.indexOfFirst { it.id == categoryId }
    if (from < 0 || typed.size < 2) return typed
    val to = (from + direction.coerceIn(-1, 1)).coerceIn(0, typed.lastIndex)
    if (from == to) return typed.mapIndexed { index, category -> category.copy(sortOrder = index) }
    return typed.toMutableList().apply { add(to, removeAt(from)) }.mapIndexed { index, category -> category.copy(sortOrder = index) }
}
