package uz.nodirbek.flashcardsapp.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import uz.nodirbek.flashcardsapp.data.repository.UnitRepository
import uz.nodirbek.flashcardsapp.data.transfer.CardParseResult
import uz.nodirbek.flashcardsapp.data.transfer.parseCardsFromUri
import uz.nodirbek.flashcardsapp.domain.model.Card
import uz.nodirbek.flashcardsapp.domain.model.Deck
import uz.nodirbek.flashcardsapp.domain.usecase.RateCardUseCase
import uz.nodirbek.flashcardsapp.ui.components.HtmlText
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.components.SnackbarData
import uz.nodirbek.flashcardsapp.ui.components.TopSnackbar
import uz.nodirbek.flashcardsapp.ui.components.rememberSnackbarState
import uz.nodirbek.flashcardsapp.ui.state.DeckWithStats
import uz.nodirbek.flashcardsapp.ui.theme.*
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel
import androidx.core.graphics.toColorInt

private enum class CardFilter { ALL, NEW, LEARNING, DUE }
private enum class DeckTab(val label: String) { PATH("Путь"), WORDS("Слова"), PRACTICE("Практика") }

private val subRowColors = listOf(
    "#4255FF", "#2EC748", "#FF9600", "#9069CD", "#FF4B4B", "#1CB0F6"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckScreen(
    deckId: String,
    viewModel: HomeViewModel,
    unitRepository: UnitRepository,
    onBack: () -> Unit,
    onNavigateToSrs: (String) -> Unit,
    onNavigateToFlashcards: (String) -> Unit,
    onNavigateToTestSetup: (String) -> Unit,
    onNavigateToMatch: (String) -> Unit,
    onNavigateToForgetting: (String) -> Unit,
    onOpenUnit: (deckId: String, unitIndex: Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val deckWithStats = remember(uiState.decks, deckId) {
        fun findDeck(list: List<DeckWithStats>): DeckWithStats? =
            list.firstOrNull { it.deck.id == deckId }
                ?: list.flatMap { it.children }.let { if (it.isEmpty()) null else findDeck(it) }
        findDeck(uiState.decks)
    }
    val descendantIds = remember(uiState.decks, deckId) { viewModel.getDeckWithDescendantIds(deckId) }
    val deckCards = remember(uiState.cards, descendantIds) {
        uiState.cards.filter { it.deckId in descendantIds }
    }
    val subRows = remember(deckWithStats) { deckWithStats?.children?.map { it.deck } ?: emptyList() }

    var activeTab by remember { mutableStateOf(DeckTab.PATH) }
    var activeFilter by remember { mutableStateOf(CardFilter.ALL) }
    var showAddCardSheet by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<Card?>(null) }
    var showAddSubRowDialog by remember { mutableStateOf(false) }
    var managingSubRow by remember { mutableStateOf<Deck?>(null) }
    var pendingImportSubRowId by remember { mutableStateOf<String?>(null) }
    val snackbar = rememberSnackbarState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val subRowImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val targetId = pendingImportSubRowId ?: return@rememberLauncherForActivityResult
        pendingImportSubRowId = null
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            when (val result = parseCardsFromUri(uri, context, targetId)) {
                is CardParseResult.Success -> {
                    viewModel.addCards(result.cards)
                    snackbar.show(SnackbarData("Импортировано ${result.cards.size} карточек", icon = "✅", color = FdGreen))
                }
                is CardParseResult.Error -> {
                    snackbar.show(SnackbarData(result.message, icon = "⚠️", color = FdRed))
                }
            }
        }
    }

    val today = RateCardUseCase.getTodayDate()
    val filteredCards = remember(deckCards, activeFilter, today) {
        when (activeFilter) {
            CardFilter.ALL -> deckCards
            CardFilter.NEW -> deckCards.filter { it.reps == 0 }
            CardFilter.LEARNING -> deckCards.filter { it.reps > 0 && it.dueDate > today }
            CardFilter.DUE -> deckCards.filter { it.dueDate <= today && it.reps > 0 }
        }
    }

    val indicatorColor = remember(deckWithStats) {
        try { Color((deckWithStats?.deck?.colorHex ?: "#4255FF").toColorInt()) }
        catch (e: Exception) { FdPrimary }
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 0.dp) {
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Box(
                            Modifier
                                .width(4.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(indicatorColor)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            deckWithStats?.deck?.name ?: "Колода",
                            fontFamily = OutfitFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showAddCardSheet = true }) {
                            Icon(Icons.Default.Add, null, tint = FdPrimary)
                        }
                    }
                    // Сегментированный переключатель табов
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DeckTab.entries.forEach { tab ->
                            val isActive = activeTab == tab
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (isActive) FdPrimary else Color.Transparent)
                                    .clickable { activeTab = tab }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    tab.label,
                                    fontFamily = OutfitFamily,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.5.dp)
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (activeTab) {
                DeckTab.PATH -> DeckPathContent(
                    courseDeckId = deckId,
                    unitRepository = unitRepository,
                    dueCount = deckWithStats?.dueCards ?: 0,
                    onOpenUnit = onOpenUnit,
                    onStartReview = { onNavigateToSrs(deckId) },
                    onAddSubRow = { showAddSubRowDialog = true },
                    onSubRowLongPress = { managingSubRow = it },
                    onAddWords = { showAddCardSheet = true },
                    onDeleteUnit = { unitDeckId, unitIndex ->
                        scope.launch {
                            unitRepository.deleteUnit(unitDeckId, unitIndex)
                            snackbar.show(
                                SnackbarData("Юнит удалён · вернуть можно в настройках", icon = "🗑️", color = FdRed)
                            )
                        }
                    }
                )

                DeckTab.WORDS -> WordsTabContent(
                    deckWithStats = deckWithStats,
                    filteredCards = filteredCards,
                    activeFilter = activeFilter,
                    onFilterChange = { activeFilter = it },
                    today = today,
                    onNavigateToForgetting = { onNavigateToForgetting(deckId) },
                    onCardLongClick = { editingCard = it }
                )

                DeckTab.PRACTICE -> PracticeTabContent(
                    dueCount = deckWithStats?.dueCards ?: 0,
                    onFlashcards = { onNavigateToFlashcards(deckId) },
                    onSrs = { onNavigateToSrs(deckId) },
                    onTest = { onNavigateToTestSetup(deckId) },
                    onMatch = { onNavigateToMatch(deckId) }
                )

            }
        }
    }

    if (showAddCardSheet) {
        AddWordBottomSheet(
            deckId = deckId,
            subRows = subRows,
            onDismiss = { showAddCardSheet = false },
            onSave = { card ->
                viewModel.addCard(card)
                showAddCardSheet = false
                scope.launch { snackbar.show(SnackbarData("Карточка добавлена", icon = "✅", color = FdGreen)) }
            },
            onSaveAndNext = { card ->
                viewModel.addCard(card)
                scope.launch { snackbar.show(SnackbarData("Карточка добавлена", icon = "✅", color = FdGreen)) }
            },
            onSaveMultiple = { cards ->
                viewModel.addCards(cards)
                showAddCardSheet = false
                scope.launch { snackbar.show(SnackbarData("Импортировано ${cards.size} карточек", icon = "✅", color = FdGreen)) }
            }
        )
    }

    editingCard?.let { card ->
        EditCardBottomSheet(
            card = card,
            onDismiss = { editingCard = null },
            onSave = { updated ->
                viewModel.updateCard(updated)
                editingCard = null
            },
            onDelete = { toDelete ->
                viewModel.deleteCard(toDelete)
                editingCard = null
                scope.launch { snackbar.show(SnackbarData("Карточка удалена", icon = "🗑️", color = FdRed)) }
            }
        )
    }

    if (showAddSubRowDialog) {
        AddSubRowDialog(
            onDismiss = { showAddSubRowDialog = false },
            onCreate = { name, colorHex ->
                viewModel.addSubRow(deckId, name, colorHex)
                showAddSubRowDialog = false
            },
            onCreateAndImport = { name, colorHex ->
                val newId = java.util.UUID.randomUUID().toString()
                viewModel.addSubRowWithId(newId, deckId, name, colorHex)
                showAddSubRowDialog = false
                pendingImportSubRowId = newId
                subRowImportLauncher.launch("*/*")
            }
        )
    }

    managingSubRow?.let { subRow ->
        SubRowManageSheet(
            subRow = subRow,
            onDismiss = { managingSubRow = null },
            onRename = { newName -> viewModel.updateDeck(subRow.copy(name = newName)); managingSubRow = null },
            onRecolor = { hex -> viewModel.updateDeck(subRow.copy(colorHex = hex)); managingSubRow = null },
            onMoveUp = { viewModel.moveSubRow(subRow, -1); managingSubRow = null },
            onMoveDown = { viewModel.moveSubRow(subRow, +1); managingSubRow = null },
            onDelete = {
                val name = subRow.name
                viewModel.deleteDeck(subRow)
                managingSubRow = null
                scope.launch { snackbar.show(SnackbarData("«$name» удалена", icon = "🗑️", color = FdRed)) }
            }
        )
    }

    TopSnackbar(data = snackbar.data)
    } // end outer Box
}

