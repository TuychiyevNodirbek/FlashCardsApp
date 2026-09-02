package uz.nodirbek.flashcardsapp.ui.navigation

import androidx.compose.runtime.Composable
import uz.nodirbek.flashcardsapp.shared.data.transfer.DeckTransferRepository
import uz.nodirbek.flashcardsapp.shared.model.Card
import uz.nodirbek.flashcardsapp.shared.model.Deck
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel

/**
 * Экраны, у которых пока нет общей реализации (WebView-скрейпинг AnkiWeb,
 * файловые пикеры, HttpURLConnection) — androidMain даёт полноценные версии
 * (SettingsScreen/ImportScreen/AnkiWebBrowseScreen/OpenTDBBrowseScreen),
 * iosMain — временные заглушки. Полный разбор на общую и платформенную часть —
 * Фаза 6. (HomeScreen уже полностью общий — см. ui/screen/HomeScreen.kt.)
 */
@Composable
expect fun PlatformSettingsScreen(
    viewModel: HomeViewModel,
    onBackClick: () -> Unit,
    onImportClick: () -> Unit,
    onNavigateToDeleted: () -> Unit
)

@Composable
expect fun PlatformImportScreen(
    viewModel: HomeViewModel,
    deckTransferRepository: DeckTransferRepository?,
    onCardsImported: (List<Card>) -> Unit,
    onBackClick: () -> Unit,
    onBrowseAnkiWeb: () -> Unit
)

@Composable
expect fun PlatformAnkiWebBrowseScreen(
    viewModel: HomeViewModel,
    onCardsImported: (List<Card>) -> Unit,
    onBackClick: () -> Unit
)

@Composable
expect fun PlatformOpenTDBBrowseScreen(
    viewModel: HomeViewModel,
    onCardsImported: (List<Card>) -> Unit,
    onBackClick: () -> Unit
)

/** Bottom sheet «Поделиться колодой»: share intent (Android) / сохранение в файл. */
@Composable
expect fun PlatformDeckShareSheet(
    deck: Deck,
    transferRepository: DeckTransferRepository,
    onDismiss: () -> Unit,
    onSuccess: (message: String) -> Unit
)
