package com.arkulz.arkmoney

import com.arkulz.arkmoney.data.Account
import com.arkulz.arkmoney.data.ArkMoneyBackup
import com.arkulz.arkmoney.data.Category
import com.arkulz.arkmoney.data.Expense
import com.arkulz.arkmoney.data.TransactionType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ArkMoneyBackupTest {
    @Test fun `round trip preserves financial records and photos`() {
        val photo = kotlin.io.path.createTempFile(suffix = ".jpg").toFile().apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val accounts = listOf(Account(7, "Карта", 125_00, 0))
        val categories = listOf(Category(8, "Кафе & чай", "☕", 0))
        val expenses = listOf(Expense(9, 450_00, "Кафе & чай", 8, 7, photoPath = photo.absolutePath, title = "Обед", createdAt = 123456789))
        val bytes = ByteArrayOutputStream().also { ArkMoneyBackup.write(it, accounts, categories, expenses) }.toByteArray()
        val restored = ArkMoneyBackup.read(ByteArrayInputStream(bytes))
        assertEquals(accounts, restored.accounts)
        assertEquals(categories, restored.categories)
        assertEquals(expenses.map { it.copy(photoPath = "") }, restored.expenses)
        assertArrayEquals(photo.readBytes(), restored.photos[9])
        photo.delete()
    }

    @Test fun `linked transfer survives round trip`() {
        val accounts = listOf(Account(1, "A"), Account(2, "B"))
        val categories = listOf(Category(1, "Другое"))
        val transfer = Expense(3, 100_00, "", 0, 1, type = TransactionType.TRANSFER.name, transferAccountId = 2, title = "Перевод")
        val bytes = ByteArrayOutputStream().also { ArkMoneyBackup.write(it, accounts, categories, listOf(transfer)) }.toByteArray()
        assertEquals(transfer.copy(photoPath = ""), ArkMoneyBackup.read(ByteArrayInputStream(bytes)).expenses.single())
    }

    @Test fun `missing local photo does not corrupt backup`() {
        val operation = Expense(2, 100, "Другое", 1, 1, photoPath = "/missing/photo.jpg")
        val bytes = ByteArrayOutputStream().also { ArkMoneyBackup.write(it, listOf(Account(1, "A")), listOf(Category(1, "Другое")), listOf(operation)) }.toByteArray()
        val restored = ArkMoneyBackup.read(ByteArrayInputStream(bytes))
        assertEquals("", restored.expenses.single().photoPath)
        assertEquals(0, restored.photos.size)
    }

    @Test fun `rejects unknown format`() {
        assertThrows(IllegalStateException::class.java) { ArkMoneyBackup.read(ByteArrayInputStream(byteArrayOf(1, 2, 3))) }
    }

    @Test fun `rejects operation with unknown account`() {
        val bytes = ByteArrayOutputStream().also { output -> ZipOutputStream(output).use { zip ->
            fun add(name: String, text: String) { zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray()); zip.closeEntry() }
            add("manifest.txt", "ArkMoneyBackup\nversion=1\n")
            add("accounts.tsv", "1\tQQ==\t0\t0\tfalse")
            add("categories.tsv", "1\t0JTRgNGD0LPQvtC1\t4oCi4oCi4oCi\t0\tEXPENSE")
            add("operations.tsv", "1\t100\t0JTRgNGD0LPQvtC1\t1\t99\t1\tfalse\t\t\tEXPENSE\t\t")
        } }.toByteArray()
        assertThrows(IllegalArgumentException::class.java) { ArkMoneyBackup.read(ByteArrayInputStream(bytes)) }
    }

    @Test fun `rejects path traversal archive entries`() {
        fun archive(names: List<String>) = ByteArrayOutputStream().also { output -> ZipOutputStream(output).use { zip -> names.forEach { name -> zip.putNextEntry(ZipEntry(name)); zip.write("x".toByteArray()); zip.closeEntry() } } }.toByteArray()
        assertThrows(IllegalArgumentException::class.java) { ArkMoneyBackup.read(ByteArrayInputStream(archive(listOf("../manifest.txt")))) }
    }

    @Test fun `rejects unsupported version and unreferenced files`() {
        fun archive(extra: Boolean) = ByteArrayOutputStream().also { output -> ZipOutputStream(output).use { zip ->
            fun add(name: String, value: String) { zip.putNextEntry(ZipEntry(name)); zip.write(value.toByteArray()); zip.closeEntry() }
            add("manifest.txt", "ArkMoneyBackup\nversion=${if (extra) 1 else 99}\n")
            add("accounts.tsv", "1\tQQ==\t0\t0\tfalse")
            add("categories.tsv", "1\tRA==\t4oCi4oCi4oCi\t0\tEXPENSE")
            add("operations.tsv", "")
            if (extra) add("unexpected.bin", "x")
        } }.toByteArray()
        assertThrows(IllegalArgumentException::class.java) { ArkMoneyBackup.read(ByteArrayInputStream(archive(false))) }
        assertThrows(IllegalArgumentException::class.java) { ArkMoneyBackup.read(ByteArrayInputStream(archive(true))) }
    }
}
