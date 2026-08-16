package com.arkulz.arkmoney

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arkulz.arkmoney.data.Category
import com.arkulz.arkmoney.data.Expense
import com.arkulz.arkmoney.data.TransactionType
import com.arkulz.arkmoney.data.transactionType
import java.time.Instant
import java.time.ZoneOffset
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val chartColors = listOf(Color(0xFF315C3B), Color(0xFFE08B32), Color(0xFF6274A8), Color(0xFF9A5E8B), Color(0xFF4F8C83), Color(0xFFB15D57))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(modifier: Modifier, expenses: List<Expense>, categories: List<Category>) {
    var period by rememberSaveable { mutableStateOf(AnalyticsPeriod.MONTH) }
    var anchorEpoch by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    val anchor = LocalDate.ofEpochDay(anchorEpoch)
    var customStart by rememberSaveable { mutableLongStateOf(LocalDate.now().withDayOfMonth(1).toEpochDay()) }
    var customEnd by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var showRangePicker by remember { mutableStateOf(false) }
    val range = if (period == AnalyticsPeriod.CUSTOM) DateRange(LocalDate.ofEpochDay(customStart), LocalDate.ofEpochDay(customEnd)) else period.range(anchor)
    val selected = expenses.inRange(range).filter { it.transactionType != TransactionType.TRANSFER }
    val expenseItems = selected.filter { it.transactionType == TransactionType.EXPENSE }
    val incomeItems = selected.filter { it.transactionType == TransactionType.INCOME }
    val total = expenseItems.sumOf { it.amountCents }
    val incomeTotal = incomeItems.sumOf { it.amountCents }
    val byCategory = expenseItems.groupBy { it.categoryId }.mapValues { it.value.sumOf(Expense::amountCents) }.entries.sortedByDescending { it.value }
    val incomeByCategory = incomeItems.groupBy { it.categoryId }.mapValues { it.value.sumOf(Expense::amountCents) }.entries.sortedByDescending { it.value }
    val today = LocalDate.now()
    val lastSevenDays = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val lastSevenValues = expenses.filter { it.transactionType == TransactionType.EXPENSE }.dailyTotals(lastSevenDays)

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Аналитика", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton({ if (period != AnalyticsPeriod.CUSTOM) anchorEpoch = period.shift(anchor, -1).toEpochDay() }) { Text("‹", style = MaterialTheme.typography.headlineMedium) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(range.title, fontWeight = FontWeight.SemiBold)
                    if (period == AnalyticsPeriod.MONTH && range.start.month == today.month && range.start.year == today.year) Text("Текущий месяц", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton({ if (period != AnalyticsPeriod.CUSTOM) anchorEpoch = period.shift(anchor, 1).toEpochDay() }) { Text("›", style = MaterialTheme.typography.headlineMedium) }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalyticsPeriod.entries.forEach { item -> PeriodChip(item.title, item == period) { period = item; anchorEpoch = LocalDate.now().toEpochDay(); if (item == AnalyticsPeriod.CUSTOM) showRangePicker = true } }
            }
        }
        item { CategoryAnalyticsCard("Доходы за период", incomeTotal, incomeByCategory.map { it.key to it.value }, categories) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Расходы за период", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatMoney(total), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    DonutChart(byCategory.map { it.value }, total, Modifier.fillMaxWidth().height(180.dp))
                    if (byCategory.isEmpty()) Text("Нет расходов за выбранный период", Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    byCategory.take(6).forEachIndexed { index, entry ->
                        val category = categories.firstOrNull { it.id == entry.key }
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(chartColors[index % chartColors.size], CircleShape))
                            Text(category?.name ?: "Другое", Modifier.padding(start = 9.dp).weight(1f))
                            Text(formatMoney(entry.value), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Последние 7 дней", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Всего ${formatMoney(lastSevenValues.sum())}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    WeeklyBars(lastSevenValues, Modifier.fillMaxWidth().height(150.dp).padding(top = 16.dp))
                    Row(Modifier.fillMaxWidth()) {
                        lastSevenDays.forEachIndexed { index, date ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(date.format(DateTimeFormatter.ofPattern("EE", Locale.forLanguageTag("ru"))).take(2), style = MaterialTheme.typography.labelSmall)
                                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(compactMoney(lastSevenValues[index]), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
        item {
            val days = java.time.temporal.ChronoUnit.DAYS.between(range.start, range.endInclusive).toInt() + 1
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("В среднем в день", formatMoney(if (days > 0) total / days else 0), Modifier.weight(1f))
                MetricCard("Операций", selected.size.toString(), Modifier.weight(1f))
            }
        }
    }
    if (showRangePicker) {
        val state = androidx.compose.material3.rememberDateRangePickerState(
            initialSelectedStartDateMillis = LocalDate.ofEpochDay(customStart).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            initialSelectedEndDateMillis = LocalDate.ofEpochDay(customEnd).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog({ showRangePicker = false }, confirmButton = { TextButton({
            val start = state.selectedStartDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
            val end = state.selectedEndDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
            if (start != null && end != null) { customStart = start.toEpochDay(); customEnd = end.toEpochDay(); period = AnalyticsPeriod.CUSTOM; showRangePicker = false }
        }) { Text("Выбрать") } }, dismissButton = { TextButton({ showRangePicker = false }) { Text("Отмена") } }) { DateRangePicker(state, title = { Text("Произвольный период", Modifier.padding(16.dp)) }) }
    }
}

@Composable private fun CategoryAnalyticsCard(title: String, total: Long, entries: List<Pair<Long, Long>>, categories: List<Category>) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) {
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatMoney(total), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        DonutChart(entries.map { it.second }, total, Modifier.fillMaxWidth().height(150.dp))
        if (entries.isEmpty()) Text("Нет операций за выбранный период", color = MaterialTheme.colorScheme.onSurfaceVariant)
        entries.take(6).forEachIndexed { index, entry -> Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).background(chartColors[index % chartColors.size], CircleShape)); Text(categories.firstOrNull { it.id == entry.first }?.name ?: "Другое", Modifier.padding(start = 9.dp).weight(1f)); Text(formatMoney(entry.second), fontWeight = FontWeight.Medium) } }
    } }
}

