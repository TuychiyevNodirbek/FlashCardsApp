package uz.nodirbek.flashcardsapp.ui.navigation

import androidx.compose.runtime.Composable
import uz.nodirbek.flashcardsapp.shared.data.transfer.DeckTransferRepository
import uz.nodirbek.flashcardsapp.shared.model.Card
import uz.nodirbek.flashcardsapp.shared.model.Deck
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel

/**
 * Экраны, у которых пока нет общей реализации (WebView-скрейпинг AnkiWeb,
 * HttpURLConnection) — androidMain даёт полноценные версии (AnkiWebBrowseScreen/
 * OpenTDBBrowseScreen), iosMain — временные заглушки. Полный разбор на общую и
 * платформенную часть — Фаза 6. (HomeScreen/SettingsScreen/ImportScreen уже
 * полностью общие.)
 */
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
