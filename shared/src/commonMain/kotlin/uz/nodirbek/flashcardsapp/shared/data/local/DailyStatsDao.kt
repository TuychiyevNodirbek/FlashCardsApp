package uz.nodirbek.flashcardsapp.shared.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: DailyStatsEntity)

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getByDate(date: String): DailyStatsEntity?

    @Query("SELECT * FROM daily_stats WHERE date >= :fromDate ORDER BY date ASC")
    fun getFrom(fromDate: String): Flow<List<DailyStatsEntity>>

    @Query("SELECT * FROM daily_stats ORDER BY date ASC")
    fun getAll(): Flow<List<DailyStatsEntity>>

    @Query("""
        UPDATE daily_stats
        SET reviewCount = reviewCount + :reviews,
            correctCount = correctCount + :correct
        WHERE date = :date
    """)
    suspend fun addToDay(date: String, reviews: Int, correct: Int)

    @Query("DELETE FROM daily_stats")
    suspend fun clearAll()
}
