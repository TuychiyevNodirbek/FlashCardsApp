package uz.nodirbek.flashcardsapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.nodirbek.flashcardsapp.data.local.preferences.PreferencesDataStore
import uz.nodirbek.flashcardsapp.data.repository.CardRepository
import uz.nodirbek.flashcardsapp.data.repository.DeckRepository
import uz.nodirbek.flashcardsapp.data.repository.StatsRepository
import uz.nodirbek.flashcardsapp.domain.model.Card
import uz.nodirbek.flashcardsapp.domain.model.Deck
import uz.nodirbek.flashcardsapp.domain.usecase.RateCardUseCase
import uz.nodirbek.flashcardsapp.ui.state.DeckWithStats
import uz.nodirbek.flashcardsapp.ui.state.HomeUiState

class HomeViewModel(
    private val cardRepository: CardRepository,
    private val deckRepository: DeckRepository,
    private val statsRepository: StatsRepository,
    private val preferencesDataStore: PreferencesDataStore,
    private val rateCardUseCase: RateCardUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            cardRepository.getAllCards().collect { cards ->
                val today = RateCardUseCase.getTodayDate()
                val dueCards = cards.filter { it.dueDate <= today }
                _uiState.update { it.copy(cards = cards, dueCards = dueCards, cardCount = cards.size, isLoading = false) }
            }
        }
        viewModelScope.launch {
            combine(
                deckRepository.getAllDecks(),
                cardRepository.getAllCards()
            ) { decks, cards ->
                buildDeckTree(decks, cards)
            }.collect { tree ->
                _uiState.update { it.copy(decks = tree) }
            }
        }
        viewModelScope.launch { preferencesDataStore.streak.collect { s -> _uiState.update { it.copy(streak = s) } } }
        viewModelScope.launch { preferencesDataStore.streakRecord.collect { r -> _uiState.update { it.copy(streakRecord = r) } } }
        viewModelScope.launch { preferencesDataStore.xp.collect { xp -> _uiState.update { it.copy(xp = xp, level = (xp / 100).toInt() + 1) } } }
        viewModelScope.launch { statsRepository.getLast7Days().collect { stats -> _uiState.update { it.copy(last7DaysStats = stats) } } }
        viewModelScope.launch { statsRepository.getAllStats().collect { stats -> _uiState.update { it.copy(allStats = stats) } } }
        viewModelScope.launch { preferencesDataStore.theme.collect { t -> _uiState.update { it.copy(theme = t) } } }
        viewModelScope.launch { preferencesDataStore.dailyGoal.collect { g -> _uiState.update { it.copy(dailyGoal = g) } } }
        viewModelScope.launch { preferencesDataStore.reminderEnabled.collect { e -> _uiState.update { it.copy(reminderEnabled = e) } } }
        viewModelScope.launch { preferencesDataStore.reminderTime.collect { t -> _uiState.update { it.copy(reminderTime = t) } } }
        viewModelScope.launch { preferencesDataStore.dailyNewLimit.collect { v -> _uiState.update { it.copy(dailyNewLimit = v) } } }
        viewModelScope.launch { preferencesDataStore.dailyReviewLimit.collect { v -> _uiState.update { it.copy(dailyReviewLimit = v) } } }
        viewModelScope.launch { preferencesDataStore.ttsLang.collect { v -> _uiState.update { it.copy(ttsLang = v) } } }
        viewModelScope.launch { preferencesDataStore.ttsSpeed.collect { v -> _uiState.update { it.copy(ttsSpeed = v) } } }
    }

    private fun buildDeckTree(decks: List<Deck>, cards: List<Card>): List<DeckWithStats> {
        val today = RateCardUseCase.getTodayDate()
        fun statsFor(deckId: String): DeckWithStats {
            val deck = decks.first { it.id == deckId }
            val deckCards = cards.filter { it.deckId == deckId }
            val children = decks.filter { it.parentId == deckId }.map { statsFor(it.id) }
            return DeckWithStats(
                deck = deck,
                totalCards = deckCards.size + children.sumOf { it.totalCards },
                newCards = deckCards.count { it.reps == 0 } + children.sumOf { it.newCards },
                dueCards = deckCards.count { it.dueDate <= today } + children.sumOf { it.dueCards },
                children = children
            )
        }
        return decks.filter { it.parentId == null }
            .map { statsFor(it.id) }
            .sortedWith(compareByDescending<DeckWithStats> { it.deck.isPinned }.thenBy { it.deck.createdAt })
    }

    /** +30 XP за юнит, +20 бонус если точность ≥ 90%. Возвращает заработанное XP. */
    fun addXpForUnit(correct: Int, total: Int): Int {
        val bonus = if (total > 0 && correct.toFloat() / total >= 0.9f) 20 else 0
        val earned = 30 + bonus
        viewModelScope.launch {
            preferencesDataStore.addXp(earned.toLong())
            updateStreak()
        }
        return earned
    }

    fun rateCard(cardId: String, quality: Int) {
        viewModelScope.launch {
            try {
                val today = RateCardUseCase.getTodayDate()
                val card = cardRepository.getCardById(cardId) ?: return@launch
                val updatedCard = rateCardUseCase(card, quality)
                cardRepository.updateCard(updatedCard)
                val isCorrect = quality >= 2
                preferencesDataStore.addXp(if (isCorrect) 10L else 4L)
                statsRepository.recordReview(date = today, reviews = 1, correct = if (isCorrect) 1 else 0)
                updateStreak()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private suspend fun updateStreak() {
        val today = RateCardUseCase.getTodayDate()
        val lastActive = preferencesDataStore.lastActiveDate.first()
        if (lastActive == today) return
        val yesterday = RateCardUseCase.addDays(today, -1)
        val currentStreak = _uiState.value.streak
        val newStreak = if (lastActive == yesterday) currentStreak + 1 else 1
        preferencesDataStore.setStreak(newStreak)
        preferencesDataStore.setLastActiveDate(today)
        preferencesDataStore.updateStreakRecord(newStreak)
    }

    fun addDeck(name: String, parentId: String? = null, colorHex: String = "#4255FF") {
        viewModelScope.launch {
            val deck = Deck(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                parentId = parentId,
                colorHex = colorHex
            )
            deckRepository.insertDeck(deck)
        }
    }

    fun deleteDeck(deck: Deck) {
        viewModelScope.launch {
            deckRepository.deleteDeck(deck)
            cardRepository.deleteCardsByDeck(deck.id)
        }
    }

    fun updateDeck(deck: Deck) {
        viewModelScope.launch { deckRepository.updateDeck(deck) }
    }

    /** Создать subRow (тему) внутри курса; sortOrder = следующий за максимальным. */
    fun addSubRow(courseDeckId: String, name: String, colorHex: String = "#4255FF") {
        viewModelScope.launch {
            val siblings = deckRepository.getChildDecks(courseDeckId).first()
            val nextOrder = (siblings.maxOfOrNull { it.sortOrder } ?: -1) + 1
            deckRepository.insertDeck(
                Deck(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    parentId = courseDeckId,
                    colorHex = colorHex,
                    sortOrder = nextOrder
                )
            )
        }
    }

    /** Переместить subRow вверх/вниз (delta = -1 / +1) среди тем курса. */
    fun moveSubRow(deck: Deck, delta: Int) {
        viewModelScope.launch {
            val parentId = deck.parentId ?: return@launch
            val siblings = deckRepository.getChildDecks(parentId).first()
            val idx = siblings.indexOfFirst { it.id == deck.id }
            val targetIdx = idx + delta
            if (idx == -1 || targetIdx !in siblings.indices) return@launch
            // Нормализуем sortOrder по текущему порядку, затем меняем местами
            val normalized = siblings.mapIndexed { i, d -> d.copy(sortOrder = i) }.toMutableList()
            val moved = normalized[idx].copy(sortOrder = targetIdx)
            normalized[idx] = normalized[targetIdx].copy(sortOrder = idx)
            normalized[targetIdx] = moved
            normalized.forEach { deckRepository.updateDeck(it) }
        }
    }

    /** Id колоды + всех её потомков (для карточек курса вместе с темами). */
    fun getDeckWithDescendantIds(deckId: String): Set<String> {
        fun findNode(list: List<DeckWithStats>): DeckWithStats? {
            list.forEach { n ->
                if (n.deck.id == deckId) return n
                findNode(n.children)?.let { return it }
            }
            return null
        }

        val node = findNode(_uiState.value.decks) ?: return setOf(deckId)
        val ids = mutableSetOf<String>()
        fun collect(n: DeckWithStats) {
            ids += n.deck.id
            n.children.forEach(::collect)
        }
        collect(node)
        return ids
    }

    fun pinDeck(deck: Deck) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            deckRepository.updateDeck(
                if (deck.isPinned) deck.copy(isPinned = false, pinnedAt = 0L)
                else deck.copy(isPinned = true, pinnedAt = now)
            )
        }
    }

    fun addCard(card: Card) {
        viewModelScope.launch { cardRepository.insertCard(card) }
    }

    fun addCards(cards: List<Card>) {
        viewModelScope.launch { cardRepository.insertCards(cards) }
    }

    fun updateCard(card: Card) {
        viewModelScope.launch { cardRepository.updateCard(card) }
    }

    fun deleteCard(card: Card) {
        viewModelScope.launch { cardRepository.deleteCard(card) }
    }

    /** Due queue for a deck (включая темы-потомки), capped by the daily limits from Settings. */
    fun getDueCardsForDeck(deckId: String): List<Card> {
        val today = RateCardUseCase.getTodayDate()
        val state = _uiState.value
        val ids = if (deckId == ALL_DECKS) null else getDeckWithDescendantIds(deckId)
        val due = state.cards.filter { (ids == null || it.deckId in ids) && it.dueDate <= today }
        val newCards = due.filter { it.reps == 0 }.take(state.dailyNewLimit)
        val reviewCards = due.filter { it.reps > 0 }.take(state.dailyReviewLimit)
        return newCards + reviewCards
    }

    fun getCardsForDeck(deckId: String): List<Card> {
        if (deckId == ALL_DECKS) return _uiState.value.cards
        val ids = getDeckWithDescendantIds(deckId)
        return _uiState.value.cards.filter { it.deckId in ids }
    }

    companion object {
        /** Псевдо-id: повторение по всем колодам сразу. */
        const val ALL_DECKS = "all"
    }

    fun setTheme(value: String) {
        viewModelScope.launch { preferencesDataStore.setTheme(value) }
    }

    fun setDailyGoal(value: Int) {
        viewModelScope.launch { preferencesDataStore.setDailyGoal(value) }
    }

    fun setReminderEnabled(value: Boolean) {
        viewModelScope.launch { preferencesDataStore.setReminderEnabled(value) }
    }

    fun setReminderTime(value: String) {
        viewModelScope.launch { preferencesDataStore.setReminderTime(value) }
    }

    fun setDailyNewLimit(value: Int) {
        viewModelScope.launch { preferencesDataStore.setDailyNewLimit(value.coerceIn(1, 100)) }
    }

    fun setDailyReviewLimit(value: Int) {
        viewModelScope.launch { preferencesDataStore.setDailyReviewLimit(value.coerceIn(10, 500)) }
    }

    fun setTtsLang(value: String) {
        viewModelScope.launch { preferencesDataStore.setTtsLang(value) }
    }

    fun setTtsSpeed(value: Float) {
        viewModelScope.launch { preferencesDataStore.setTtsSpeed(value.coerceIn(0.5f, 2f)) }
    }

    /** Full progress reset: streak/XP/daily stats and SRS state of every card. */
    fun resetProgress() {
        viewModelScope.launch {
            preferencesDataStore.resetProgress()
            statsRepository.clearAll()
            val today = RateCardUseCase.getTodayDate()
            _uiState.value.cards.forEach { card ->
                cardRepository.updateCard(
                    card.copy(ease = 2.5f, reps = 0, interval = 0, dueDate = today, lastReviewed = null)
                )
            }
        }
    }

    fun searchDecks(
        query: String,
        decks: List<DeckWithStats>
    ): List<DeckWithStats> {
        return decks.filter { item ->
            item.deck.name.contains(query, ignoreCase = true) ||
            searchDecks(query, item.children).isNotEmpty()
        }.map { item ->
            item.copy(children = searchDecks(query, item.children))
        }
    }
}
