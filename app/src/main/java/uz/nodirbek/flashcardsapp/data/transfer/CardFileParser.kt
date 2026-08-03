package uz.nodirbek.flashcardsapp.data.transfer

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.nodirbek.flashcardsapp.domain.model.Card
import uz.nodirbek.flashcardsapp.domain.usecase.RateCardUseCase
import java.util.UUID

sealed class CardParseResult {
    data class Success(val cards: List<Card>) : CardParseResult()
    data class Error(val message: String) : CardParseResult()
}

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

        if (content.isBlank()) return CardParseResult.Error("Файл пустой")

        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.size > 5000) return CardParseResult.Error("Файл слишком большой (макс. 5000 строк)")

        val delimiter = detectCsvDelimiter(lines)
        val today = RateCardUseCase.getTodayDate()
        val cards = mutableListOf<Card>()

        for (line in lines) {
            val parts = line.split(delimiter).map { it.trim() }
            if (parts.size >= 2) {
                val front = parts[0]
                val back = parts.drop(1).joinToString(", ")
                if (front.isNotBlank() && back.isNotBlank()) {
                    cards.add(
                        Card(
                            id = UUID.randomUUID().toString(),
                            front = front,
                            back = back,
                            deckId = deckId,
                            dueDate = today,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        if (cards.isEmpty()) CardParseResult.Error("Не найдено подходящих пар в файле")
        else CardParseResult.Success(cards)
    } catch (e: Exception) {
        CardParseResult.Error("Ошибка: ${e.message}")
    }
}

private fun detectCsvDelimiter(lines: List<String>): Char {
    var tabs = 0; var semis = 0; var commas = 0
    for (line in lines.take(10)) {
        tabs += line.count { it == '\t' }
        semis += line.count { it == ';' }
        commas += line.count { it == ',' }
    }
    return when {
        tabs > semis && tabs > commas -> '\t'
        semis > commas -> ';'
        else -> ','
    }
}
