package com.arkulz.arkmoney.data

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

data class ImportedExpense(val createdAt: Long, val account: String, val category: String, val amountCents: Long, val title: String, val comment: String, val type: TransactionType = TransactionType.EXPENSE, val transferAccount: String = "")
data class ImportedAccount(val name: String, val openingBalanceCents: Long)
data class ImportedCategory(val name: String, val emoji: String, val type: TransactionType)
data class ImportedWorkbook(val expenses: List<ImportedExpense>, val accounts: List<ImportedAccount>, val categories: List<ImportedCategory> = emptyList())

object ExcelImporter {
    fun read(input: InputStream): ImportedWorkbook {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name in setOf("xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml", "xl/worksheets/sheet3.xml")) entries[entry.name] = zip.readBytes()
                entry = zip.nextEntry
            }
        }
        val expenseRows = entries["xl/worksheets/sheet1.xml"]?.let(::rows) ?: error("В файле нет листа расходов")
        val accountRows = entries["xl/worksheets/sheet2.xml"]?.let(::rows).orEmpty()
        val header = expenseRows.firstOrNull().orEmpty()
        val legacy = header.take(4) == listOf("Дата", "Счёт", "Категория", "Сумма, ₽")
        val current = header.take(6) == listOf("DateTime", "Type", "Account", "TransferAccount", "Category", "AmountRUB")
        require(legacy || current) { "Это не файл ArkMoney" }
        val legacyDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val currentDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        val hasTitleColumn = legacy && header.getOrNull(4) == "Название"
        val expenses = expenseRows.drop(1).filter { it.size >= 4 }.map { row ->
            ImportedExpense(
                createdAt = LocalDateTime.parse(row[0], if (current) currentDateFormat else legacyDateFormat).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                account = row[if (current) 2 else 1].ifBlank { "Основной" },
                category = row[if (current) 4 else 2].ifBlank { "Другое" },
                amountCents = row[if (current) 5 else 3].toBigDecimal().setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact().also { require(it > 0) { "Сумма должна быть больше нуля" } },
                title = if (current) row.getOrElse(6) { "" } else if (hasTitleColumn) row.getOrElse(4) { "" } else "",
                comment = if (current) row.getOrElse(7) { "" } else if (hasTitleColumn) row.getOrElse(5) { "" } else row.getOrElse(4) { "" },
                type = if (current) runCatching { TransactionType.valueOf(row[1]) }.getOrElse { error("Неизвестный тип операции") } else TransactionType.EXPENSE,
                transferAccount = if (current) row.getOrElse(3) { "" } else "",
            )
        }
        val accounts = accountRows.drop(1).filter { it.size >= 2 && it[0].isNotBlank() }.map { row ->
            ImportedAccount(row[0], BigDecimal(row[1]).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact())
        }
        require(accounts.map { it.name.lowercase() }.distinct().size == accounts.size) { "В файле есть повторяющиеся счета" }
        val categoryRows = entries["xl/worksheets/sheet3.xml"]?.let(::rows).orEmpty()
        val categories = categoryRows.drop(1).filter { it.size >= 3 && it[0].isNotBlank() }.map { row ->
            ImportedCategory(row[0], row[1].ifBlank { "•••" }, runCatching { TransactionType.valueOf(row[2]) }.getOrElse { error("Неизвестный тип категории") })
        }
        expenses.filter { it.type == TransactionType.TRANSFER }.forEach { require(it.transferAccount.isNotBlank() && !it.account.equals(it.transferAccount, true)) { "Некорректный перевод" } }
        return ImportedWorkbook(expenses, accounts, categories)
    }

    private fun rows(bytes: ByteArray): List<List<String>> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isXIncludeAware = false
            isExpandEntityReferences = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        }
        val document = factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
        val rows = document.getElementsByTagNameNS("*", "row")
        return (0 until rows.length).map { rowIndex ->
            val cells = (rows.item(rowIndex) as org.w3c.dom.Element).getElementsByTagNameNS("*", "c")
            val values = mutableMapOf<Int, String>()
            for (index in 0 until cells.length) {
                val cell = cells.item(index) as org.w3c.dom.Element
                val column = cell.getAttribute("r").takeWhile(Char::isLetter).fold(0) { acc, char -> acc * 26 + (char.code - 'A'.code + 1) } - 1
                val inline = cell.getElementsByTagNameNS("*", "t").item(0)?.textContent
                val numeric = cell.getElementsByTagNameNS("*", "v").item(0)?.textContent
                values[column] = inline ?: numeric ?: ""
            }
            (0..(values.keys.maxOrNull() ?: -1)).map { values[it] ?: "" }
        }
    }
}
