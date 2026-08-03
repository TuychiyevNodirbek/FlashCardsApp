package uz.nodirbek.flashcardsapp.ui.screen

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import uz.nodirbek.flashcardsapp.data.remote.ankiweb.AnkiWebBrowser
import uz.nodirbek.flashcardsapp.data.remote.ankiweb.AnkiWebDeckDetail
import uz.nodirbek.flashcardsapp.data.remote.ankiweb.AnkiWebDeckSummary
import uz.nodirbek.flashcardsapp.data.remote.ankiweb.AnkiWebException
import uz.nodirbek.flashcardsapp.data.transfer.AnkiApkgImporter
import uz.nodirbek.flashcardsapp.data.transfer.AnkiImportException
import uz.nodirbek.flashcardsapp.data.transfer.AnkiImportResult
import uz.nodirbek.flashcardsapp.domain.model.Card
import uz.nodirbek.flashcardsapp.domain.usecase.RateCardUseCase
import uz.nodirbek.flashcardsapp.notification.DownloadNotificationHelper
import uz.nodirbek.flashcardsapp.ui.components.HtmlText
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.components.UnifiedAppBar
import uz.nodirbek.flashcardsapp.ui.state.DeckWithStats
import uz.nodirbek.flashcardsapp.ui.theme.*
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel

/**
 * Поиск и импорт публичных колод из каталога AnkiWeb Shared Decks —
 * нативный UI вместо HTML-вёрстки самого AnkiWeb.
 */
