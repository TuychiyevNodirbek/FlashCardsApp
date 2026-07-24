package uz.nodirbek.flashcardsapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.nodirbek.flashcardsapp.data.repository.CardRepository
import uz.nodirbek.flashcardsapp.data.repository.StatsRepository
import uz.nodirbek.flashcardsapp.data.repository.UnitRepository
import uz.nodirbek.flashcardsapp.domain.model.Card
import uz.nodirbek.flashcardsapp.domain.usecase.RateCardUseCase

enum class FlowStep { FLASHCARDS, MATCH, TEST, AUDIO, SCRAMBLE, WRITE }

data class UnitFlowUiState(
    val cards: List<Card> = emptyList(),
    val stepIndex: Int = 0,
    val steps: List<FlowStep> = listOf(
        FlowStep.FLASHCARDS,
        FlowStep.MATCH,
        FlowStep.TEST,
        FlowStep.AUDIO,
        FlowStep.SCRAMBLE,
        FlowStep.WRITE
    ),
    val correctAnswers: Int = 0,
    val totalAnswers: Int = 0,
    val finished: Boolean = false,
    val isLoading: Boolean = true,
    val ttsLang: String = "en",
    val ttsSpeed: Float = 1f
) {
    val totalSteps: Int get() = steps.size
    val currentStep: FlowStep? get() = steps.getOrNull(stepIndex)
    val accuracy: Float get() = if (totalAnswers == 0) 1f else correctAnswers.toFloat() / totalAnswers
}

class UnitFlowViewModel(
    private val unitRepository: UnitRepository,
    private val cardRepository: CardRepository,
    private val statsRepository: StatsRepository,
    private val rateCardUseCase: RateCardUseCase,
    private val deckId: String,
    private val unitIndex: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnitFlowUiState())
    val uiState: StateFlow<UnitFlowUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val units = unitRepository.getUnits(deckId).first()
            val unit = units.getOrNull(unitIndex)
            if (unit != null) {
                _uiState.update { it.copy(cards = unit.cards, isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onStepFinished(correct: Int, total: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            val newCorrect = state.correctAnswers + correct
            val newTotal = state.totalAnswers + total
            val nextStep = state.stepIndex + 1
            val isLast = nextStep >= state.steps.size

            if (isLast) {
                val accuracy = if (newTotal == 0) 1f else newCorrect.toFloat() / newTotal
                unitRepository.saveProgress(
                    deckId = deckId,
                    unitIndex = unitIndex,
                    completedSteps = state.steps.size,
                    completed = true,
                    accuracy = accuracy
                )
                // Записать статистику дня
                val today = RateCardUseCase.getTodayDate()
                if (newTotal > 0) {
                    statsRepository.recordReview(date = today, reviews = newTotal, correct = newCorrect)
                }
                // Инициализировать SM-2 для новых карточек юнита (reps == 0)
                state.cards.filter { it.reps == 0 }.forEach { card ->
                    val rated = rateCardUseCase(card, quality = 2, todayDate = today)
                    cardRepository.updateCard(rated)
                }
                _uiState.update { it.copy(correctAnswers = newCorrect, totalAnswers = newTotal, finished = true) }
            } else {
                unitRepository.saveProgress(
                    deckId = deckId,
                    unitIndex = unitIndex,
                    completedSteps = nextStep,
                    completed = false,
                    accuracy = 0f
                )
                _uiState.update { it.copy(correctAnswers = newCorrect, totalAnswers = newTotal, stepIndex = nextStep) }
            }
        }
    }

    class Factory(
        private val unitRepository: UnitRepository,
        private val cardRepository: CardRepository,
        private val statsRepository: StatsRepository,
        private val rateCardUseCase: RateCardUseCase,
        private val deckId: String,
        private val unitIndex: Int
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            UnitFlowViewModel(unitRepository, cardRepository, statsRepository, rateCardUseCase, deckId, unitIndex) as T
    }
}
