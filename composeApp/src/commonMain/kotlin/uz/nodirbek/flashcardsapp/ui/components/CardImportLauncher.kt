package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.runtime.Composable
import uz.nodirbek.flashcardsapp.shared.data.transfer.CardParseResult

/** Открывает системный выбор файла и парсит CSV/.apkg в карточки для [deckId]. */
expect class CardImportLauncher {
    fun launch(deckId: String)
}

/** iOS: пока заглушка, TODO Фаза 6 — UIDocumentPickerViewController. */
@Composable
expect fun rememberCardImportLauncher(onResult: (CardParseResult) -> Unit): CardImportLauncher
