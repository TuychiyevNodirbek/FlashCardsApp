package uz.nodirbek.flashcardsapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import uz.nodirbek.flashcardsapp.data.local.preferences.PreferencesDataStore
import uz.nodirbek.flashcardsapp.data.repository.CardRepository
import uz.nodirbek.flashcardsapp.data.repository.DeckRepository
import uz.nodirbek.flashcardsapp.data.repository.StatsRepository
import uz.nodirbek.flashcardsapp.domain.usecase.RateCardUseCase

class HomeViewModelFactory(
    private val cardRepository: CardRepository,
    private val deckRepository: DeckRepository,
    private val statsRepository: StatsRepository,
    private val preferencesDataStore: PreferencesDataStore,
    private val rateCardUseCase: RateCardUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(
            cardRepository = cardRepository,
            deckRepository = deckRepository,
            statsRepository = statsRepository,
            preferencesDataStore = preferencesDataStore,
            rateCardUseCase = rateCardUseCase
        ) as T
    }
}
