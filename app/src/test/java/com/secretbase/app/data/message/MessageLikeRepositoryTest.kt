package com.secretbase.app.data.message

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageLikeRepositoryTest {
    @Test
    fun toggleLike_updatesRepositoryState() = runTest {
        val repository = FakeMessageRepository(SecretBaseUsers.SHEEP_ID)
        val messageId = repository.observeMessages().first().first().id

        assertTrue(repository.toggleLike(messageId).getOrThrow())
        assertTrue(
            SecretBaseUsers.SHEEP_ID in repository.observeMessages().first()
                .first { it.id == messageId }
                .likedByUserIds,
        )

        assertFalse(repository.toggleLike(messageId).getOrThrow())
        assertFalse(
            SecretBaseUsers.SHEEP_ID in repository.observeMessages().first()
                .first { it.id == messageId }
                .likedByUserIds,
        )
    }
}
