package uz.nodirbek.flashcardsapp.shared.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: DeckEntity)

    @Update
    suspend fun updateDeck(deck: DeckEntity)

    @Delete
    suspend fun deleteDeck(deck: DeckEntity)

    @Query("SELECT * FROM decks WHERE isDeleted = 0 ORDER BY createdAt ASC")
    fun getAllDecks(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE id = :id")
    suspend fun getDeckById(id: String): DeckEntity?

    @Query("SELECT * FROM decks WHERE parentId IS NULL AND isDeleted = 0 ORDER BY createdAt ASC")
    fun getRootDecks(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE parentId = :parentId AND isDeleted = 0 ORDER BY sortOrder ASC, createdAt ASC")
    fun getChildDecks(parentId: String): Flow<List<DeckEntity>>

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND isDeleted = 0")
    fun getCardCountForDeck(deckId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND reps = 0 AND isDeleted = 0")
    fun getNewCardCountForDeck(deckId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND reps > 0 AND interval < 21 AND isDeleted = 0")
    fun getLearningCardCountForDeck(deckId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND dueDate <= :todayDate AND isDeleted = 0")
    fun getDueCardCountForDeck(deckId: String, todayDate: String): Flow<Int>

    // ── Soft-delete ───────────────────────────────────────────────────────────

    @Query("UPDATE decks SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteDeck(id: String, deletedAt: Long)

    @Query("UPDATE decks SET isDeleted = 0, deletedAt = 0 WHERE id = :id")
    suspend fun restoreDeck(id: String)

    @Query("SELECT * FROM decks WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedDecks(): Flow<List<DeckEntity>>

    @Query("DELETE FROM decks WHERE isDeleted = 1")
    suspend fun permanentlyDeleteAllDeleted()

    @Query("DELETE FROM decks WHERE id = :id")
    suspend fun permanentlyDeleteDeckById(id: String)
}
