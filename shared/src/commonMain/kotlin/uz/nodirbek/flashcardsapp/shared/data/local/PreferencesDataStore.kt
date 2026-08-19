package uz.nodirbek.flashcardsapp.shared.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

/** Общая фабрика: платформенный код передаёт только путь к файлу настроек. */
internal fun createPreferencesDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })

class PreferencesDataStore(private val dataStore: DataStore<Preferences>) {
    companion object {
        private val STREAK_KEY = intPreferencesKey("streak")
        private val STREAK_RECORD_KEY = intPreferencesKey("streak_record")
        private val LAST_ACTIVE_DATE_KEY = stringPreferencesKey("last_active_date")
        private val XP_KEY = longPreferencesKey("xp")
        private val REMINDER_ENABLED_KEY = booleanPreferencesKey("reminder_enabled")
        private val REMINDER_TIME_KEY = stringPreferencesKey("reminder_time")
        private val THEME_KEY = stringPreferencesKey("theme") // "light" | "dark" | "system"
        private val DAILY_GOAL_KEY = intPreferencesKey("daily_goal")
        private val DAILY_NEW_LIMIT_KEY = intPreferencesKey("daily_new_limit")
        private val DAILY_REVIEW_LIMIT_KEY = intPreferencesKey("daily_review_limit")
        private val TTS_LANG_KEY = stringPreferencesKey("tts_lang") // "en" | "en-gb" | "ru" | "de" | "es" | "fr" | "it" | "pt" | "zh" | "ja" | "ko" | "ar" | "tr" | "la"
        private val TTS_SPEED_KEY = floatPreferencesKey("tts_speed")
        private val ONBOARDING_SEEN_KEY = booleanPreferencesKey("onboarding_seen")
        private val UNLOCKED_ACHIEVEMENTS_KEY = stringSetPreferencesKey("unlocked_achievements")
    }

    val streak: Flow<Int> = dataStore.data.map { it[STREAK_KEY] ?: 0 }
    val streakRecord: Flow<Int> = dataStore.data.map { it[STREAK_RECORD_KEY] ?: 0 }
    val lastActiveDate: Flow<String?> = dataStore.data.map { it[LAST_ACTIVE_DATE_KEY] }
    val xp: Flow<Long> = dataStore.data.map { it[XP_KEY] ?: 0L }
    val reminderEnabled: Flow<Boolean> = dataStore.data.map { it[REMINDER_ENABLED_KEY] ?: false }
    val reminderTime: Flow<String> = dataStore.data.map { it[REMINDER_TIME_KEY] ?: "09:00" }
    val theme: Flow<String> = dataStore.data.map { it[THEME_KEY] ?: "system" }
    val dailyGoal: Flow<Int> = dataStore.data.map { it[DAILY_GOAL_KEY] ?: 20 }
    val dailyNewLimit: Flow<Int> = dataStore.data.map { it[DAILY_NEW_LIMIT_KEY] ?: 20 }
    val dailyReviewLimit: Flow<Int> = dataStore.data.map { it[DAILY_REVIEW_LIMIT_KEY] ?: 100 }
    val ttsLang: Flow<String> = dataStore.data.map { it[TTS_LANG_KEY] ?: "en" }
    val ttsSpeed: Flow<Float> = dataStore.data.map { it[TTS_SPEED_KEY] ?: 1f }
    val onboardingSeen: Flow<Boolean> = dataStore.data.map { it[ONBOARDING_SEEN_KEY] ?: false }
    val unlockedAchievements: Flow<Set<String>> = dataStore.data.map { it[UNLOCKED_ACHIEVEMENTS_KEY] ?: emptySet() }

    suspend fun setStreak(value: Int) {
        dataStore.edit { it[STREAK_KEY] = value }
    }

    suspend fun setLastActiveDate(value: String) {
        dataStore.edit { it[LAST_ACTIVE_DATE_KEY] = value }
    }

    suspend fun addXp(amount: Long) {
        dataStore.edit {
            val current = it[XP_KEY] ?: 0L
            it[XP_KEY] = current + amount
        }
    }

    suspend fun setXp(value: Long) {
        dataStore.edit { it[XP_KEY] = value }
    }

    suspend fun setReminderEnabled(value: Boolean) {
        dataStore.edit { it[REMINDER_ENABLED_KEY] = value }
    }

    suspend fun setReminderTime(value: String) {
        dataStore.edit { it[REMINDER_TIME_KEY] = value }
    }

    suspend fun setStreakRecord(value: Int) {
        dataStore.edit { it[STREAK_RECORD_KEY] = value }
    }

    suspend fun updateStreakRecord(currentStreak: Int) {
        dataStore.edit { prefs ->
            val record = prefs[STREAK_RECORD_KEY] ?: 0
            if (currentStreak > record) prefs[STREAK_RECORD_KEY] = currentStreak
        }
    }

    suspend fun setTheme(value: String) {
        dataStore.edit { it[THEME_KEY] = value }
    }

    suspend fun setDailyGoal(value: Int) {
        dataStore.edit { it[DAILY_GOAL_KEY] = value }
    }

    suspend fun setDailyNewLimit(value: Int) {
        dataStore.edit { it[DAILY_NEW_LIMIT_KEY] = value }
    }

    suspend fun setDailyReviewLimit(value: Int) {
        dataStore.edit { it[DAILY_REVIEW_LIMIT_KEY] = value }
    }

    suspend fun setTtsLang(value: String) {
        dataStore.edit { it[TTS_LANG_KEY] = value }
    }

    suspend fun setTtsSpeed(value: Float) {
        dataStore.edit { it[TTS_SPEED_KEY] = value }
    }

    suspend fun setOnboardingSeen() {
        dataStore.edit { it[ONBOARDING_SEEN_KEY] = true }
    }

    suspend fun unlockAchievement(id: String) {
        dataStore.edit { prefs ->
            val current = prefs[UNLOCKED_ACHIEVEMENTS_KEY] ?: emptySet()
            prefs[UNLOCKED_ACHIEVEMENTS_KEY] = current + id
        }
    }

    /** Wipes streak, record, XP and activity date; keeps theme and other settings. */
    suspend fun resetProgress() {
        dataStore.edit {
            it[STREAK_KEY] = 0
            it[STREAK_RECORD_KEY] = 0
            it[XP_KEY] = 0L
            it.remove(LAST_ACTIVE_DATE_KEY)
        }
    }
}
