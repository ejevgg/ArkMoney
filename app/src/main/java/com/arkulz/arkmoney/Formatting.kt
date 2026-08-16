package com.arkulz.arkmoney

import com.arkulz.arkmoney.data.Expense
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

internal fun Expense.localDate(): LocalDate = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()

internal fun formatMoney(cents: Long): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("ru-RU"))
    .apply { maximumFractionDigits = if (cents % 100 == 0L) 0 else 2 }
    .format(cents / 100.0)

internal fun parseMoneyCents(value: String): Long? = value.replace(',', '.').toBigDecimalOrNull()
    ?.movePointRight(2)?.toLong()
