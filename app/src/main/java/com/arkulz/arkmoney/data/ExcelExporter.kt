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
            zip.add("xl/worksheets/sheet3.xml", categorySheet(categories))
        }
    }

    private fun expenseSheet(
        expenses: List<Expense>,
        categories: Map<Long, String>,
        accounts: Map<Long, String>,
    ): String {
        val rows = buildString {
            append(row(1, listOf(textCell("A1", "DateTime", 1), textCell("B1", "Type", 1), textCell("C1", "Account", 1), textCell("D1", "TransferAccount", 1), textCell("E1", "Category", 1), textCell("F1", "AmountRUB", 1), textCell("G1", "Title", 1), textCell("H1", "Description", 1))))
            expenses.forEachIndexed { index, expense ->
                val number = index + 2
                val date = Instant.ofEpochMilli(expense.createdAt).atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
                append(row(number, listOf(
                    textCell("A$number", date),
                    textCell("B$number", expense.transactionType.name),
                    textCell("C$number", accounts[expense.accountId] ?: "Основной"),
                    textCell("D$number", expense.transferAccountId?.let { accounts[it] }.orEmpty()),
                    textCell("E$number", categories[expense.categoryId] ?: expense.category),
                    numberCell("F$number", expense.amountCents / 100.0, 2),
                    textCell("G$number", expense.title),
                    textCell("H$number", expense.comment),
                )))
            }
        }
        return worksheetXml("<cols><col min=\"1\" max=\"1\" width=\"20\" customWidth=\"1\"/><col min=\"2\" max=\"5\" width=\"18\" customWidth=\"1\"/><col min=\"6\" max=\"6\" width=\"14\" customWidth=\"1\"/><col min=\"7\" max=\"8\" width=\"30\" customWidth=\"1\"/></cols>", rows)
    }

    private fun accountSheet(accounts: List<Account>, expenses: List<Expense>): String {
        val rows = buildString {
            append(row(1, listOf(textCell("A1", "Account", 1), textCell("B1", "OpeningBalanceRUB", 1), textCell("C1", "CurrentBalanceRUB", 1))))
            accounts.forEachIndexed { index, account ->
                val number = index + 2
                val current = account.openingBalanceCents + expenses.sumOf { transaction -> when (transaction.transactionType) {
                    TransactionType.EXPENSE -> if (transaction.accountId == account.id) -transaction.amountCents else 0L
                    TransactionType.INCOME -> if (transaction.accountId == account.id) transaction.amountCents else 0L
                    TransactionType.TRANSFER -> if (transaction.accountId == account.id) -transaction.amountCents else if (transaction.transferAccountId == account.id) transaction.amountCents else 0L
                } }
                append(row(number, listOf(
                    textCell("A$number", account.name),
                    numberCell("B$number", account.openingBalanceCents / 100.0, 2),
                    numberCell("C$number", current / 100.0, 2),
                )))
            }
        }
        return worksheetXml("<cols><col min=\"1\" max=\"1\" width=\"22\" customWidth=\"1\"/><col min=\"2\" max=\"4\" width=\"20\" customWidth=\"1\"/></cols>", rows)
    }

    private fun categorySheet(categories: List<Category>): String {
        val rows = buildString {
            append(row(1, listOf(textCell("A1", "Category", 1), textCell("B1", "Emoji", 1), textCell("C1", "Type", 1))))
            categories.forEachIndexed { index, category ->
                val number = index + 2
                append(row(number, listOf(textCell("A$number", category.name), textCell("B$number", category.emoji), textCell("C$number", category.type))))
            }
        }
        return worksheetXml("<cols><col min=\"1\" max=\"1\" width=\"24\" customWidth=\"1\"/><col min=\"2\" max=\"3\" width=\"16\" customWidth=\"1\"/></cols>", rows)
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

    private val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>"""
    private val rootRelationships = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""
    private val workbook = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Transactions" sheetId="1" r:id="rId1"/><sheet name="Accounts" sheetId="2" r:id="rId2"/><sheet name="Categories" sheetId="3" r:id="rId3"/></sheets></workbook>"""
    private val workbookRelationships = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/><Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>"""
    private val styles = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><numFmts count="1"><numFmt numFmtId="164" formatCode="#,##0.00"/></numFmts><fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/><color rgb="FFFFFFFF"/></font></fonts><fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FF315C3B"/><bgColor indexed="64"/></patternFill></fill></fills><borders count="1"><border/></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="3"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFill="1" applyFont="1"/><xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/></cellXfs></styleSheet>"""
}
