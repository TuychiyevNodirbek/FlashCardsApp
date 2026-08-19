package uz.nodirbek.flashcardsapp.shared.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import uz.nodirbek.flashcardsapp.shared.data.local.CardDao
import uz.nodirbek.flashcardsapp.shared.data.local.CardEntity
import uz.nodirbek.flashcardsapp.shared.data.local.DeckDao
import uz.nodirbek.flashcardsapp.shared.data.local.DeckEntity
import uz.nodirbek.flashcardsapp.shared.data.local.UnitProgressDao
import uz.nodirbek.flashcardsapp.shared.data.local.UnitProgressEntity
import uz.nodirbek.flashcardsapp.shared.model.Card
import uz.nodirbek.flashcardsapp.shared.model.Deck
import uz.nodirbek.flashcardsapp.shared.model.StudyUnit

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

    /**
     * Удаляет юнит вместе с его карточками (soft-delete — восстанавливается
     * из «Недавно удалённых»).
     *
     * Юниты вычисляемые: [getUnits] режет карточки колоды на чанки, а `unit_progress`
     * хранит прогресс по ИНДЕКСУ чанка. Поэтому после удаления карточек юнита
     * оставшиеся чанки сдвигаются влево, и прогресс нужно пересобрать: строку
     * удалённого юнита выбросить, а всё, что было правее, сдвинуть на -1.
     * Пересобираем через delete-all + re-insert, а не UPDATE ... unitIndex - 1,
     * потому что (deckId, unitIndex) — первичный ключ, и порядок построчного
     * апдейта мог бы транзитно нарушить его уникальность.
     */
    suspend fun deleteUnit(deckId: String, unitIndex: Int) {
        val entities = cardDao.getCardsByDeck(deckId).first()
        val sorted = entities.sortedWith(compareBy({ it.createdAt }, { it.id }))
        val chunks = buildChunks(sorted.map { it.toDomain() })
        val chunk = chunks.getOrNull(unitIndex) ?: return

        cardDao.softDeleteCardsByIds(chunk.map { it.id }, Clock.System.now().toEpochMilliseconds())

        val shifted = unitProgressDao.getAllForDeckOnce(deckId)
            .filter { it.unitIndex != unitIndex }
            .map { if (it.unitIndex > unitIndex) it.copy(unitIndex = it.unitIndex - 1) else it }
        unitProgressDao.deleteAllForDeck(deckId)
        shifted.forEach { unitProgressDao.upsert(it) }
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

private fun CardEntity.toDomain(): Card = Card(
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
