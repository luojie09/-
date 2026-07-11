package com.secretbase.app.data.wish

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.secretbase.app.data.supabase.SupabaseImageStorage
import com.secretbase.app.data.supabase.SupabaseRestClient
import com.secretbase.app.data.supabase.isoInstantToMillis
import com.secretbase.app.data.supabase.millisToIsoInstant
import com.secretbase.app.data.supabase.millisToNullableIsoDate
import com.secretbase.app.data.supabase.nullableIsoDateToMillis

class SupabaseWishRepository(
    private val client: SupabaseRestClient,
    private val imageStorage: SupabaseImageStorage? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : WishRepository {

    private val wishes = MutableStateFlow<List<Wish>>(emptyList())
    private val refreshMutex = Mutex()

    init {
        scope.launch {
            runCatching { refreshWishes() }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load wishes from Supabase", error)
                }
        }
        scope.launch {
            while (isActive) {
                delay(REMOTE_REFRESH_INTERVAL_MS)
                runCatching { refreshWishes() }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to refresh wishes from Supabase", error)
                    }
            }
        }
    }

    override fun observeWishes(): Flow<List<Wish>> = wishes.asStateFlow()

    override suspend fun addWish(wish: Wish): Result<Unit> = runCatching {
        val wishWithUploadedImages = wish.withUploadedImages(folder = "wishes/${wish.id}")
        client.insert(TABLE_NAME, wishWithUploadedImages.toRow())
        refreshWishes()
    }

    override suspend fun updateWish(wish: Wish): Result<Unit> = runCatching {
        val wishWithUploadedImages = wish.withUploadedImages(folder = "wishes/${wish.id}")
        client.update(TABLE_NAME, client.eq("id", wish.id), wishWithUploadedImages.toRow())
        refreshWishes()
    }

    override suspend fun deleteWish(wishId: String): Result<Unit> = runCatching {
        client.delete(TABLE_NAME, client.eq("id", wishId))
        refreshWishes()
    }

    override suspend fun completeWish(
        wishId: String,
        completion: WishCompletion,
    ): Result<Unit> = runCatching {
        val wish = wishes.value.firstOrNull { it.id == wishId }
            ?: error("Wish not found")
        val uploadedCompletion = completion.withUploadedImages(folder = "wishes/$wishId/completion")
        val updated = wish.copy(
            status = WishStatus.REALIZED,
            completion = uploadedCompletion,
        )
        client.update(TABLE_NAME, client.eq("id", wishId), updated.toRow())
        refreshWishes()
    }

    private suspend fun refreshWishes() {
        refreshMutex.withLock {
            val rows = client.select<WishRow>(
                table = TABLE_NAME,
                query = client.and("select=*", client.order("created_at", descending = true)),
            )

            wishes.value = rows.map { row -> row.toDomain() }
        }
    }

    private suspend fun Wish.withUploadedImages(folder: String): Wish {
        val uploadedCover = coverImagePath?.let { imagePath ->
            imageStorage?.uploadLocalImageIfNeeded(
                imagePath = imagePath,
                folder = "$folder/cover",
            ) ?: imagePath
        }
        return copy(coverImagePath = uploadedCover)
    }

    private suspend fun WishCompletion.withUploadedImages(folder: String): WishCompletion {
        val uploadedImages = imageStorage?.uploadLocalImages(
            imagePaths = imagePaths,
            folder = folder,
        ) ?: imagePaths
        return copy(imagePaths = uploadedImages)
    }

    private fun Wish.toRow(): WishRow =
        WishRow(
            id = id,
            title = title,
            description = description,
            coverImagePath = coverImagePath,
            plannedDate = millisToNullableIsoDate(plannedDate),
            createdAt = millisToIsoInstant(createdAt),
            status = status.name,
            completionText = completion?.text,
            completionImagePaths = completion?.imagePaths.orEmpty(),
            completedAt = completion?.completedAt?.let(::millisToIsoInstant),
        )

    private fun WishRow.toDomain(): Wish =
        Wish(
            id = id,
            title = title,
            description = description,
            coverImagePath = coverImagePath,
            plannedDate = nullableIsoDateToMillis(plannedDate),
            createdAt = isoInstantToMillis(createdAt),
            status = WishStatus.entries.firstOrNull { it.name == status } ?: WishStatus.UNREALIZED,
            completion = completedAt?.let {
                WishCompletion(
                    text = completionText.orEmpty(),
                    imagePaths = completionImagePaths,
                    completedAt = isoInstantToMillis(it),
                )
            },
        )

    @Serializable
    private data class WishRow(
        val id: String,
        val title: String,
        val description: String,
        @SerialName("cover_image_path") val coverImagePath: String? = null,
        @SerialName("planned_date") val plannedDate: String? = null,
        @SerialName("created_at") val createdAt: String,
        val status: String,
        @SerialName("completion_text") val completionText: String? = null,
        @SerialName("completion_image_paths") val completionImagePaths: List<String> = emptyList(),
        @SerialName("completed_at") val completedAt: String? = null,
    )

    private companion object {
        private const val TABLE_NAME = "wishes"
        private const val REMOTE_REFRESH_INTERVAL_MS = 10_000L
        private const val TAG = "SupabaseWishRepo"
    }
}
