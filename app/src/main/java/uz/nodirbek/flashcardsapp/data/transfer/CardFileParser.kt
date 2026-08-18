package uz.nodirbek.flashcardsapp.data.transfer

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.nodirbek.flashcardsapp.shared.data.transfer.CardParseResult
import uz.nodirbek.flashcardsapp.shared.data.transfer.parseCsvContent
import uz.nodirbek.flashcardsapp.shared.model.Card
import uz.nodirbek.flashcardsapp.shared.scheduler.RateCardUseCase
import java.util.UUID

/** Parses CSV or .apkg files and returns a flat list of cards assigned to [deckId]. */
suspend fun parseCardsFromUri(
    uri: Uri,
    context: Context,
    deckId: String
): CardParseResult = withContext(Dispatchers.IO) {
    val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
        val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
    }.orEmpty()

    if (fileName.endsWith(".apkg", ignoreCase = true)) {
        return@withContext parseApkg(uri, context, deckId)
    }

    parseCsv(uri, context, deckId)
}

private suspend fun parseApkg(uri: Uri, context: Context, deckId: String): CardParseResult {
    return try {
        val importer = AnkiApkgImporter(context)
        val result = importer.import(uri)
        val today = RateCardUseCase.getTodayDate()
        val cards = result.cards.map { c ->
            Card(
                id = UUID.randomUUID().toString(),
                front = c.front,
                back = c.back,
                deckId = deckId,
                dueDate = today,
                createdAt = System.currentTimeMillis()
            )
        }
        if (cards.isEmpty()) CardParseResult.Error("В файле не найдено карточек")
        else CardParseResult.Success(cards)
    } catch (e: AnkiImportException) {
        CardParseResult.Error(e.message ?: "Не удалось прочитать файл Anki")
    } catch (e: Exception) {
        CardParseResult.Error("Ошибка: ${e.message}")
    }
}

private fun parseCsv(uri: Uri, context: Context, deckId: String): CardParseResult {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return CardParseResult.Error("Не удалось открыть файл")
        val content = inputStream.bufferedReader().readText()
        inputStream.close()

        parseCsvContent(content, deckId, RateCardUseCase.getTodayDate())
    } catch (e: Exception) {
        CardParseResult.Error("Ошибка: ${e.message}")
    }
}
