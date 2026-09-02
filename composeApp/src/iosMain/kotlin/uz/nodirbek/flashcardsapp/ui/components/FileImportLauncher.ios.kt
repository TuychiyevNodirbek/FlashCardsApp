package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// TODO Фаза 6: UIDocumentPickerViewController + чтение содержимого файла.
actual class FileImportLauncher(private val onResult: (FileImportOutcome) -> Unit) {
    actual fun launch() {
        onResult(FileImportOutcome.Error("Импорт файлов на iOS появится в Фазе 6"))
    }
}

@Composable
actual fun rememberFileImportLauncher(onResult: (FileImportOutcome) -> Unit): FileImportLauncher =
    remember { FileImportLauncher(onResult) }
