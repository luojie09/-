package com.secretbase.app.data.wish

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.secretbase.app.data.supabase.isoInstantToMillis
import com.secretbase.app.data.supabase.millisToIsoInstant
import com.secretbase.app.data.supabase.millisToNullableIsoDate
import com.secretbase.app.data.supabase.nullableIsoDateToMillis

class SupabaseWishRepository(
    private val client: SupabaseClient,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : WishRepository {

    private val wishes = MutableStateFlow<List<Wish>>(emptyList())

    init {
        scope.launch {
            runCatching { refreshWishes() }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load wishes from Supabase", error)
                }
        }
    }

    override fun observeWishes(): Flow<List<Wish>> = wishes.asStateFlow()

    override suspend fun addWish(wish: Wish): Result<Unit> = runCatching {
        client.from(TABLE_NAME).insert(wish.toRow())
        refreshWishes()
    }

    override suspend fun updateWish(wish: Wish): Result<Unit> = runCatching {
        client.from(TABLE_NAME).update(wish.toRow()) {
            filter {
                eq("id", wish.id)
            }
        }
        refreshWishes()
    }

    override suspend fun deleteWish(wishId: String): Result<Unit> = runCatching {
        client.from(TABLE_NAME).delete {
            filter {
                eq("id", wishId)
            }
        }
        refreshWishes()
    }

    override suspend fun completeWish(
        wishId: String,
        completion: WishCompletion,
    ): Result<Unit> = runCatching {
        val wish = wishes.value.firstOrNull { it.id == wishId }
            ?: error("Wish not found")
        val updated = wish.copy(
            status = WishStatus.REALIZED,
            completion = completion,
        )
        client.from(TABLE_NAME).update(updated.toRow()) {
            filter {
                eq("id", wishId)
            }
        }
        refreshWishes()
    }

    private suspend fun refreshWishes() {
        val rows = client.from(TABLE_NAME)
            .select {
                order("created_at", order = Order.DESCENDING)
            }
            .decodeList<WishRow>()

        wishes.value = rows.map { row -> row.toDomain() }
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
        private const val TAG = "SupabaseWishRepo"
    }
}
