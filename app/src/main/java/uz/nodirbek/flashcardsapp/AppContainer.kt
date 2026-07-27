package uz.nodirbek.flashcardsapp

import android.content.Context
import uz.nodirbek.flashcardsapp.data.local.database.FlashCardsDatabase
import uz.nodirbek.flashcardsapp.data.local.preferences.PreferencesDataStore
import uz.nodirbek.flashcardsapp.data.repository.CardRepository
import uz.nodirbek.flashcardsapp.data.repository.DeckRepository
import uz.nodirbek.flashcardsapp.data.repository.StatsRepository
import uz.nodirbek.flashcardsapp.data.repository.UnitRepository
import uz.nodirbek.flashcardsapp.data.transfer.DeckTransferRepository
import uz.nodirbek.flashcardsapp.domain.usecase.RateCardUseCase

/**
 * Единая точка сборки зависимостей приложения (вместо DI-фреймворка).
 * Создаётся один раз в MainActivity.
 */
class AppContainer(context: Context) {
    private val database = FlashCardsDatabase.getDatabase(context)

    val cardRepository = CardRepository(database.cardDao())
    val deckRepository = DeckRepository(database.deckDao())
    val statsRepository = StatsRepository(database.dailyStatsDao())
    val unitRepository = UnitRepository(database.cardDao(), database.unitProgressDao(), database.deckDao())
    val preferencesDataStore = PreferencesDataStore(context)
    val rateCardUseCase = RateCardUseCase()
    val deckTransferRepository = DeckTransferRepository(cardRepository, deckRepository)
}
