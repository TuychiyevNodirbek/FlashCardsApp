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
        viewModelScope.launch { preferencesDataStore.xp.collect { xp -> _uiState.update { it.copy(xp = xp, level = (xp / 100).toInt() + 1) } } }
        viewModelScope.launch { statsRepository.getLast7Days().collect { stats -> _uiState.update { it.copy(last7DaysStats = stats) } } }
        viewModelScope.launch { statsRepository.getAllStats().collect { stats -> _uiState.update { it.copy(allStats = stats) } } }
        viewModelScope.launch { preferencesDataStore.theme.collect { t -> _uiState.update { it.copy(theme = t) } } }
        viewModelScope.launch { preferencesDataStore.dailyGoal.collect { g -> _uiState.update { it.copy(dailyGoal = g) } } }
        viewModelScope.launch { preferencesDataStore.reminderEnabled.collect { e -> _uiState.update { it.copy(reminderEnabled = e) } } }
        viewModelScope.launch { preferencesDataStore.reminderTime.collect { t -> _uiState.update { it.copy(reminderTime = t) } } }
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
        return decks.filter { it.parentId == null }.map { statsFor(it.id) }
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

    fun getDueCardsForDeck(deckId: String): List<Card> {
        val today = RateCardUseCase.getTodayDate()
        return _uiState.value.cards.filter { it.deckId == deckId && it.dueDate <= today }
    }

    fun getCardsForDeck(deckId: String): List<Card> =
        _uiState.value.cards.filter { it.deckId == deckId }

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
}