// ─────────────────────────── Words tab ───────────────────────────

@Composable
private fun WordsTabContent(
    deckWithStats: DeckWithStats?,
    filteredCards: List<Card>,
    activeFilter: CardFilter,
    onFilterChange: (CardFilter) -> Unit,
    today: String,
    onNavigateToForgetting: () -> Unit,
    onCardLongClick: (Card) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        // Stats row
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatPill("${deckWithStats?.totalCards ?: 0}", "Всего", MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
                StatPill("${deckWithStats?.newCards ?: 0}", "Новых", FdPrimary, Modifier.weight(1f))
                StatPill("${deckWithStats?.dueCards ?: 0}", "На повтор", FdOrange, Modifier.weight(1f))
                StatPill(
                    "${((deckWithStats?.totalCards ?: 0) - (deckWithStats?.newCards ?: 0) - (deckWithStats?.dueCards ?: 0)).coerceAtLeast(0)}",
                    "Изучено",
                    FdGreen,
                    Modifier.weight(1f)
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
        }

        // Forgetting edge warning
        if ((deckWithStats?.dueCards ?: 0) > 0) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(FdOrangeLight)
                        .border(1.5.dp, FdOrange.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onNavigateToForgetting)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠️", fontSize = 14.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${deckWithStats?.dueCards} карточек на грани забывания",
                            fontFamily = OutfitFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = FdOrangeDark,
                            modifier = Modifier.weight(1f)
                        )
                        Text("›", fontSize = 16.sp, color = FdOrange)
                    }
                }
            }
        }

        // Filter chips
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CardFilter.entries) { filter ->
                    val label = when (filter) {
                        CardFilter.ALL -> "Все"
                        CardFilter.NEW -> "Новые"
                        CardFilter.LEARNING -> "Изучаются"
                        CardFilter.DUE -> "Повторение"
                    }
                    val isActive = activeFilter == filter
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(if (isActive) FdPrimary else MaterialTheme.colorScheme.surface)
                            .border(1.5.dp, if (isActive) FdPrimaryDark else MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { onFilterChange(filter) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            label,
                            fontFamily = OutfitFamily,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Card list
        if (filteredCards.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Нет карточек", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        } else {
            items(filteredCards, key = { it.id }) { card ->
                CardListItem(
                    card = card,
                    todayDate = today,
                    onLongClick = { onCardLongClick(card) }
                )
            }
        }
    }
}

// ─────────────────────────── Practice tab ───────────────────────────

@Composable
private fun PracticeTabContent(
    dueCount: Int,
    onFlashcards: () -> Unit,
    onSrs: () -> Unit,
    onTest: () -> Unit,
    onMatch: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Свободная практика",
            fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "Отдельные упражнения вне пути юнитов",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StudyModeBtn(
                label = "Карточки", emoji = "🃏",
                color = FdGreen, shadowColor = FdGreenDark,
                modifier = Modifier.weight(1f),
                onClick = onFlashcards
            )
            Box(Modifier.weight(1f)) {
                StudyModeBtn(
                    label = "Учить SRS", emoji = "🔄",
                    color = FdPrimary, shadowColor = FdPrimaryDark,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSrs
                )
                if (dueCount > 0) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(FdRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$dueCount",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StudyModeBtn(
                label = "Тест", emoji = "✏️",
                color = FdOrange, shadowColor = FdOrangeDark,
                modifier = Modifier.weight(1f),
                onClick = onTest
            )
            StudyModeBtn(
                label = "Match", emoji = "🎯",
                color = FdPurple, shadowColor = FdPurpleDark,
                modifier = Modifier.weight(1f),
                onClick = onMatch
            )
        }
    }
}

