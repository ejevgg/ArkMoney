package com.arkulz.arkmoney.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.content.ContentValues
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

fun storeExpensePhoto(context: Context, source: Uri): String {
    val target = createExpensePhotoTarget(context)
    context.contentResolver.openInputStream(source).use { input ->
        requireNotNull(input) { "Не удалось открыть изображение" }
        target.outputStream().use(input::copyTo)
    }
    compressExpensePhoto(target)
    return target.absolutePath
}

fun compressExpensePhoto(file: File): File {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Некорректное изображение" }
    var sample = 1
    while (bounds.outWidth / sample > 1920 || bounds.outHeight / sample > 1920) sample *= 2
    val bitmap = requireNotNull(BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample }))
    val orientation = runCatching { ExifInterface(file).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
        }
    }
    val oriented = if (!matrix.isIdentity) Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also { if (it !== bitmap) bitmap.recycle() } else bitmap
    var scaled = if (oriented.width > 1920 || oriented.height > 1920) {
        val factor = 1920f / maxOf(oriented.width, oriented.height)
        Bitmap.createScaledBitmap(oriented, (oriented.width * factor).toInt(), (oriented.height * factor).toInt(), true).also { if (it !== oriented) oriented.recycle() }
    } else oriented
    var quality = 86
    var bytes: ByteArray
    do {
        bytes = ByteArrayOutputStream().use { output -> scaled.compress(Bitmap.CompressFormat.JPEG, quality, output); output.toByteArray() }
        quality -= 6
    } while (bytes.size > 240_000 && quality >= 58)
    while (bytes.size > 260_000 && maxOf(scaled.width, scaled.height) > 900) {
        val smaller = Bitmap.createScaledBitmap(scaled, (scaled.width * .84f).toInt(), (scaled.height * .84f).toInt(), true)
        if (smaller !== scaled) scaled.recycle()
        scaled = smaller
        bytes = ByteArrayOutputStream().use { output -> scaled.compress(Bitmap.CompressFormat.JPEG, 72, output); output.toByteArray() }
    }
    file.writeBytes(bytes)
    scaled.recycle()
    return file
}

fun createExpensePhotoTarget(context: Context): File =
    File(File(context.filesDir, "expense_photos").apply { mkdirs() }, "${UUID.randomUUID()}.jpg")

fun saveExpensePhotoToGallery(context: Context, file: File): Boolean {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "ArkMoney-${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ArkMoney")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    return runCatching {
        context.contentResolver.openOutputStream(uri)?.use { output -> file.inputStream().use { it.copyTo(output) } } ?: error("Нет доступа к галерее")
        values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0); context.contentResolver.update(uri, values, null, null)
        true
    }.getOrElse { context.contentResolver.delete(uri, null, null); false }
}
