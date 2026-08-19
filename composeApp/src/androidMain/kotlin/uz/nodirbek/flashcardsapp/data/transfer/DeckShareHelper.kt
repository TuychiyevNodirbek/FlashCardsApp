package uz.nodirbek.flashcardsapp.data.transfer

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import uz.nodirbek.flashcardsapp.shared.data.transfer.FdeckFile
import java.io.File

/** Подготовка и отправка .md файла колоды через системный share sheet. */
object DeckShareHelper {

    fun appVersion(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    } catch (e: Exception) {
        ""
    }

    fun safeFileName(deckName: String): String {
        val cleaned = deckName.replace(Regex("[^\\p{L}\\p{N} _-]"), "").trim()
        return (cleaned.ifBlank { "deck" }) + "." + FdeckFile.EXTENSION
    }

    /** Пишет файл в cacheDir/exports и открывает системный share chooser. */
    fun shareFile(context: Context, fileName: String, content: String) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outFile = File(dir, fileName)
        outFile.writeText(content)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/markdown"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться колодой"))
    }
}