// ─────────────────────────── SubRow dialogs ───────────────────────────

@Composable
private fun AddSubRowDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, colorHex: String) -> Unit,
    onCreateAndImport: ((name: String, colorHex: String) -> Unit)? = null
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(subRowColors.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                "Новая тема",
                fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Название темы") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                ColorPickerRow(selected = selectedColor, onSelect = { selectedColor = it })
                if (onCreateAndImport != null) {
                    Spacer(Modifier.height(14.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(FdPrimaryLight)
                            .border(1.dp, FdPrimary.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .clickable(enabled = name.isNotBlank()) {
                                if (name.isNotBlank()) onCreateAndImport(name.trim(), selectedColor)
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "📂 Создать и добавить из файла",
                            fontFamily = OutfitFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (name.isNotBlank()) FdPrimary else FdPrimary.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name.trim(), selectedColor) },
                enabled = name.isNotBlank()
            ) { Text("Создать", fontWeight = FontWeight.Bold, color = FdPrimary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubRowManageSheet(
    subRow: Deck,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onRecolor: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(subRow.name) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

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
                "Тема «${subRow.name}»",
                fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            ColorPickerRow(selected = subRow.colorHex, onSelect = onRecolor)
            Spacer(Modifier.height(18.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PressButton(
                    onClick = onMoveUp,
                    modifier = Modifier.weight(1f).height(44.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowColor = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(10.dp)
                ) { Text("↑ Выше", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface) }
                PressButton(
                    onClick = onMoveDown,
                    modifier = Modifier.weight(1f).height(44.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowColor = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(10.dp)
                ) { Text("↓ Ниже", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface) }
            }
            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PressButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.weight(1f).height(46.dp),
                    color = FdRed.copy(alpha = 0.12f),
                    shadowColor = FdRed.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Удалить", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FdRed) }
                PressButton(
                    onClick = { if (name.isNotBlank()) onRename(name.trim()) },
                    modifier = Modifier.weight(1f).height(46.dp),
                    color = FdPrimary, shadowColor = FdPrimaryDark,
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Сохранить", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White) }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Удалить тему?", fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold) },
            text = { Text("Все слова темы «${subRow.name}» будут удалены безвозвратно.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Удалить", fontWeight = FontWeight.Bold, color = FdRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Отмена", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
private fun ColorPickerRow(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        subRowColors.forEach { hex ->
            val color = remember(hex) {
                try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { FdPrimary }
            }
            val isSelected = hex.equals(selected, ignoreCase = true)
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onSelect(hex) }
            )
        }
    }
}

// ─────────────────────────── Shared pieces ───────────────────────────

@Composable
private fun StatPill(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = color)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StudyModeBtn(
    label: String, emoji: String, color: Color, shadowColor: Color,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    PressButton(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        color = color,
        shadowColor = shadowColor,
        shape = RoundedCornerShape(14.dp),
        shadowDp = 4.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(emoji, fontSize = 20.sp)
            Text(label, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CardListItem(card: Card, todayDate: String, onLongClick: () -> Unit = {}) {
    val isDue = card.reps > 0 && card.dueDate <= todayDate
    val isNew = card.reps == 0
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .combinedClickable(onClick = {}, onLongClick = onLongClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                HtmlText(card.front, fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                HtmlText(card.back, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(Modifier.width(8.dp))
            val badgeColor = when { isNew -> FdPrimary; isDue -> FdOrange; else -> FdGreen }
            val badgeLabel = when { isNew -> "Новая"; isDue -> "Сейчас"; else -> "✓" }
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.12f))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(badgeLabel, fontSize = 11.sp, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, color = badgeColor)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
    }
}
