package com.arkulz.arkmoney.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.random.Random

object DemoDataGenerator {
    fun expensesForYear(categories: List<Category>, accountId: Long, today: LocalDate = LocalDate.now()): List<Expense> {
        val expenseCategories = categories.filter { it.type == TransactionType.EXPENSE.name }
        val incomeCategories = categories.filter { it.type == TransactionType.INCOME.name }
        if (expenseCategories.isEmpty()) return emptyList()
        val random = Random(240816)
        val preferred = expenseCategories.associateBy { it.name }
        val result = mutableListOf<Expense>()
        repeat(365) { offset ->
            val date = today.minusDays((364 - offset).toLong())
            val count = random.nextInt(1, 4)
            repeat(count) { index ->
                val category = chooseCategory(preferred, expenseCategories, date.dayOfWeek.value, random)
                val range = when (category.name) {
                    "Продукты" -> 350..3200
                    "Кафе" -> 180..1400
                    "Транспорт" -> 80..900
                    "Дом" -> 500..6500
                    "Здоровье" -> 250..4000
                    "Развлечения" -> 300..3500
                    else -> 100..1800
                }
                val rubles = random.nextInt(range.first, range.last + 1)
                val time = LocalTime.of((9 + index * 4 + random.nextInt(4)).coerceAtMost(22), random.nextInt(60))
                result += Expense(
                    amountCents = rubles * 100L,
                    category = category.name,
                    categoryId = category.id,
                    accountId = accountId,
                    isDemo = true,
                    title = if (random.nextInt(100) < 38) demoTitle(category.name, random) else "",
                    createdAt = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                )
            }
            if ((date.dayOfMonth == 10 || date.dayOfMonth == 25) && incomeCategories.isNotEmpty()) {
                val category = incomeCategories[(date.monthValue + date.dayOfMonth) % incomeCategories.size]
                val salary = if (date.dayOfMonth == 10) 75_000_00L else 28_000_00L
                result += Expense(
                    amountCents = salary,
                    category = category.name,
                    categoryId = category.id,
                    accountId = accountId,
                    isDemo = true,
                    title = if (date.dayOfMonth == 10) "Зарплата" else "Аванс",
                    type = TransactionType.INCOME.name,
                    createdAt = date.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                )
            }
        }
        return result
    }

    private fun demoTitle(category: String, random: Random): String {
        val titles = when (category) {
            "Продукты" -> listOf("Продукты на неделю", "Покупки к ужину", "Овощи и фрукты")
            "Кафе" -> listOf("Кофе по дороге", "Обед с коллегами", "Завтрак в кафе")
            "Транспорт" -> listOf("Такси домой", "Проездной", "Поездка в центр")
            "Дом" -> listOf("Товары для дома", "Коммунальные услуги", "Мелкий ремонт")
            "Здоровье" -> listOf("Аптека", "Приём врача", "Витамины")
            "Развлечения" -> listOf("Кино", "Встреча с друзьями", "Билеты на концерт")
            else -> listOf("Небольшая покупка", "Разовые расходы")
        }
        return titles[random.nextInt(titles.size)]
    }

    private fun chooseCategory(
        preferred: Map<String, Category>,
        categories: List<Category>,
        dayOfWeek: Int,
        random: Random,
    ): Category {
        val weighted = buildList {
            repeat(5) { preferred["Продукты"]?.let(::add) }
            repeat(3) { preferred["Транспорт"]?.let(::add) }
            repeat(3) { preferred["Кафе"]?.let(::add) }
            repeat(2) { preferred["Дом"]?.let(::add) }
            preferred["Здоровье"]?.let(::add)
            repeat(if (dayOfWeek >= 6) 4 else 1) { preferred["Развлечения"]?.let(::add) }
            preferred["Другое"]?.let(::add)
        }
        return (weighted.ifEmpty { categories })[random.nextInt(weighted.ifEmpty { categories }.size)]
    }
}
