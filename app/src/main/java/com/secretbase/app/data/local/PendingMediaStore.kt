package com.secretbase.app.data.local

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PendingMediaStore {
    suspend fun import(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = context.applicationContext.contentResolver
        val extension = resolver.getType(uri)
            ?.let(MimeTypeMap.getSingleton()::getExtensionFromMimeType)
            ?.takeIf { it.isNotBlank() }
            ?: "jpg"
        val directory = File(context.applicationContext.filesDir, "pending-media")
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("无法创建离线图片目录")
        }
        val target = File(directory, "${UUID.randomUUID()}.$extension")
        resolver.openInputStream(uri)?.use { input ->
            target.outputStream().use(input::copyTo)
        } ?: throw IOException("无法读取所选图片")
        Uri.fromFile(target).toString()
    }

    suspend fun importAll(context: Context, uris: List<Uri>): List<String> =
        uris.map { import(context, it) }
}
