package uz.nodirbek.flashcardsapp.domain.model

data class Card(
    val id: String,
    val deckId: String = "default",
    val front: String,
    val back: String,
    val ease: Float = 2.5f,
    val reps: Int = 0,
    val interval: Int = 0,
    val dueDate: String,
    val lastReviewed: String? = null,
    val createdAt: Long
)
