package com.secretbase.app.data.supabase

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SupabaseAuthManagerTest {
    @Test
    fun parsesAnonymousSessionAndExpiry() {
        val session = parseAuthSessionResponse(
            response = JSONObject(
                """
                {
                  "access_token": "access-token",
                  "refresh_token": "refresh-token",
                  "expires_in": 3600,
                  "user": { "id": "auth-user-id" }
                }
                """.trimIndent(),
            ),
            nowEpochSeconds = 1_000L,
        )

        assertEquals("access-token", session.accessToken)
        assertEquals("refresh-token", session.refreshToken)
        assertEquals("auth-user-id", session.userId)
        assertEquals(4_600L, session.expiresAtEpochSeconds)
    }

    @Test
    fun parsesBoundCoupleIdentity() {
        val identity = parseBoundIdentityResponse(
            """{"couple_id":"couple-id","role":"sheep"}""",
        )

        assertEquals("couple-id", identity.coupleId)
        assertEquals("sheep", identity.role)
    }

    @Test
    fun rejectsUnsupportedRoleFromServer() {
        assertThrows(IllegalArgumentException::class.java) {
            parseBoundIdentityResponse(
                """{"couple_id":"couple-id","role":"intruder"}""",
            )
        }
    }
}
