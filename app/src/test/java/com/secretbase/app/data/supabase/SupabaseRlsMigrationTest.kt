package com.secretbase.app.data.supabase

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseRlsMigrationTest {
    @Test
    fun secureMigrationProtectsEveryApplicationTableAndStorageWrite() {
        val sql = migration("secure_couple_auth_migration.sql")
        val requiredPolicies = listOf(
            "members_select_self",
            "anniversaries_couple_all",
            "wishes_couple_all",
            "messages_couple_select",
            "messages_author_delete",
            "replies_couple_select",
            "replies_author_delete",
            "drafts_own_all",
            "moods_own_insert",
            "quick_notes_couple_all",
            "secretbase_images_couple_insert",
            "secretbase_images_couple_delete",
        )

        requiredPolicies.forEach { policy ->
            assertTrue("Missing RLS policy $policy", sql.contains("\"$policy\""))
        }
        assertTrue(sql.contains("(storage.foldername(name))[1] = public.current_secret_base_couple_id()::text"))
        assertFalse(
            "Secure migration must never grant application access to anon",
            Regex("(?is)grant\\s+[^;]+\\s+to\\s+anon\\b").containsMatchIn(sql),
        )
    }

    @Test
    fun likesAreCoupleScopedAndOnlyMutableByTheirOwner() {
        val sql = migration("phase4_features.sql")

        assertTrue(sql.contains("primary key (couple_id, message_id, user_id)"))
        assertTrue(sql.contains("message_likes_couple_select"))
        assertTrue(sql.contains("message_likes_own_insert"))
        assertTrue(sql.contains("message_likes_own_delete"))
        assertTrue(sql.contains("user_id = public.current_secret_base_role()"))
        assertTrue(sql.contains("revoke all on table public.message_likes from anon"))
    }

    private fun migration(name: String): String {
        val workingDirectory = File(requireNotNull(System.getProperty("user.dir")))
        val candidates = listOf(
            workingDirectory.resolve("supabase/$name"),
            workingDirectory.resolve("../supabase/$name"),
        )
        val file: File = candidates.firstOrNull { candidate -> candidate.isFile }
            ?: error("Cannot locate Supabase migration $name from $workingDirectory")
        return file.readText(Charsets.UTF_8).lowercase()
    }
}
