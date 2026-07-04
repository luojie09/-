package com.secretbase.app

import android.content.Context
import android.util.Log
import com.secretbase.app.data.HomeRepository
import com.secretbase.app.data.anniversary.AnniversaryRepository
import com.secretbase.app.data.anniversary.FakeAnniversaryRepository
import com.secretbase.app.data.anniversary.SupabaseAnniversaryRepository
import com.secretbase.app.data.message.FakeMessageRepository
import com.secretbase.app.data.message.MessageRepository
import com.secretbase.app.data.message.SupabaseMessageRepository
import com.secretbase.app.data.supabase.SupabaseClientProvider
import com.secretbase.app.data.supabase.SupabaseConfig
import com.secretbase.app.data.wish.FakeWishRepository
import com.secretbase.app.data.wish.SupabaseWishRepository
import com.secretbase.app.data.wish.WishRepository

class SecretBaseDependencies(context: Context) {
    private val supabaseClient =
        if (SupabaseConfig.isConfigured) {
            runCatching { SupabaseClientProvider.client }
                .onFailure { error ->
                    Log.e(
                        TAG,
                        "Failed to initialize Supabase client. Falling back to local repositories.",
                        error,
                    )
                }
                .getOrNull()
        } else {
            null
        }

    val homeRepository = HomeRepository(context, supabaseClient)
    val messageRepository: MessageRepository =
        supabaseClient
            ?.let { client ->
                runCatching { SupabaseMessageRepository(client) }
                    .onFailure { error ->
                        Log.e(
                            TAG,
                            "Failed to initialize Supabase message repository. Falling back to fake repository.",
                            error,
                        )
                    }
                    .getOrNull()
            }
            ?: FakeMessageRepository()
    val wishRepository: WishRepository =
        supabaseClient
            ?.let { client ->
                runCatching { SupabaseWishRepository(client) }
                    .onFailure { error ->
                        Log.e(
                            TAG,
                            "Failed to initialize Supabase wish repository. Falling back to fake repository.",
                            error,
                        )
                    }
                    .getOrNull()
            }
            ?: FakeWishRepository()
    val anniversaryRepository: AnniversaryRepository =
        supabaseClient
            ?.let { client ->
                runCatching { SupabaseAnniversaryRepository(client) }
                    .onFailure { error ->
                        Log.e(
                            TAG,
                            "Failed to initialize Supabase anniversary repository. Falling back to fake repository.",
                            error,
                        )
                    }
                    .getOrNull()
            }
            ?: FakeAnniversaryRepository()

    private companion object {
        private const val TAG = "SecretBaseDeps"
    }
}
