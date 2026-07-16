package com.secretbase.app.data.supabase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.UUID

class SupabaseImageStorage(
    context: Context,
    private val client: SupabaseRestClient,
    private val bucket: String = DEFAULT_BUCKET,
    private val pathPrefix: String = "",
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
        val uploadPayload = readUploadPayload(uri)
        val safeFolder = folder.trim('/').ifBlank { "uploads" }
        val safePrefix = pathPrefix.trim('/')
        val objectPath = listOfNotNull(
            safePrefix.takeIf { it.isNotBlank() },
            safeFolder,
            "${System.currentTimeMillis()}-$order-${UUID.randomUUID()}.${uploadPayload.extension}",
        ).joinToString("/")

        client.uploadStorageObject(
            bucket = bucket,
            objectPath = objectPath,
            bytes = uploadPayload.bytes,
            contentType = uploadPayload.contentType,
        )
        return client.publicStorageUrl(bucket, objectPath)
    }

    private suspend fun readUploadPayload(uri: Uri): UploadPayload = withContext(Dispatchers.IO) {
        val sourceContentType = appContext.contentResolver.getType(uri) ?: DEFAULT_CONTENT_TYPE
        if (sourceContentType.equals("image/gif", ignoreCase = true)) {
            return@withContext UploadPayload(
                bytes = readOriginalBytes(uri),
                contentType = sourceContentType,
                extension = extensionFor(uri, sourceContentType),
            )
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return@withContext UploadPayload(
                bytes = readOriginalBytes(uri),
                contentType = sourceContentType,
                extension = extensionFor(uri, sourceContentType),
            )
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        val bitmap = openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        } ?: throw IOException("无法读取所选图片，请重新选择")

        val bytes = bitmap.useAndCompress()
        UploadPayload(
            bytes = bytes,
            contentType = DEFAULT_CONTENT_TYPE,
            extension = DEFAULT_EXTENSION,
        )
    }

    private fun readOriginalBytes(uri: Uri): ByteArray =
        openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: throw IOException("无法读取所选图片，请重新选择")

    private fun openInputStream(uri: Uri) =
        appContext.contentResolver.openInputStream(uri)
            ?: uri.path?.let { path -> FileInputStream(File(path)) }

    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > MAX_IMAGE_DIMENSION_PX || height / sampleSize > MAX_IMAGE_DIMENSION_PX) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun Bitmap.useAndCompress(): ByteArray {
        try {
            var quality = JPEG_QUALITY
            var bytes = compressToJpeg(quality)
            while (bytes.size > MAX_UPLOAD_BYTES && quality > MIN_JPEG_QUALITY) {
                quality -= 8
                bytes = compressToJpeg(quality)
            }
            return bytes
        } finally {
            recycle()
        }
    }

    private fun Bitmap.compressToJpeg(quality: Int): ByteArray =
        ByteArrayOutputStream().use { output ->
            compress(Bitmap.CompressFormat.JPEG, quality, output)
            output.toByteArray()
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
        private const val MAX_UPLOAD_BYTES = 1_200_000
        private const val MAX_IMAGE_DIMENSION_PX = 1600
        private const val JPEG_QUALITY = 86
        private const val MIN_JPEG_QUALITY = 62
    }

    private data class UploadPayload(
        val bytes: ByteArray,
        val contentType: String,
        val extension: String,
    )
}
