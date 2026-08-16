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

data class ImportedExpense(val createdAt: Long, val account: String, val category: String, val amountCents: Long, val title: String, val comment: String)
data class ImportedAccount(val name: String, val openingBalanceCents: Long)
data class ImportedWorkbook(val expenses: List<ImportedExpense>, val accounts: List<ImportedAccount>)

object ExcelImporter {
    fun read(input: InputStream): ImportedWorkbook {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name in setOf("xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml")) entries[entry.name] = zip.readBytes()
                entry = zip.nextEntry
            }
        }
        val expenseRows = entries["xl/worksheets/sheet1.xml"]?.let(::rows) ?: error("В файле нет листа расходов")
        val accountRows = entries["xl/worksheets/sheet2.xml"]?.let(::rows).orEmpty()
        require(expenseRows.firstOrNull()?.take(4) == listOf("Дата", "Счёт", "Категория", "Сумма, ₽")) { "Это не файл ArkMoney" }
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val hasTitleColumn = expenseRows.firstOrNull()?.getOrNull(4) == "Название"
        val expenses = expenseRows.drop(1).filter { it.size >= 4 }.map { row ->
            ImportedExpense(
                createdAt = LocalDateTime.parse(row[0], dateFormat).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                account = row[1].ifBlank { "Основной" },
                category = row[2].ifBlank { "Другое" },
                amountCents = row[3].toBigDecimal().setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact(),
                title = if (hasTitleColumn) row.getOrElse(4) { "" } else "",
                comment = if (hasTitleColumn) row.getOrElse(5) { "" } else row.getOrElse(4) { "" },
            )
        }
        val accounts = accountRows.drop(1).filter { it.size >= 2 && it[0].isNotBlank() }.map { row ->
            ImportedAccount(row[0], BigDecimal(row[1]).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact())
        }
        return ImportedWorkbook(expenses, accounts)
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
