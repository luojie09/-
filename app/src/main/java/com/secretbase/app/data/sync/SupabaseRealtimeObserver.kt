package com.secretbase.app.data.sync

import android.util.Log
import com.secretbase.app.data.supabase.SupabaseAuthManager
import com.secretbase.app.data.supabase.SupabaseConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class SupabaseRealtimeObserver(
    private val authManager: SupabaseAuthManager,
    private val syncManager: SecretBaseSyncManager,
    private val scopeId: String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    init {
        scope.launch { connect() }
    }

    private suspend fun connect() {
        while (currentCoroutineContext().isActive) {
            runCatching { connectSession() }
                .onFailure { error ->
                    Log.w(TAG, "Realtime unavailable; polling remains active", error)
                    syncManager.setRealtimeConnected(false)
                }
            delay(RECONNECT_RETRY_MS)
        }
    }

    private suspend fun connectSession() = coroutineScope {
            val token = authManager.validAccessToken()
            val client = createSupabaseClient(
                supabaseUrl = SupabaseConfig.url,
                supabaseKey = SupabaseConfig.publishableKey,
            ) {
                httpEngine = CIO.create()
                install(Realtime) {
                    jwtToken = token
                    reconnectDelay = 5.seconds
                }
            }
            val channel = client.channel("secret-base-$scopeId")
            val moodChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "user_moods"
            }.onEach { syncManager.notifyRemoteChange(SyncModules.HOME) }
            val noteChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "home_quick_notes"
            }.onEach { syncManager.notifyRemoteChange(SyncModules.HOME) }
            val messageChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "messages"
            }.onEach { syncManager.notifyRemoteChange(SyncModules.MESSAGES) }
            val replyChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "message_replies"
            }.onEach { syncManager.notifyRemoteChange(SyncModules.MESSAGES) }
            val draftChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "message_drafts"
            }.onEach { syncManager.notifyRemoteChange(SyncModules.MESSAGES) }
            val likeChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "message_likes"
            }.onEach { syncManager.notifyRemoteChange(SyncModules.MESSAGES) }
            val wishChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "wishes"
            }.onEach { syncManager.notifyRemoteChange(SyncModules.WISHES) }
            val anniversaryChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "anniversaries"
            }.onEach { syncManager.notifyRemoteChange(SyncModules.ANNIVERSARIES) }

            merge(
                moodChanges,
                noteChanges,
                messageChanges,
                replyChanges,
                draftChanges,
                likeChanges,
                wishChanges,
                anniversaryChanges,
            ).launchIn(this)
            channel.subscribe(blockUntilSubscribed = true)
            syncManager.setRealtimeConnected(true)

            while (isActive) {
                delay(TOKEN_REFRESH_INTERVAL_MS)
                channel.updateAuth(authManager.validAccessToken())
            }
    }

    private companion object {
        private const val TOKEN_REFRESH_INTERVAL_MS = 45 * 60 * 1_000L
        private const val RECONNECT_RETRY_MS = 15_000L
        private const val TAG = "SecretBaseRealtime"
    }
}
