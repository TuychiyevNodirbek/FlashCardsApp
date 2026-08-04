package uz.nodirbek.flashcardsapp.ui.state

import uz.nodirbek.flashcardsapp.domain.model.Card
import uz.nodirbek.flashcardsapp.domain.model.DailyStats
import uz.nodirbek.flashcardsapp.domain.model.Deck
import uz.nodirbek.flashcardsapp.domain.model.Achievement

data class DeletedDeckItem(
    val deck: Deck,
    val cardCount: Int,
    val deletedAt: Long
)

/**
 * Пачка карточек, удалённая одной операцией (юнит) из живой колоды.
 * Юниты вычисляемые, поэтому «удалённый юнит» опознаётся не по индексу,
 * а по группе карточек с общим deckId и меткой времени удаления.
 */
data class DeletedCardBatch(
    val deckId: String,
    val deckName: String,
    val cards: List<Card>,
    val deletedAt: Long
) {
    val id: String get() = "$deckId@$deletedAt"
}

data class DeckWithStats(
    val deck: Deck,
    val totalCards: Int = 0,
    val newCards: Int = 0,
    val dueCards: Int = 0,
    val children: List<DeckWithStats> = emptyList()
)

data class HomeUiState(
    val cards: List<Card> = emptyList(),
    val dueCards: List<Card> = emptyList(),
    val decks: List<DeckWithStats> = emptyList(),
    val streak: Int = 0,
    val streakRecord: Int = 0,
    val xp: Long = 0L,
    val level: Int = 1,
    val cardCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val last7DaysStats: List<DailyStats> = emptyList(),
    val allStats: List<DailyStats> = emptyList(),
    val theme: String = "system",
    val dailyGoal: Int = 20,
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "09:00",
    val dailyNewLimit: Int = 20,
    val dailyReviewLimit: Int = 100,
    val ttsLang: String = "en",
    val ttsSpeed: Float = 1f,
    val unlockedAchievements: Set<String> = emptySet(),
    val rawNewCount: Int = 0,
    val rawReviewCount: Int = 0,
    val deletedDecks: List<DeletedDeckItem> = emptyList(),
    val deletedCardBatches: List<DeletedCardBatch> = emptyList()
) {
    val todayNewCount get() = minOf(rawNewCount, dailyNewLimit)
    val todayReviewCount get() = minOf(rawReviewCount, dailyReviewLimit)
}
