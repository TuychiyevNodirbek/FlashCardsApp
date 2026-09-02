package uz.nodirbek.flashcardsapp.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberAppExiter(): () -> Unit {
    val context = LocalContext.current
    return { (context as? ComponentActivity)?.finish() }
}
