package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.runtime.Composable

/** Android: разрешение POST_NOTIFICATIONS (API 33+). iOS: TODO Фаза 6 — UNUserNotificationCenter, пока считаем разрешённым. */
data class NotificationPermissionState(val granted: Boolean, val request: () -> Unit)

@Composable
expect fun rememberNotificationPermissionState(): NotificationPermissionState
