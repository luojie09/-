package com.secretbase.app.data.anniversary

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
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
    private val client: SupabaseClient,
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
        client.from(TABLE_NAME).insert(item.toRow())
        refreshAnniversaries()
    }

    override suspend fun updateAnniversary(item: Anniversary): Result<Unit> = runCatching {
        client.from(TABLE_NAME).update(item.toRow()) {
            filter {
                eq("id", item.id)
            }
        }
        refreshAnniversaries()
    }

    override suspend fun deleteAnniversary(id: String): Result<Unit> = runCatching {
        client.from(TABLE_NAME).delete {
            filter {
                eq("id", id)
            }
        }
        refreshAnniversaries()
    }

    private suspend fun refreshAnniversaries() {
        val rows = client.from(TABLE_NAME)
            .select {
                order("date", order = Order.ASCENDING)
            }
            .decodeList<AnniversaryRow>()

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
        )

    private fun AnniversaryRow.toDomain(): Anniversary =
        Anniversary(
            id = id,
            title = title,
            date = dateToMillis(date),
            repeatYearly = repeatYearly,
            reminderType = reminderType.toReminderType(),
            createdAt = instantToMillis(createdAt),
        )

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
    )

    private companion object {
        private const val TABLE_NAME = "anniversaries"
        private const val TAG = "SupabaseAnniversaryRepo"
    }
}
