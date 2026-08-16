package com.arkulz.arkmoney

import com.arkulz.arkmoney.data.Expense
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class AnalyticsPeriod(val title: String) { WEEK("Неделя"), MONTH("Месяц"), QUARTER("3 месяца"), YEAR("Год"), CUSTOM("Период") }

data class DateRange(val start: LocalDate, val endInclusive: LocalDate) {
    init { require(!endInclusive.isBefore(start)) }
    val title: String get() = if (start.year == endInclusive.year && start.month == endInclusive.month) {
        "${start.dayOfMonth}–${endInclusive.dayOfMonth} ${start.month.russianName()}"
    } else "${start.dayOfMonth} ${start.month.russianShort()} — ${endInclusive.dayOfMonth} ${endInclusive.month.russianShort()}"
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