@Composable private fun PeriodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Text(label, Modifier.clip(shape).background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
}

@Composable private fun DonutChart(values: List<Long>, total: Long, modifier: Modifier) { Canvas(modifier) {
    val diameter = size.minDimension * .7f; val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
    if (total == 0L) drawArc(Color.Gray.copy(alpha = .2f), -90f, 360f, false, topLeft, Size(diameter, diameter), style = Stroke(diameter * .14f, cap = StrokeCap.Round))
    else { var start = -90f; values.forEachIndexed { index, value -> val sweep = value.toFloat() / total * 360f; drawArc(chartColors[index % chartColors.size], start, sweep.coerceAtLeast(1f), false, topLeft, Size(diameter, diameter), style = Stroke(diameter * .14f)); start += sweep } }
} }

@Composable private fun WeeklyBars(values: List<Long>, modifier: Modifier) { val color = MaterialTheme.colorScheme.primary; Canvas(modifier) {
    val max = values.maxOrNull()?.coerceAtLeast(1) ?: 1; val slot = size.width / values.size; val width = slot * .48f
    values.forEachIndexed { index, value -> val height = size.height * value / max; drawRoundRect(color, Offset(slot * index + (slot - width) / 2, size.height - height), Size(width, height.coerceAtLeast(3f)), androidx.compose.ui.geometry.CornerRadius(width / 3)) }
} }

@Composable private fun MetricCard(label: String, value: String, modifier: Modifier) { Card(modifier) { Column(Modifier.padding(16.dp)) { Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) } } }

private fun compactMoney(cents: Long): String = when {
    cents >= 100_000_000 -> "${cents / 100_000_000}м"
    cents >= 100_000 -> "${cents / 100_000}к"
    else -> "${cents / 100}"
}
