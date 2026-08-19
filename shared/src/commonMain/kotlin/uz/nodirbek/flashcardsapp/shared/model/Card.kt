package uz.nodirbek.flashcardsapp.shared.model

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
    val createdAt: Long,
    val lapses: Int = 0,
    val isDeleted: Boolean = false,
    val deletedAt: Long = 0L,
    /** FSRS: память "прочности" (в днях) — 0 значит, что FSRS ещё не инициализировал карточку. */
    val stability: Float = 0f,
    /** FSRS: сложность карточки, 1 (легко) .. 10 (сложно). */
    val difficulty: Float = 0f
)

fun Card.isNew() = lastReviewed == null
fun Card.isDueReview(today: String) = lastReviewed != null && dueDate <= today
