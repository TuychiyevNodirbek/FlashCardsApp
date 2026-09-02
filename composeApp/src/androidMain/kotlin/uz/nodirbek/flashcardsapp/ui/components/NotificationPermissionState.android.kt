package uz.nodirbek.flashcardsapp.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

@Composable
actual fun rememberNotificationPermissionState(): NotificationPermissionState {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasNotificationPermission(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        granted = result
    }
    return remember(granted) {
        NotificationPermissionState(granted = granted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                granted = true
            }
        }
    }
}
