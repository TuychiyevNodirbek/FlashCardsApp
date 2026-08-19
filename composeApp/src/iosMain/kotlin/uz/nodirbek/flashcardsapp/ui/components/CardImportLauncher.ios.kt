package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import uz.nodirbek.flashcardsapp.shared.data.transfer.CardParseResult

// TODO Фаза 6: UIDocumentPickerViewController на iOS.
actual class CardImportLauncher {
    actual fun launch(deckId: String) {}
}

@Composable
actual fun rememberCardImportLauncher(onResult: (CardParseResult) -> Unit): CardImportLauncher =
    remember { CardImportLauncher() }
