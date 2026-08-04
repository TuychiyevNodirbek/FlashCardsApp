package uz.nodirbek.flashcardsapp.ui.screen

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import android.util.Log
import uz.nodirbek.flashcardsapp.data.transfer.AnkiApkgImporter
import uz.nodirbek.flashcardsapp.data.transfer.AnkiImportResult
import uz.nodirbek.flashcardsapp.data.transfer.FdeckCard
import uz.nodirbek.flashcardsapp.domain.model.Card
import uz.nodirbek.flashcardsapp.domain.usecase.RateCardUseCase
import uz.nodirbek.flashcardsapp.ui.components.DeckPickerSection
import uz.nodirbek.flashcardsapp.ui.components.UnifiedAppBar
import uz.nodirbek.flashcardsapp.ui.state.DeckWithStats
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private data class GitHubDeckItem(
    val fileName: String,
    val repoName: String,
    val repoDescription: String,
    val downloadUrl: String
)

private val SUGGESTIONS = listOf(
    "anki english" to "Английский",
    "anki japanese" to "Японский",
    "anki spanish" to "Испанский",
    "anki medical" to "Медицина",
    "anki anatomy" to "Анатомия",
    "anki chemistry" to "Химия",
    "anki history" to "История",
    "anki math" to "Математика",
    "anki programming" to "Программирование",
    "anki geography" to "География",
)

private const val TAG = "GitHubDeckBrowse"

private suspend fun searchGitHub(query: String): List<GitHubDeckItem> =
    withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode("extension:apkg $query", "UTF-8")
        val url = "https://api.github.com/search/code?q=$encoded&per_page=20"
        Log.d(TAG, "search() request: GET $url")
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "FlashCardsApp/1.0")
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        val code = conn.responseCode
        val remaining = conn.getHeaderField("X-RateLimit-Remaining")
        val resetEpoch = conn.getHeaderField("X-RateLimit-Reset")
        Log.d(TAG, "search() response: HTTP $code rateLimit-remaining=$remaining rateLimit-reset=$resetEpoch")
        if (code == 403 || code == 429) {
            val err = conn.errorStream?.bufferedReader()?.readText().orEmpty()
            Log.e(TAG, "search() rate-limited: $err")
            conn.disconnect()
            throw Exception("GitHub временно ограничил запросы. Подождите минуту и попробуйте снова.")
        }
        if (code != 200) {
            val err = conn.errorStream?.bufferedReader()?.readText().orEmpty()
            Log.e(TAG, "search() error HTTP $code: $err")
            conn.disconnect()
            throw Exception("GitHub ответил: HTTP $code")
        }
        val body = conn.inputStream.bufferedReader().readText()
        conn.disconnect()
        Log.d(TAG, "search() body (first 300 chars): ${body.take(300)}")

        val root = JSONObject(body)
        val totalCount = root.optInt("total_count", -1)
        val items = root.getJSONArray("items")
        Log.d(TAG, "search() parsed: total_count=$totalCount items=${items.length()} for query=\"$query\"")
        (0 until items.length()).mapNotNull { i ->
            val obj = items.getJSONObject(i)
            val fileName = obj.getString("name")
            if (!fileName.endsWith(".apkg")) return@mapNotNull null
            val htmlUrl = obj.getString("html_url")
            val repo = obj.getJSONObject("repository")
            val repoName = repo.getString("full_name")
            val repoDesc = repo.optString("description", "")
            val downloadUrl = htmlUrlToRaw(htmlUrl) ?: return@mapNotNull null
            Log.d(TAG, "search() item[$i]: file=$fileName repo=$repoName downloadUrl=$downloadUrl")
            GitHubDeckItem(
                fileName = fileName,
                repoName = repoName,
                repoDescription = repoDesc,
                downloadUrl = downloadUrl
            )
        }
    }

// https://github.com/user/repo/blob/branch/path.apkg
// → https://raw.githubusercontent.com/user/repo/branch/path.apkg
private fun htmlUrlToRaw(htmlUrl: String): String? {
    return try {
        val withoutScheme = htmlUrl.removePrefix("https://github.com/")
        val parts = withoutScheme.split("/")
        // parts: [user, repo, "blob", branch, ...path]
        if (parts.size < 5 || parts[2] != "blob") return null
        val user = parts[0]
        val repo = parts[1]
        val branch = parts[3]
        val path = parts.drop(4).joinToString("/")
        "https://raw.githubusercontent.com/$user/$repo/$branch/$path"
    } catch (e: Exception) {
        null
    }
}

private suspend fun downloadApkg(context: Context, downloadUrl: String): File =
    withContext(Dispatchers.IO) {
        Log.d(TAG, "download() request: GET $downloadUrl")
        val conn = URL(downloadUrl).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "FlashCardsApp/1.0")
        conn.connectTimeout = 15_000
        conn.readTimeout = 60_000
        val code = conn.responseCode
        val contentType = conn.contentType
        val contentLength = conn.contentLength
        Log.d(TAG, "download() response: HTTP $code content-type=$contentType content-length=$contentLength")
        if (code != 200) {
            val err = conn.errorStream?.bufferedReader()?.readText().orEmpty()
            Log.e(TAG, "download() error HTTP $code: $err")
            conn.disconnect()
            throw Exception("Ошибка скачивания: HTTP $code")
        }
        val outFile = File(context.cacheDir, "gh_import_${System.currentTimeMillis()}.apkg")
        conn.inputStream.use { input -> outFile.outputStream().use { input.copyTo(it) } }
        conn.disconnect()
        Log.d(TAG, "download() saved ${outFile.length()} bytes to ${outFile.name}")
        outFile
    }

