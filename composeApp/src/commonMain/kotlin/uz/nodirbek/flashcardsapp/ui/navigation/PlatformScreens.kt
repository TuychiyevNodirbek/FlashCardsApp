package uz.nodirbek.flashcardsapp.ui.navigation

import androidx.compose.runtime.Composable
import uz.nodirbek.flashcardsapp.shared.data.transfer.DeckTransferRepository
import uz.nodirbek.flashcardsapp.shared.model.Card
import uz.nodirbek.flashcardsapp.shared.model.Deck
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel

/**
 * Экран, у которого пока нет общей реализации (WebView-скрейпинг AnkiWeb) —
 * androidMain даёт полноценную версию (AnkiWebBrowseScreen), iosMain — временную
 * заглушку. Полный разбор на общую и платформенную часть — Фаза 6.
 * (HomeScreen/SettingsScreen/ImportScreen/OpenTDBBrowseScreen уже полностью общие.)
 */
@Composable
expect fun PlatformAnkiWebBrowseScreen(
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
