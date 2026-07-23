package uz.nodirbek.flashcardsapp.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_stats")

class PreferencesDataStore(private val context: Context) {
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
        private val TTS_LANG_KEY = stringPreferencesKey("tts_lang") // "en" | "en-gb" | "ru" | "de"
        private val TTS_SPEED_KEY = floatPreferencesKey("tts_speed")
    }

    val streak: Flow<Int> = context.dataStore.data.map { it[STREAK_KEY] ?: 0 }
    val streakRecord: Flow<Int> = context.dataStore.data.map { it[STREAK_RECORD_KEY] ?: 0 }
    val lastActiveDate: Flow<String?> = context.dataStore.data.map { it[LAST_ACTIVE_DATE_KEY] }
    val xp: Flow<Long> = context.dataStore.data.map { it[XP_KEY] ?: 0L }
    val reminderEnabled: Flow<Boolean> = context.dataStore.data.map { it[REMINDER_ENABLED_KEY] ?: false }
    val reminderTime: Flow<String> = context.dataStore.data.map { it[REMINDER_TIME_KEY] ?: "09:00" }
    val theme: Flow<String> = context.dataStore.data.map { it[THEME_KEY] ?: "system" }
    val dailyGoal: Flow<Int> = context.dataStore.data.map { it[DAILY_GOAL_KEY] ?: 20 }
    val dailyNewLimit: Flow<Int> = context.dataStore.data.map { it[DAILY_NEW_LIMIT_KEY] ?: 20 }
    val dailyReviewLimit: Flow<Int> = context.dataStore.data.map { it[DAILY_REVIEW_LIMIT_KEY] ?: 100 }
    val ttsLang: Flow<String> = context.dataStore.data.map { it[TTS_LANG_KEY] ?: "en" }
    val ttsSpeed: Flow<Float> = context.dataStore.data.map { it[TTS_SPEED_KEY] ?: 1f }

    suspend fun setStreak(value: Int) {
        context.dataStore.edit { it[STREAK_KEY] = value }
    }

    suspend fun setLastActiveDate(value: String) {
        context.dataStore.edit { it[LAST_ACTIVE_DATE_KEY] = value }
    }

    suspend fun addXp(amount: Long) {
        context.dataStore.edit {
            val current = it[XP_KEY] ?: 0L
            it[XP_KEY] = current + amount
        }
    }

    suspend fun setXp(value: Long) {
        context.dataStore.edit { it[XP_KEY] = value }
    }

    suspend fun setReminderEnabled(value: Boolean) {
        context.dataStore.edit { it[REMINDER_ENABLED_KEY] = value }
    }

    suspend fun setReminderTime(value: String) {
        context.dataStore.edit { it[REMINDER_TIME_KEY] = value }
    }

    suspend fun setStreakRecord(value: Int) {
        context.dataStore.edit { it[STREAK_RECORD_KEY] = value }
    }

    suspend fun updateStreakRecord(currentStreak: Int) {
        context.dataStore.edit { prefs ->
            val record = prefs[STREAK_RECORD_KEY] ?: 0
            if (currentStreak > record) prefs[STREAK_RECORD_KEY] = currentStreak
        }
    }

    suspend fun setTheme(value: String) {
        context.dataStore.edit { it[THEME_KEY] = value }
    }

    suspend fun setDailyGoal(value: Int) {
        context.dataStore.edit { it[DAILY_GOAL_KEY] = value }
    }

    suspend fun setDailyNewLimit(value: Int) {
        context.dataStore.edit { it[DAILY_NEW_LIMIT_KEY] = value }
    }

    suspend fun setDailyReviewLimit(value: Int) {
        context.dataStore.edit { it[DAILY_REVIEW_LIMIT_KEY] = value }
    }

    suspend fun setTtsLang(value: String) {
        context.dataStore.edit { it[TTS_LANG_KEY] = value }
    }

    suspend fun setTtsSpeed(value: Float) {
        context.dataStore.edit { it[TTS_SPEED_KEY] = value }
    }

    /** Wipes streak, record, XP and activity date; keeps theme and other settings. */
    suspend fun resetProgress() {
        context.dataStore.edit {
            it[STREAK_KEY] = 0
            it[STREAK_RECORD_KEY] = 0
            it[XP_KEY] = 0L
            it.remove(LAST_ACTIVE_DATE_KEY)
        }
    }
}
