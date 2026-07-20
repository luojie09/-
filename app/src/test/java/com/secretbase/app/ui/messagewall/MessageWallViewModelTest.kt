package com.secretbase.app.ui.messagewall

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.secretbase.app.data.HomeRepository
import com.secretbase.app.data.message.FakeMessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MessageWallViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun togglingRepliesRebuildsVisibleReplyModels() = runTest(dispatcher) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = MessageWallViewModel(
            homeRepository = HomeRepository(context, currentUserId = "sheep"),
            messageRepository = FakeMessageRepository(currentUserId = "sheep"),
            currentUserId = "sheep",
        )
        advanceUntilIdle()

        val collapsed = viewModel.uiState.value.messages.first { it.replyCount > 3 }
        assertEquals(3, collapsed.visibleReplies.size)
        assertEquals(collapsed.replyCount - 3, collapsed.hiddenReplyCount)

        viewModel.toggleExpandedReplies(collapsed.id)
        advanceUntilIdle()

        val expanded = viewModel.uiState.value.messages.first { it.id == collapsed.id }
        assertEquals(expanded.replyCount, expanded.visibleReplies.size)
        assertEquals(0, expanded.hiddenReplyCount)
    }

    @Test
    fun likingAndReplyingUpdateTheRenderedMessage() = runTest(dispatcher) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = MessageWallViewModel(
            homeRepository = HomeRepository(context, currentUserId = "sheep"),
            messageRepository = FakeMessageRepository(currentUserId = "sheep"),
            currentUserId = "sheep",
        )
        advanceUntilIdle()

        val initial = viewModel.uiState.value.messages.first()
        assertFalse(initial.isLiked)

        viewModel.toggleLike(initial.id)
        advanceUntilIdle()

        val liked = viewModel.uiState.value.messages.first { it.id == initial.id }
        assertTrue(liked.isLiked)
        assertEquals(initial.likeCount + 1, liked.likeCount)

        viewModel.openReplyComposer(initial.id)
        viewModel.updateReplyText("想和你一起去看")
        viewModel.sendReply()
        advanceUntilIdle()

        val replied = viewModel.uiState.value.messages.first { it.id == initial.id }
        assertEquals(initial.replyCount + 1, replied.replyCount)
        assertNull(viewModel.uiState.value.activeReplyMessageId)
        assertEquals("", viewModel.uiState.value.replyText)
    }
}
