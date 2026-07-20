package com.secretbase.app.ui.anniversary

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.secretbase.app.data.HomeRepository
import com.secretbase.app.data.anniversary.Anniversary
import com.secretbase.app.data.anniversary.AnniversaryReminder
import com.secretbase.app.data.anniversary.AnniversaryRepository
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AnniversaryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2026, 1, 10)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun recurringLeapDayUsesFebruary28InCommonYear() = runTest(dispatcher) {
        val leapDay = Anniversary(
            id = "leap-day",
            title = "特别纪念日",
            date = LocalDate.of(2024, 2, 29).toMillis(),
            repeatYearly = true,
            reminderType = AnniversaryReminder.SAME_DAY,
            createdAt = 1L,
            iconEmoji = "💗",
        )
        val viewModel = createViewModel(listOf(leapDay))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertEquals("还有 49 天", state.items.single().statusText)
        assertEquals(AnniversaryStatusTone.UPCOMING, state.items.single().statusTone)
    }

    @Test
    fun addingAnniversaryClosesEditorAndRendersNewItem() = runTest(dispatcher) {
        val repository = TestAnniversaryRepository(emptyList())
        val viewModel = createViewModel(repository = repository)
        advanceUntilIdle()

        viewModel.showAddEditor()
        viewModel.updateTitle("第一次看雪")
        viewModel.updateDate(LocalDate.of(2026, 12, 20).toMillis())
        viewModel.updateIconEmoji("❄️")
        viewModel.saveEditor()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.items.size)
        assertEquals("第一次看雪", state.items.single().title)
        assertEquals(DefaultAnniversaryEmoji, state.items.single().iconEmoji)
        assertFalse(state.editorVisible)
        assertFalse(state.isSaving)
    }

    private fun createViewModel(items: List<Anniversary>): AnniversaryViewModel =
        createViewModel(repository = TestAnniversaryRepository(items))

    private fun createViewModel(repository: AnniversaryRepository): AnniversaryViewModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return AnniversaryViewModel(
            homeRepository = HomeRepository(context, currentUserId = "sheep"),
            anniversaryRepository = repository,
            todayProvider = { today },
        )
    }

    private fun LocalDate.toMillis(): Long =
        atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private class TestAnniversaryRepository(
    initialItems: List<Anniversary>,
) : AnniversaryRepository {
    private val items = MutableStateFlow(initialItems)

    override fun observeAnniversaries(): Flow<List<Anniversary>> = items

    override suspend fun addAnniversary(item: Anniversary): Result<Unit> = runCatching {
        items.value = items.value + item
    }

    override suspend fun updateAnniversary(item: Anniversary): Result<Unit> = runCatching {
        items.value = items.value.map { existing -> if (existing.id == item.id) item else existing }
    }

    override suspend fun deleteAnniversary(id: String): Result<Unit> = runCatching {
        items.value = items.value.filterNot { it.id == id }
    }
}
