package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.runtime.Composable

/**
 * Планирует/отменяет ежедневное напоминание о повторении. Android — AlarmManager
 * (см. notification/NotificationScheduler), iOS — TODO Фаза 6: UNUserNotificationCenter.
 */
expect class ReminderScheduler {
    fun scheduleReminder(time: String)
    fun cancelReminder()
}

@Composable
expect fun rememberReminderScheduler(): ReminderScheduler
