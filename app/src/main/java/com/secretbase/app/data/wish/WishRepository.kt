package com.secretbase.app.data.wish

import kotlinx.coroutines.flow.Flow

interface WishRepository {
    fun observeWishes(): Flow<List<Wish>>

    suspend fun addWish(wish: Wish): Result<Unit>

    suspend fun updateWish(wish: Wish): Result<Unit>

    suspend fun deleteWish(wishId: String): Result<Unit>

    suspend fun completeWish(
        wishId: String,
        completion: WishCompletion,
    ): Result<Unit>
}
