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
import uz.nodirbek.flashcardsapp.data.repository.UnitRepository
import uz.nodirbek.flashcardsapp.domain.model.Card

enum class FlowStep { FLASHCARDS, MATCH, TEST }

data class UnitFlowUiState(
    val cards: List<Card> = emptyList(),
    val stepIndex: Int = 0,
    val steps: List<FlowStep> = listOf(FlowStep.FLASHCARDS, FlowStep.MATCH, FlowStep.TEST),
    val correctAnswers: Int = 0,
    val totalAnswers: Int = 0,
    val finished: Boolean = false,
    val isLoading: Boolean = true
) {
    val totalSteps: Int get() = steps.size
    val currentStep: FlowStep? get() = steps.getOrNull(stepIndex)
    val accuracy: Float get() = if (totalAnswers == 0) 1f else correctAnswers.toFloat() / totalAnswers
}

class UnitFlowViewModel(
    private val unitRepository: UnitRepository,
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
        private val deckId: String,
        private val unitIndex: Int
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            UnitFlowViewModel(unitRepository, deckId, unitIndex) as T
    }
}
