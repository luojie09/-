package com.secretbase.app.data.anniversary

import android.util.Log
import com.secretbase.app.data.supabase.SupabaseRestClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class SupabaseAnniversaryRepository(
    private val client: SupabaseRestClient,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : AnniversaryRepository {

    private val anniversaries = MutableStateFlow<List<Anniversary>>(emptyList())

    init {
        scope.launch {
            runCatching { refreshAnniversaries() }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load anniversaries from Supabase", error)
                }
        }
    }

    override fun observeAnniversaries(): Flow<List<Anniversary>> = anniversaries.asStateFlow()

    override suspend fun addAnniversary(item: Anniversary): Result<Unit> = runCatching {
        insertWithIconCompatibility(item)
        refreshAnniversaries()
    }

    override suspend fun updateAnniversary(item: Anniversary): Result<Unit> = runCatching {
        updateWithIconCompatibility(item)
        refreshAnniversaries()
    }

    override suspend fun deleteAnniversary(id: String): Result<Unit> = runCatching {
        client.delete(TABLE_NAME, client.eq("id", id))
        refreshAnniversaries()
    }

    private suspend fun refreshAnniversaries() {
        val rows = client.select<AnniversaryRow>(
            table = TABLE_NAME,
            query = client.and("select=*", client.order("date")),
        )

        anniversaries.value = rows.map { row -> row.toDomain() }
    }

    private fun Anniversary.toRow(): AnniversaryRow =
        AnniversaryRow(
            id = id.ifBlank { "anniversary-${UUID.randomUUID()}" },
            title = title,
            date = millisToDate(date),
            repeatYearly = repeatYearly,
            reminderType = reminderType.name,
            createdAt = millisToInstant(createdAt),
            iconEmoji = iconEmoji,
        )

    private fun Anniversary.toLegacyRow(): LegacyAnniversaryRow =
        LegacyAnniversaryRow(
            id = id.ifBlank { "anniversary-${UUID.randomUUID()}" },
            title = title,
            date = millisToDate(date),
            repeatYearly = repeatYearly,
            reminderType = reminderType.name,
            createdAt = millisToInstant(createdAt),
        )

    private fun AnniversaryRow.toDomain(): Anniversary =
        Anniversary(
            id = id,
            title = title,
            date = dateToMillis(date),
            repeatYearly = repeatYearly,
            reminderType = reminderType.toReminderType(),
            createdAt = instantToMillis(createdAt),
            iconEmoji = iconEmoji,
        )

    private suspend fun insertWithIconCompatibility(item: Anniversary) {
        runCatching {
            client.insert(TABLE_NAME, item.toRow())
        }.getOrElse { error ->
            if (error.isMissingIconEmojiColumn()) {
                client.insert(TABLE_NAME, item.toLegacyRow())
            } else {
                throw error
            }
        }
    }

    private suspend fun updateWithIconCompatibility(item: Anniversary) {
        runCatching {
            client.update(TABLE_NAME, client.eq("id", item.id), item.toRow())
        }.getOrElse { error ->
            if (error.isMissingIconEmojiColumn()) {
                client.update(TABLE_NAME, client.eq("id", item.id), item.toLegacyRow())
            } else {
                throw error
            }
        }
    }

    private fun Throwable.isMissingIconEmojiColumn(): Boolean =
        message?.contains("icon_emoji", ignoreCase = true) == true

    private fun String.toReminderType(): AnniversaryReminder =
        AnniversaryReminder.entries.firstOrNull { it.name == this } ?: AnniversaryReminder.NONE

    private fun millisToDate(value: Long): String =
        Instant.ofEpochMilli(value)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE)

    private fun dateToMillis(value: String): Long =
        LocalDate.parse(value)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun millisToInstant(value: Long): String =
        Instant.ofEpochMilli(value).toString()

    private fun instantToMillis(value: String): Long =
        Instant.parse(value).toEpochMilli()

    @Serializable
    private data class AnniversaryRow(
        val id: String,
        val title: String,
        val date: String,
        @SerialName("repeat_yearly") val repeatYearly: Boolean,
        @SerialName("reminder_type") val reminderType: String,
        @SerialName("created_at") val createdAt: String,
        @SerialName("icon_emoji") val iconEmoji: String? = null,
    )

    @Serializable
    private data class LegacyAnniversaryRow(
        val id: String,
        val title: String,
        val date: String,
        @SerialName("repeat_yearly") val repeatYearly: Boolean,
        @SerialName("reminder_type") val reminderType: String,
        @SerialName("created_at") val createdAt: String,
    )

    private companion object {
        private const val TABLE_NAME = "anniversaries"
        private const val TAG = "SupabaseAnniversaryRepo"
    }
}
