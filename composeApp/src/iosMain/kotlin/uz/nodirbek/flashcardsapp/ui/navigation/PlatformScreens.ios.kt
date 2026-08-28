package uz.nodirbek.flashcardsapp.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uz.nodirbek.flashcardsapp.shared.data.transfer.DeckTransferRepository
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel

/**
 * Минимальный, но реально рабочий список колод для iOS: без импорта/GitHub/
 * WebView (это всё ещё androidMain-only), но с полноценным переходом к
 * DeckScreen → изучение/матч/тест — эти экраны уже общие (commonMain).
 */
@Composable
actual fun PlatformHomeScreen(
    viewModel: HomeViewModel,
    deckTransferRepository: DeckTransferRepository?,
    onNavigateToStudy: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDeck: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Мои колоды", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Настройки")
            }
        }

        if (uiState.decks.isEmpty()) {
            Text(
                "Колод пока нет. Импорт колод на iOS появится в Фазе 6.",
                modifier = Modifier.padding(top = 24.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.decks, key = { it.deck.id }) { deckWithStats ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        onClick = { onNavigateToDeck(deckWithStats.deck.id) }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(deckWithStats.deck.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${deckWithStats.totalCards} карточек, ${deckWithStats.dueCards} к повторению",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

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
    onCardsImported: (List<uz.nodirbek.flashcardsapp.shared.model.Card>) -> Unit,
    onBackClick: () -> Unit,
    onBrowseAnkiWeb: () -> Unit
) = NotAvailableOnIosScreen("Импорт колод", onBackClick)

@Composable
actual fun PlatformAnkiWebBrowseScreen(
    viewModel: HomeViewModel,
    onCardsImported: (List<uz.nodirbek.flashcardsapp.shared.model.Card>) -> Unit,
    onBackClick: () -> Unit
) = NotAvailableOnIosScreen("AnkiWeb", onBackClick)

@Composable
actual fun PlatformOpenTDBBrowseScreen(
    viewModel: HomeViewModel,
    onCardsImported: (List<uz.nodirbek.flashcardsapp.shared.model.Card>) -> Unit,
    onBackClick: () -> Unit
) = NotAvailableOnIosScreen("Open Trivia DB", onBackClick)
