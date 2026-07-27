package uz.nodirbek.flashcardsapp.domain.model

data class Deck(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val colorHex: String = "#4255FF",
    val createdAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val pinnedAt: Long = 0L,
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
