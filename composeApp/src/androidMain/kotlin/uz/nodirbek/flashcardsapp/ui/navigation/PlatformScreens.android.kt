package uz.nodirbek.flashcardsapp.ui.navigation

import androidx.compose.runtime.Composable
import uz.nodirbek.flashcardsapp.shared.data.transfer.DeckTransferRepository
import uz.nodirbek.flashcardsapp.shared.model.Card
import uz.nodirbek.flashcardsapp.ui.screen.AnkiWebBrowseScreen
import uz.nodirbek.flashcardsapp.ui.screen.HomeScreen
import uz.nodirbek.flashcardsapp.ui.screen.ImportScreen
import uz.nodirbek.flashcardsapp.ui.screen.OpenTDBBrowseScreen
import uz.nodirbek.flashcardsapp.ui.screen.SettingsScreen
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel

@Composable
actual fun PlatformHomeScreen(
    viewModel: HomeViewModel,
    deckTransferRepository: DeckTransferRepository?,
    onNavigateToStudy: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDeck: (String) -> Unit
) {
    HomeScreen(
        viewModel = viewModel,
        deckTransferRepository = deckTransferRepository,
        onNavigateToStudy = onNavigateToStudy,
        onNavigateToImport = onNavigateToImport,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToDeck = onNavigateToDeck
    )
}

@Composable
actual fun PlatformSettingsScreen(
    viewModel: HomeViewModel,
    onBackClick: () -> Unit,
    onImportClick: () -> Unit,
    onNavigateToDeleted: () -> Unit
) {
    SettingsScreen(
        viewModel = viewModel,
        onBackClick = onBackClick,
        onImportClick = onImportClick,
        onNavigateToDeleted = onNavigateToDeleted
    )
}

@Composable
actual fun PlatformImportScreen(
    viewModel: HomeViewModel,
    deckTransferRepository: DeckTransferRepository?,
    onCardsImported: (List<Card>) -> Unit,
    onBackClick: () -> Unit,
    onBrowseAnkiWeb: () -> Unit
) {
    ImportScreen(
        viewModel = viewModel,
        deckTransferRepository = deckTransferRepository,
        onCardsImported = onCardsImported,
        onBackClick = onBackClick,
        onBrowseAnkiWeb = onBrowseAnkiWeb
    )
}

@Composable
actual fun PlatformAnkiWebBrowseScreen(
    viewModel: HomeViewModel,
    onCardsImported: (List<Card>) -> Unit,
    onBackClick: () -> Unit
) {
    AnkiWebBrowseScreen(
        viewModel = viewModel,
        onCardsImported = onCardsImported,
        onBackClick = onBackClick
    )
}

@Composable
actual fun PlatformOpenTDBBrowseScreen(
    viewModel: HomeViewModel,
    onCardsImported: (List<Card>) -> Unit,
    onBackClick: () -> Unit
) {
    OpenTDBBrowseScreen(
        viewModel = viewModel,
        onCardsImported = onCardsImported,
        onBackClick = onBackClick
    )
}
