package uz.nodirbek.flashcardsapp.data.local.database

import androidx.room.Entity

@Entity(tableName = "unit_progress", primaryKeys = ["deckId", "unitIndex"])
data class UnitProgressEntity(
    val deckId: String,
    val unitIndex: Int,
    val completedSteps: Int = 0,
    val completed: Boolean = false,
    val bestAccuracy: Float = 0f
)
