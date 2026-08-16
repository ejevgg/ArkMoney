package com.arkulz.arkmoney.data

import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExcelExporter {
    fun write(
        output: OutputStream,
        expenses: List<Expense>,
        categories: List<Category>,
        accounts: List<Account>,
    ) {
        val categoryNames = categories.associate { it.id to it.name }
        val accountNames = accounts.associate { it.id to it.name }
        ZipOutputStream(output).use { zip ->
            zip.add("[Content_Types].xml", contentTypes)
            zip.add("_rels/.rels", rootRelationships)
            zip.add("xl/workbook.xml", workbook)
            zip.add("xl/_rels/workbook.xml.rels", workbookRelationships)
            zip.add("xl/styles.xml", styles)
            zip.add("xl/worksheets/sheet1.xml", expenseSheet(expenses, categoryNames, accountNames))
            zip.add("xl/worksheets/sheet2.xml", accountSheet(accounts, expenses))
        }
    }

    private fun expenseSheet(
        expenses: List<Expense>,
        categories: Map<Long, String>,
        accounts: Map<Long, String>,
    ): String {
        val rows = buildString {
            append(row(1, listOf(textCell("A1", "Дата", 1), textCell("B1", "Счёт", 1), textCell("C1", "Категория", 1), textCell("D1", "Сумма, ₽", 1), textCell("E1", "Название", 1), textCell("F1", "Описание", 1))))
            expenses.forEachIndexed { index, expense ->
                val number = index + 2
                val date = Instant.ofEpochMilli(expense.createdAt).atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                append(row(number, listOf(
                    textCell("A$number", date),
                    textCell("B$number", accounts[expense.accountId] ?: "Основной"),
                    textCell("C$number", categories[expense.categoryId] ?: expense.category),
                    numberCell("D$number", expense.amountCents / 100.0, 2),
                    textCell("E$number", expense.title),
                    textCell("F$number", expense.comment),
                )))
            }
        }
        return worksheetXml("<cols><col min=\"1\" max=\"1\" width=\"20\" customWidth=\"1\"/><col min=\"2\" max=\"3\" width=\"18\" customWidth=\"1\"/><col min=\"4\" max=\"4\" width=\"14\" customWidth=\"1\"/><col min=\"5\" max=\"6\" width=\"30\" customWidth=\"1\"/></cols>", rows)
    }

    private fun accountSheet(accounts: List<Account>, expenses: List<Expense>): String {
        val rows = buildString {
            append(row(1, listOf(textCell("A1", "Счёт", 1), textCell("B1", "Начальный баланс, ₽", 1), textCell("C1", "Расходы, ₽", 1), textCell("D1", "Текущий баланс, ₽", 1))))
            accounts.forEachIndexed { index, account ->
                val number = index + 2
                val spent = expenses.filter { it.accountId == account.id }.sumOf { it.amountCents }
                append(row(number, listOf(
                    textCell("A$number", account.name),
                    numberCell("B$number", account.openingBalanceCents / 100.0, 2),
                    numberCell("C$number", spent / 100.0, 2),
                    numberCell("D$number", (account.openingBalanceCents - spent) / 100.0, 2),
                )))
            }
        }
        return worksheetXml("<cols><col min=\"1\" max=\"1\" width=\"22\" customWidth=\"1\"/><col min=\"2\" max=\"4\" width=\"20\" customWidth=\"1\"/></cols>", rows)
    }

    private fun worksheetXml(columns: String, rows: String) =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">$columns<sheetData>$rows</sheetData></worksheet>"""

    private fun row(number: Int, cells: List<String>) = "<row r=\"$number\">${cells.joinToString("")}</row>"
    private fun textCell(reference: String, value: String, style: Int = 0) =
        "<c r=\"$reference\" t=\"inlineStr\" s=\"$style\"><is><t>${xml(value)}</t></is></c>"
    private fun numberCell(reference: String, value: Double, style: Int) =
        "<c r=\"$reference\" s=\"$style\"><v>$value</v></c>"
    private fun xml(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    private fun ZipOutputStream.add(path: String, content: String) {
        putNextEntry(ZipEntry(path)); write(content.toByteArray(Charsets.UTF_8)); closeEntry()
    }

    private val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>"""
    private val rootRelationships = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""
    private val workbook = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Расходы" sheetId="1" r:id="rId1"/><sheet name="Счета" sheetId="2" r:id="rId2"/></sheets></workbook>"""
    private val workbookRelationships = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>"""
    private val styles = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><numFmts count="1"><numFmt numFmtId="164" formatCode="#,##0.00"/></numFmts><fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/><color rgb="FFFFFFFF"/></font></fonts><fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FF315C3B"/><bgColor indexed="64"/></patternFill></fill></fills><borders count="1"><border/></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="3"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFill="1" applyFont="1"/><xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/></cellXfs></styleSheet>"""
}
