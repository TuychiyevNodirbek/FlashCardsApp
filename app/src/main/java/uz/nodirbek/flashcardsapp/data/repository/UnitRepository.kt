package uz.nodirbek.flashcardsapp.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import uz.nodirbek.flashcardsapp.data.local.database.CardDao
import uz.nodirbek.flashcardsapp.data.local.database.DeckDao
import uz.nodirbek.flashcardsapp.data.local.database.DeckEntity
import uz.nodirbek.flashcardsapp.data.local.database.UnitProgressDao
import uz.nodirbek.flashcardsapp.data.local.database.UnitProgressEntity
import uz.nodirbek.flashcardsapp.domain.model.Card
import uz.nodirbek.flashcardsapp.domain.model.Deck
import uz.nodirbek.flashcardsapp.domain.model.StudyUnit

/** Секция пути: subRow (тема) и её юниты. */
data class SubRowUnits(
    val deck: Deck,
    val units: List<StudyUnit>
)

class UnitRepository(
    private val cardDao: CardDao,
    private val unitProgressDao: UnitProgressDao,
    private val deckDao: DeckDao
) {
    companion object {
        const val UNIT_SIZE = 10

        /**
         * Равномерное разбиение: каждый юнит ≤ maxSize, размеры отличаются максимум на 1.
         * 23 карты → 8/8/7 (вместо 10/13 у старого алгоритма с приклеиванием хвоста).
         */
        fun buildChunks(cards: List<Card>, maxSize: Int = UNIT_SIZE): List<List<Card>> {
            if (cards.isEmpty()) return emptyList()
            val numUnits = (cards.size + maxSize - 1) / maxSize
            val base = cards.size / numUnits
            val extra = cards.size % numUnits // первые extra юнитов получают base+1
            var offset = 0
            return List(numUnits) { i ->
                val size = base + if (i < extra) 1 else 0
                cards.subList(offset, offset + size).also { offset += size }
            }
        }
    }

    fun getUnits(deckId: String): Flow<List<StudyUnit>> =
        combine(
            cardDao.getCardsByDeck(deckId),
            unitProgressDao.getForDeck(deckId)
        ) { entities, progressList ->
            val sorted = entities.sortedWith(compareBy({ it.createdAt }, { it.id }))
            val chunks = buildChunks(sorted.map { it.toDomain() })
            chunks.mapIndexed { i, chunk ->
                val p = progressList.find { it.unitIndex == i }
                val prevCompleted = i == 0 || progressList.find { it.unitIndex == i - 1 }?.completed == true
                StudyUnit(
                    deckId = deckId,
                    index = i,
                    cards = chunk,
                    completedSteps = p?.completedSteps ?: 0,
                    completed = p?.completed ?: false,
                    bestAccuracy = p?.bestAccuracy ?: 0f,
                    locked = !prevCompleted
                )
            }
        }

    /**
     * Путь курса: секции subRow (дочерние колоды) со своими юнитами.
     * Прямые карточки курса — неявная первая секция (скрывается, если пуста и есть subRow).
     * Юниты последовательны внутри subRow; сами subRow независимы.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getPath(courseDeckId: String): Flow<List<SubRowUnits>> =
        deckDao.getChildDecks(courseDeckId).flatMapLatest { children ->
            val subRowFlows: List<Flow<SubRowUnits?>> = buildList {
                // Неявная секция из прямых карточек курса
                add(getUnits(courseDeckId).map { units ->
                    if (units.isEmpty() && children.isNotEmpty()) {
                        null
                    } else {
                        deckDao.getDeckById(courseDeckId)?.let { SubRowUnits(it.toDomain(), units) }
                    }
                })
                children.forEach { child ->
                    add(getUnits(child.id).map { SubRowUnits(child.toDomain(), it) })
                }
            }
            combine(subRowFlows) { rows -> rows.filterNotNull() }
        }

    suspend fun saveProgress(
        deckId: String,
        unitIndex: Int,
        completedSteps: Int,
        completed: Boolean,
        accuracy: Float
    ) {
        val existing = unitProgressDao.get(deckId, unitIndex)
        unitProgressDao.upsert(
            UnitProgressEntity(
                deckId = deckId,
                unitIndex = unitIndex,
                completedSteps = maxOf(completedSteps, existing?.completedSteps ?: 0),
                completed = completed || (existing?.completed ?: false),
                bestAccuracy = maxOf(accuracy, existing?.bestAccuracy ?: 0f)
            )
        )
    }
}

private fun uz.nodirbek.flashcardsapp.data.local.database.CardEntity.toDomain(): Card = Card(
    id = id,
    deckId = deckId,
    front = front,
    back = back,
    ease = ease,
    reps = reps,
    interval = interval,
    dueDate = dueDate,
    lastReviewed = lastReviewed,
    createdAt = createdAt
)

private fun DeckEntity.toDomain(): Deck = Deck(
    id = id,
    name = name,
    parentId = parentId,
    colorHex = colorHex,
    createdAt = createdAt,
    isPinned = isPinned,
    pinnedAt = pinnedAt,
    sortOrder = sortOrder,
    updatedAt = updatedAt
)
