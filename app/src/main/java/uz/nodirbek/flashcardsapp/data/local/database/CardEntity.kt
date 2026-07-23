package uz.nodirbek.flashcardsapp.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val deckId: String = "default",
    val front: String,
    val back: String,
    val ease: Float = 2.5f,
    val reps: Int = 0,
    val interval: Int = 0,
    val dueDate: String,
    val lastReviewed: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
