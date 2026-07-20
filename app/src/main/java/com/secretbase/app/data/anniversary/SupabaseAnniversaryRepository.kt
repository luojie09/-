package com.secretbase.app.data.anniversary

import android.util.Log
import com.secretbase.app.data.local.SecretBaseCache
import com.secretbase.app.data.supabase.SupabaseRestClient
import com.secretbase.app.data.supabase.isoInstantToMillis
import com.secretbase.app.data.sync.SecretBaseSyncManager
import com.secretbase.app.data.sync.SyncModules
import com.secretbase.app.data.sync.SyncOperations
import java.time.LocalDate
import java.time.ZoneId
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

class SupabaseAnniversaryRepository(
    private val client: SupabaseRestClient,
    private val cache: SecretBaseCache,
    private val syncManager: SecretBaseSyncManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : AnniversaryRepository {
    private val refreshMutex = Mutex()
    private val json = Json { encodeDefaults = true }

    init {
        scope.launch { refreshSafely() }
        scope.launch {
            syncManager.refreshEvents
                .filter { it == SyncModules.ANNIVERSARIES }
                .collect { refreshSafely() }
        }
        scope.launch {
            while (isActive) {
                delay(FALLBACK_REFRESH_INTERVAL_MS)
                refreshSafely()
            }
        }
    }

    override fun observeAnniversaries(): Flow<List<Anniversary>> = cache.observeAnniversaries()

    override suspend fun addAnniversary(item: Anniversary): Result<Unit> = upsertLocally(item)

    override suspend fun updateAnniversary(item: Anniversary): Result<Unit> = upsertLocally(item)

    override suspend fun deleteAnniversary(id: String): Result<Unit> = runCatching {
        val current = cache.anniversaries()
        check(current.any { it.id == id }) { "Anniversary not found" }
        cache.saveAnniversaries(current.filterNot { it.id == id })
        syncManager.enqueue(SyncModules.ANNIVERSARIES, SyncOperations.ANNIVERSARY_DELETE, id)
    }

    private suspend fun upsertLocally(item: Anniversary): Result<Unit> = runCatching {
        val current = cache.anniversaries()
        val exists = current.any { it.id == item.id }
        cache.saveAnniversaries(
            if (exists) current.map { if (it.id == item.id) item else it } else current + item,
        )
        syncManager.enqueue(
            SyncModules.ANNIVERSARIES,
            SyncOperations.ANNIVERSARY_UPSERT,
            item.id,
            json.encodeToString(item),
        )
    }

    private suspend fun refreshSafely() {
        if (syncManager.hasPending(SyncModules.ANNIVERSARIES)) return
        runCatching { refreshAnniversaries() }
            .onFailure { Log.w(TAG, "Remote refresh failed; keeping Room cache", it) }
    }

    private suspend fun refreshAnniversaries() = refreshMutex.withLock {
        val rows = client.select<AnniversaryRow>(
            TABLE_NAME,
            client.and("select=*", client.order("date")),
        )
        cache.saveAnniversaries(rows.map { it.toDomain() })
    }

    private fun AnniversaryRow.toDomain() = Anniversary(
        id = id,
        title = title,
        date = LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        repeatYearly = repeatYearly,
        reminderType = AnniversaryReminder.entries.firstOrNull { it.name == reminderType }
            ?: AnniversaryReminder.NONE,
        createdAt = isoInstantToMillis(createdAt),
        iconEmoji = iconEmoji,
    )

    @Serializable
    private data class AnniversaryRow(
        val id: String,
        val title: String,
        val date: String,
        @SerialName("repeat_yearly") val repeatYearly: Boolean,
        @SerialName("reminder_type") val reminderType: String,
        @SerialName("created_at") val createdAt: String,
        @SerialName("icon_emoji") val iconEmoji: String? = null,
    )

    private companion object {
        private const val TABLE_NAME = "anniversaries"
        private const val FALLBACK_REFRESH_INTERVAL_MS = 60_000L
        private const val TAG = "SupabaseAnniversaryRepo"
    }
}
