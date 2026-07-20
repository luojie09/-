package com.secretbase.app.data.wish

import android.util.Log
import com.secretbase.app.data.local.SecretBaseCache
import com.secretbase.app.data.supabase.SupabaseRestClient
import com.secretbase.app.data.supabase.isoInstantToMillis
import com.secretbase.app.data.supabase.nullableIsoDateToMillis
import com.secretbase.app.data.sync.SecretBaseSyncManager
import com.secretbase.app.data.sync.SyncModules
import com.secretbase.app.data.sync.SyncOperations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SupabaseWishRepository(
    private val client: SupabaseRestClient,
    private val cache: SecretBaseCache,
    private val syncManager: SecretBaseSyncManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : WishRepository {
    private val refreshMutex = Mutex()
    private val json = Json { encodeDefaults = true }

    init {
        scope.launch { refreshSafely() }
        scope.launch {
            syncManager.refreshEvents
                .filter { it == SyncModules.WISHES }
                .collect { refreshSafely() }
        }
        scope.launch {
            while (isActive) {
                delay(FALLBACK_REFRESH_INTERVAL_MS)
                refreshSafely()
            }
        }
    }

    override fun observeWishes(): Flow<List<Wish>> = cache.observeWishes()

    override suspend fun addWish(wish: Wish): Result<Unit> = upsertLocally(wish)

    override suspend fun updateWish(wish: Wish): Result<Unit> = upsertLocally(wish)

    override suspend fun deleteWish(wishId: String): Result<Unit> = runCatching {
        val current = cache.wishes()
        check(current.any { it.id == wishId }) { "Wish not found" }
        cache.saveWishes(current.filterNot { it.id == wishId })
        syncManager.enqueue(SyncModules.WISHES, SyncOperations.WISH_DELETE, wishId)
    }

    override suspend fun completeWish(wishId: String, completion: WishCompletion): Result<Unit> = runCatching {
        val current = cache.wishes()
        val wish = current.firstOrNull { it.id == wishId } ?: error("Wish not found")
        val updated = wish.copy(status = WishStatus.REALIZED, completion = completion)
        cache.saveWishes(current.map { if (it.id == wishId) updated else it })
        syncManager.enqueue(
            SyncModules.WISHES,
            SyncOperations.WISH_UPSERT,
            wishId,
            json.encodeToString(updated),
        )
    }

    private suspend fun upsertLocally(wish: Wish): Result<Unit> = runCatching {
        val current = cache.wishes()
        val exists = current.any { it.id == wish.id }
        val updated = if (exists) {
            current.map { if (it.id == wish.id) wish else it }
        } else {
            listOf(wish) + current
        }
        cache.saveWishes(updated)
        syncManager.enqueue(
            SyncModules.WISHES,
            SyncOperations.WISH_UPSERT,
            wish.id,
            json.encodeToString(wish),
        )
    }

    private suspend fun refreshSafely() {
        if (syncManager.hasPending(SyncModules.WISHES)) return
        runCatching { refreshWishes() }
            .onFailure { Log.w(TAG, "Remote refresh failed; keeping Room cache", it) }
    }

    private suspend fun refreshWishes() = refreshMutex.withLock {
        val rows = client.select<WishRow>(
            TABLE_NAME,
            client.and("select=*", client.order("created_at", descending = true)),
        )
        cache.saveWishes(rows.map { it.toDomain() })
    }

    private fun WishRow.toDomain() = Wish(
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
        private const val FALLBACK_REFRESH_INTERVAL_MS = 60_000L
        private const val TAG = "SupabaseWishRepo"
    }
}
