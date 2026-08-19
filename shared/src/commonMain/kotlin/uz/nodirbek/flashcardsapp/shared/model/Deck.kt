package uz.nodirbek.flashcardsapp.shared.model

import kotlinx.datetime.Clock

data class Deck(
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
