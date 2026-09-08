@file:OptIn(kotlinx.cinterop.BetaInteropApi::class)

package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.lastPathComponent
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.darwin.NSObject

/**
 * Открывает системный выбор файла (UIDocumentPickerViewController). CSV/.md читаются как
 * текст — так же, как на Android. .apkg (Anki) не поддерживается: на iOS нет доступа к
 * SQLite/zip-разбору без сторонних библиотек — TODO Фаза 6.
 */
actual class FileImportLauncher(private val trigger: () -> Unit) {
    actual fun launch() = trigger()
}

@Composable
actual fun rememberFileImportLauncher(onResult: (FileImportOutcome) -> Unit): FileImportLauncher {
    val scope = rememberCoroutineScope()

    val delegate = remember {
        FileImportPickerDelegate { fileName, content, error ->
            scope.launch {
                when {
                    error != null -> onResult(FileImportOutcome.Error(error))
                    fileName.endsWith(".apkg", ignoreCase = true) ->
                        onResult(FileImportOutcome.Error("Импорт Anki (.apkg) на iOS пока не поддерживается"))
                    content != null -> onResult(FileImportOutcome.PlainText(fileName, content))
                    else -> onResult(FileImportOutcome.Error("Не удалось прочитать файл"))
                }
            }
        }
    }

    return remember {
        FileImportLauncher {
            val picker = UIDocumentPickerViewController(
                documentTypes = listOf("public.item"),
                inMode = UIDocumentPickerMode.UIDocumentPickerModeImport
            )
            picker.delegate = delegate
            UIApplication.sharedApplication.keyWindow
                ?.rootViewController
                ?.presentViewController(picker, animated = true, completion = null)
        }
    }
}

private class FileImportPickerDelegate(
    private val onPicked: (fileName: String, content: String?, error: String?) -> Unit
) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? platform.Foundation.NSURL ?: return
        val fileName = url.lastPathComponent ?: "file"
        if (fileName.endsWith(".apkg", ignoreCase = true)) {
            onPicked(fileName, null, null)
            return
        }
        val accessed = url.startAccessingSecurityScopedResource()
        try {
            val data = NSData.dataWithContentsOfURL(url)
            if (data == null) {
                onPicked(fileName, null, "Не удалось открыть файл")
                return
            }
            val content = NSString.create(data, NSUTF8StringEncoding) as? String
            if (content.isNullOrBlank()) {
                onPicked(fileName, null, "Файл пустой")
            } else {
                onPicked(fileName, content, null)
            }
        } finally {
            if (accessed) url.stopAccessingSecurityScopedResource()
        }
    }
}
