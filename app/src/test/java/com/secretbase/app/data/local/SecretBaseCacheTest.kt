package com.secretbase.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.secretbase.app.data.anniversary.Anniversary
import com.secretbase.app.data.anniversary.AnniversaryReminder
import com.secretbase.app.data.message.Message
import com.secretbase.app.data.message.MessageDraft
import com.secretbase.app.data.sync.SyncModules
import com.secretbase.app.data.sync.SyncOperations
import com.secretbase.app.data.wish.Wish
import com.secretbase.app.data.wish.WishStatus
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SecretBaseCacheTest {
    private lateinit var database: SecretBaseDatabase
    private lateinit var cache: SecretBaseCache

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SecretBaseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cache = SecretBaseCache(database, scopeId = "couple", currentUserId = "sheep")
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun cachesAllOfflineFirstModules() = runTest {
        val message = Message("message-1", "sheep", "小羊", "晚安", emptyList(), 1L)
        val draft = MessageDraft("草稿", listOf("content://photo"), 2L)
        val wish = Wish("wish-1", "看海", "", null, null, 3L, WishStatus.UNREALIZED, null)
        val anniversary = Anniversary(
            "anniversary-1",
            "第一次约会",
            4L,
            true,
            AnniversaryReminder.ONE_DAY_BEFORE,
            5L,
            "🌸",
        )

        cache.saveMessages(listOf(message))
        cache.saveDraft(draft)
        cache.saveWishes(listOf(wish))
        cache.saveAnniversaries(listOf(anniversary))

        assertEquals(listOf(message), cache.observeMessages().first())
        assertEquals(draft, cache.observeDraft().first())
        assertEquals(listOf(wish), cache.observeWishes().first())
        assertEquals(listOf(anniversary), cache.observeAnniversaries().first())
    }

    @Test
    fun latestDedupeOperationReplacesOlderPayload() = runTest {
        val dao = database.pendingOperations()
        fun operation(payload: String) = PendingOperationEntity(
            id = UUID.randomUUID().toString(),
            scopeId = "couple",
            module = SyncModules.MESSAGES,
            operation = SyncOperations.DRAFT_UPSERT,
            entityId = "sheep",
            payload = payload,
            dedupeKey = "draft:sheep",
            createdAt = System.currentTimeMillis(),
        )

        dao.upsert(operation("旧草稿"))
        dao.upsert(operation("新草稿"))

        val pending = dao.getPending("couple")
        assertEquals(1, pending.size)
        assertEquals("新草稿", pending.single().payload)
    }

    @Test
    fun failedOperationKeepsPayloadAndRecordsAttempt() = runTest {
        val dao = database.pendingOperations()
        val operation = PendingOperationEntity(
            id = "operation-1",
            scopeId = "couple",
            module = SyncModules.WISHES,
            operation = SyncOperations.WISH_UPSERT,
            entityId = "wish-1",
            payload = "payload",
            dedupeKey = "wish:wish-1",
            createdAt = 1L,
        )
        dao.upsert(operation)

        dao.recordFailure(operation.id, "temporary network error")

        val failed = dao.getPending("couple").single()
        assertEquals(1, failed.attemptCount)
        assertEquals("temporary network error", failed.lastError)
        assertEquals("payload", failed.payload)
        assertTrue(dao.observeCount("couple").first() > 0)
    }
}
