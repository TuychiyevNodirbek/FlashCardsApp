package uz.nodirbek.flashcardsapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.nodirbek.flashcardsapp.data.local.database.CardDao
import uz.nodirbek.flashcardsapp.data.local.database.CardEntity
import uz.nodirbek.flashcardsapp.domain.model.Card

class CardRepository(private val cardDao: CardDao) {

    fun getAllCards(): Flow<List<Card>> =
        cardDao.getAllCards().map { it.map(CardEntity::toDomainModel) }

    fun getCardsByDeck(deckId: String): Flow<List<Card>> =
        cardDao.getCardsByDeck(deckId).map { it.map(CardEntity::toDomainModel) }

    fun getDueCards(todayDate: String): Flow<List<Card>> =
        cardDao.getDueCards(todayDate).map { it.map(CardEntity::toDomainModel) }

    fun getDueCardsByDeck(deckId: String, todayDate: String): Flow<List<Card>> =
        cardDao.getDueCardsByDeck(deckId, todayDate).map { it.map(CardEntity::toDomainModel) }

    suspend fun getNewCardsByDeck(deckId: String): List<Card> =
        cardDao.getNewCardsByDeck(deckId).map(CardEntity::toDomainModel)

    suspend fun getForgettingEdgeCards(deckId: String, tomorrowDate: String): List<Card> =
        cardDao.getForgettingEdgeCards(deckId, tomorrowDate).map(CardEntity::toDomainModel)

    fun getCardCount(): Flow<Int> = cardDao.getCardCount()

    fun getCardCountByDeck(deckId: String): Flow<Int> = cardDao.getCardCountByDeck(deckId)

    suspend fun getCardById(id: String): Card? =
        cardDao.getCardById(id)?.toDomainModel()

    suspend fun insertCard(card: Card) = cardDao.insertCard(card.toEntity())

    suspend fun insertCards(cards: List<Card>) = cardDao.insertCards(cards.map(Card::toEntity))

    suspend fun updateCard(card: Card) = cardDao.updateCard(card.toEntity())

    suspend fun deleteCard(card: Card) = cardDao.deleteCard(card.toEntity())

    suspend fun deleteAllCards() = cardDao.deleteAllCards()

    suspend fun deleteCardsByDeck(deckId: String) = cardDao.deleteCardsByDeck(deckId)

    suspend fun deleteOrphanedCards() = cardDao.deleteOrphanedCards()

    suspend fun softDeleteCardsByDeck(deckId: String) =
        cardDao.softDeleteCardsByDeck(deckId, System.currentTimeMillis())

    suspend fun restoreCardsByDeck(deckId: String) = cardDao.restoreCardsByDeck(deckId)

    fun getDeletedCards(): Flow<List<Card>> =
        cardDao.getDeletedCards().map { it.map(CardEntity::toDomainModel) }

    suspend fun permanentlyDeleteCardsByDeck(deckId: String) =
        cardDao.permanentlyDeleteCardsByDeck(deckId)

    suspend fun softDeleteCardsByIds(ids: List<String>) =
        cardDao.softDeleteCardsByIds(ids, System.currentTimeMillis())

    suspend fun restoreCardsByIds(ids: List<String>) = cardDao.restoreCardsByIds(ids)

    suspend fun permanentlyDeleteCardsByIds(ids: List<String>) =
        cardDao.permanentlyDeleteCardsByIds(ids)
}

private fun CardEntity.toDomainModel(): Card = Card(
    id = id,
    deckId = deckId,
    front = front,
    back = back,
    ease = ease,
    reps = reps,
    interval = interval,
    dueDate = dueDate,
    lastReviewed = lastReviewed,
    createdAt = createdAt,
    lapses = lapses,
    isDeleted = isDeleted,
    deletedAt = deletedAt,
    stability = stability,
    difficulty = difficulty
)

private fun Card.toEntity(): CardEntity = CardEntity(
    id = id,
    deckId = deckId,
    front = front,
    back = back,
    ease = ease,
    reps = reps,
    interval = interval,
    dueDate = dueDate,
    lastReviewed = lastReviewed,
    createdAt = createdAt,
    lapses = lapses,
    isDeleted = isDeleted,
    deletedAt = deletedAt,
    stability = stability,
    difficulty = difficulty
)
