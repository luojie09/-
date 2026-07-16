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
import com.secretbase.app.data.supabase.SupabaseAuthManager
import com.secretbase.app.data.supabase.SupabaseImageStorage
import com.secretbase.app.data.wish.FakeWishRepository
import com.secretbase.app.data.wish.SupabaseWishRepository
import com.secretbase.app.data.wish.WishRepository

class SecretBaseDependencies(
    context: Context,
    val currentUserId: String,
    authManager: SupabaseAuthManager? = null,
    coupleId: String? = null,
    enableRemoteModules: Boolean = true,
) {
    private val useSupabase = enableRemoteModules && SupabaseConfig.isConfigured
    private val supabaseClient = if (useSupabase) {
        val manager = requireNotNull(authManager) { "Authenticated Supabase session is required" }
        SupabaseClientProvider.client(manager::validAccessToken)
    } else {
        null
    }
    private val imageStorage = if (useSupabase) {
        SupabaseImageStorage(
            context = context,
            client = requireNotNull(supabaseClient),
            pathPrefix = requireNotNull(coupleId) { "Couple id is required for secure image storage" },
        )
    } else {
        null
    }

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
                imageStorage = imageStorage,
            )
        } else {
            FakeMessageRepository(currentUserId = currentUserId)
        }
    val wishRepository: WishRepository =
        if (useSupabase) {
            SupabaseWishRepository(
                client = requireNotNull(supabaseClient),
                imageStorage = imageStorage,
            )
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
