package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// TODO Фаза 6: запрос через UNUserNotificationCenter.requestAuthorization. Пока считаем разрешённым.
@Composable
actual fun rememberNotificationPermissionState(): NotificationPermissionState =
    remember { NotificationPermissionState(granted = true, request = {}) }
