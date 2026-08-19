package uz.nodirbek.flashcardsapp.shared.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitProgressDao {
    @Query("SELECT * FROM unit_progress WHERE deckId = :deckId")
    fun getForDeck(deckId: String): Flow<List<UnitProgressEntity>>

    @Query("SELECT * FROM unit_progress WHERE deckId = :deckId AND unitIndex = :unitIndex")
    suspend fun get(deckId: String, unitIndex: Int): UnitProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: UnitProgressEntity)

    @Query("SELECT * FROM unit_progress WHERE deckId = :deckId ORDER BY unitIndex ASC")
    suspend fun getAllForDeckOnce(deckId: String): List<UnitProgressEntity>

    @Query("DELETE FROM unit_progress WHERE deckId = :deckId")
    suspend fun deleteAllForDeck(deckId: String)
}
