package com.secretbase.app.data.supabase

import org.junit.Assert.assertNotNull
import org.junit.Test

class SupabaseRuntimeDependenciesTest {
    @Test
    fun realtime_has_auth_types_on_the_runtime_classpath() {
        val classLoader = javaClass.classLoader
        // RealtimeImpl.init resolves these classes on its own background scope.
        // Excluding Auth compiles successfully but terminates the paired app.
        assertNotNull(Class.forName("io.github.jan.supabase.auth.Auth", false, classLoader))
        assertNotNull(Class.forName("io.github.jan.supabase.auth.status.SessionStatus", false, classLoader))
    }
}
