package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// TODO Фаза 6: реализовать через UNUserNotificationCenter.
actual class ReminderScheduler {
    actual fun scheduleReminder(time: String) {}
    actual fun cancelReminder() {}
}

@Composable
actual fun rememberReminderScheduler(): ReminderScheduler = remember { ReminderScheduler() }
