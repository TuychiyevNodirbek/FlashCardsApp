package uz.nodirbek.flashcardsapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.nodirbek.flashcardsapp.data.local.database.DeckDao
import uz.nodirbek.flashcardsapp.data.local.database.DeckEntity
import uz.nodirbek.flashcardsapp.domain.model.Deck

class DeckRepository(private val deckDao: DeckDao) {

    fun getAllDecks(): Flow<List<Deck>> =
        deckDao.getAllDecks().map { it.map(DeckEntity::toDomain) }

    fun getRootDecks(): Flow<List<Deck>> =
        deckDao.getRootDecks().map { it.map(DeckEntity::toDomain) }

    fun getChildDecks(parentId: String): Flow<List<Deck>> =
        deckDao.getChildDecks(parentId).map { it.map(DeckEntity::toDomain) }

    suspend fun getDeckById(id: String): Deck? =
        deckDao.getDeckById(id)?.toDomain()

    suspend fun insertDeck(deck: Deck) = deckDao.insertDeck(deck.toEntity())

    suspend fun updateDeck(deck: Deck) =
        deckDao.updateDeck(deck.toEntity().copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteDeck(deck: Deck) = deckDao.deleteDeck(deck.toEntity())

    suspend fun softDeleteDeck(id: String) = deckDao.softDeleteDeck(id, System.currentTimeMillis())
    suspend fun restoreDeck(id: String) = deckDao.restoreDeck(id)
    fun getDeletedDecks(): Flow<List<Deck>> = deckDao.getDeletedDecks().map { it.map(DeckEntity::toDomain) }
    suspend fun permanentlyDeleteDeckById(id: String) = deckDao.permanentlyDeleteDeckById(id)

    fun getCardCount(deckId: String): Flow<Int> = deckDao.getCardCountForDeck(deckId)
    fun getNewCount(deckId: String): Flow<Int> = deckDao.getNewCardCountForDeck(deckId)
    fun getLearningCount(deckId: String): Flow<Int> = deckDao.getLearningCardCountForDeck(deckId)
    fun getDueCount(deckId: String, todayDate: String): Flow<Int> = deckDao.getDueCardCountForDeck(deckId, todayDate)
}

private fun DeckEntity.toDomain(): Deck = Deck(
    id = id,
    name = name,
    parentId = parentId,
    colorHex = colorHex,
    createdAt = createdAt,
    isPinned = isPinned,
    pinnedAt = pinnedAt,
    sortOrder = sortOrder,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

private fun Deck.toEntity(): DeckEntity = DeckEntity(
    id = id,
    name = name,
    parentId = parentId,
    colorHex = colorHex,
    createdAt = createdAt,
    isPinned = isPinned,
    pinnedAt = pinnedAt,
    sortOrder = sortOrder,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)
