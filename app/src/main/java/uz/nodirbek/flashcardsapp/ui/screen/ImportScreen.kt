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
import uz.nodirbek.flashcardsapp.data.transfer.DeckTransferRepository
import uz.nodirbek.flashcardsapp.data.transfer.FdeckFile
import uz.nodirbek.flashcardsapp.data.transfer.FdeckParseException
import uz.nodirbek.flashcardsapp.data.transfer.FdeckVersionException
import uz.nodirbek.flashcardsapp.domain.model.Card
import uz.nodirbek.flashcardsapp.domain.usecase.RateCardUseCase
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.theme.*
import uz.nodirbek.flashcardsapp.ui.components.UnifiedAppBar
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel

@Composable
fun ImportScreen(
    viewModel: HomeViewModel,
    deckTransferRepository: DeckTransferRepository? = null,
    onCardsImported: (List<Card>) -> Unit,
    onBackClick: () -> Unit
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSuccessSnackbar by remember { mutableStateOf(false) }
    var fdeckPreview by remember { mutableStateOf<FdeckFile?>(null) }

    val allDecks = uiState.decks.flatMap { listOf(it) + it.children.flatMap { child ->
        fun flattenDecks(d: uz.nodirbek.flashcardsapp.ui.state.DeckWithStats): List<uz.nodirbek.flashcardsapp.ui.state.DeckWithStats> {
            return listOf(d) + d.children.flatMap { flattenDecks(it) }
        }
        flattenDecks(child)
    }}.distinctBy { it.deck.id }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
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

                // .fdeck detection — if JSON, try to parse as fdeck before CSV logic
                if (content.trimStart().startsWith("{")) {
                    if (deckTransferRepository != null) {
                        try {
                            fdeckPreview = deckTransferRepository.parse(content)
                        } catch (e: FdeckVersionException) {
                            message = "Файл создан в более новой версии приложения"
                            isError = true
                        } catch (e: FdeckParseException) {
                            message = e.message ?: "Не удалось прочитать .fdeck файл"
                            isError = true
                        }
                    } else {
                        message = "Импорт .fdeck не поддерживается"
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
                                "CSV, TSV или .fdeck файл",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // .fdeck preview — shown after picking a .fdeck file, before user confirms import
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
                                "📦 Найдена колода .fdeck",
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
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PressButton(
                                    onClick = { fdeckPreview = null },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    shadowColor = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "Отмена",
                                        fontFamily = OutfitFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
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
                                    modifier = Modifier.weight(2f).height(44.dp),
                                    color = FdPrimary,
                                    shadowColor = FdPrimaryDark,
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !isLoading
                                ) {
                                    Text(
                                        if (isLoading) "Импорт..." else "Импортировать",
                                        fontFamily = OutfitFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
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
                        FormatRow("Разделитель", "Tab (\\t), точка с запятой (;) или запятая (,)")
                        Spacer(Modifier.height(8.dp))
                        FormatRow("Структура", "лицевая сторона [разделитель] оборотная сторона")
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
            },
            confirmButton = {
                PressButton(
                    onClick = {
                        if (newDeckName.isNotBlank()) {
                            viewModel.addDeck(newDeckName)
                            newDeckName = ""
                            showCreateDeckDialog = false
                        }
                    },
                    modifier = Modifier.height(40.dp),
                    color = FdPrimary,
                    shadowColor = FdPrimaryDark,
                    shape = RoundedCornerShape(10.dp),
                    enabled = newDeckName.isNotBlank()
                ) {
                    Text("Создать", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                }
            },
            dismissButton = {
                PressButton(
                    onClick = { showCreateDeckDialog = false },
                    modifier = Modifier.height(40.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowColor = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Отмена", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            },
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
            modifier = Modifier.width(90.dp)
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
