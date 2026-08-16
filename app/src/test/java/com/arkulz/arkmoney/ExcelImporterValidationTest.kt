package com.arkulz.arkmoney

import com.arkulz.arkmoney.data.ExcelImporter
import com.arkulz.arkmoney.data.TransactionType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ExcelImporterValidationTest {
    @Test fun `rejects non zip input`() {
        assertThrows(Exception::class.java) { ExcelImporter.read(ByteArrayInputStream("not xlsx".toByteArray())) }
    }

    @Test fun `rejects workbook without transaction sheet`() {
        assertThrows(IllegalStateException::class.java) { ExcelImporter.read(workbook(accountSheet = accounts())) }
    }

    @Test fun `rejects unknown workbook headers`() {
        assertThrows(IllegalArgumentException::class.java) { ExcelImporter.read(workbook(transactionSheet = sheet(listOf(listOf("Wrong", "Header"))))) }
    }

    @Test fun `rejects zero and negative amounts`() {
        assertThrows(IllegalArgumentException::class.java) { ExcelImporter.read(workbook(transactionSheet = currentTransactions("0"), accountSheet = accounts())) }
        assertThrows(IllegalArgumentException::class.java) { ExcelImporter.read(workbook(transactionSheet = currentTransactions("-1"), accountSheet = accounts())) }
    }

    @Test fun `rejects unknown operation type`() {
        assertThrows(IllegalStateException::class.java) { ExcelImporter.read(workbook(transactionSheet = currentTransactions("1", "UNKNOWN"), accountSheet = accounts())) }
    }

    @Test fun `rejects transfer without destination`() {
        assertThrows(IllegalArgumentException::class.java) { ExcelImporter.read(workbook(transactionSheet = currentTransactions("10", "TRANSFER", ""), accountSheet = accounts())) }
    }

    @Test fun `rejects transfer to same account`() {
        assertThrows(IllegalArgumentException::class.java) { ExcelImporter.read(workbook(transactionSheet = currentTransactions("10", "TRANSFER", "Основной"), accountSheet = accounts())) }
    }

    @Test fun `rejects duplicate accounts ignoring case`() {
        val duplicate = sheet(listOf(listOf("Account", "OpeningBalanceRUB"), listOf("Основной", "1"), listOf("основной", "2")))
        assertThrows(IllegalArgumentException::class.java) { ExcelImporter.read(workbook(transactionSheet = currentTransactions("1"), accountSheet = duplicate)) }
    }

    @Test fun `imports legacy zero point one expense format`() {
        val legacy = sheet(listOf(listOf("Дата", "Счёт", "Категория", "Сумма, ₽", "Название", "Описание"), listOf("2026-08-16 12:34", "Основной", "Кафе", "125.50", "Обед", "Встреча")))
        val imported = ExcelImporter.read(workbook(transactionSheet = legacy, accountSheet = sheet(listOf(listOf("Счёт", "Начальный баланс, ₽"), listOf("Основной", "1000")))))
        assertEquals(TransactionType.EXPENSE, imported.expenses.single().type)
        assertEquals(12_550L, imported.expenses.single().amountCents)
        assertEquals("Обед", imported.expenses.single().title)
    }

    private fun currentTransactions(amount: String, type: String = "EXPENSE", destination: String = "") = sheet(listOf(
        listOf("DateTime", "Type", "Account", "TransferAccount", "Category", "AmountRUB", "Title", "Description"),
        listOf("2026-08-16 12:34:00.000", type, "Основной", destination, "Другое", amount, "", ""),
    ))
    private fun accounts() = sheet(listOf(listOf("Account", "OpeningBalanceRUB"), listOf("Основной", "1000")))
    private fun workbook(transactionSheet: String? = null, accountSheet: String? = null): ByteArrayInputStream {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            transactionSheet?.let { zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml")); zip.write(it.toByteArray()); zip.closeEntry() }
            accountSheet?.let { zip.putNextEntry(ZipEntry("xl/worksheets/sheet2.xml")); zip.write(it.toByteArray()); zip.closeEntry() }
        }
        return ByteArrayInputStream(output.toByteArray())
    }
    private fun sheet(rows: List<List<String>>) = """<?xml version="1.0"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>${rows.mapIndexed { rowIndex, values -> "<row r=\"${rowIndex + 1}\">" + values.mapIndexed { column, value -> "<c r=\"${('A'.code + column).toChar()}${rowIndex + 1}\" t=\"inlineStr\"><is><t>${escape(value)}</t></is></c>" }.joinToString("") + "</row>" }.joinToString("")}</sheetData></worksheet>"""
    private fun escape(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
