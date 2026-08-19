package uz.nodirbek.flashcardsapp.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import uz.nodirbek.flashcardsapp.data.transfer.parseCardsFromUri
import uz.nodirbek.flashcardsapp.shared.data.transfer.CardParseResult

actual class CardImportLauncher(
    private val launcher: ActivityResultLauncher<String>,
    private val setPendingDeckId: (String) -> Unit
) {
    actual fun launch(deckId: String) {
        setPendingDeckId(deckId)
        launcher.launch("*/*")
    }
}

@Composable
actual fun rememberCardImportLauncher(onResult: (CardParseResult) -> Unit): CardImportLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingDeckId by remember { mutableStateOf<String?>(null) }

    val activityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val deckId = pendingDeckId
        pendingDeckId = null
        if (uri == null || deckId == null) return@rememberLauncherForActivityResult
        scope.launch {
            onResult(parseCardsFromUri(uri, context, deckId))
        }
    }

    return remember(activityLauncher) {
        CardImportLauncher(activityLauncher) { pendingDeckId = it }
    }
}
