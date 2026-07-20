package com.secretbase.app.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.secretbase.app.data.local.PendingOperationEntity
import com.secretbase.app.data.local.SecretBaseDatabase
import com.secretbase.app.data.local.SyncMetadataEntity
import com.secretbase.app.data.supabase.SupabaseAuthManager
import com.secretbase.app.data.supabase.SupabaseClientProvider
import com.secretbase.app.data.supabase.SupabaseConfig
import com.secretbase.app.data.supabase.SupabaseImageStorage
import com.secretbase.app.data.user.UserIdentityStore
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SecretBaseSyncManager(
    private val context: Context,
    private val database: SecretBaseDatabase,
    private val scopeId: String,
    private val executor: PendingOperationExecutor,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val processor = PendingSyncProcessor(database, scopeId, executor)
    private val metadataDao = database.syncMetadata()
    private val pendingDao = database.pendingOperations()
    private var immediateSyncJob: Job? = null

    val refreshEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)

    val status: Flow<SyncStatus> = combine(
        pendingDao.observeCount(scopeId),
        metadataDao.observe(scopeId),
    ) { pendingCount, metadata ->
        SyncStatus(
            phase = when {
                metadata?.isSyncing == true -> SyncPhase.SYNCING
                pendingCount > 0 && metadata?.lastError != null -> SyncPhase.ERROR
                pendingCount > 0 -> SyncPhase.PENDING
                else -> SyncPhase.SYNCED
            },
            pendingCount = pendingCount,
            lastSyncedAt = metadata?.lastSyncedAt,
            lastError = metadata?.lastError,
            realtimeConnected = metadata?.realtimeConnected == true,
        )
    }.distinctUntilChanged()

    init {
        scheduleWork()
        requestInProcessSync()
    }

    suspend fun enqueue(
        module: String,
        operation: String,
        entityId: String,
        payload: String = "",
        dedupeKey: String = "$scopeId:$operation:$entityId",
    ) {
        pendingDao.upsert(
            PendingOperationEntity(
                id = UUID.randomUUID().toString(),
                scopeId = scopeId,
                module = module,
                operation = operation,
                entityId = entityId,
                payload = payload,
                dedupeKey = dedupeKey,
                createdAt = System.currentTimeMillis(),
            ),
        )
        updateMetadata { it.copy(lastError = null) }
        scheduleWork()
        requestInProcessSync()
    }

    suspend fun hasPending(module: String): Boolean =
        pendingDao.countForModule(scopeId, module) > 0

    suspend fun syncNow(): Boolean {
        val result = processor.process { module -> refreshEvents.emit(module) }
        if (!result) scheduleWork()
        return result
    }

    suspend fun setRealtimeConnected(connected: Boolean) {
        updateMetadata { it.copy(realtimeConnected = connected) }
    }

    fun notifyRemoteChange(module: String) {
        refreshEvents.tryEmit(module)
    }

    private suspend fun updateMetadata(
        transform: (SyncMetadataEntity) -> SyncMetadataEntity,
    ) {
        val current = metadataDao.get(scopeId) ?: SyncMetadataEntity(scopeId = scopeId)
        metadataDao.upsert(transform(current))
    }

    private fun scheduleWork() {
        val request = OneTimeWorkRequestBuilder<SecretBaseSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .setInputData(workDataOf(SecretBaseSyncWorker.KEY_SCOPE_ID to scopeId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "secret-base-sync-$scopeId",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun requestInProcessSync() {
        immediateSyncJob?.cancel()
        immediateSyncJob = scope.launch {
            delay(IN_PROCESS_SYNC_DEBOUNCE_MS)
            syncNow()
        }
    }

    private companion object {
        private const val IN_PROCESS_SYNC_DEBOUNCE_MS = 650L
    }
}

private class PendingSyncProcessor(
    private val database: SecretBaseDatabase,
    private val scopeId: String,
    private val executor: PendingOperationExecutor,
) {
    suspend fun process(onSynced: suspend (String) -> Unit = {}): Boolean = processMutex.withLock {
        val pendingDao = database.pendingOperations()
        val metadataDao = database.syncMetadata()
        val currentMetadata = metadataDao.get(scopeId) ?: SyncMetadataEntity(scopeId)
        metadataDao.upsert(currentMetadata.copy(isSyncing = true, lastError = null))

        while (true) {
            val operation = pendingDao.getPending(scopeId, limit = 1).firstOrNull() ?: break
            runCatching {
                executor.execute(operation.operation, operation.entityId, operation.payload)
            }.onSuccess {
                pendingDao.delete(operation.id)
                onSynced(operation.module)
            }.onFailure { error ->
                val message = error.message?.take(300) ?: error.javaClass.simpleName
                pendingDao.recordFailure(operation.id, message)
                metadataDao.upsert(
                    (metadataDao.get(scopeId) ?: SyncMetadataEntity(scopeId)).copy(
                        isSyncing = false,
                        lastError = message,
                    ),
                )
                return@withLock false
            }
        }

        metadataDao.upsert(
            (metadataDao.get(scopeId) ?: SyncMetadataEntity(scopeId)).copy(
                isSyncing = false,
                lastSyncedAt = System.currentTimeMillis(),
                lastError = null,
            ),
        )
        true
    }

    private companion object {
        val processMutex = Mutex()
    }
}

class SecretBaseSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (!SupabaseConfig.isConfigured) return Result.success()
        val identityStore = UserIdentityStore(applicationContext)
        if (!identityStore.hasAuthenticatedIdentity()) return Result.failure()
        val scopeId = inputData.getString(KEY_SCOPE_ID)
            ?: identityStore.coupleId()
            ?: return Result.failure()
        if (scopeId != identityStore.coupleId()) return Result.failure()

        val authManager = SupabaseAuthManager(identityStore)
        val client = SupabaseClientProvider.client(authManager::validAccessToken)
        val imageStorage = SupabaseImageStorage(applicationContext, client, scopeId)
        val processor = PendingSyncProcessor(
            database = SecretBaseDatabase.get(applicationContext),
            scopeId = scopeId,
            executor = PendingOperationExecutor(client, imageStorage),
        )
        return if (processor.process()) Result.success() else Result.retry()
    }

    companion object {
        const val KEY_SCOPE_ID = "scope_id"
    }
}
