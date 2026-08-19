package uz.nodirbek.flashcardsapp.shared.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import uz.nodirbek.flashcardsapp.shared.data.local.DailyStatsDao
import uz.nodirbek.flashcardsapp.shared.data.local.DailyStatsEntity
import uz.nodirbek.flashcardsapp.shared.model.DailyStats

class StatsRepository(private val dao: DailyStatsDao) {

    private fun daysAgo(n: Int): String =
        Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(DatePeriod(days = n)).toString()

    fun getLast7Days(): Flow<List<DailyStats>> =
        dao.getFrom(daysAgo(6)).map { it.map(DailyStatsEntity::toDomain) }

    fun getLast30Days(): Flow<List<DailyStats>> =
        dao.getFrom(daysAgo(29)).map { it.map(DailyStatsEntity::toDomain) }

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
