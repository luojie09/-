package com.secretbase.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class HomeRepository(private val context: Context) {

    private val sharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val zoneId: ZoneId = ZoneId.systemDefault()

    fun loadSnapshot(): HomeSnapshot {
        val visuals = parseVisualConfig(readAssetJson(VISUAL_CONFIG_ASSET))
        val content = readAssetJson(CONTENT_CONFIG_ASSET)

        val coupleJson = content.getJSONObject("couple")
        val couple = CoupleConfig(
            leftName = coupleJson.getString("leftName"),
            rightName = coupleJson.getString("rightName"),
            currentUserId = coupleJson.getString("currentUserId"),
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
                editable = json.getBoolean("editable"),
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

    fun saveMood(userId: String, mood: MoodOption) {
        val root = loadJsonObject(sharedPreferences.getString(KEY_MOOD_RECORDS, null))
        val record = JSONObject()
            .put("label", mood.label)
            .put("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
        root.put(userId, record)
        sharedPreferences.edit().putString(KEY_MOOD_RECORDS, root.toString()).apply()
    }

    fun addQuickNote(note: String) {
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

    private fun loadPersistedMoods(): Map<String, PersistedMood> {
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

    private fun loadUserActivities(): List<ActivityRecord> {
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

    companion object {
        private const val PREFS_NAME = "secret_base_home_state"
        private const val KEY_MOOD_RECORDS = "mood_records"
        private const val KEY_USER_ACTIVITIES = "user_activities"
        private const val VISUAL_CONFIG_ASSET = "homepage_visual_config.json"
        private const val CONTENT_CONFIG_ASSET = "homepage_content.json"
    }
}
