package com.secretbase.app.data

import android.content.Context
import com.secretbase.app.data.supabase.SupabaseConfig
import com.secretbase.app.data.supabase.isoInstantToMillis
import com.secretbase.app.data.supabase.millisToIsoDate
import com.secretbase.app.data.supabase.millisToIsoInstant
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class HomeRepository(
    private val context: Context,
    private val currentUserId: String,
    private val client: SupabaseClient? = null,
) {

    private val sharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val zoneId: ZoneId = ZoneId.systemDefault()

    suspend fun loadSnapshot(): HomeSnapshot {
        val visuals = parseVisualConfig(readAssetJson(VISUAL_CONFIG_ASSET))
        val content = readAssetJson(CONTENT_CONFIG_ASSET)

        val coupleJson = content.getJSONObject("couple")
        val couple = CoupleConfig(
            leftName = coupleJson.getString("leftName"),
            rightName = coupleJson.getString("rightName"),
            currentUserId = currentUserId,
            relationshipLabel = coupleJson.getString("relationshipLabel"),
            relationshipStartDate = LocalDate.parse(coupleJson.getString("relationshipStartDate")),
        )

        val topActionMessages = content.getJSONObject("topActions").let { json ->
            TopActionMessages(
                message = json.getString("message"),
                settings = json.getString("settings"),
            )
        }

        val quickRecord = content.getJSONObject("quickRecord").let { json ->
            QuickRecordConfig(
                placeholder = json.getString("placeholder"),
                entryMessage = json.getString("entryMessage"),
                galleryMessage = json.getString("galleryMessage"),
                cameraMessage = json.getString("cameraMessage"),
                calendarMessage = json.getString("calendarMessage"),
            )
        }

        val bottomNavMessages = content.getJSONObject("bottomNavMessages").let { json ->
            BottomNavMessages(
                anniversary = json.getString("anniversary"),
                album = json.getString("album"),
                profile = json.getString("profile"),
            )
        }

        val today = LocalDate.now()
        val persistedMoods = loadPersistedMoods()

        val users = content.getJSONArray("users").mapObjects { json ->
            val defaultMood = MoodOption.fromLabel(json.getString("defaultMood")) ?: MoodOption.Happy
            val persisted = persistedMoods[json.getString("id")]
            val hasRecordedToday = persisted?.date == today
            UserMoodState(
                id = json.getString("id"),
                name = json.getString("name"),
                editable = json.getString("id") == currentUserId,
                currentMood = if (hasRecordedToday) {
                    MoodOption.fromLabel(persisted?.label.orEmpty()) ?: defaultMood
                } else {
                    defaultMood
                },
                showUnrecordedState = persisted != null && !hasRecordedToday,
            )
        }

        val stats = content.getJSONObject("stats").let { json ->
            HomeStats(
                albumCount = json.getInt("albumCount"),
                wishCompleted = json.getInt("wishCompleted"),
                wishTotal = json.getInt("wishTotal"),
                messageUnread = json.getInt("messageUnread"),
                diaryStreak = json.getInt("diaryStreak"),
                activeTasks = json.getInt("activeTasks"),
            )
        }

        val features = content.getJSONArray("features").mapObjects { json ->
            FeatureSpec(
                id = json.getString("id"),
                title = json.getString("title"),
                iconSlot = json.getString("iconSlot"),
                clickMessage = json.getString("clickMessage"),
            )
        }

        val seededActivities = content.getJSONArray("recentActivities").mapObjects { json ->
            ActivityRecord(
                id = json.getString("id"),
                title = json.getString("title"),
                iconSlot = json.getString("iconSlot"),
                timestamp = parseDateTime(json.getString("timestamp")),
                clickMessage = json.getString("clickMessage"),
            )
        }

        val userActivities = loadUserActivities()

        return HomeSnapshot(
            visuals = visuals,
            couple = couple,
            topActionMessages = topActionMessages,
            quickRecord = quickRecord,
            bottomNavMessages = bottomNavMessages,
            users = users,
            stats = stats,
            features = features,
            recentActivities = (seededActivities + userActivities).sortedByDescending { it.timestamp },
            recentActivityEmptyText = content.getString("recentActivityEmptyText"),
            recentActivityListMessage = content.getString("recentActivityListMessage"),
        )
    }

    suspend fun saveMood(userId: String, mood: MoodOption) {
        if (SupabaseConfig.isConfigured && client != null) {
            val supabaseClient = client
            runCatching {
                supabaseClient.from(MOODS_TABLE).delete {
                    filter {
                        eq("user_id", userId)
                    }
                }
                supabaseClient.from(MOODS_TABLE).insert(
                    MoodRow(
                        userId = userId,
                        moodLabel = mood.label,
                        recordedDate = millisToIsoDate(System.currentTimeMillis()),
                        updatedAt = millisToIsoInstant(System.currentTimeMillis()),
                    ),
                )
            }.onSuccess {
                return
            }
        }

        val root = loadJsonObject(sharedPreferences.getString(KEY_MOOD_RECORDS, null))
        val record = JSONObject()
            .put("label", mood.label)
            .put("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
        root.put(userId, record)
        sharedPreferences.edit().putString(KEY_MOOD_RECORDS, root.toString()).apply()
    }

    suspend fun addQuickNote(note: String) {
        if (SupabaseConfig.isConfigured && client != null) {
            val supabaseClient = client
            runCatching {
                supabaseClient.from(NOTES_TABLE).insert(
                    QuickNoteRow(
                        id = "note-${UUID.randomUUID()}",
                        title = note,
                        iconSlot = "activityNote",
                        createdAt = millisToIsoInstant(System.currentTimeMillis()),
                        clickMessage = "待接入动态详情页",
                    ),
                )
            }.onSuccess {
                return
            }
        }

        val root = loadJsonArray(sharedPreferences.getString(KEY_USER_ACTIVITIES, null))
        val activity = JSONObject()
            .put("id", "note-${UUID.randomUUID()}")
            .put("title", note)
            .put("iconSlot", "activityNote")
            .put("timestamp", Instant.now().toEpochMilli())
            .put("clickMessage", "待接入动态详情页")
        root.put(activity)
        sharedPreferences.edit().putString(KEY_USER_ACTIVITIES, root.toString()).apply()
    }

    private suspend fun loadPersistedMoods(): Map<String, PersistedMood> {
        if (SupabaseConfig.isConfigured && client != null) {
            val supabaseClient = client
            runCatching {
                supabaseClient.from(MOODS_TABLE)
                    .select {
                        order("updated_at", order = Order.DESCENDING)
                    }
                    .decodeList<MoodRow>()
                    .associate { row ->
                        row.userId to PersistedMood(
                            label = row.moodLabel,
                            date = LocalDate.parse(row.recordedDate),
                        )
                    }
            }.onSuccess { return it }
        }

        val root = loadJsonObject(sharedPreferences.getString(KEY_MOOD_RECORDS, null))
        return buildMap {
            val iterator = root.keys()
            while (iterator.hasNext()) {
                val key = iterator.next()
                val record = root.getJSONObject(key)
                put(
                    key,
                    PersistedMood(
                        label = record.getString("label"),
                        date = LocalDate.parse(record.getString("date")),
                    ),
                )
            }
        }
    }

    private suspend fun loadUserActivities(): List<ActivityRecord> {
        if (SupabaseConfig.isConfigured && client != null) {
            val supabaseClient = client
            runCatching {
                supabaseClient.from(NOTES_TABLE)
                    .select {
                        order("created_at", order = Order.DESCENDING)
                    }
                    .decodeList<QuickNoteRow>()
                    .map { row ->
                        ActivityRecord(
                            id = row.id,
                            title = row.title,
                            iconSlot = row.iconSlot,
                            timestamp = Instant.ofEpochMilli(isoInstantToMillis(row.createdAt)),
                            clickMessage = row.clickMessage,
                        )
                    }
            }.onSuccess { return it }
        }

        val root = loadJsonArray(sharedPreferences.getString(KEY_USER_ACTIVITIES, null))
        return root.mapObjects { json ->
            ActivityRecord(
                id = json.getString("id"),
                title = json.getString("title"),
                iconSlot = json.getString("iconSlot"),
                timestamp = Instant.ofEpochMilli(json.getLong("timestamp")),
                clickMessage = json.getString("clickMessage"),
            )
        }
    }

    private fun parseVisualConfig(json: JSONObject): HomeVisuals {
        val avatarResByUserId = buildMap {
            val avatarJson = json.getJSONObject("avatarSlots")
            val iterator = avatarJson.keys()
            while (iterator.hasNext()) {
                val key = iterator.next()
                resolveDrawable(avatarJson.getString(key))?.let { put(key, it) }
            }
        }

        val iconResBySlot = buildMap {
            val iconJson = json.getJSONObject("iconSlots")
            val iterator = iconJson.keys()
            while (iterator.hasNext()) {
                val key = iterator.next()
                resolveDrawable(iconJson.getString(key))?.let { put(key, it) }
            }
        }

        val heroImageName = json.optString("heroImageRes")
            .ifBlank { json.optString("heroIllustration") }

        return HomeVisuals(
            hero = HeroVisualConfig(
                imageRes = resolveDrawable(heroImageName),
                gradientStartHex = json.optString("heroGradientStart", "#FFEEF3"),
                gradientMiddleHex = json.optString("heroGradientMiddle", "#FFF7F8"),
                gradientEndHex = json.optString("heroGradientEnd", "#FFFBFA"),
                heightDp = json.optInt("heroHeightDp", 250),
                bottomFadeHeightDp = json.optInt("heroBottomFadeHeightDp", 72),
                relationshipCardOverlapDp = json.optInt("relationshipCardOverlapDp", 24),
            ),
            backgroundOverlayRes = resolveDrawable(json.optString("backgroundOverlay")),
            avatarResByUserId = avatarResByUserId,
            iconResBySlot = iconResBySlot,
        )
    }

    private fun readAssetJson(fileName: String): JSONObject =
        JSONObject(
            context.assets.open(fileName).bufferedReader().use { reader ->
                reader.readText()
            },
        )

    private fun resolveDrawable(name: String?): Int? {
        if (name.isNullOrBlank()) return null
        val resourceId = context.resources.getIdentifier(name, "drawable", context.packageName)
        return resourceId.takeIf { it != 0 }
    }

    private fun parseDateTime(value: String): Instant =
        LocalDateTime.parse(value).atZone(zoneId).toInstant()

    private fun loadJsonObject(raw: String?): JSONObject =
        if (raw.isNullOrBlank()) JSONObject() else JSONObject(raw)

    private fun loadJsonArray(raw: String?): JSONArray =
        if (raw.isNullOrBlank()) JSONArray() else JSONArray(raw)

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
        val items = mutableListOf<T>()
        for (index in 0 until length()) {
            items += transform(getJSONObject(index))
        }
        return items
    }

    private data class PersistedMood(
        val label: String,
        val date: LocalDate,
    )

    @Serializable
    private data class MoodRow(
        @SerialName("user_id") val userId: String,
        @SerialName("mood_label") val moodLabel: String,
        @SerialName("recorded_date") val recordedDate: String,
        @SerialName("updated_at") val updatedAt: String,
    )

    @Serializable
    private data class QuickNoteRow(
        val id: String,
        val title: String,
        @SerialName("icon_slot") val iconSlot: String,
        @SerialName("created_at") val createdAt: String,
        @SerialName("click_message") val clickMessage: String,
    )

    companion object {
        private const val PREFS_NAME = "secret_base_home_state"
        private const val KEY_MOOD_RECORDS = "mood_records"
        private const val KEY_USER_ACTIVITIES = "user_activities"
        private const val VISUAL_CONFIG_ASSET = "homepage_visual_config.json"
        private const val CONTENT_CONFIG_ASSET = "homepage_content.json"
        private const val MOODS_TABLE = "user_moods"
        private const val NOTES_TABLE = "home_quick_notes"
    }
}
