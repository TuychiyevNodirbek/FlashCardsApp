package uz.nodirbek.flashcardsapp.shared.model

data class DailyStats(
    val date: String,
    val reviewCount: Int,
    val correctCount: Int
) {
    val accuracy: Float get() = if (reviewCount == 0) 0f else correctCount.toFloat() / reviewCount
}
