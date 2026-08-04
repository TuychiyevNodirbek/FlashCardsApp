package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import uz.nodirbek.flashcardsapp.data.transfer.FdeckCard
import uz.nodirbek.flashcardsapp.domain.model.Card
import uz.nodirbek.flashcardsapp.domain.usecase.RateCardUseCase
import android.util.Log
import uz.nodirbek.flashcardsapp.ui.components.DeckPickerSection
import uz.nodirbek.flashcardsapp.ui.components.UnifiedAppBar
import uz.nodirbek.flashcardsapp.ui.state.DeckWithStats
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

private data class TDBCategory(val id: Int, val emoji: String, val name: String)

private val CATEGORIES = listOf(
    TDBCategory(9,  "🌍", "Общие знания"),
    TDBCategory(17, "🔬", "Наука и природа"),
    TDBCategory(18, "💻", "Компьютеры"),
    TDBCategory(19, "📐", "Математика"),
    TDBCategory(20, "🏛️", "Мифология"),
    TDBCategory(21, "⚽", "Спорт"),
    TDBCategory(22, "🗺️", "География"),
    TDBCategory(23, "📜", "История"),
    TDBCategory(25, "🎨", "Искусство"),
    TDBCategory(27, "🐾", "Животные"),
    TDBCategory(28, "🚗", "Техника"),
)

private val COUNTS = listOf(20, 50, 100)

private const val TAG = "OpenTDBBrowse"

private suspend fun fetchOpenTDB(categoryId: Int, count: Int): List<FdeckCard> =
    withContext(Dispatchers.IO) {
        val url = "https://opentdb.com/api.php?amount=$count&category=$categoryId&type=multiple&encode=url3986"
        Log.d(TAG, "request: GET $url")
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "FlashCardsApp/1.0")
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        val code = conn.responseCode
        Log.d(TAG, "response: HTTP $code content-type=${conn.contentType} content-length=${conn.contentLength}")
        if (code != 200) {
            val err = conn.errorStream?.bufferedReader()?.readText().orEmpty()
            Log.e(TAG, "response error body: $err")
            conn.disconnect()
            return@withContext emptyList()
        }
        val body = conn.inputStream.bufferedReader().readText()
        conn.disconnect()
        Log.d(TAG, "response body (first 300 chars): ${body.take(300)}")

        val root = JSONObject(body)
        val responseCode = root.getInt("response_code")
        Log.d(TAG, "opentdb response_code=$responseCode")
        if (responseCode != 0) {
            Log.e(TAG, "opentdb error: response_code=$responseCode (0=success,1=no results,2=invalid param,3=token not found,4=token empty,5=rate limit)")
            return@withContext emptyList()
        }
        val arr = root.getJSONArray("results")
        Log.d(TAG, "parsed: ${arr.length()} questions for categoryId=$categoryId")
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val q = URLDecoder.decode(obj.getString("question"), "UTF-8")
            val a = URLDecoder.decode(obj.getString("correct_answer"), "UTF-8")
            FdeckCard(front = q, back = a)
        }
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
fun OpenTDBBrowseScreen(
    viewModel: HomeViewModel,
    onCardsImported: (List<Card>) -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    fun flattenDecks(d: DeckWithStats): List<DeckWithStats> =
        listOf(d) + d.children.flatMap { flattenDecks(it) }
    val allDecks = uiState.decks.flatMap { flattenDecks(it) }.distinctBy { it.deck.id }

    var selectedCategory by remember { mutableStateOf(CATEGORIES[0]) }
    var selectedCount by remember { mutableIntStateOf(50) }
    var selectedDeckId by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<List<FdeckCard>?>(null) }

    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            isLoading = true
            error = null
            try {
                val cards = fetchOpenTDB(selectedCategory.id, selectedCount)
                if (cards.isEmpty()) error = "Нет данных для этой категории"
                else preview = cards
            } catch (e: Exception) {
                error = e.message ?: "Ошибка сети"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            UnifiedAppBar(
                title = "Open Trivia DB",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Категория",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CATEGORIES) { cat ->
                        FilterChip(
                            selected = cat.id == selectedCategory.id,
                            onClick = {
                                selectedCategory = cat
                                preview = null
                                error = null
                            },
                            label = { Text("${cat.emoji} ${cat.name}") }
                        )
                    }
                }
            }

            item {
                Text(
                    "Количество карточек",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    COUNTS.forEach { count ->
                        FilterChip(
                            selected = count == selectedCount,
                            onClick = { selectedCount = count; preview = null },
                            label = { Text("$count") }
                        )
                    }
                }
            }

            if (error != null) {
                item {
                    Text(
                        text = "Ошибка: $error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (preview == null) {
                item {
                    Button(
                        onClick = ::load,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (isLoading) "Загружаю..." else "Загрузить карточки")
                    }
                }
            }

            preview?.let { cards ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${selectedCategory.emoji} ${selectedCategory.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "${cards.size} карточек",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            "opentdb.com · бесплатно · без логина",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(cards.take(3)) { card ->
                    TDBPreviewCard(front = card.front, back = card.back)
                }

                if (cards.size > 3) {
                    item {
                        Text(
                            "... и ещё ${cards.size - 3} карточек",
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
                        onSelectDeck = { selectedDeckId = it },
                        onCreateDeck = { name ->
                            val id = java.util.UUID.randomUUID().toString()
                            viewModel.addDeckWithId(id, name)
                            selectedDeckId = id
                        },
                        suggestedName = "${selectedCategory.emoji} ${selectedCategory.name}"
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { preview = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Отмена") }
                        Button(
                            onClick = { onCardsImported(cards.toCards(selectedDeckId ?: "default")) },
                            enabled = selectedDeckId != null,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Добавить") }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun TDBPreviewCard(front: String, back: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = front,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 4
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Text(
                text = back,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
