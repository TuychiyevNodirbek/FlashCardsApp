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
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.darwin.NSObject
import uz.nodirbek.flashcardsapp.shared.data.transfer.CardParseResult
import uz.nodirbek.flashcardsapp.shared.data.transfer.parseCsvContent
import uz.nodirbek.flashcardsapp.shared.scheduler.RateCardUseCase

actual class CardImportLauncher(private val trigger: (String) -> Unit) {
    actual fun launch(deckId: String) = trigger(deckId)
}

/** Открывает системный выбор файла (UIDocumentPickerViewController) и парсит CSV в карточки для [deckId]. */
@Composable
actual fun rememberCardImportLauncher(onResult: (CardParseResult) -> Unit): CardImportLauncher {
    val scope = rememberCoroutineScope()

    val delegate = remember {
        CardImportPickerDelegate { content ->
            scope.launch {
                val deckId = pendingDeckIdHolder.value ?: return@launch
                pendingDeckIdHolder.value = null
                onResult(parseCsvContent(content, deckId, RateCardUseCase.getTodayDate()))
            }
        }
    }

    return remember {
        CardImportLauncher { deckId ->
            pendingDeckIdHolder.value = deckId
            val picker = UIDocumentPickerViewController(
                documentTypes = listOf("public.plain-text", "public.comma-separated-values-text", "public.text"),
                inMode = UIDocumentPickerMode.UIDocumentPickerModeImport
            )
            picker.delegate = delegate
            UIApplication.sharedApplication.keyWindow
                ?.rootViewController
                ?.presentViewController(picker, animated = true, completion = null)
        }
    }
}

private object pendingDeckIdHolder {
    var value: String? = null
}

private class CardImportPickerDelegate(
    private val onTextRead: (String) -> Unit
) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? platform.Foundation.NSURL ?: return
        val accessed = url.startAccessingSecurityScopedResource()
        try {
            val data = NSData.dataWithContentsOfURL(url) ?: return
            val content = NSString.create(data, NSUTF8StringEncoding) as String
            onTextRead(content)
        } finally {
            if (accessed) url.stopAccessingSecurityScopedResource()
        }
    }
}
