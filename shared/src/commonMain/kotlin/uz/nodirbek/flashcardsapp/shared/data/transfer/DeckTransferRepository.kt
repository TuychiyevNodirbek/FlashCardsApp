package uz.nodirbek.flashcardsapp.shared.data.transfer

import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import uz.nodirbek.flashcardsapp.shared.data.repository.CardRepository
import uz.nodirbek.flashcardsapp.shared.data.repository.DeckRepository
import uz.nodirbek.flashcardsapp.shared.model.Card
import uz.nodirbek.flashcardsapp.shared.model.Deck
import uz.nodirbek.flashcardsapp.shared.scheduler.RateCardUseCase
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class FdeckParseException(message: String) : Exception(message)
class FdeckVersionException(val fileVersion: Int) :
    Exception("Файл создан в более новой версии приложения (v$fileVersion)")

/** Экспорт/импорт курса в формате .md. */
@OptIn(ExperimentalUuidApi::class)
class DeckTransferRepository(
    private val cardRepository: CardRepository,
    private val deckRepository: DeckRepository
) {

    suspend fun exportDeck(courseDeckId: String): FdeckFile {
        val course = deckRepository.getDeckById(courseDeckId)
            ?: throw FdeckParseException("Колода не найдена")
        val children = deckRepository.getChildDecks(courseDeckId).first()

        return FdeckFile(
            deck = FdeckDeck(
                name = course.name,
                colorHex = course.colorHex,
                cards = cardRepository.getCardsByDeck(courseDeckId).first().map { it.toFdeck() },
                subRows = children.map { child ->
                    FdeckSubRow(
                        name = child.name,
                        colorHex = child.colorHex,
                        cards = cardRepository.getCardsByDeck(child.id).first().map { it.toFdeck() }
                    )
                }
            )
        )
    }

    fun serialize(file: FdeckFile): String = buildString {
        append("<!-- ${FdeckFile.FORMAT}:${file.version} exportedAt=${file.exportedAt} appVersion=${file.appVersion} -->\n")
        append("# ${file.deck.name.oneLine()} <!-- color:${file.deck.colorHex} -->\n\n")
        file.deck.cards.forEach { append("${it.front.oneLine()} :: ${it.back.oneLine()}\n") }
        if (file.deck.cards.isNotEmpty()) append("\n")
        file.deck.subRows.forEach { subRow ->
            append("## ${subRow.name.oneLine()} <!-- color:${subRow.colorHex} -->\n\n")
            subRow.cards.forEach { append("${it.front.oneLine()} :: ${it.back.oneLine()}\n") }
            append("\n")
        }
    }

    private fun String.oneLine() = replace("\n", " ").replace("\r", " ").trim()

    private val headerCommentRegex = Regex("""<!--\s*fdeck:(\d+).*?-->""")
    private val colorCommentRegex = Regex("""<!--\s*color:(#[0-9A-Fa-f]{6})\s*-->""")

    /** @throws FdeckParseException / FdeckVersionException при неподходящем файле. */
    fun parse(text: String): FdeckFile {
        var version = FdeckFile.VERSION
        var deckName: String? = null
        var deckColor = "#4255FF"
        val deckCards = mutableListOf<FdeckCard>()
        val subRows = mutableListOf<FdeckSubRow>()

        var currentSubRowName: String? = null
        var currentSubRowColor = "#4255FF"
        var currentSubRowCards = mutableListOf<FdeckCard>()

        fun flushSubRow() {
            val name = currentSubRowName ?: return
            subRows.add(
                FdeckSubRow(
                    name = name,
                    colorHex = currentSubRowColor,
                    sortOrder = subRows.size,
                    cards = currentSubRowCards.toList()
                )
            )
            currentSubRowCards = mutableListOf()
        }

        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            headerCommentRegex.find(line)?.let { version = it.groupValues[1].toIntOrNull() ?: version }

            when {
                line.startsWith("## ") -> {
                    flushSubRow()
                    val content = line.removePrefix("## ").trim()
                    currentSubRowName = colorCommentRegex.find(content)?.let {
                        currentSubRowColor = it.groupValues[1]
                        content.substringBefore("<!--").trim()
                    } ?: content
                }
                line.startsWith("# ") -> {
                    val content = line.removePrefix("# ").trim()
                    deckName = colorCommentRegex.find(content)?.let {
                        deckColor = it.groupValues[1]
                        content.substringBefore("<!--").trim()
                    } ?: content
                }
                line.startsWith("<!--") -> Unit
                line.contains("::") -> {
                    val front = line.substringBefore("::").trim()
                    val back = line.substringAfter("::").trim()
                    if (front.isNotEmpty() && back.isNotEmpty()) {
                        val card = FdeckCard(front = front, back = back)
                        if (currentSubRowName != null) currentSubRowCards.add(card) else deckCards.add(card)
                    }
                }
            }
        }
        flushSubRow()

        val name = deckName ?: throw FdeckParseException("Не найден заголовок колоды")
        if (version > FdeckFile.VERSION) throw FdeckVersionException(version)

        return FdeckFile(
            version = version,
            deck = FdeckDeck(
                name = name,
                colorHex = deckColor,
                subRows = subRows,
                cards = deckCards
            )
        )
    }

    /**
     * Импорт: создаёт новый курс с темами и карточками.
     * UUID перегенерируются, SRS-состояние сбрасывается (dueDate = сегодня).
     * @return id созданного курса.
     */
    suspend fun importDeck(file: FdeckFile): String {
        val today = RateCardUseCase.getTodayDate()
        val courseId = Uuid.random().toString()

        deckRepository.insertDeck(
            Deck(id = courseId, name = file.deck.name, colorHex = file.deck.colorHex)
        )
        insertCards(file.deck.cards, courseId, today)

        file.deck.subRows.sortedBy { it.sortOrder }.forEachIndexed { i, subRow ->
            val subRowId = Uuid.random().toString()
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
                    id = Uuid.random().toString(),
                    deckId = deckId,
                    front = c.front,
                    back = c.back,
                    dueDate = today,
                    createdAt = Clock.System.now().toEpochMilliseconds()
                )
            }
        )
    }

    private fun Card.toFdeck() = FdeckCard(front = front, back = back)
}
