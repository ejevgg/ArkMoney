package com.arkulz.arkmoney.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

fun storeExpensePhoto(context: Context, source: Uri): String {
    val directory = File(context.filesDir, "expense_photos").apply { mkdirs() }
    val target = File(directory, "${UUID.randomUUID()}.jpg")
    context.contentResolver.openInputStream(source).use { input ->
        requireNotNull(input) { "Не удалось открыть изображение" }
        target.outputStream().use(input::copyTo)
    }
    return target.absolutePath
}
