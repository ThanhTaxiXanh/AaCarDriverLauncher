package com.cartaxi.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Sao chép ảnh người dùng chọn vào bộ nhớ app (thu nhỏ ≤1600px) để không phụ thuộc quyền URI. */
object ImageStore {
    fun import(context: Context, uri: Uri): String? = try {
        val dir = File(context.filesDir, "widget_images").apply { mkdirs() }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
        if (bmp == null) null else {
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            bmp.recycle()
            file.absolutePath
        }
    } catch (e: Exception) {
        null
    }

    fun delete(paths: List<String>) {
        paths.forEach { runCatching { File(it).delete() } }
    }
}
