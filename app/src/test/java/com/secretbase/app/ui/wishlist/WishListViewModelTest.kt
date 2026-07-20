package com.secretbase.app.ui.wishlist

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.secretbase.app.data.HomeRepository
import com.secretbase.app.data.wish.FakeWishRepository
import com.secretbase.app.data.wish.WishStatus
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WishListViewModelTest {
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
    fun addingWishUpdatesCountsAndClosesEditor() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val initialCount = viewModel.uiState.value.unrealizedCount

        viewModel.showAddWish()
        viewModel.updateEditorTitle("一起去露营")
        viewModel.updateEditorDescription("在山里等一次日出")
        viewModel.saveWish()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(initialCount + 1, state.unrealizedCount)
        assertTrue(state.wishes.any { it.title == "一起去露营" })
        assertFalse(state.editorVisible)
        assertFalse(state.isSaving)
    }

    @Test
    fun completingWishMovesItToRealizedList() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val wish = viewModel.uiState.value.wishes.first { it.status == WishStatus.UNREALIZED }
        var onSavedCalled = false

        viewModel.startCompletion(wish.id)
        viewModel.updateCompletionText("这一天比想象中还要开心")
        viewModel.saveCompletion(wish.id) { onSavedCalled = true }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(WishStatus.REALIZED, state.wishes.first { it.id == wish.id }.status)
        assertEquals(WishStatus.REALIZED, state.selectedStatus)
        assertTrue(onSavedCalled)
        assertFalse(state.isSaving)
    }

    private fun createViewModel(): WishListViewModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return WishListViewModel(
            homeRepository = HomeRepository(context, currentUserId = "sheep"),
            wishRepository = FakeWishRepository(),
        )
    }
}
