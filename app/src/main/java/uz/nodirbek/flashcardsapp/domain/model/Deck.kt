package uz.nodirbek.flashcardsapp.domain.model

data class Deck(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val colorHex: String = "#4255FF",
    val createdAt: Long = System.currentTimeMillis()
)
