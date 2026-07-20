package com.secretbase.app.data.message

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeMessageRepositoryTest {
    @Test
    fun draftPublishReplyLikeAndDeleteFollowRepositoryContract() = runTest {
        val repository = FakeMessageRepository(currentUserId = SecretBaseUsers.SHEEP_ID)
        repository.updateDraft("  想你了  ", listOf("one", "two"))
        assertEquals("  想你了  ", repository.observeDraft().first().content)

        val published = repository.publishMessage(
            content = "  今天也很想你  ",
            imagePaths = listOf("mock://blossom-sky"),
        ).getOrThrow()
        assertEquals("今天也很想你", published.content)
        assertEquals(MessageDraft(), repository.observeDraft().first())

        val reply = repository.addReply(published.id, "  我也是  ").getOrThrow()
        assertEquals("我也是", reply.content)
        assertTrue(repository.observeMessages().first().first { it.id == published.id }.replies.any { it.id == reply.id })

        assertTrue(repository.toggleLike(published.id).getOrThrow())
        assertTrue(SecretBaseUsers.SHEEP_ID in repository.observeMessages().first().first { it.id == published.id }.likedByUserIds)
        assertFalse(repository.toggleLike(published.id).getOrThrow())

        repository.deleteReply(reply.id).getOrThrow()
        assertTrue(repository.observeMessages().first().first { it.id == published.id }.replies.isEmpty())
        repository.deleteMessage(published.id).getOrThrow()
        assertFalse(repository.observeMessages().first().any { it.id == published.id })
    }

    @Test
    fun publishingEmptyMessageFailsWithoutMutatingState() = runTest {
        val repository = FakeMessageRepository(currentUserId = SecretBaseUsers.SHEEP_ID)
        val before = repository.observeMessages().first()

        val result = repository.publishMessage("   ", emptyList())

        assertTrue(result.isFailure)
        assertEquals(before, repository.observeMessages().first())
    }
}
