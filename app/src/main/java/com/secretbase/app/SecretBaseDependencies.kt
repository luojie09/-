package com.secretbase.app

import android.content.Context
import com.secretbase.app.data.HomeRepository
import com.secretbase.app.data.anniversary.AnniversaryRepository
import com.secretbase.app.data.anniversary.FakeAnniversaryRepository
import com.secretbase.app.data.message.FakeMessageRepository
import com.secretbase.app.data.message.MessageRepository
import com.secretbase.app.data.wish.FakeWishRepository
import com.secretbase.app.data.wish.WishRepository

class SecretBaseDependencies(context: Context) {
    val homeRepository = HomeRepository(context)
    val messageRepository: MessageRepository = FakeMessageRepository()
    val wishRepository: WishRepository = FakeWishRepository()
    val anniversaryRepository: AnniversaryRepository = FakeAnniversaryRepository()
}
