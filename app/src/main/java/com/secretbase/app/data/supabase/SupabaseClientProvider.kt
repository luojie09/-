package com.secretbase.app.data.supabase

import com.secretbase.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseConfig {
    val url: String get() = BuildConfig.SUPABASE_URL.trim()
    val publishableKey: String get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()

    val isConfigured: Boolean
        get() = url.isNotBlank() && publishableKey.isNotBlank()
}

object SupabaseClientProvider {
    val client: SupabaseClient by lazy {
        check(SupabaseConfig.isConfigured) {
            "Supabase is not configured. Please set SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY in local.properties."
        }
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.url,
            supabaseKey = SupabaseConfig.publishableKey,
        ) {
            install(Postgrest)
        }
    }
}
