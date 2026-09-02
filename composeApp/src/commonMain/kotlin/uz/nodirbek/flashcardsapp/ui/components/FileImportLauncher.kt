package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.runtime.Composable
import uz.nodirbek.flashcardsapp.shared.data.transfer.AnkiImportResult

/** Результат выбора файла для импорта: обычный текст (CSV/.md-колода), разобранная колода Anki, либо ошибка. */
sealed interface FileImportOutcome {
    data class PlainText(val fileName: String, val content: String) : FileImportOutcome
    data class Anki(val result: AnkiImportResult) : FileImportOutcome
    data class Error(val message: String) : FileImportOutcome
}

/**
 * Открывает системный выбор файла. Android — полноценно: CSV/.md читаются как текст,
 * .apkg разбирается через AnkiApkgImporter (SQLite). iOS — TODO Фаза 6
 * (UIDocumentPickerViewController + разбор .apkg без Android SQLite API), пока
 * launch() сразу отдаёт [FileImportOutcome.Error].
 */
expect class FileImportLauncher {
    fun launch()
}

@Composable
expect fun rememberFileImportLauncher(onResult: (FileImportOutcome) -> Unit): FileImportLauncher
