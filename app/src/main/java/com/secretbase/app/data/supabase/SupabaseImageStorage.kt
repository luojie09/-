package com.secretbase.app.data.supabase

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class SupabaseImageStorage(
    context: Context,
    private val client: SupabaseRestClient,
    private val bucket: String = DEFAULT_BUCKET,
) {
    private val appContext = context.applicationContext

    suspend fun uploadLocalImages(
        imagePaths: List<String>,
        folder: String,
    ): List<String> =
        imagePaths.mapIndexed { index, imagePath ->
            uploadLocalImageIfNeeded(
                imagePath = imagePath,
                folder = folder,
                order = index,
            )
        }

    suspend fun uploadLocalImageIfNeeded(
        imagePath: String,
        folder: String,
        order: Int = 0,
    ): String {
        if (!imagePath.isLocalImageReference()) return imagePath

        val uri = Uri.parse(imagePath)
        val contentType = appContext.contentResolver.getType(uri) ?: DEFAULT_CONTENT_TYPE
        val extension = extensionFor(uri, contentType)
        val safeFolder = folder.trim('/').ifBlank { "uploads" }
        val objectPath = "$safeFolder/${System.currentTimeMillis()}-$order-${UUID.randomUUID()}.$extension"
        val bytes = readBytes(uri)

        client.uploadStorageObject(
            bucket = bucket,
            objectPath = objectPath,
            bytes = bytes,
            contentType = contentType,
        )
        return client.publicStorageUrl(bucket, objectPath)
    }

    private suspend fun readBytes(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: throw IOException("无法读取所选图片，请重新选择")
    }

    private fun extensionFor(uri: Uri, contentType: String): String {
        val fromMime = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)
        if (!fromMime.isNullOrBlank()) return fromMime

        val fromPath = uri.lastPathSegment
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
            ?.takeIf { it.length in 2..5 && it.all(Char::isLetterOrDigit) }

        return fromPath ?: DEFAULT_EXTENSION
    }

    private fun String.isLocalImageReference(): Boolean {
        val value = trim()
        return value.startsWith("content://", ignoreCase = true) ||
            value.startsWith("file://", ignoreCase = true)
    }

    private companion object {
        private const val DEFAULT_BUCKET = "secretbase-images"
        private const val DEFAULT_CONTENT_TYPE = "image/jpeg"
        private const val DEFAULT_EXTENSION = "jpg"
    }
}