private fun List<FdeckCard>.toCards(deckId: String = "default"): List<Card> {
    val today = RateCardUseCase.getTodayDate()
    return map { c ->
        Card(
            id = java.util.UUID.randomUUID().toString(),
            front = c.front,
            back = c.back,
            deckId = deckId,
            dueDate = today,
            createdAt = System.currentTimeMillis()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubDeckBrowseScreen(
    viewModel: HomeViewModel,
    onCardsImported: (List<Card>) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<GitHubDeckItem>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }

    var downloadingUrl by remember { mutableStateOf<String?>(null) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<AnkiImportResult?>(null) }

    val uiState by viewModel.uiState.collectAsState()
    fun flattenDecks(d: DeckWithStats): List<DeckWithStats> = listOf(d) + d.children.flatMap { flattenDecks(it) }
    val allDecks = uiState.decks.flatMap { flattenDecks(it) }.distinctBy { it.deck.id }
    var selectedDeckId by remember { mutableStateOf<String?>(null) }

    fun search() {
        if (query.isBlank()) return
        keyboard?.hide()
        scope.launch {
            isSearching = true
            searchError = null
            hasSearched = true
            try {
                results = searchGitHub(query)
            } catch (e: Exception) {
                searchError = e.message ?: "Ошибка поиска"
                results = emptyList()
            } finally {
                isSearching = false
            }
        }
    }

    fun download(item: GitHubDeckItem) {
        scope.launch {
            downloadingUrl = item.downloadUrl
            downloadError = null
            try {
                val file = downloadApkg(context, item.downloadUrl)
                val importer = AnkiApkgImporter(context)
                val uri = android.net.Uri.fromFile(file)
                preview = importer.import(uri)
                file.delete()
            } catch (e: Exception) {
                downloadError = e.message ?: "Ошибка загрузки"
            } finally {
                downloadingUrl = null
            }
        }
    }

    if (preview != null) {
        GitHubPreviewSheet(
            result = preview!!,
            allDecks = allDecks,
            selectedDeckId = selectedDeckId,
            onSelectDeck = { selectedDeckId = it },
            onCreateDeck = { name ->
                val id = java.util.UUID.randomUUID().toString()
                viewModel.addDeckWithId(id, name)
                selectedDeckId = id
            },
            onImport = onCardsImported,
            onCancel = { preview = null }
        )
        return
    }

    Scaffold(
        topBar = {
            UnifiedAppBar(
                title = "GitHub Decks",
                onBackClick = onBackClick,
                showBackButton = true,
                showDivider = true
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Например: anki english") },
                    trailingIcon = {
                        IconButton(onClick = ::search, enabled = !isSearching) {
                            Icon(Icons.Default.Search, contentDescription = "Поиск")
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { search() }),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            if (!hasSearched) {
                item {
                    Text(
                        "Популярные запросы",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SUGGESTIONS) { (eng, rus) ->
                            SuggestionChip(
                                onClick = { query = eng; search() },
                                label = { Text(rus) }
                            )
                        }
                    }
                }
            }

            if (isSearching) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                    }
                }
            }

            searchError?.let { err ->
                item {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            downloadError?.let { err ->
                item {
                    Text(
                        text = "Ошибка загрузки: $err",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (hasSearched && !isSearching && results.isEmpty() && searchError == null) {
                item {
                    Text(
                        "Ничего не найдено. Попробуйте другой запрос.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(results) { item ->
                GitHubDeckCard(
                    item = item,
                    isDownloading = downloadingUrl == item.downloadUrl,
                    onDownload = { download(item) }
                )
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun GitHubDeckCard(
    item: GitHubDeckItem,
    isDownloading: Boolean,
    onDownload: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = item.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.repoName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.repoDescription.isNotBlank()) {
                    Text(
                        text = item.repoDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp
                    )
                }
            }
            if (isDownloading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                FilledTonalButton(
                    onClick = onDownload,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Скачать", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun GitHubPreviewSheet(
    result: AnkiImportResult,
    allDecks: List<DeckWithStats>,
    selectedDeckId: String?,
    onSelectDeck: (String) -> Unit,
    onCreateDeck: (String) -> Unit,
    onImport: (List<Card>) -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(
        topBar = {
            UnifiedAppBar(
                title = "Предпросмотр",
                onBackClick = onCancel,
                showBackButton = true,
                showDivider = true
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = result.deckName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${result.cards.size} карточек",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(result.cards.take(5)) { card ->
                GHPreviewCard(front = card.front, back = card.back)
            }

            if (result.cards.size > 5) {
                item {
                    Text(
                        "... и ещё ${result.cards.size - 5} карточек",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Text(
                    "Выберите колоду",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                DeckPickerSection(
                    allDecks = allDecks,
                    selectedDeckId = selectedDeckId,
                    onSelectDeck = onSelectDeck,
                    onCreateDeck = onCreateDeck,
                    suggestedName = result.deckName
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Отмена") }
                    Button(
                        onClick = { onImport(result.cards.toCards(selectedDeckId ?: "default")) },
                        enabled = selectedDeckId != null,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Добавить колоду") }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun GHPreviewCard(front: String, back: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = front, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 4)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Text(text = back, style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
        }
    }
}