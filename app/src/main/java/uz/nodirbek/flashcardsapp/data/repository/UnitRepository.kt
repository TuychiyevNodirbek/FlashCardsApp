package uz.nodirbek.flashcardsapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import uz.nodirbek.flashcardsapp.data.local.database.CardDao
import uz.nodirbek.flashcardsapp.data.local.database.UnitProgressDao
import uz.nodirbek.flashcardsapp.data.local.database.UnitProgressEntity
import uz.nodirbek.flashcardsapp.domain.model.Card
import uz.nodirbek.flashcardsapp.domain.model.StudyUnit

class UnitRepository(
    private val cardDao: CardDao,
    private val unitProgressDao: UnitProgressDao
) {
    companion object {
        const val UNIT_SIZE = 10
        // Хвост меньше 4 слов приклеивается к предыдущему юниту
        private const val MIN_TAIL_SIZE = 4
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

    private fun buildChunks(cards: List<Card>): List<List<Card>> {
        if (cards.isEmpty()) return emptyList()
        val raw = cards.chunked(UNIT_SIZE)
        if (raw.size <= 1) return raw
        val last = raw.last()
        return if (last.size < MIN_TAIL_SIZE) {
            // Приклеить хвост к предыдущему юниту
            raw.dropLast(2) + listOf(raw[raw.size - 2] + last)
        } else {
            raw
        }
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
