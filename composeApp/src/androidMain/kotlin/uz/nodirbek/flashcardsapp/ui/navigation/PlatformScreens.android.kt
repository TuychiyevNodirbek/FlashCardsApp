package uz.nodirbek.flashcardsapp.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import uz.nodirbek.flashcardsapp.data.transfer.DeckShareHelper
import uz.nodirbek.flashcardsapp.shared.data.transfer.DeckTransferRepository
import uz.nodirbek.flashcardsapp.shared.model.Card
import uz.nodirbek.flashcardsapp.shared.model.Deck
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.screen.AnkiWebBrowseScreen
import uz.nodirbek.flashcardsapp.ui.theme.FdPrimary
import uz.nodirbek.flashcardsapp.ui.theme.FdPrimaryDark
import uz.nodirbek.flashcardsapp.ui.theme.FdRed
import uz.nodirbek.flashcardsapp.ui.theme.OutfitFamily
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun PlatformDeckShareSheet(
    deck: Deck,
    transferRepository: DeckTransferRepository,
    onDismiss: () -> Unit,
    onSuccess: (message: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportError by remember { mutableStateOf<String?>(null) }

    var pendingContent by remember { mutableStateOf<String?>(null) }
    val createDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val content = pendingContent
        if (uri != null && content != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                }
                onSuccess("Файл сохранён")
            } catch (e: Exception) {
                exportError = e.message
                onDismiss()
            }
        }
        pendingContent = null
    }

    suspend fun buildContent(): String {
        val file = transferRepository.exportDeck(deck.id)
        return transferRepository.serialize(file)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "«${deck.name}»",
                fontFamily = OutfitFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Файл .md можно отправить в любой мессенджер — получатель импортирует его в приложении",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            exportError?.let {
                Text(
                    "Ошибка: $it",
                    fontSize = 12.sp,
                    color = FdRed,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(Modifier.height(18.dp))
            PressButton(
                onClick = {
                    scope.launch {
                        try {
                            DeckShareHelper.shareFile(
                                context,
                                DeckShareHelper.safeFileName(deck.name),
                                buildContent()
                            )
                            onSuccess("Колода отправлена")
                        } catch (e: Exception) {
                            exportError = e.message
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                color = FdPrimary, shadowColor = FdPrimaryDark,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "📤 Поделиться колодой",
                    fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    color = Color.White
                )
            }
            Spacer(Modifier.height(10.dp))
            PressButton(
                onClick = {
                    scope.launch {
                        try {
                            pendingContent = buildContent()
                            createDocLauncher.launch(DeckShareHelper.safeFileName(deck.name))
                        } catch (e: Exception) {
                            exportError = e.message
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowColor = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "💾 Сохранить в файл",
                    fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
