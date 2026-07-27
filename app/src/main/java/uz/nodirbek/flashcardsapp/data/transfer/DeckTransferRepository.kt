package uz.nodirbek.flashcardsapp.data.transfer

import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import uz.nodirbek.flashcardsapp.data.repository.CardRepository
import uz.nodirbek.flashcardsapp.data.repository.DeckRepository
import uz.nodirbek.flashcardsapp.domain.model.Card
import uz.nodirbek.flashcardsapp.domain.model.Deck
import uz.nodirbek.flashcardsapp.domain.usecase.RateCardUseCase
import java.util.UUID

class FdeckParseException(message: String) : Exception(message)
class FdeckVersionException(val fileVersion: Int) :
    Exception("Файл создан в более новой версии приложения (v$fileVersion)")

/** Экспорт/импорт курса в формате .fdeck. */
class DeckTransferRepository(
    private val cardRepository: CardRepository,
    private val deckRepository: DeckRepository
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun exportDeck(courseDeckId: String, appVersion: String): FdeckFile {
        val course = deckRepository.getDeckById(courseDeckId)
            ?: throw FdeckParseException("Колода не найдена")
        val children = deckRepository.getChildDecks(courseDeckId).first()

        return FdeckFile(
            exportedAt = System.currentTimeMillis(),
            appVersion = appVersion,
            deck = FdeckDeck(
                id = course.id,
                name = course.name,
                colorHex = course.colorHex,
                cards = cardRepository.getCardsByDeck(courseDeckId).first().map { it.toFdeck() },
                subRows = children.mapIndexed { i, child ->
                    FdeckSubRow(
                        id = child.id,
                        name = child.name,
                        colorHex = child.colorHex,
                        sortOrder = i,
                        cards = cardRepository.getCardsByDeck(child.id).first().map { it.toFdeck() }
                    )
                }
            )
        )
    }

    fun serialize(file: FdeckFile): String = json.encodeToString(FdeckFile.serializer(), file)

    /** @throws FdeckParseException / FdeckVersionException при неподходящем файле. */
    fun parse(jsonText: String): FdeckFile {
        val file = try {
            json.decodeFromString(FdeckFile.serializer(), jsonText)
        } catch (e: Exception) {
            throw FdeckParseException("Не удалось прочитать файл: ${e.message}")
        }
        if (file.format != FdeckFile.FORMAT) throw FdeckParseException("Неизвестный формат «${file.format}»")
        if (file.version > FdeckFile.VERSION) throw FdeckVersionException(file.version)
        return file
    }

    /**
     * Импорт: создаёт новый курс с темами и карточками.
     * UUID перегенерируются, SRS-состояние сбрасывается (dueDate = сегодня).
     * @return id созданного курса.
     */
    suspend fun importDeck(file: FdeckFile): String {
        val today = RateCardUseCase.getTodayDate()
        val courseId = UUID.randomUUID().toString()

        deckRepository.insertDeck(
            Deck(id = courseId, name = file.deck.name, colorHex = file.deck.colorHex)
        )
        insertCards(file.deck.cards, courseId, today)

        file.deck.subRows.sortedBy { it.sortOrder }.forEachIndexed { i, subRow ->
            val subRowId = UUID.randomUUID().toString()
            deckRepository.insertDeck(
                Deck(
                    id = subRowId,
                    name = subRow.name,
                    parentId = courseId,
                    colorHex = subRow.colorHex,
                    sortOrder = i
                )
            )
            insertCards(subRow.cards, subRowId, today)
        }
        return courseId
    }

    private suspend fun insertCards(cards: List<FdeckCard>, deckId: String, today: String) {
        if (cards.isEmpty()) return
        cardRepository.insertCards(
            cards.map { c ->
                Card(
                    id = UUID.randomUUID().toString(),
                    deckId = deckId,
                    front = c.front,
                    back = c.back,
                    dueDate = today,
                    createdAt = System.currentTimeMillis()
                )
            }
        )
    }

    private fun Card.toFdeck() = FdeckCard(id = id, front = front, back = back)
}
