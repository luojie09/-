package com.secretbase.app

import android.content.Context
import com.secretbase.app.data.HomeRepository
import com.secretbase.app.data.anniversary.AnniversaryRepository
import com.secretbase.app.data.anniversary.AnniversaryNotificationScheduler
import com.secretbase.app.data.anniversary.FakeAnniversaryRepository
import com.secretbase.app.data.anniversary.SupabaseAnniversaryRepository
import com.secretbase.app.data.message.FakeMessageRepository
import com.secretbase.app.data.message.MessageRepository
import com.secretbase.app.data.message.SupabaseMessageRepository
import com.secretbase.app.data.local.SecretBaseCache
import com.secretbase.app.data.local.SecretBaseDatabase
import com.secretbase.app.data.supabase.SupabaseClientProvider
import com.secretbase.app.data.supabase.SupabaseConfig
import com.secretbase.app.data.supabase.SupabaseAuthManager
import com.secretbase.app.data.supabase.SupabaseImageStorage
import com.secretbase.app.data.sync.PendingOperationExecutor
import com.secretbase.app.data.sync.SecretBaseSyncManager
import com.secretbase.app.data.sync.SyncStatus
import com.secretbase.app.data.sync.SupabaseRealtimeObserver
import com.secretbase.app.data.wish.FakeWishRepository
import com.secretbase.app.data.wish.SupabaseWishRepository
import com.secretbase.app.data.wish.WishRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class SecretBaseDependencies(
    context: Context,
    val currentUserId: String,
    authManager: SupabaseAuthManager? = null,
    coupleId: String? = null,
    enableRemoteModules: Boolean = true,
) : AutoCloseable {
    private val dependencyScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val anniversaryNotificationScheduler = AnniversaryNotificationScheduler(context)
    private val useSupabase = enableRemoteModules && SupabaseConfig.isConfigured
    private val supabaseClient = if (useSupabase) {
        val manager = requireNotNull(authManager) { "Authenticated Supabase session is required" }
        SupabaseClientProvider.client(manager::validAccessToken)
    } else {
        null
    }
    private val imageStorage = if (useSupabase) {
        SupabaseImageStorage(
            context = context,
            client = requireNotNull(supabaseClient),
            pathPrefix = requireNotNull(coupleId) { "Couple id is required for secure image storage" },
        )
    } else {
        null
    }
    private val database = if (useSupabase) SecretBaseDatabase.get(context) else null
    private val cache = if (useSupabase) {
        SecretBaseCache(
            database = requireNotNull(database),
            scopeId = requireNotNull(coupleId),
            currentUserId = currentUserId,
        )
    } else {
        null
    }
    private val remoteSyncManager = if (useSupabase) {
        SecretBaseSyncManager(
            context = context.applicationContext,
            database = requireNotNull(database),
            scopeId = requireNotNull(coupleId),
            executor = PendingOperationExecutor(
                client = requireNotNull(supabaseClient),
                imageStorage = imageStorage,
            ),
            scope = dependencyScope,
        )
    } else {
        null
    }

    val syncStatus: Flow<SyncStatus> = remoteSyncManager?.status ?: flowOf(SyncStatus())
    @Suppress("unused")
    private val realtimeObserver = if (useSupabase) {
        SupabaseRealtimeObserver(
            authManager = requireNotNull(authManager),
            syncManager = requireNotNull(remoteSyncManager),
            scopeId = requireNotNull(coupleId),
            scope = dependencyScope,
        )
    } else {
        null
    }

    val homeRepository = HomeRepository(
        context = context,
        currentUserId = currentUserId,
        client = supabaseClient,
        syncManager = remoteSyncManager,
    )
    val remoteRefreshEvents: Flow<String> = remoteSyncManager?.refreshEvents ?: kotlinx.coroutines.flow.emptyFlow()
    val messageRepository: MessageRepository =
        if (useSupabase) {
            SupabaseMessageRepository(
                client = requireNotNull(supabaseClient),
                currentUserId = currentUserId,
                cache = requireNotNull(cache),
                syncManager = requireNotNull(remoteSyncManager),
                scope = dependencyScope,
            )
        } else {
            FakeMessageRepository(currentUserId = currentUserId)
        }
    val wishRepository: WishRepository =
        if (useSupabase) {
            SupabaseWishRepository(
                client = requireNotNull(supabaseClient),
                cache = requireNotNull(cache),
                syncManager = requireNotNull(remoteSyncManager),
                scope = dependencyScope,
            )
        } else {
            FakeWishRepository()
        }
    val anniversaryRepository: AnniversaryRepository =
        if (useSupabase) {
            SupabaseAnniversaryRepository(
                client = requireNotNull(supabaseClient),
                cache = requireNotNull(cache),
                syncManager = requireNotNull(remoteSyncManager),
                scope = dependencyScope,
            )
        } else {
            FakeAnniversaryRepository()
        }

    override fun close() {
        dependencyScope.cancel()
    }
}
