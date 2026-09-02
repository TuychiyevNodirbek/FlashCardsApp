package uz.nodirbek.flashcardsapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uz.nodirbek.flashcardsapp.shared.data.transfer.DeckTransferRepository
import uz.nodirbek.flashcardsapp.shared.model.Card
import uz.nodirbek.flashcardsapp.shared.model.Deck
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel

@Composable
private fun NotAvailableOnIosScreen(title: String, onBackClick: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                "Пока недоступно на iOS — появится в Фазе 6.",
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onBackClick) { Text("Назад") }
        }
    }
}

@Composable
actual fun PlatformSettingsScreen(
    viewModel: HomeViewModel,
    onBackClick: () -> Unit,
    onImportClick: () -> Unit,
    onNavigateToDeleted: () -> Unit
) = NotAvailableOnIosScreen("Настройки", onBackClick)

@Composable
actual fun PlatformImportScreen(
    viewModel: HomeViewModel,
    deckTransferRepository: DeckTransferRepository?,
    onCardsImported: (List<Card>) -> Unit,
    onBackClick: () -> Unit,
    onBrowseAnkiWeb: () -> Unit
) = NotAvailableOnIosScreen("Импорт колод", onBackClick)

@Composable
actual fun PlatformAnkiWebBrowseScreen(
    viewModel: HomeViewModel,
    onCardsImported: (List<Card>) -> Unit,
    onBackClick: () -> Unit
) = NotAvailableOnIosScreen("AnkiWeb", onBackClick)

@Composable
actual fun PlatformOpenTDBBrowseScreen(
    viewModel: HomeViewModel,
    onCardsImported: (List<Card>) -> Unit,
    onBackClick: () -> Unit
) = NotAvailableOnIosScreen("Open Trivia DB", onBackClick)

@Composable
actual fun PlatformDeckShareSheet(
    deck: Deck,
    transferRepository: DeckTransferRepository,
    onDismiss: () -> Unit,
    onSuccess: (message: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Скоро на iOS") },
        text = { Text("Экспорт и отправка колод появятся в Фазе 6.") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Ок") } }
    )
}
