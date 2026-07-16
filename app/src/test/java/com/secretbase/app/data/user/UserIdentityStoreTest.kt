package com.secretbase.app.data.user

import androidx.test.core.app.ApplicationProvider
import com.secretbase.app.data.message.SecretBaseUsers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserIdentityStoreTest {
    private lateinit var store: UserIdentityStore

    @Before
    fun setUp() {
        store = UserIdentityStore(ApplicationProvider.getApplicationContext())
        store.clearAuthenticatedIdentity()
    }

    @Test
    fun persistsAuthenticatedIdentityAndSession() {
        val session = StoredAuthSession(
            accessToken = "access",
            refreshToken = "refresh",
            userId = "auth-user",
            expiresAtEpochSeconds = 4_600L,
        )

        store.saveAuthenticatedIdentity(
            userId = SecretBaseUsers.SHEEP_ID,
            coupleId = "couple-id",
            session = session,
        )

        assertEquals(SecretBaseUsers.SHEEP_ID, store.currentUserId())
        assertEquals("couple-id", store.coupleId())
        assertEquals(session, store.authSession())
        assertTrue(store.hasAuthenticatedIdentity())
    }

    @Test
    fun clearingSessionKeepsRoleForRebinding() {
        store.saveAuthenticatedIdentity(
            userId = SecretBaseUsers.CHICK_ID,
            coupleId = "couple-id",
            session = StoredAuthSession(
                accessToken = "access",
                refreshToken = "refresh",
                userId = "auth-user",
                expiresAtEpochSeconds = 4_600L,
            ),
        )

        store.clearAuthSessionKeepingRole()

        assertEquals(SecretBaseUsers.CHICK_ID, store.currentUserId())
        assertNull(store.coupleId())
        assertNull(store.authSession())
        assertFalse(store.hasAuthenticatedIdentity())
    }
}
