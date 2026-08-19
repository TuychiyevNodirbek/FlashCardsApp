package uz.nodirbek.flashcardsapp.shared.data.transfer

import kotlinx.datetime.Clock
import uz.nodirbek.flashcardsapp.shared.model.Card
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

sealed class CardParseResult {
    data class Success(val cards: List<Card>) : CardParseResult()
    data class Error(val message: String) : CardParseResult()
}

/** Разбирает сырой текст CSV/TSV в список карточек колоды [deckId]. Не зависит от платформы ввода-вывода. */
@OptIn(ExperimentalUuidApi::class)
fun parseCsvContent(content: String, deckId: String, todayDate: String): CardParseResult {
    return try {
        if (content.isBlank()) return CardParseResult.Error("Файл пустой")

        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.size > 5000) return CardParseResult.Error("Файл слишком большой (макс. 5000 строк)")

        val delimiter = detectCsvDelimiter(lines)
        val cards = mutableListOf<Card>()

        for (line in lines) {
            val parts = line.split(delimiter).map { it.trim() }
            if (parts.size >= 2) {
                val front = parts[0]
                val back = parts.drop(1).joinToString(", ")
                if (front.isNotBlank() && back.isNotBlank()) {
                    cards.add(
                        Card(
                            id = Uuid.random().toString(),
                            front = front,
                            back = back,
                            deckId = deckId,
                            dueDate = todayDate,
                            createdAt = Clock.System.now().toEpochMilliseconds()
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

fun detectCsvDelimiter(lines: List<String>): Char {
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
