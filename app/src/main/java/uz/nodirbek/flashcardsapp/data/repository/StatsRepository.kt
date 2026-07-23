package uz.nodirbek.flashcardsapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.nodirbek.flashcardsapp.data.local.database.DailyStatsDao
import uz.nodirbek.flashcardsapp.data.local.database.DailyStatsEntity
import uz.nodirbek.flashcardsapp.domain.model.DailyStats
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StatsRepository(private val dao: DailyStatsDao) {

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getLast7Days(): Flow<List<DailyStats>> =
        dao.getFrom(LocalDate.now().minusDays(6).format(fmt)).map { it.map(DailyStatsEntity::toDomain) }

    fun getLast30Days(): Flow<List<DailyStats>> =
        dao.getFrom(LocalDate.now().minusDays(29).format(fmt)).map { it.map(DailyStatsEntity::toDomain) }

    fun getAllStats(): Flow<List<DailyStats>> =
        dao.getAll().map { it.map(DailyStatsEntity::toDomain) }

    suspend fun recordReview(date: String, reviews: Int, correct: Int) {
        val existing = dao.getByDate(date)
        if (existing == null) {
            dao.upsert(DailyStatsEntity(date = date, reviewCount = reviews, correctCount = correct))
        } else {
            dao.addToDay(date, reviews, correct)
        }
    }

    suspend fun clearAll() = dao.clearAll()
}

private fun DailyStatsEntity.toDomain(): DailyStats =
    DailyStats(date = date, reviewCount = reviewCount, correctCount = correctCount)
