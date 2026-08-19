package uz.nodirbek.flashcardsapp.shared.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock

@Entity(tableName = "decks", indices = [Index("parentId")])
data class DeckEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val parentId: String? = null,
    val colorHex: String = "#4255FF",
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val isPinned: Boolean = false,
    val pinnedAt: Long = 0L,
    val sortOrder: Int = 0,
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val isDeleted: Boolean = false,
    val deletedAt: Long = 0L
)
