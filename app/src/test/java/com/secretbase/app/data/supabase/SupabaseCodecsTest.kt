package com.secretbase.app.data.supabase

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SupabaseCodecsTest {

    @Test
    fun isoInstantToMillis_parsesUtcInstant() {
        val value = "2026-07-03T08:10:46.83812Z"

        assertEquals(
            Instant.parse("2026-07-03T08:10:46.838120Z").toEpochMilli(),
            isoInstantToMillis(value),
        )
    }

    @Test
    fun isoInstantToMillis_parsesSupabaseOffsetTimestamp() {
        val value = "2026-07-03T08:10:46.83812+00:00"

        assertEquals(
            Instant.parse("2026-07-03T08:10:46.838120Z").toEpochMilli(),
            isoInstantToMillis(value),
        )
    }
}
