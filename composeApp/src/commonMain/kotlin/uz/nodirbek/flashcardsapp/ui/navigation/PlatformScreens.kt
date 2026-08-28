package uz.nodirbek.flashcardsapp.ui.navigation

import androidx.compose.runtime.Composable
import uz.nodirbek.flashcardsapp.shared.data.transfer.DeckTransferRepository
import uz.nodirbek.flashcardsapp.shared.model.Card
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel

/**
 * Экраны, у которых пока нет общей реализации (WebView-скрейпинг AnkiWeb,
 * файловые пикеры, HttpURLConnection) — androidMain даёт полноценные версии
 * (HomeScreen/SettingsScreen/ImportScreen/AnkiWebBrowseScreen/OpenTDBBrowseScreen),
 * iosMain — временные заглушки/минимальные реализации. Полный разбор на общую
 * и платформенную часть — Фаза 6.
 */
@Composable
expect fun PlatformHomeScreen(
    viewModel: HomeViewModel,
    deckTransferRepository: DeckTransferRepository?,
    onNavigateToStudy: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDeck: (String) -> Unit
)

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
