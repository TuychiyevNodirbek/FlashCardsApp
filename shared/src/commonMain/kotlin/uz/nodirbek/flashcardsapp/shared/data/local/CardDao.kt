package uz.nodirbek.flashcardsapp.shared.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Insert
    suspend fun insertCard(card: CardEntity)

    @Insert
    suspend fun insertCards(cards: List<CardEntity>)

    @Update
    suspend fun updateCard(card: CardEntity)

    @Delete
    suspend fun deleteCard(card: CardEntity)

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getCardById(id: String): CardEntity?

    @Query("SELECT * FROM cards WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getCardsByDeck(deckId: String): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE dueDate <= :todayDate AND isDeleted = 0 ORDER BY dueDate ASC")
    fun getDueCards(todayDate: String): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId AND dueDate <= :todayDate AND isDeleted = 0 ORDER BY dueDate ASC")
    fun getDueCardsByDeck(deckId: String, todayDate: String): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId AND reps = 0 AND isDeleted = 0 ORDER BY createdAt ASC")
    suspend fun getNewCardsByDeck(deckId: String): List<CardEntity>

    @Query("""
        SELECT * FROM cards
        WHERE deckId = :deckId
          AND reps > 1
          AND interval > 1
          AND dueDate <= :tomorrowDate
          AND isDeleted = 0
        ORDER BY dueDate ASC
    """)
    suspend fun getForgettingEdgeCards(deckId: String, tomorrowDate: String): List<CardEntity>

    @Query("SELECT COUNT(*) FROM cards WHERE isDeleted = 0")
    fun getCardCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND isDeleted = 0")
    fun getCardCountByDeck(deckId: String): Flow<Int>

    @Query("DELETE FROM cards")
    suspend fun deleteAllCards()

    @Query("DELETE FROM cards WHERE deckId = :deckId")
    suspend fun deleteCardsByDeck(deckId: String)

    @Query("DELETE FROM cards WHERE deckId NOT IN (SELECT id FROM decks)")
    suspend fun deleteOrphanedCards()

    // ── Soft-delete ───────────────────────────────────────────────────────────

    @Query("UPDATE cards SET isDeleted = 1, deletedAt = :deletedAt WHERE deckId = :deckId")
    suspend fun softDeleteCardsByDeck(deckId: String, deletedAt: Long)

    @Query("UPDATE cards SET isDeleted = 0, deletedAt = 0 WHERE deckId = :deckId")
    suspend fun restoreCardsByDeck(deckId: String)

    @Query("SELECT * FROM cards WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedCards(): Flow<List<CardEntity>>

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND isDeleted = 1")
    suspend fun getDeletedCardCountByDeck(deckId: String): Int

    @Query("DELETE FROM cards WHERE deckId = :deckId AND isDeleted = 1")
    suspend fun permanentlyDeleteCardsByDeck(deckId: String)

    @Query("UPDATE cards SET isDeleted = 1, deletedAt = :deletedAt WHERE id IN (:ids)")
    suspend fun softDeleteCardsByIds(ids: List<String>, deletedAt: Long)

    @Query("UPDATE cards SET isDeleted = 0, deletedAt = 0 WHERE id IN (:ids)")
    suspend fun restoreCardsByIds(ids: List<String>)

    @Query("DELETE FROM cards WHERE id IN (:ids)")
    suspend fun permanentlyDeleteCardsByIds(ids: List<String>)
}
