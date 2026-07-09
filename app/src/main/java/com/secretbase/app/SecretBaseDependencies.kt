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

class SecretBaseDependencies(
    context: Context,
    val currentUserId: String,
    enableRemoteModules: Boolean = true,
) {
    private val useSupabase = enableRemoteModules && SupabaseConfig.isConfigured
    private val supabaseClient = if (useSupabase) SupabaseClientProvider.client else null

    val homeRepository = HomeRepository(
        context = context,
        currentUserId = currentUserId,
        client = supabaseClient,
    )
    val messageRepository: MessageRepository =
        if (useSupabase) {
            SupabaseMessageRepository(
                client = requireNotNull(supabaseClient),
                currentUserId = currentUserId,
            )
        } else {
            FakeMessageRepository(currentUserId = currentUserId)
        }
    val wishRepository: WishRepository =
        if (useSupabase) {
            SupabaseWishRepository(requireNotNull(supabaseClient))
        } else {
            FakeWishRepository()
        }
    val anniversaryRepository: AnniversaryRepository =
        if (useSupabase) {
            SupabaseAnniversaryRepository(requireNotNull(supabaseClient))
        } else {
            FakeAnniversaryRepository()
        }
}
