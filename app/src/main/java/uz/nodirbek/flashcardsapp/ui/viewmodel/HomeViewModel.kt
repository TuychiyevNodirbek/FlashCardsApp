package uz.nodirbek.flashcardsapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.nodirbek.flashcardsapp.data.local.preferences.PreferencesDataStore
import uz.nodirbek.flashcardsapp.data.repository.CardRepository
import uz.nodirbek.flashcardsapp.data.repository.DeckRepository
import uz.nodirbek.flashcardsapp.data.repository.StatsRepository
import uz.nodirbek.flashcardsapp.domain.model.Achievement
import uz.nodirbek.flashcardsapp.domain.model.Achievements
import uz.nodirbek.flashcardsapp.domain.model.Card
import uz.nodirbek.flashcardsapp.domain.model.Deck
import uz.nodirbek.flashcardsapp.domain.model.isDueReview
import uz.nodirbek.flashcardsapp.domain.model.isNew
import uz.nodirbek.flashcardsapp.domain.usecase.RateCardUseCase
import uz.nodirbek.flashcardsapp.ui.state.DeletedCardBatch
import uz.nodirbek.flashcardsapp.ui.state.DeletedDeckItem
import uz.nodirbek.flashcardsapp.ui.state.DeckWithStats
import uz.nodirbek.flashcardsapp.ui.state.HomeUiState
import java.time.LocalTime

