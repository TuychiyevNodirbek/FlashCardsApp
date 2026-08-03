package uz.nodirbek.flashcardsapp.data.local.database

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

    @Query("SELECT * FROM cards ORDER BY createdAt DESC")
    fun getAllCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY createdAt DESC")
    fun getCardsByDeck(deckId: String): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE dueDate <= :todayDate ORDER BY dueDate ASC")
    fun getDueCards(todayDate: String): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId AND dueDate <= :todayDate ORDER BY dueDate ASC")
    fun getDueCardsByDeck(deckId: String, todayDate: String): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId AND reps = 0 ORDER BY createdAt ASC")
    suspend fun getNewCardsByDeck(deckId: String): List<CardEntity>

    @Query("""
        SELECT * FROM cards
        WHERE deckId = :deckId
          AND reps > 1
          AND interval > 1
          AND dueDate <= :tomorrowDate
        ORDER BY dueDate ASC
    """)
    suspend fun getForgettingEdgeCards(deckId: String, tomorrowDate: String): List<CardEntity>

    @Query("SELECT COUNT(*) FROM cards")
    fun getCardCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId")
    fun getCardCountByDeck(deckId: String): Flow<Int>

    @Query("DELETE FROM cards")
    suspend fun deleteAllCards()

    @Query("DELETE FROM cards WHERE deckId = :deckId")
    suspend fun deleteCardsByDeck(deckId: String)

    @Query("DELETE FROM cards WHERE deckId NOT IN (SELECT id FROM decks)")
    suspend fun deleteOrphanedCards()
}
