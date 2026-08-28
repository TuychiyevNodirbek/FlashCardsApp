package uz.nodirbek.flashcardsapp

import android.content.Context
import uz.nodirbek.flashcardsapp.shared.data.local.FlashCardsDatabase
import uz.nodirbek.flashcardsapp.shared.data.local.PreferencesDataStore
import uz.nodirbek.flashcardsapp.shared.data.local.buildDatabase
import uz.nodirbek.flashcardsapp.shared.data.local.createDataStore
import uz.nodirbek.flashcardsapp.shared.data.local.getDatabaseBuilder
import uz.nodirbek.flashcardsapp.shared.data.repository.CardRepository
import uz.nodirbek.flashcardsapp.shared.data.repository.DeckRepository
import uz.nodirbek.flashcardsapp.shared.data.repository.StatsRepository
import uz.nodirbek.flashcardsapp.shared.data.repository.UnitRepository
import uz.nodirbek.flashcardsapp.shared.data.transfer.DeckTransferRepository
import uz.nodirbek.flashcardsapp.shared.scheduler.RateCardUseCase
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Единая точка сборки зависимостей приложения (вместо DI-фреймворка).
 * Создаётся один раз в MainActivity.
 */
actual class AppContainer(context: Context) {
    private val database = getOrCreateDatabase(context)

    actual val cardRepository = CardRepository(database.cardDao())
    actual val deckRepository = DeckRepository(database.deckDao())
    actual val statsRepository = StatsRepository(database.dailyStatsDao())
    actual val unitRepository = UnitRepository(database.cardDao(), database.unitProgressDao(), database.deckDao())
    actual val preferencesDataStore = PreferencesDataStore(getOrCreateDataStore(context))
    actual val rateCardUseCase = RateCardUseCase()
    actual val deckTransferRepository = DeckTransferRepository(cardRepository, deckRepository)

    companion object {
        // Room/DataStore не переживают повторное создание нескольких инстансов на один файл —
        // MainActivity.onCreate пересоздаёт AppContainer при пересоздании Activity (поворот
        // экрана и т.п.), поэтому кэшируем на уровне процесса, как раньше делал
        // FlashCardsDatabase.getDatabase().
        @Volatile
        private var databaseInstance: FlashCardsDatabase? = null

        @Volatile
        private var dataStoreInstance: DataStore<Preferences>? = null

        private fun getOrCreateDatabase(context: Context): FlashCardsDatabase =
            databaseInstance ?: synchronized(this) {
                databaseInstance ?: buildDatabase(getDatabaseBuilder(context)).also { databaseInstance = it }
            }

        private fun getOrCreateDataStore(context: Context): DataStore<Preferences> =
            dataStoreInstance ?: synchronized(this) {
                dataStoreInstance ?: createDataStore(context).also { dataStoreInstance = it }
            }

        /** Для мест вне [AppContainer] (например [uz.nodirbek.flashcardsapp.notification.StreakWarningReceiver]),
         *  которым нужен тот же файл настроек без второго открытого DataStore. */
        fun dataStoreFor(context: Context): DataStore<Preferences> = getOrCreateDataStore(context)
    }
}
