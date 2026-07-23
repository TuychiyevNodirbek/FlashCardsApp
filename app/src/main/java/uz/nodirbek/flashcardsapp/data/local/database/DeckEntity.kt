package uz.nodirbek.flashcardsapp.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val parentId: String? = null,
    val colorHex: String = "#4255FF",
    val createdAt: Long = System.currentTimeMillis()
)
