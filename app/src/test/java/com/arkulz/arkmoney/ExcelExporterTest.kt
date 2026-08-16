package com.arkulz.arkmoney

import com.arkulz.arkmoney.data.Account
import com.arkulz.arkmoney.data.Category
import com.arkulz.arkmoney.data.ExcelExporter
import com.arkulz.arkmoney.data.ExcelImporter
import com.arkulz.arkmoney.data.Expense
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class ExcelExporterTest {
    @Test fun `exports a valid xlsx package with expense and account sheets`() {
        val output = ByteArrayOutputStream()
        ExcelExporter.write(output, listOf(Expense(amountCents = 12550, category = "Кафе", categoryId = 2)), listOf(Category(2, "Кафе", "☕")), listOf(Account(1, "Основной", 100000)))
        val entries = mutableSetOf<String>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) { entries += entry.name; entry = zip.nextEntry }
        }
        assertTrue("xl/worksheets/sheet1.xml" in entries)
        assertTrue("xl/worksheets/sheet2.xml" in entries)
        assertTrue("xl/workbook.xml" in entries)
    }

    @Test fun `exported workbook imports without losing core fields`() {
        val output = ByteArrayOutputStream()
        val expense = Expense(amountCents = 12550, category = "Кафе", categoryId = 2, accountId = 1, title = "Обед", comment = "Встреча")
        ExcelExporter.write(output, listOf(expense), listOf(Category(2, "Кафе", "☕")), listOf(Account(1, "Основной", 100000)))
        val imported = ExcelImporter.read(ByteArrayInputStream(output.toByteArray()))
        assertEquals(1, imported.expenses.size)
        assertEquals(12550L, imported.expenses.single().amountCents)
        assertEquals("Кафе", imported.expenses.single().category)
        assertEquals("Основной", imported.expenses.single().account)
        assertEquals("Встреча", imported.expenses.single().comment)
        assertEquals("Обед", imported.expenses.single().title)
        assertEquals(100000L, imported.accounts.single().openingBalanceCents)
    }

    @Test fun `round trip preserves xml special characters`() {
        val output = ByteArrayOutputStream()
        ExcelExporter.write(output, listOf(Expense(amountCents = 100, category = "Еда & кофе", categoryId = 4, comment = "<утро> & друзья")), listOf(Category(4, "Еда & кофе", "☕")), listOf(Account(1, "Карта & наличные", 500)))
        val imported = ExcelImporter.read(ByteArrayInputStream(output.toByteArray()))
        assertEquals("Еда & кофе", imported.expenses.single().category)
        assertEquals("<утро> & друзья", imported.expenses.single().comment)
    }
}
