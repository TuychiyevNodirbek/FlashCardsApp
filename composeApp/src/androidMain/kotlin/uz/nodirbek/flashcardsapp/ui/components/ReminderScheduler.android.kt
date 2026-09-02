package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import uz.nodirbek.flashcardsapp.notification.NotificationScheduler

actual class ReminderScheduler(private val delegate: NotificationScheduler) {
    actual fun scheduleReminder(time: String) = delegate.scheduleReminder(time)
    actual fun cancelReminder() = delegate.cancelReminder()
}

@Composable
actual fun rememberReminderScheduler(): ReminderScheduler {
    val context = LocalContext.current
    return remember { ReminderScheduler(NotificationScheduler(context)) }
}