@Composable
fun AnkiWebBrowseScreen(
    viewModel: HomeViewModel,
    onCardsImported: (List<Card>) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    val browser = remember { AnkiWebBrowser(context) }
    val ankiImporter = remember { AnkiApkgImporter(context) }

    var queryField by remember { mutableStateOf(TextFieldValue("")) }
    val query = queryField.text
    var isSearching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<AnkiWebDeckSummary>>(emptyList()) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }

    var selectedDeckSummary by remember { mutableStateOf<AnkiWebDeckSummary?>(null) }
    var deckDetail by remember { mutableStateOf<AnkiWebDeckDetail?>(null) }
    var isLoadingDetail by remember { mutableStateOf(false) }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var ankiPreview by remember { mutableStateOf<AnkiImportResult?>(null) }
    var selectedTargetDeckId by remember { mutableStateOf<String?>(null) }
    var importedCount by remember { mutableStateOf(0) }
    var isSuccess by remember { mutableStateOf(false) }

    fun flattenDecks(d: DeckWithStats): List<DeckWithStats> = listOf(d) + d.children.flatMap { flattenDecks(it) }
    val allDecks = uiState.decks.flatMap { flattenDecks(it) }.distinctBy { it.deck.id }

    fun doSearch() {
        val q = query.trim()
        if (q.isBlank()) return
        isSearching = true
        searchError = null
        hasSearched = true
        scope.launch {
            try {
                results = browser.search(q)
            } catch (e: Exception) {
                searchError = "Не удалось выполнить поиск: ${e.message}"
                results = emptyList()
            }
            isSearching = false
        }
    }

    fun openDeck(summary: AnkiWebDeckSummary) {
        selectedDeckSummary = summary
        deckDetail = null
        downloadError = null
        isLoadingDetail = true
        scope.launch {
            try {
                deckDetail = browser.fetchDetail(summary.sharedId)
            } catch (e: AnkiWebException) {
                deckDetail = AnkiWebDeckDetail(summary.sharedId, summary.title, "", "")
                downloadError = e.message
            } catch (e: Exception) {
                deckDetail = AnkiWebDeckDetail(summary.sharedId, summary.title, "", "")
            }
            isLoadingDetail = false
        }
    }

    fun downloadAndPreview(summary: AnkiWebDeckSummary) {
        isDownloading = true
        downloadError = null
        DownloadNotificationHelper.showDownloading(context, summary.title)
        scope.launch {
            try {
                val file = browser.download(summary.sharedId)
                DownloadNotificationHelper.showImporting(context, summary.title)
                val result = ankiImporter.import(Uri.fromFile(file))
                DownloadNotificationHelper.showSuccess(context, summary.title, result.cards.size)
                ankiPreview = result
            } catch (e: AnkiWebException) {
                DownloadNotificationHelper.cancel(context)
                downloadError = e.message ?: "Не удалось скачать колоду"
            } catch (e: AnkiImportException) {
                DownloadNotificationHelper.cancel(context)
                downloadError = e.message ?: "Не удалось прочитать скачанный файл"
            } catch (e: Exception) {
                DownloadNotificationHelper.cancel(context)
                downloadError = "Ошибка: ${e.message}"
            }
            isDownloading = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            UnifiedAppBar(
                title = "AnkiWeb: Shared Decks",
                onBackClick = {
                    if (selectedDeckSummary != null) {
                        selectedDeckSummary = null
                        deckDetail = null
                        downloadError = null
                    } else {
                        onBackClick()
                    }
                },
                showBackButton = true,
                showDivider = true
            )
        }
    ) { padding ->
        if (isSuccess) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("✅", fontSize = 40.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Добавлено $importedCount карточек",
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(20.dp))
                PressButton(
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    color = FdPrimary,
                    shadowColor = FdPrimaryDark,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Готово", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            return@Scaffold
        }

        val preview = ankiPreview
        if (preview != null) {
            // Preview screen for the downloaded deck — deck name, count, sample cards, target deck picker
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "🎴 ${preview.deckName}",
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${preview.cards.size} карточек",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(preview.cards.take(5)) { card ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            HtmlText(card.front, fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                            Spacer(Modifier.height(4.dp))
                            HtmlText(card.back, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                    }
                }
                item {
                    Text(
                        "Куда импортировать",
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    if (allDecks.isEmpty()) {
                        Text("Нет колод. Создайте колоду в приложении перед импортом.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            allDecks.forEach { deck ->
                                val isSelected = selectedTargetDeckId == deck.deck.id
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) FdPrimaryLight else MaterialTheme.colorScheme.surface)
                                        .border(1.5.dp, if (isSelected) FdPrimary else MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                        .clickable { selectedTargetDeckId = deck.deck.id }
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, null, tint = FdPrimary, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                        }
                                        Text(deck.deck.name, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PressButton(
                            onClick = { ankiPreview = null },
                            modifier = Modifier.weight(1f).height(48.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowColor = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Отмена", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        PressButton(
                            onClick = {
                                val today = RateCardUseCase.getTodayDate()
                                val cards = preview.cards.map { c ->
                                    Card(
                                        id = java.util.UUID.randomUUID().toString(),
                                        front = c.front,
                                        back = c.back,
                                        deckId = selectedTargetDeckId ?: "default",
                                        dueDate = today,
                                        createdAt = System.currentTimeMillis()
                                    )
                                }
                                onCardsImported(cards)
                                importedCount = cards.size
                                isSuccess = true
                            },
                            modifier = Modifier.weight(2f).height(48.dp),
                            color = FdPrimary,
                            shadowColor = FdPrimaryDark,
                            shape = RoundedCornerShape(12.dp),
                            enabled = selectedTargetDeckId != null
                        ) {
                            Text("Импортировать", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
            return@Scaffold
        }

        val selected = selectedDeckSummary
        if (selected != null) {
            // Deck detail screen — описание может быть длинным, поэтому оно скроллится отдельно,
            // а кнопка «Скачать и импортировать» всегда закреплена внизу и видна.
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (isLoadingDetail) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = FdPrimary)
                    }
                } else {
                    val detail = deckDetail
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(
                            detail?.title?.ifBlank { null } ?: selected.title,
                            fontFamily = OutfitFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!detail?.statsLine.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(detail!!.statsLine, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!detail?.description.isNullOrBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(detail!!.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        if (downloadError != null) {
                            Text(downloadError!!, fontSize = 13.sp, color = FdRed)
                            Spacer(Modifier.height(12.dp))
                        }
                        PressButton(
                            onClick = { downloadAndPreview(selected) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            color = FdPrimary,
                            shadowColor = FdPrimaryDark,
                            shape = RoundedCornerShape(14.dp),
                            enabled = !isDownloading
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Скачать и импортировать", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
            return@Scaffold
        }

        // Search screen
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextField(
                    value = queryField,
                    onValueChange = { newValue ->
                        queryField = newValue
                        if (newValue.text.isBlank()) {
                            // Поле поиска очищено — возвращаем подсказки-теги вместо результатов.
                            hasSearched = false
                            results = emptyList()
                            searchError = null
                        }
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Например: spanish, biology…") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                        focusedIndicatorColor = FdPrimary
                    )
                )
                PressButton(
                    onClick = { doSearch() },
                    modifier = Modifier.size(52.dp),
                    color = FdPrimary,
                    shadowColor = FdPrimaryDark,
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSearching && query.isNotBlank()
                ) {
                    Icon(Icons.Default.Search, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }

            if (!hasSearched) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        "Популярное",
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    SuggestionChipsFlow(
                        suggestions = SUGGESTED_CATEGORIES,
                        onClick = { label ->
                            // Курсор явно ставим в конец введённого тега, иначе TextFieldValue
                            // по умолчанию оставляет выделение/каретку в начале строки.
                            queryField = TextFieldValue(text = label, selection = TextRange(label.length))
                            doSearch()
                        }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            when {
                isSearching -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FdPrimary)
                }
                searchError != null -> Text(searchError!!, modifier = Modifier.padding(16.dp), color = FdRed)
                hasSearched && results.isEmpty() -> Text(
                    "Ничего не найдено",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results) { summary ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable { openDeck(summary) }
                                .padding(14.dp)
                        ) {
                            Text(
                                summary.title,
                                fontFamily = OutfitFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Метка для UI -> поисковый запрос на AnkiWeb (каталог индексирован на английском). */
private val SUGGESTED_CATEGORIES = listOf(
    "Английский" to "english",
    "Испанский" to "spanish",
    "Немецкий" to "german",
    "Французский" to "french",
    "Китайский" to "chinese",
    "Японский" to "japanese",
    "Корейский" to "korean",
    "Анатомия" to "anatomy",
    "Биология" to "biology",
    "Химия" to "chemistry",
    "История" to "history",
    "Математика" to "math",
    "Физика" to "physics",
    "Медицина" to "medicine"
)

@Composable
private fun SuggestionChipsFlow(
    suggestions: List<Pair<String, String>>,
    onClick: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { (label, searchQuery) ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(FdPrimaryLight)
                    .border(1.dp, FdPrimary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .clickable { onClick(searchQuery) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    label,
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = FdPrimary
                )
            }
        }
    }
}
