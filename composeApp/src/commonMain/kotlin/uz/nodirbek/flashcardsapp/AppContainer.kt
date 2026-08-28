package uz.nodirbek.flashcardsapp

import uz.nodirbek.flashcardsapp.shared.data.local.PreferencesDataStore
import uz.nodirbek.flashcardsapp.shared.data.repository.CardRepository
import uz.nodirbek.flashcardsapp.shared.data.repository.DeckRepository
import uz.nodirbek.flashcardsapp.shared.data.repository.StatsRepository
import uz.nodirbek.flashcardsapp.shared.data.repository.UnitRepository
import uz.nodirbek.flashcardsapp.shared.data.transfer.DeckTransferRepository
import uz.nodirbek.flashcardsapp.shared.scheduler.RateCardUseCase

/**
 * Единая точка сборки зависимостей приложения (вместо DI-фреймворка).
 * Конструктор — платформенный (Android нужен Context, iOS — нет), поэтому
 * expect-класс не объявляет общий constructor: AppContainer создаётся только
 * в платформенном коде (MainActivity/MainViewController) и передаётся в
 * общий App()/NavGraph() уже готовым объектом.
 */
expect class AppContainer {
    val cardRepository: CardRepository
    val deckRepository: DeckRepository
    val statsRepository: StatsRepository
    val unitRepository: UnitRepository
    val preferencesDataStore: PreferencesDataStore
    val rateCardUseCase: RateCardUseCase
    val deckTransferRepository: DeckTransferRepository
}
