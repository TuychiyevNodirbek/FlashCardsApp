package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/** Разрешение на локальные уведомления через UNUserNotificationCenter. */
@Composable
actual fun rememberNotificationPermissionState(): NotificationPermissionState {
    var granted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
            val authorized = settings?.authorizationStatus == UNAuthorizationStatusAuthorized
            dispatch_async(dispatch_get_main_queue()) {
                granted = authorized
            }
        }
    }

    return remember(granted) {
        NotificationPermissionState(granted = granted) {
            val options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound
            UNUserNotificationCenter.currentNotificationCenter()
                .requestAuthorizationWithOptions(options) { success, _ ->
                    dispatch_async(dispatch_get_main_queue()) {
                        granted = success
                    }
                }
        }
    }
}
