package com.arkulz.arkmoney.data

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupArchive(
    val accounts: List<Account>,
    val categories: List<Category>,
    val expenses: List<Expense>,
    val photos: Map<Long, ByteArray>,
)

object ArkMoneyBackup {
    private const val FORMAT_VERSION = "1"

    fun write(output: OutputStream, accounts: List<Account>, categories: List<Category>, expenses: List<Expense>) {
        val photoFiles = expenses.mapNotNull { expense ->
            expense.photoPath.takeIf(String::isNotBlank)?.let { java.io.File(it) }?.takeIf { it.isFile }?.let { expense.id to it }
        }.toMap()
        ZipOutputStream(output).use { zip ->
            zip.text("manifest.txt", "ArkMoneyBackup\nversion=$FORMAT_VERSION\n")
            zip.text("accounts.tsv", accounts.joinToString("\n") { listOf(it.id, encoded(it.name), it.openingBalanceCents, it.sortOrder, it.isDemo).joinToString("\t") })
            zip.text("categories.tsv", categories.joinToString("\n") { listOf(it.id, encoded(it.name), encoded(it.emoji), it.sortOrder, it.type).joinToString("\t") })
            zip.text("operations.tsv", expenses.joinToString("\n") {
                listOf(it.id, it.amountCents, encoded(it.category), it.categoryId, it.accountId, it.createdAt, it.isDemo, encoded(it.comment), encoded(it.title), it.type, it.transferAccountId ?: "", if (it.id in photoFiles) "photos/${it.id}.jpg" else "").joinToString("\t")
            })
            photoFiles.forEach { (expenseId, file) ->
                zip.putNextEntry(ZipEntry("photos/$expenseId.jpg")); file.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
            }
        }
    }

    fun read(input: InputStream): BackupArchive {
        val entries = mutableMapOf<String, ByteArray>()
        var totalBytes = 0L
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory && !entry.name.contains("..") && !entry.name.startsWith('/')) { "Некорректный путь в архиве" }
                require(entry.name !in entries) { "Повторяющийся файл в архиве" }
                val data = ByteArrayOutputStream()
                zip.copyTo(data)
                require(data.size() <= 15_000_000) { "Слишком большой файл в архиве" }
                totalBytes += data.size(); require(totalBytes <= 300_000_000 && entries.size < 20_000) { "Резервная копия слишком большая" }
                entries[entry.name] = data.toByteArray()
                zip.closeEntry()
            }
        }
        val manifest = entries["manifest.txt"]?.decodeToString() ?: error("Это не резервная копия ArkMoney")
        require(manifest.startsWith("ArkMoneyBackup\n") && manifest.contains("version=$FORMAT_VERSION")) { "Неподдерживаемая версия резервной копии" }
        val accounts = lines(entries, "accounts.tsv").map { fields ->
            require(fields.size == 5); Account(fields[0].toLong(), decoded(fields[1]), fields[2].toLong(), fields[3].toInt(), fields[4].toBooleanStrict())
        }
        val categories = lines(entries, "categories.tsv").map { fields ->
            require(fields.size == 5); Category(fields[0].toLong(), decoded(fields[1]), decoded(fields[2]), fields[3].toInt(), fields[4])
        }
        val photoEntries = mutableMapOf<Long, ByteArray>()
        val expenses = lines(entries, "operations.tsv").map { fields ->
            require(fields.size == 12)
            val id = fields[0].toLong()
            if (fields[11].isNotBlank()) photoEntries[id] = entries[fields[11]] ?: error("В резервной копии отсутствует фотография")
            Expense(
                id = id,
                amountCents = fields[1].toLong(),
                category = decoded(fields[2]),
                categoryId = fields[3].toLong(),
                accountId = fields[4].toLong(),
                createdAt = fields[5].toLong(),
                isDemo = fields[6].toBooleanStrict(),
                comment = decoded(fields[7]),
                title = decoded(fields[8]),
                type = fields[9],
                transferAccountId = fields[10].takeIf(String::isNotBlank)?.toLong(),
            )
        }
        require(accounts.isNotEmpty()) { "В резервной копии нет счетов" }
        require(categories.isNotEmpty()) { "В резервной копии нет категорий" }
        val accountIds = accounts.map { it.id }.toSet(); val categoryIds = categories.map { it.id }.toSet()
        require(accountIds.size == accounts.size && categoryIds.size == categories.size && expenses.map { it.id }.toSet().size == expenses.size) { "Повторяющиеся идентификаторы" }
        require(categories.all { it.type == TransactionType.EXPENSE.name || it.type == TransactionType.INCOME.name }) { "Неизвестный тип категории" }
        require(expenses.all { it.amountCents > 0 && TransactionType.entries.any { type -> type.name == it.type } }) { "Некорректный тип или сумма операции" }
        require(expenses.all { it.accountId in accountIds && (it.transferAccountId == null || it.transferAccountId in accountIds) }) { "Операция ссылается на неизвестный счёт" }
        require(expenses.all { it.transactionType == TransactionType.TRANSFER || it.categoryId in categoryIds }) { "Операция ссылается на неизвестную категорию" }
        require(expenses.filter { it.transactionType == TransactionType.TRANSFER }.all { it.transferAccountId != null && it.transferAccountId != it.accountId }) { "Некорректный перевод" }
        return BackupArchive(accounts, categories, expenses, photoEntries)
    }

    private fun lines(entries: Map<String, ByteArray>, name: String): List<List<String>> = entries[name]?.decodeToString().orEmpty().lineSequence().filter(String::isNotBlank).map { it.split('\t') }.toList()
    private fun encoded(value: String) = Base64.getEncoder().encodeToString(value.toByteArray())
    private fun decoded(value: String) = String(Base64.getDecoder().decode(value))
    private fun ZipOutputStream.text(name: String, value: String) { putNextEntry(ZipEntry(name)); write(value.toByteArray()); closeEntry() }
}
