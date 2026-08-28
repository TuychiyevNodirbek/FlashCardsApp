package uz.nodirbek.flashcardsapp

import uz.nodirbek.flashcardsapp.shared.data.local.buildDatabase
import uz.nodirbek.flashcardsapp.shared.data.local.createDataStore
import uz.nodirbek.flashcardsapp.shared.data.local.getDatabaseBuilder
import uz.nodirbek.flashcardsapp.shared.data.local.PreferencesDataStore
import uz.nodirbek.flashcardsapp.shared.data.repository.CardRepository
import uz.nodirbek.flashcardsapp.shared.data.repository.DeckRepository
import uz.nodirbek.flashcardsapp.shared.data.repository.StatsRepository
import uz.nodirbek.flashcardsapp.shared.data.repository.UnitRepository
import uz.nodirbek.flashcardsapp.shared.data.transfer.DeckTransferRepository
import uz.nodirbek.flashcardsapp.shared.scheduler.RateCardUseCase

actual class AppContainer {
    private val database = SharedInstances.database

    actual val cardRepository = CardRepository(database.cardDao())
    actual val deckRepository = DeckRepository(database.deckDao())
    actual val statsRepository = StatsRepository(database.dailyStatsDao())
    actual val unitRepository = UnitRepository(database.cardDao(), database.unitProgressDao(), database.deckDao())
    actual val preferencesDataStore = PreferencesDataStore(SharedInstances.dataStore)
    actual val rateCardUseCase = RateCardUseCase()
    actual val deckTransferRepository = DeckTransferRepository(cardRepository, deckRepository)
}

/**
 * Room/DataStore не переживают повторное создание нескольких инстансов на один файл —
 * кэшируем на уровне процесса (аналогично Android-версии AppContainer), на случай если
 * ComposeUIViewController/MainViewController() будет вызван больше одного раза.
 */
private object SharedInstances {
    val database by lazy { buildDatabase(getDatabaseBuilder()) }
    val dataStore by lazy { createDataStore() }
}
