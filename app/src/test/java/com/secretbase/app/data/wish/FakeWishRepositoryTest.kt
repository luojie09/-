package com.secretbase.app.data.wish

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeWishRepositoryTest {
    @Test
    fun addUpdateCompleteAndDeleteMutateObservedState() = runTest {
        val repository = FakeWishRepository()
        val wish = Wish(
            id = "wish-test",
            title = "一起去露营",
            description = "看星星",
            coverImagePath = null,
            plannedDate = null,
            createdAt = 1L,
            status = WishStatus.UNREALIZED,
            completion = null,
        )

        repository.addWish(wish).getOrThrow()
        assertTrue(repository.observeWishes().first().any { it.id == wish.id })

        repository.updateWish(wish.copy(title = "一起去山里露营")).getOrThrow()
        assertEquals("一起去山里露营", repository.observeWishes().first().first { it.id == wish.id }.title)

        val completion = WishCompletion("实现啦", listOf("mock://wish-test"), 2L)
        repository.completeWish(wish.id, completion).getOrThrow()
        val realized = repository.observeWishes().first().first { it.id == wish.id }
        assertEquals(WishStatus.REALIZED, realized.status)
        assertEquals(completion, realized.completion)

        repository.deleteWish(wish.id).getOrThrow()
        assertFalse(repository.observeWishes().first().any { it.id == wish.id })
    }
}
