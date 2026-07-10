package com.secretbase.app.data.anniversary

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class FakeAnniversaryRepository : AnniversaryRepository {

    private val anniversaries = MutableStateFlow(seedAnniversaries())

    override fun observeAnniversaries(): Flow<List<Anniversary>> = anniversaries.asStateFlow()

    override suspend fun addAnniversary(item: Anniversary): Result<Unit> = runCatching {
        anniversaries.update { current ->
            current + item.copy(id = if (item.id.isBlank()) "anniversary-${UUID.randomUUID()}" else item.id)
        }
    }

    override suspend fun updateAnniversary(item: Anniversary): Result<Unit> = runCatching {
        anniversaries.update { current ->
            current.map { anniversary -> if (anniversary.id == item.id) item else anniversary }
        }
    }

    override suspend fun deleteAnniversary(id: String): Result<Unit> = runCatching {
        anniversaries.update { current -> current.filterNot { it.id == id } }
    }

    private fun seedAnniversaries(): List<Anniversary> {
        val zone = ZoneId.systemDefault()
        fun millis(year: Int, month: Int, day: Int): Long =
            LocalDate.of(year, month, day).atStartOfDay(zone).toInstant().toEpochMilli()

        val now = System.currentTimeMillis()
        return listOf(
            Anniversary(
                id = "anniversary-love",
                title = "在一起的日子",
                date = millis(2023, 1, 1),
                repeatYearly = false,
                reminderType = AnniversaryReminder.NONE,
                createdAt = now - 500L * 24 * 60 * 60 * 1000,
                iconEmoji = "💗",
            ),
            Anniversary(
                id = "anniversary-first-date",
                title = "第一次约会",
                date = millis(2023, 1, 15),
                repeatYearly = true,
                reminderType = AnniversaryReminder.SAME_DAY,
                createdAt = now - 460L * 24 * 60 * 60 * 1000,
                iconEmoji = "🌸",
            ),
            Anniversary(
                id = "anniversary-first-trip",
                title = "第一次旅行",
                date = millis(2023, 5, 20),
                repeatYearly = true,
                reminderType = AnniversaryReminder.ONE_DAY_BEFORE,
                createdAt = now - 390L * 24 * 60 * 60 * 1000,
                iconEmoji = "✈️",
            ),
            Anniversary(
                id = "anniversary-first-growth",
                title = "第一次见家长",
                date = millis(2023, 10, 1),
                repeatYearly = false,
                reminderType = AnniversaryReminder.THREE_DAYS_BEFORE,
                createdAt = now - 260L * 24 * 60 * 60 * 1000,
                iconEmoji = "🍀",
            ),
            Anniversary(
                id = "anniversary-sheep-bday",
                title = "生日 - 小羊",
                date = millis(2023, 8, 15),
                repeatYearly = true,
                reminderType = AnniversaryReminder.ONE_DAY_BEFORE,
                createdAt = now - 300L * 24 * 60 * 60 * 1000,
                iconEmoji = "🎂",
            ),
            Anniversary(
                id = "anniversary-chick-bday",
                title = "生日 - 小耶",
                date = millis(2023, 3, 22),
                repeatYearly = true,
                reminderType = AnniversaryReminder.THREE_DAYS_BEFORE,
                createdAt = now - 300L * 24 * 60 * 60 * 1000,
                iconEmoji = "🎁",
            ),
        )
    }
}
