package uz.nodirbek.flashcardsapp.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.nodirbek.flashcardsapp.data.transfer.AnkiApkgImporter
import uz.nodirbek.flashcardsapp.data.transfer.AnkiImportException
import uz.nodirbek.flashcardsapp.data.transfer.AnkiImportResult
import uz.nodirbek.flashcardsapp.data.transfer.DeckTransferRepository
import uz.nodirbek.flashcardsapp.data.transfer.FdeckFile
import uz.nodirbek.flashcardsapp.data.transfer.FdeckParseException
import uz.nodirbek.flashcardsapp.data.transfer.FdeckVersionException
import uz.nodirbek.flashcardsapp.shared.model.Card
import uz.nodirbek.flashcardsapp.shared.scheduler.RateCardUseCase
import uz.nodirbek.flashcardsapp.ui.components.HtmlText
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.theme.*
import uz.nodirbek.flashcardsapp.ui.components.UnifiedAppBar
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel

@Composable
fun ImportScreen(
    viewModel: HomeViewModel,
    deckTransferRepository: DeckTransferRepository? = null,
    onCardsImported: (List<Card>) -> Unit,
    onBackClick: () -> Unit,
    onBrowseAnkiWeb: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var message by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var importedCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedDeckId by remember { mutableStateOf<String?>(null) }
    var showCreateDeckDialog by remember { mutableStateOf(false) }
    var newDeckName by remember { mutableStateOf("") }
    var ankiNewDeckName by remember { mutableStateOf("") }
    var ankiShowNewDeckField by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSuccessSnackbar by remember { mutableStateOf(false) }
    var fdeckPreview by remember { mutableStateOf<FdeckFile?>(null) }
    var ankiPreview by remember { mutableStateOf<AnkiImportResult?>(null) }

    val allDecks = uiState.decks.flatMap { listOf(it) + it.children.flatMap { child ->
        fun flattenDecks(d: uz.nodirbek.flashcardsapp.ui.state.DeckWithStats): List<uz.nodirbek.flashcardsapp.ui.state.DeckWithStats> {
            return listOf(d) + d.children.flatMap { flattenDecks(it) }
        }
        flattenDecks(child)
    }}.distinctBy { it.deck.id }

    val ankiImporter = remember { AnkiApkgImporter(context) }

    fun displayNameOf(uri: android.net.Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val fileName = displayNameOf(uri).orEmpty()
            if (fileName.endsWith(".apkg", ignoreCase = true)) {
                isLoading = true
                scope.launch {
                    try {
                        ankiPreview = ankiImporter.import(uri)
                        isError = false
                        message = ""
                    } catch (e: AnkiImportException) {
                        message = e.message ?: "Не удалось прочитать файл Anki"
                        isError = true
                        isSuccess = false
                    } catch (e: Exception) {
                        message = "Ошибка: ${e.message}"
                        isError = true
                        isSuccess = false
                    }
                    isLoading = false
                }
                return@rememberLauncherForActivityResult
            }
            isLoading = true
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    message = "Не удалось открыть файл"
                    isError = true
                    isLoading = false
                    return@rememberLauncherForActivityResult
                }

                val content = inputStream.bufferedReader().readText()
                inputStream.close()

                if (content.isBlank()) {
                    message = "Файл пустой"
                    isError = true
                    isLoading = false
                    return@rememberLauncherForActivityResult
                }

                // .md deck detection — if it looks like a deck (header/metadata comment), try to parse before CSV logic
                val sniff = content.lineSequence()
                    .map { it.trim() }
                    .firstOrNull { it.isNotEmpty() && !it.startsWith("<!--") }
                    .orEmpty()
                if (sniff.startsWith("# ")) {
                    if (deckTransferRepository != null) {
                        try {
                            fdeckPreview = deckTransferRepository.parse(content)
                        } catch (e: FdeckVersionException) {
                            message = "Файл создан в более новой версии приложения"
                            isError = true
                        } catch (e: FdeckParseException) {
                            message = e.message ?: "Не удалось прочитать .md файл"
                            isError = true
                        }
                    } else {
                        message = "Импорт .md колоды не поддерживается"
                        isError = true
                    }
                    isLoading = false
                    return@rememberLauncherForActivityResult
                }

                val lines = content.lines().filter { it.isNotBlank() }
                if (lines.size > 5000) {
                    message = "Файл слишком большой (макс. 5000 строк)"
                    isError = true
                    isLoading = false
                    return@rememberLauncherForActivityResult
                }

                val delimiter = detectDelimiter(lines)
                val cards = mutableListOf<Card>()
                val today = RateCardUseCase.getTodayDate()

                for (line in lines) {
                    val parts = line.split(delimiter).map { it.trim() }
                    if (parts.size >= 2) {
                        val front = parts[0]
                        val back = parts.drop(1).joinToString(", ")
                        if (front.isNotBlank() && back.isNotBlank()) {
                            cards.add(
                                Card(
                                    id = java.util.UUID.randomUUID().toString(),
                                    front = front,
                                    back = back,
                                    deckId = selectedDeckId ?: "default",
                                    dueDate = today,
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }

                if (cards.isEmpty()) {
                    message = "Не найдено подходящих пар в файле"
                    isError = true
                    isSuccess = false
                } else {
                    onCardsImported(cards)
                    importedCount = cards.size
                    isSuccess = true
                    isError = false
                    message = ""
                    showSuccessSnackbar = true
                    scope.launch {
                        delay(3000)
                        showSuccessSnackbar = false
                    }
                }
            } catch (e: Exception) {
                message = "Ошибка: ${e.message}"
                isError = true
                isSuccess = false
            }
            isLoading = false
        }
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            UnifiedAppBar(
                title = "Импорт карточек",
                onBackClick = onBackClick,
                showBackButton = true,
                showDivider = true
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Drop zone
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(FdPrimaryLight)
                        .border(2.dp, FdPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .clickable(enabled = !isLoading) { fileLauncher.launch("*/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = FdPrimary, modifier = Modifier.size(36.dp))
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📂", fontSize = 40.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Нажмите, чтобы выбрать файл",
                                fontFamily = OutfitFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = FdPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Импортировать файл с карточками",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                PressButton(
                    onClick = onBrowseAnkiWeb,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowColor = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "🌐 Найти колоды в AnkiWeb",
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = FdPrimary
                    )
                }
            }

            // .md deck preview — shown after picking a deck .md file, before user confirms import
            if (fdeckPreview != null) {
                item {
                    val preview = fdeckPreview!!
                    val totalWords = preview.deck.cards.size +
                        preview.deck.subRows.sumOf { it.cards.size }
                    val themesCount = preview.deck.subRows.size

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(FdPrimaryLight)
                            .border(2.dp, FdPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                "📦 Найдена колода .md",
                                fontFamily = OutfitFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = FdPrimary
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                preview.deck.name,
                                fontFamily = OutfitFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            val subtitle = buildString {
                                if (themesCount > 0) append("$themesCount ${pluralThemes(themesCount)}, ")
                                append("$totalWords ${pluralWords(totalWords)}")
                            }
                            Text(
                                subtitle,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                PressButton(
                                    onClick = {
                                        scope.launch {
                                            isLoading = true
                                            try {
                                                deckTransferRepository!!.importDeck(preview)
                                                fdeckPreview = null
                                                importedCount = totalWords
                                                isSuccess = true
                                                isError = false
                                                showSuccessSnackbar = true
                                                delay(1800)
                                                onBackClick()
                                            } catch (e: Exception) {
                                                message = e.message ?: "Ошибка импорта"
                                                isError = true
                                                fdeckPreview = null
                                            }
                                            isLoading = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    color = FdPrimary,
                                    shadowColor = FdPrimaryDark,
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !isLoading
                                ) {
                                    Text(
                                        if (isLoading) "Импорт..." else "Импортировать",
                                        fontFamily = OutfitFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                                PressButton(
                                    onClick = { fdeckPreview = null },
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    shadowColor = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "Отмена",
                                        fontFamily = OutfitFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Anki .apkg preview — shown after picking an Anki file, before user confirms import
            if (ankiPreview != null) {
                item {
                    val preview = ankiPreview!!
                    val canImport = selectedDeckId != null || ankiNewDeckName.isNotBlank()
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(FdPrimaryLight)
                            .border(2.dp, FdPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                "🎴 Найдена колода Anki",
                                fontFamily = OutfitFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = FdPrimary
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                preview.deckName,
                                fontFamily = OutfitFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${preview.cards.size} ${pluralWords(preview.cards.size)}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                preview.cards.take(3).forEach { card ->
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            HtmlText(
                                                card.front,
                                                fontFamily = OutfitFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            HtmlText(
                                                card.back,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                            }

                            // Deck picker inside preview
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = FdPrimary.copy(alpha = 0.15f))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "В какую колоду?",
                                fontFamily = OutfitFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(8.dp))

                            if (allDecks.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    allDecks.take(4).forEach { deckItem ->
                                        val isSelected = selectedDeckId == deckItem.deck.id
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) FdPrimary else MaterialTheme.colorScheme.surface)
                                                .border(
                                                    1.5.dp,
                                                    if (isSelected) FdPrimary else MaterialTheme.colorScheme.outline,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    selectedDeckId = if (isSelected) null else deckItem.deck.id
                                                    ankiNewDeckName = ""
                                                    ankiShowNewDeckField = false
                                                }
                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isSelected) {
                                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                }
                                                Text(
                                                    deckItem.deck.name,
                                                    fontFamily = OutfitFamily,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(Modifier.weight(1f))
                                                Text(
                                                    "${deckItem.totalCards} карт.",
                                                    fontSize = 11.sp,
                                                    color = if (isSelected) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }

                            // Inline new deck creation
                            if (ankiShowNewDeckField) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TextField(
                                        value = ankiNewDeckName,
                                        onValueChange = {
                                            ankiNewDeckName = it
                                            if (it.isNotBlank()) selectedDeckId = null
                                        },
                                        placeholder = { Text("Название новой колоды", fontSize = 13.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                            focusedIndicatorColor = FdPrimary,
                                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                                        )
                                    )
                                    PressButton(
                                        onClick = { ankiShowNewDeckField = false; ankiNewDeckName = "" },
                                        modifier = Modifier.size(40.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        shadowColor = MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("✕", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            } else {
                                PressButton(
                                    onClick = {
                                        ankiShowNewDeckField = true
                                        selectedDeckId = null
                                    },
                                    modifier = Modifier.fillMaxWidth().height(38.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    shadowColor = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        "+ Создать новую колоду",
                                        fontFamily = OutfitFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = FdPrimary
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                PressButton(
                                    onClick = {
                                        val today = RateCardUseCase.getTodayDate()
                                        val targetDeckId = if (ankiNewDeckName.isNotBlank()) {
                                            val newId = java.util.UUID.randomUUID().toString()
                                            viewModel.addDeckWithId(newId, ankiNewDeckName.trim())
                                            newId
                                        } else {
                                            selectedDeckId ?: return@PressButton
                                        }
                                        val cards = preview.cards.map { c ->
                                            Card(
                                                id = java.util.UUID.randomUUID().toString(),
                                                front = c.front,
                                                back = c.back,
                                                deckId = targetDeckId,
                                                dueDate = today,
                                                createdAt = System.currentTimeMillis()
                                            )
                                        }
                                        onCardsImported(cards)
                                        importedCount = cards.size
                                        ankiPreview = null
                                        ankiNewDeckName = ""
                                        ankiShowNewDeckField = false
                                        isSuccess = true
                                        isError = false
                                        message = ""
                                        showSuccessSnackbar = true
                                        scope.launch {
                                            delay(3000)
                                            showSuccessSnackbar = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    color = if (canImport) FdPrimary else FdPrimary.copy(alpha = 0.4f),
                                    shadowColor = if (canImport) FdPrimaryDark else FdPrimaryDark.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = canImport
                                ) {
                                    Text(
                                        "Импортировать",
                                        fontFamily = OutfitFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                                PressButton(
                                    onClick = {
                                        ankiPreview = null
                                        ankiNewDeckName = ""
                                        ankiShowNewDeckField = false
                                    },
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    shadowColor = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "Отмена",
                                        fontFamily = OutfitFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Format instructions
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            "📋 Формат файла",
                            fontFamily = OutfitFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(10.dp))
                        FormatRow("Разделитель", "Tab, точка с запятой (;) или запятая (,)")
                        Spacer(Modifier.height(8.dp))
                        FormatRow("Структура", "термин [разделитель] перевод")
                        Spacer(Modifier.height(8.dp))
                        FormatRow("Anki", "файл .apkg — первые 2 поля заметки станут term/перевод")
                        Spacer(Modifier.height(12.dp))
                        // Example box
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    "Пример CSV:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    "apple,яблоко\nbanana,банан\nhello,привет",
                                    fontFamily = OutfitFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Deck selection
            item {
                Column {
                    Text(
                        "Выберите колоду",
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    if (allDecks.isEmpty()) {
                        Text(
                            "Нет колод. Создайте колоду перед импортом.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                            items(allDecks.size) { idx ->
                                val deck = allDecks[idx]
                                val isSelected = selectedDeckId == deck.deck.id
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) FdPrimaryLight else MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.5.dp,
                                            if (isSelected) FdPrimary else MaterialTheme.colorScheme.outline,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedDeckId = deck.deck.id }
                                        .padding(12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, null, tint = FdPrimary, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                        }
                                        Column {
                                            Text(
                                                deck.deck.name,
                                                fontFamily = OutfitFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                "${deck.totalCards} карточек",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    PressButton(
                        onClick = { showCreateDeckDialog = true },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowColor = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "+ Новая колода",
                            fontFamily = OutfitFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = FdPrimary
                        )
                    }
                }
            }

            // Result / error
            if (isSuccess) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(FdGreenLight)
                            .border(1.5.dp, FdGreen.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                "✅ Импорт успешен!",
                                fontFamily = OutfitFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = FdGreenDark
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Добавлено $importedCount карточек",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(14.dp))
                            PressButton(
                                onClick = onBackClick,
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                color = FdGreen,
                                shadowColor = FdGreenDark,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "Готово",
                                    fontFamily = OutfitFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            if (isError) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(FdRedLight)
                            .border(1.5.dp, FdRed.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                "⚠️ Ошибка",
                                fontFamily = OutfitFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = FdRed
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Main action button
            if (!isSuccess) {
                item {
                    PressButton(
                        onClick = { fileLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        color = FdPrimary,
                        shadowColor = FdPrimaryDark,
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isLoading && selectedDeckId != null
                    ) {
                        Text(
                            if (isLoading) "Загрузка..." else "Выбрать файл",
                            fontFamily = OutfitFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (selectedDeckId != null) Color.White else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }

    // Top success snackbar
    AnimatedVisibility(
        visible = showSuccessSnackbar,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1AA34A))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("✅", fontSize = 18.sp)
                Column {
                    Text(
                        "Импорт успешен!",
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        "Добавлено $importedCount карточек",
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
    } // end outer Box

    // Create deck dialog
    if (showCreateDeckDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDeckDialog = false },
            title = { Text("Создать новую колоду", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column {
                    TextField(
                        value = newDeckName,
                        onValueChange = { newDeckName = it },
                        placeholder = { Text("Название колоды") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.material3.LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                            focusedIndicatorColor = FdPrimary
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                    PressButton(
                        onClick = {
                            if (newDeckName.isNotBlank()) {
                                viewModel.addDeck(newDeckName)
                                newDeckName = ""
                                showCreateDeckDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        color = FdPrimary,
                        shadowColor = FdPrimaryDark,
                        shape = RoundedCornerShape(12.dp),
                        enabled = newDeckName.isNotBlank()
                    ) {
                        Text("Создать", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    }
                    Spacer(Modifier.height(8.dp))
                    PressButton(
                        onClick = { showCreateDeckDialog = false },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowColor = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Отмена", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            },
            confirmButton = {},
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FormatRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(100.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(value, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
    }
}

private fun pluralWords(n: Int) = when {
    n % 100 in 11..19 -> "слов"
    n % 10 == 1 -> "слово"
    n % 10 in 2..4 -> "слова"
    else -> "слов"
}

private fun pluralThemes(n: Int) = when {
    n % 100 in 11..19 -> "тем"
    n % 10 == 1 -> "тема"
    n % 10 in 2..4 -> "темы"
    else -> "тем"
}

private fun detectDelimiter(lines: List<String>): Char {
    var tabCount = 0
    var semicolonCount = 0
    var commaCount = 0

    for (line in lines.take(10)) {
        tabCount += line.count { it == '\t' }
        semicolonCount += line.count { it == ';' }
        commaCount += line.count { it == ',' }
    }

    return when {
        tabCount > semicolonCount && tabCount > commaCount -> '\t'
        semicolonCount > commaCount -> ';'
        else -> ','
    }
}
