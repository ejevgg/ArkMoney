package com.arkulz.arkmoney.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

fun storeExpensePhoto(context: Context, source: Uri): String {
    val target = createExpensePhotoTarget(context)
    context.contentResolver.openInputStream(source).use { input ->
        requireNotNull(input) { "Не удалось открыть изображение" }
        target.outputStream().use(input::copyTo)
    }
    return target.absolutePath
}

fun createExpensePhotoTarget(context: Context): File =
    File(File(context.filesDir, "expense_photos").apply { mkdirs() }, "${UUID.randomUUID()}.jpg")
