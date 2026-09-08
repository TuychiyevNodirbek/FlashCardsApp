package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

private const val REMINDER_IDENTIFIER = "flashcards.daily_reminder"

/** Ежедневное локальное уведомление через UNUserNotificationCenter (аналог Android AlarmManager-напоминания). */
actual class ReminderScheduler {
    actual fun scheduleReminder(time: String) {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val content = UNMutableNotificationContent().apply {
            setTitle("Пора повторить слова")
            setBody("Загляни во FlashDeck, чтобы не потерять серию")
        }

        val dateComponents = NSDateComponents().apply {
            this.hour = hour.toLong()
            this.minute = minute.toLong()
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents = dateComponents,
            repeats = true
        )
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = REMINDER_IDENTIFIER,
            content = content,
            trigger = trigger
        )

        UNUserNotificationCenter.currentNotificationCenter()
            .addNotificationRequest(request, withCompletionHandler = null)
    }

    actual fun cancelReminder() {
        UNUserNotificationCenter.currentNotificationCenter()
            .removePendingNotificationRequestsWithIdentifiers(listOf(REMINDER_IDENTIFIER))
    }
}

@Composable
actual fun rememberReminderScheduler(): ReminderScheduler = remember { ReminderScheduler() }
