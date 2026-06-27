package com.secretbase.app.data.wish

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class FakeWishRepository : WishRepository {

    private val wishes = MutableStateFlow(seedWishes())

    override fun observeWishes(): Flow<List<Wish>> = wishes.asStateFlow()

    override suspend fun addWish(wish: Wish): Result<Unit> = runCatching {
        wishes.update { current ->
            listOf(wish.copy(id = if (wish.id.isBlank()) "wish-${UUID.randomUUID()}" else wish.id)) + current
        }
    }

    override suspend fun updateWish(wish: Wish): Result<Unit> = runCatching {
        wishes.update { current ->
            current.map { item -> if (item.id == wish.id) wish else item }
        }
    }

    override suspend fun deleteWish(wishId: String): Result<Unit> = runCatching {
        wishes.update { current -> current.filterNot { it.id == wishId } }
    }

    override suspend fun completeWish(
        wishId: String,
        completion: WishCompletion,
    ): Result<Unit> = runCatching {
        wishes.update { current ->
            current.map { item ->
                if (item.id == wishId) {
                    item.copy(
                        status = WishStatus.REALIZED,
                        completion = completion,
                    )
                } else {
                    item
                }
            }
        }
    }

    private fun seedWishes(): List<Wish> {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000
        return listOf(
            Wish(
                id = "wish-paris",
                title = "一起去旅行",
                description = "去看海，去看日出日落，去我们从未去过的城市。",
                coverImagePath = "mock://wish-paris",
                plannedDate = now + 180 * day,
                createdAt = now - 38 * day,
                status = WishStatus.UNREALIZED,
                completion = null,
            ),
            Wish(
                id = "wish-couple-photo",
                title = "拍一组情侣写真",
                description = "记录下最具勇气的我们，把镜头里的笑也好好珍藏。",
                coverImagePath = "mock://wish-photo",
                plannedDate = now + 65 * day,
                createdAt = now - 22 * day,
                status = WishStatus.UNREALIZED,
                completion = null,
            ),
            Wish(
                id = "wish-escape",
                title = "一起玩一次密室逃脱",
                description = "谁先被吓到就请晚饭吧。",
                coverImagePath = "mock://wish-game",
                plannedDate = now + 12 * day,
                createdAt = now - 7 * day,
                status = WishStatus.UNREALIZED,
                completion = null,
            ),
            Wish(
                id = "wish-sunrise",
                title = "一起去看日出",
                description = "去海边，看一场属于我们的清晨。",
                coverImagePath = "mock://wish-sunrise",
                plannedDate = now - 210 * day,
                createdAt = now - 400 * day,
                status = WishStatus.REALIZED,
                completion = WishCompletion(
                    text = "我们在天还没亮的时候出发，看到了最温柔的天光。",
                    imagePaths = listOf(
                        "mock://wish-sunrise-1",
                        "mock://wish-sunrise-2",
                        "mock://wish-sunrise-3",
                    ),
                    completedAt = now - 26 * day,
                ),
            ),
            Wish(
                id = "wish-cake",
                title = "一起做小蛋糕",
                description = "想试一次把奶油抹得歪歪扭扭也很开心的下午。",
                coverImagePath = "mock://wish-cake",
                plannedDate = now - 45 * day,
                createdAt = now - 120 * day,
                status = WishStatus.REALIZED,
                completion = WishCompletion(
                    text = "虽然奶油裱花翻车了，但你笑得太可爱了，所以一切都值得。",
                    imagePaths = listOf("mock://wish-cake-1"),
                    completedAt = now - 18 * day,
                ),
            ),
            Wish(
                id = "wish-movie",
                title = "一起看一场露天电影",
                description = "带上小毯子和汽水，找个晚风舒服的夜晚。",
                coverImagePath = "mock://wish-movie",
                plannedDate = now - 15 * day,
                createdAt = now - 90 * day,
                status = WishStatus.REALIZED,
                completion = WishCompletion(
                    text = "电影已经记不太清了，但肩膀靠在一起的那一刻特别清楚。",
                    imagePaths = listOf(
                        "mock://wish-movie-1",
                        "mock://wish-movie-2",
                    ),
                    completedAt = now - 8 * day,
                ),
            ),
        )
    }
}
