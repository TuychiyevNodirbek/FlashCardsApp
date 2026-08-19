package uz.nodirbek.flashcardsapp.shared.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey
    val date: String,
    val reviewCount: Int = 0,
    val correctCount: Int = 0
)
