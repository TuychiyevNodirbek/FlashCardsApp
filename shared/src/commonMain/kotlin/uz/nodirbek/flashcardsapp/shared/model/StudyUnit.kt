package uz.nodirbek.flashcardsapp.shared.model

data class StudyUnit(
    val deckId: String,
    val index: Int,
    val cards: List<Card>,
    val completedSteps: Int,
    val completed: Boolean,
    val bestAccuracy: Float,
    val locked: Boolean
)