class HomeViewModel(
    private val cardRepository: CardRepository,
    private val deckRepository: DeckRepository,
    private val statsRepository: StatsRepository,
    private val preferencesDataStore: PreferencesDataStore,
    private val rateCardUseCase: RateCardUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Fires when a card earns double XP (1/15 chance on correct answer)
    private val _doubleXpEvent = MutableSharedFlow<Unit>(replay = 0)
    val doubleXpEvent: SharedFlow<Unit> = _doubleXpEvent.asSharedFlow()

    // Fires when a new achievement is unlocked
    private val _achievementEvent = MutableSharedFlow<Achievement>(replay = 0)
    val achievementEvent: SharedFlow<Achievement> = _achievementEvent.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch { cardRepository.deleteOrphanedCards() }
        viewModelScope.launch {
            cardRepository.getAllCards().collect { cards ->
                val today = RateCardUseCase.getTodayDate()
                val dueCards = cards.filter { it.dueDate <= today }
                val rawNewCount = cards.count { it.isNew() }
                val rawReviewCount = cards.count { it.isDueReview(today) }
                _uiState.update { it.copy(cards = cards, dueCards = dueCards, cardCount = cards.size, isLoading = false, rawNewCount = rawNewCount, rawReviewCount = rawReviewCount) }
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
        viewModelScope.launch { preferencesDataStore.unlockedAchievements.collect { a -> _uiState.update { it.copy(unlockedAchievements = a) } } }
        viewModelScope.launch {
            combine(
                deckRepository.getDeletedDecks(),
                cardRepository.getDeletedCards(),
                deckRepository.getAllDecks()
            ) { deletedDecks, deletedCards, liveDecks ->
                val deletedDeckIds = deletedDecks.map { it.id }.toSet()

                val deckItems = deletedDecks.map { deck ->
                    DeletedDeckItem(
                        deck = deck,
                        cardCount = deletedCards.count { it.deckId == deck.id },
                        deletedAt = deck.deletedAt
                    )
                }.sortedByDescending { it.deletedAt }

                // Карточки из ЖИВЫХ колод — это удалённые юниты; группируем их
                // по (колода, момент удаления): одна операция = одна пачка.
                val batches = deletedCards
                    .filter { it.deckId !in deletedDeckIds }
                    .groupBy { it.deckId to it.deletedAt }
                    .map { (key, cards) ->
                        val (batchDeckId, deletedAt) = key
                        DeletedCardBatch(
                            deckId = batchDeckId,
                            deckName = liveDecks.firstOrNull { it.id == batchDeckId }?.name ?: "Колода",
                            cards = cards,
                            deletedAt = deletedAt
                        )
                    }
                    .sortedByDescending { it.deletedAt }

                deckItems to batches
            }.collect { (deckItems, batches) ->
                _uiState.update { it.copy(deletedDecks = deckItems, deletedCardBatches = batches) }
            }
        }
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
                newCards = deckCards.count { it.isNew() } + children.sumOf { it.newCards },
                dueCards = deckCards.count { it.isDueReview(today) } + children.sumOf { it.dueCards },
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
                val updatedCard = rateCardUseCase(card, quality, today)
                cardRepository.updateCard(updatedCard)
                val isCorrect = quality >= 2
                val isDoubleXp = isCorrect && (0 until 15).random() == 0
                preferencesDataStore.addXp(if (isCorrect) (if (isDoubleXp) 20L else 10L) else 4L)
                if (isDoubleXp) _doubleXpEvent.emit(Unit)
                statsRepository.recordReview(date = today, reviews = 1, correct = if (isCorrect) 1 else 0)
                updateStreak()
                checkAchievements(updatedCard)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /** Called after a session completes — checks time-based and session achievements. */
    fun onSessionCompleted(reviewed: Int, accuracy: Float) {
        viewModelScope.launch {
            val unlocked = _uiState.value.unlockedAchievements
            val now = LocalTime.now()
            if ("early_bird" !in unlocked && now.hour < 8) unlock("early_bird")
            if ("night_owl" !in unlocked && now.hour >= 22) unlock("night_owl")
            if ("perfect_session" !in unlocked && reviewed >= 10 && accuracy >= 1f) unlock("perfect_session")
        }
    }

    private suspend fun checkAchievements(updatedCard: Card) {
        val state = _uiState.value
        val unlocked = state.unlockedAchievements
        val totalReviewed = state.allStats.sumOf { it.reviewCount }

        if ("streak_7" !in unlocked && state.streak >= 7) unlock("streak_7")
        if ("streak_30" !in unlocked && state.streak >= 30) unlock("streak_30")
        if ("cards_100" !in unlocked && totalReviewed >= 100) unlock("cards_100")
        if ("cards_1000" !in unlocked && totalReviewed >= 1000) unlock("cards_1000")

        // Lapse recover: card that had ≥3 lapses just reached interval ≥7
        if ("lapse_recover" !in unlocked && updatedCard.lapses >= 3 && updatedCard.interval >= 7) {
            unlock("lapse_recover")
        }

        // Deck master: all cards in updatedCard's deck have interval ≥21
        if ("deck_master" !in unlocked) {
            val deckCards = state.cards.filter { it.deckId == updatedCard.deckId }
            if (deckCards.isNotEmpty() && deckCards.all { it.interval >= 21 }) unlock("deck_master")
        }
    }

    private suspend fun unlock(id: String) {
        preferencesDataStore.unlockAchievement(id)
        Achievements.findById(id)?.let { _achievementEvent.emit(it) }
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

    fun addDeckWithId(id: String, name: String, parentId: String? = null, colorHex: String = "#4255FF") {
        viewModelScope.launch {
            deckRepository.insertDeck(Deck(id = id, name = name, parentId = parentId, colorHex = colorHex))
        }
    }

    fun deleteDeck(deck: Deck) {
        viewModelScope.launch {
            val allIds = getDeckWithDescendantIds(deck.id)
            allIds.forEach { id ->
                cardRepository.softDeleteCardsByDeck(id)
                deckRepository.softDeleteDeck(id)
            }
        }
    }

    fun restoreDeck(deck: Deck) {
        viewModelScope.launch {
            // Restore the deck + all deleted child decks that belong to it
            val toRestore = mutableListOf(deck.id)
            val deleted = _uiState.value.deletedDecks.map { it.deck }
            fun collectDeletedChildren(parentId: String) {
                deleted.filter { it.parentId == parentId }.forEach { child ->
                    toRestore.add(child.id)
                    collectDeletedChildren(child.id)
                }
            }
            collectDeletedChildren(deck.id)
            toRestore.forEach { id ->
                cardRepository.restoreCardsByDeck(id)
                deckRepository.restoreDeck(id)
            }
        }
    }

    fun permanentlyDeleteDeck(deck: Deck) {
        viewModelScope.launch {
            val toDelete = mutableListOf(deck.id)
            val deleted = _uiState.value.deletedDecks.map { it.deck }
            fun collectDeletedChildren(parentId: String) {
                deleted.filter { it.parentId == parentId }.forEach { child ->
                    toDelete.add(child.id)
                    collectDeletedChildren(child.id)
                }
            }
            collectDeletedChildren(deck.id)
            toDelete.forEach { id ->
                cardRepository.permanentlyDeleteCardsByDeck(id)
                deckRepository.permanentlyDeleteDeckById(id)
            }
        }
    }

    /** Восстановить удалённый юнит — карточки возвращаются в свою колоду и заново разбиваются на юниты. */
    fun restoreCardBatch(batch: DeletedCardBatch) {
        viewModelScope.launch {
            cardRepository.restoreCardsByIds(batch.cards.map { it.id })
        }
    }

    fun permanentlyDeleteCardBatch(batch: DeletedCardBatch) {
        viewModelScope.launch {
            cardRepository.permanentlyDeleteCardsByIds(batch.cards.map { it.id })
        }
    }

    private fun collectAll(list: List<DeckWithStats>): List<DeckWithStats> =
        list.flatMap { listOf(it) + collectAll(it.children) }

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

    /** Создать subRow с известным ID (для немедленной ссылки, например при импорте файла). */
    fun addSubRowWithId(id: String, courseDeckId: String, name: String, colorHex: String = "#4255FF") {
        viewModelScope.launch {
            val siblings = deckRepository.getChildDecks(courseDeckId).first()
            val nextOrder = (siblings.maxOfOrNull { it.sortOrder } ?: -1) + 1
            deckRepository.insertDeck(
                Deck(id = id, name = name, parentId = courseDeckId, colorHex = colorHex, sortOrder = nextOrder)
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

    /** Due queue for a deck (включая темы-потомки), capped by the daily limits from Settings.
     *  Review cards sorted by dueDate ascending (most overdue = highest urgency first). */
    fun getDueCardsForDeck(deckId: String): List<Card> {
        val today = RateCardUseCase.getTodayDate()
        val state = _uiState.value
        val knownDeckIds = mutableSetOf<String>()
        fun collectKnown(list: List<DeckWithStats>) { list.forEach { knownDeckIds += it.deck.id; collectKnown(it.children) } }
        collectKnown(state.decks)
        val ids = if (deckId == ALL_DECKS) null else getDeckWithDescendantIds(deckId)
        val due = state.cards.filter {
            it.deckId in knownDeckIds &&
            (ids == null || it.deckId in ids) &&
            it.dueDate <= today
        }
        val newCards = due.filter { it.isNew() }.take(state.dailyNewLimit)
        val reviewCards = due.filter { !it.isNew() }
            .sortedBy { it.dueDate }  // most overdue first
            .take(state.dailyReviewLimit)
        return newCards + reviewCards
    }

    /** Total due count for a deck (used to show remaining cards after a session). */
    fun getTotalDueCount(deckId: String): Int {
        val today = RateCardUseCase.getTodayDate()
        val state = _uiState.value
        val ids = if (deckId == ALL_DECKS) null else getDeckWithDescendantIds(deckId)
        return state.cards.count { (ids == null || it.deckId in ids) && it.dueDate <= today }
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
