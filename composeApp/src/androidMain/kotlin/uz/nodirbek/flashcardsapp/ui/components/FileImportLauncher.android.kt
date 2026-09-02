package uz.nodirbek.flashcardsapp.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import uz.nodirbek.flashcardsapp.data.transfer.AnkiApkgImporter
import uz.nodirbek.flashcardsapp.shared.data.transfer.AnkiImportException

actual class FileImportLauncher(private val launcher: ActivityResultLauncher<String>) {
    actual fun launch() = launcher.launch("*/*")
}

private fun displayNameOf(context: android.content.Context, uri: Uri): String? =
    context.contentResolver.query(uri, null, null, null, null)?.use { c ->
        val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
    }

@Composable
actual fun rememberFileImportLauncher(onResult: (FileImportOutcome) -> Unit): FileImportLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ankiImporter = remember { AnkiApkgImporter(context) }

    val activityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val fileName = displayNameOf(context, uri).orEmpty()

        if (fileName.endsWith(".apkg", ignoreCase = true)) {
            scope.launch {
                try {
                    onResult(FileImportOutcome.Anki(ankiImporter.import(uri)))
                } catch (e: AnkiImportException) {
                    onResult(FileImportOutcome.Error(e.message ?: "Не удалось прочитать файл Anki"))
                } catch (e: Exception) {
                    onResult(FileImportOutcome.Error("Ошибка: ${e.message}"))
                }
            }
            return@rememberLauncherForActivityResult
        }

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                onResult(FileImportOutcome.Error("Не удалось открыть файл"))
                return@rememberLauncherForActivityResult
            }
            val content = inputStream.bufferedReader().readText()
            inputStream.close()

            if (content.isBlank()) {
                onResult(FileImportOutcome.Error("Файл пустой"))
            } else {
                onResult(FileImportOutcome.PlainText(fileName, content))
            }
        } catch (e: Exception) {
            onResult(FileImportOutcome.Error("Ошибка: ${e.message}"))
        }
    }

    return remember(activityLauncher) { FileImportLauncher(activityLauncher) }
}
