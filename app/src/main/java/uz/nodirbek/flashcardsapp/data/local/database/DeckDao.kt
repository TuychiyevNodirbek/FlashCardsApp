package uz.nodirbek.flashcardsapp.data.local.database

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

    @Query("SELECT * FROM decks ORDER BY createdAt ASC")
    fun getAllDecks(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE id = :id")
    suspend fun getDeckById(id: String): DeckEntity?

    @Query("SELECT * FROM decks WHERE parentId IS NULL ORDER BY createdAt ASC")
    fun getRootDecks(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE parentId = :parentId ORDER BY createdAt ASC")
    fun getChildDecks(parentId: String): Flow<List<DeckEntity>>

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId")
    fun getCardCountForDeck(deckId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND reps = 0")
    fun getNewCardCountForDeck(deckId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND reps > 0 AND interval < 21")
    fun getLearningCardCountForDeck(deckId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND dueDate <= :todayDate")
    fun getDueCardCountForDeck(deckId: String, todayDate: String): Flow<Int>
}
