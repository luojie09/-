package com.secretbase.app

import android.content.Context
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
    private val supabaseClient = SupabaseClientProvider.client.takeIf { SupabaseConfig.isConfigured }

    val homeRepository = HomeRepository(context, supabaseClient)
    val messageRepository: MessageRepository =
        supabaseClient?.let(::SupabaseMessageRepository) ?: FakeMessageRepository()
    val wishRepository: WishRepository =
        supabaseClient?.let(::SupabaseWishRepository) ?: FakeWishRepository()
    val anniversaryRepository: AnniversaryRepository =
        supabaseClient?.let(::SupabaseAnniversaryRepository) ?: FakeAnniversaryRepository()
}
