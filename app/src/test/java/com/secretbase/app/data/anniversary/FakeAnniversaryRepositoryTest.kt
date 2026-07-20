package com.secretbase.app.data.anniversary

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAnniversaryRepositoryTest {
    @Test
    fun addUpdateAndDeleteMutateObservedState() = runTest {
        val repository = FakeAnniversaryRepository()
        val item = Anniversary(
            id = "anniversary-test",
            title = "第一次看雪",
            date = 1L,
            repeatYearly = true,
            reminderType = AnniversaryReminder.ONE_DAY_BEFORE,
            createdAt = 2L,
            iconEmoji = "🌸",
        )

        repository.addAnniversary(item).getOrThrow()
        assertTrue(repository.observeAnniversaries().first().any { it.id == item.id })

        repository.updateAnniversary(item.copy(title = "一起看的第一场雪")).getOrThrow()
        assertEquals(
            "一起看的第一场雪",
            repository.observeAnniversaries().first().first { it.id == item.id }.title,
        )

        repository.deleteAnniversary(item.id).getOrThrow()
        assertFalse(repository.observeAnniversaries().first().any { it.id == item.id })
    }
}
