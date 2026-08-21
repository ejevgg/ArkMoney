package com.arkulz.arkmoney

import com.arkulz.arkmoney.data.Expense
import com.arkulz.arkmoney.data.transactionType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class AnalyticsPeriod(val title: String) { WEEK("Неделя"), MONTH("Месяц"), QUARTER("3 месяца"), YEAR("Год"), CUSTOM("Период") }

data class DateRange(val start: LocalDate, val endInclusive: LocalDate) {
    init { require(!endInclusive.isBefore(start)) }
    val title: String get() = if (start.year == endInclusive.year && start.month == endInclusive.month) {
        "${start.dayOfMonth}–${endInclusive.dayOfMonth} ${start.month.russianName()}"
    } else "${start.dayOfMonth} ${start.month.russianShort()} — ${endInclusive.dayOfMonth} ${endInclusive.month.russianShort()}"

    fun localizedTitle(locale: Locale): String = if (start.year == endInclusive.year && start.month == endInclusive.month) {
        "${start.dayOfMonth}–${endInclusive.dayOfMonth} ${start.format(DateTimeFormatter.ofPattern("MMMM", locale))}"
    } else "${start.format(DateTimeFormatter.ofPattern("d MMM", locale))} — ${endInclusive.format(DateTimeFormatter.ofPattern("d MMM", locale))}"
}

data class AnalyticsSummary(val expenses: Long, val income: Long, val net: Long, val previousExpenses: Long, val expenseChangePercent: Int?, val projectedExpenses: Long?)

fun analyticsSummary(items: List<Expense>, range: DateRange, today: LocalDate = LocalDate.now()): AnalyticsSummary {
    val selected = items.inRange(range)
    val expenses = selected.filter { it.transactionType == com.arkulz.arkmoney.data.TransactionType.EXPENSE }.sumOf { it.amountCents }
    val income = selected.filter { it.transactionType == com.arkulz.arkmoney.data.TransactionType.INCOME }.sumOf { it.amountCents }
    val days = java.time.temporal.ChronoUnit.DAYS.between(range.start, range.endInclusive) + 1
    val previous = DateRange(range.start.minusDays(days), range.start.minusDays(1))
    val previousExpenses = items.inRange(previous).filter { it.transactionType == com.arkulz.arkmoney.data.TransactionType.EXPENSE }.sumOf { it.amountCents }
    val change = if (previousExpenses > 0) (((expenses - previousExpenses) * 100.0) / previousExpenses).toInt() else null
    val projected = if (range.start == today.withDayOfMonth(1) && range.endInclusive == today.withDayOfMonth(today.lengthOfMonth())) {
        expenses * today.lengthOfMonth() / today.dayOfMonth.coerceAtLeast(1)
    } else null
    return AnalyticsSummary(expenses, income, income - expenses, previousExpenses, change, projected)
}

fun AnalyticsPeriod.range(anchor: LocalDate): DateRange = when (this) {
    AnalyticsPeriod.WEEK -> {
        val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        DateRange(start, start.plusDays(6))
    }
    AnalyticsPeriod.MONTH -> DateRange(anchor.withDayOfMonth(1), anchor.withDayOfMonth(anchor.lengthOfMonth()))
    AnalyticsPeriod.QUARTER -> {
        val firstMonth = ((anchor.monthValue - 1) / 3) * 3 + 1
        val start = LocalDate.of(anchor.year, firstMonth, 1)
        DateRange(start, start.plusMonths(3).minusDays(1))
    }
    AnalyticsPeriod.YEAR -> DateRange(LocalDate.of(anchor.year, 1, 1), LocalDate.of(anchor.year, 12, 31))
    AnalyticsPeriod.CUSTOM -> DateRange(anchor, anchor)
}

fun AnalyticsPeriod.shift(anchor: LocalDate, amount: Long): LocalDate = when (this) {
    AnalyticsPeriod.WEEK -> anchor.plusWeeks(amount)
    AnalyticsPeriod.MONTH -> anchor.plusMonths(amount)
    AnalyticsPeriod.QUARTER -> anchor.plusMonths(amount * 3)
    AnalyticsPeriod.YEAR -> anchor.plusYears(amount)
    AnalyticsPeriod.CUSTOM -> anchor
}

fun List<Expense>.inRange(range: DateRange): List<Expense> = filter {
    val date = it.localDate()
    !date.isBefore(range.start) && !date.isAfter(range.endInclusive)
}

fun List<Expense>.dailyTotals(days: List<LocalDate>): List<Long> {
    val totals = groupBy { it.localDate() }.mapValues { entry -> entry.value.sumOf { it.amountCents } }
    return days.map { totals[it] ?: 0L }
}

private fun java.time.Month.russianName() = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")[value - 1]
private fun java.time.Month.russianShort() = listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")[value - 1]
