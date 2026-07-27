package uz.nodirbek.flashcardsapp.ui.screen

import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import uz.nodirbek.flashcardsapp.R
import uz.nodirbek.flashcardsapp.data.transfer.DeckShareHelper
import uz.nodirbek.flashcardsapp.data.transfer.DeckTransferRepository
import uz.nodirbek.flashcardsapp.domain.model.Deck
import uz.nodirbek.flashcardsapp.domain.usecase.RateCardUseCase
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.state.DeckWithStats
import uz.nodirbek.flashcardsapp.ui.theme.*
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel
import uz.nodirbek.flashcardsapp.ui.components.UnifiedAppBar

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    deckTransferRepository: DeckTransferRepository? = null,
    onNavigateToStudy: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDeck: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDeckSheet by remember { mutableStateOf(false) }
    var sharingDeck by remember { mutableStateOf<Deck?>(null) }
    val expandedDecks = remember { mutableStateOf(setOf<String>()) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var backPressedOnce by remember { mutableStateOf(false) }
    var showExitHint by remember { mutableStateOf(false) }
    BackHandler {
        if (backPressedOnce) {
            (context as? androidx.activity.ComponentActivity)?.finish()
        } else {
            backPressedOnce = true
            showExitHint = true
            scope.launch {
                delay(2000)
                backPressedOnce = false
                showExitHint = false
            }
        }
    }

    val today = RateCardUseCase.getTodayDate()

    val filteredDecks = remember(uiState.decks, searchQuery) {
        if (searchQuery.isBlank()) uiState.decks
        else viewModel.searchDecks(searchQuery, uiState.decks)
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDeckSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(6.dp, 8.dp),
                modifier = Modifier.padding(bottom = 92.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить колоду", modifier = Modifier.size(24.dp))
            }
        },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp
            ) {
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "FlashDeck",
                            fontFamily = OutfitFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        // Streak badge
                        Badge(
                            streak = uiState.streak,
                            icon = "🔥",
                            bgColor = FdOrangeLight,
                            textColor = FdOrange
                        )
                        Spacer(Modifier.width(6.dp))
                        // Level chip
                        LevelChip(level = uiState.level, xpInLevel = (uiState.xp % 100).toInt())
                        Spacer(Modifier.width(8.dp))
                        // Search button
                        IconBox(onClick = { searchOpen = !searchOpen }) {
                            Icon(painterResource(R.drawable.ic_search), contentDescription = "Поиск", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                    // Search bar
                    AnimatedVisibility(visible = searchOpen) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(2.dp, if (searchQuery.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(painterResource(R.drawable.ic_search), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f),
                                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontFamily = InterFamily),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) Text("Поиск колод...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontFamily = InterFamily)
                                    inner()
                                }
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.5.dp)
                }
            }
        }
    ) { padding ->
        // ── Deck List or Empty State ───────────────────────────────
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FdPrimary)
                }
            } else if (filteredDecks.isEmpty()) {
                EmptyHomeState(
                    onCreateDeck = { showAddDeckSheet = true },
                    onImport = onNavigateToImport
                )
            } else {
                // Цель дня + повторение
                val reviewedToday = remember(uiState.allStats, today) {
                    uiState.allStats.firstOrNull { it.date == today }?.reviewCount ?: 0
                }
                DailyGoalHeader(
                    reviewedToday = reviewedToday,
                    dailyGoal = uiState.dailyGoal,
                    dueCount = uiState.dueCards.size,
                    onStartReview = onNavigateToStudy
                )
                LazyColumn(Modifier.fillMaxSize()) {
                    fun renderDeck(deck: DeckWithStats, depth: Int) {
                        item(key = deck.deck.id) {
                            val isExpanded = deck.deck.id in expandedDecks.value
                            DeckRow(
                                deck = deck,
                                depth = depth,
                                isExpanded = isExpanded,
                                todayDate = today,
                                onRowClick = { onNavigateToDeck(deck.deck.id) },
                                onExpandClick = {
                                    expandedDecks.value = if (isExpanded)
                                        expandedDecks.value - deck.deck.id
                                    else
                                        expandedDecks.value + deck.deck.id
                                },
                                onDelete = { viewModel.deleteDeck(deck.deck) },
                                onPin = { viewModel.pinDeck(deck.deck) },
                                onLongClick = if (depth == 0 && deckTransferRepository != null) {
                                    { sharingDeck = deck.deck }
                                } else null
                            )
                        }
                        if (deck.deck.id in expandedDecks.value) {
                            deck.children.forEach { child -> renderDeck(child, depth + 1) }
                        }
                    }
                    filteredDecks.forEach { deck -> renderDeck(deck, 0) }
                }
            }
        }
    }

    if (showAddDeckSheet) {
        AddDeckBottomSheet(
            onDismiss = { showAddDeckSheet = false },
            onAdd = { name ->
                viewModel.addDeck(name)
                showAddDeckSheet = false
            }
        )
    }

    sharingDeck?.let { deck ->
        if (deckTransferRepository != null) {
            DeckShareSheet(
                deck = deck,
                transferRepository = deckTransferRepository,
                onDismiss = { sharingDeck = null }
            )
        }
    }

    // Custom top snackbar for double-back-to-exit hint
    AnimatedVisibility(
        visible = showExitHint,
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
                .background(Color(0xFF4255FF))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Нажмите ещё раз для выхода",
                fontFamily = OutfitFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color.White
            )
        }
    }
    } // end outer Box
}

@Composable
private fun DailyGoalHeader(
    reviewedToday: Int,
    dailyGoal: Int,
    dueCount: Int,
    onStartReview: () -> Unit
) {
    val goalMet = dailyGoal > 0 && reviewedToday >= dailyGoal
    val progress = if (dailyGoal > 0) (reviewedToday.toFloat() / dailyGoal).coerceIn(0f, 1f) else 0f

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(46.dp),
                color = if (goalMet) FdGreen else FdPrimary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                strokeWidth = 5.dp
            )
            Text(
                if (goalMet) "✓" else "${(progress * 100).toInt()}%",
                fontFamily = OutfitFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (goalMet) 18.sp else 11.sp,
                color = if (goalMet) FdGreen else MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Цель дня",
                fontFamily = OutfitFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                if (goalMet) "Выполнена! $reviewedToday из $dailyGoal"
                else "$reviewedToday из $dailyGoal повторений",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (dueCount > 0) {
            uz.nodirbek.flashcardsapp.ui.components.PressButton(
                onClick = onStartReview,
                modifier = Modifier.height(38.dp),
                color = FdOrange,
                shadowColor = FdOrangeDark,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "🔄 $dueCount",
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun Badge(streak: Int, icon: String, bgColor: Color, textColor: Color) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(icon, fontSize = 11.sp)
        Text(
            "$streak",
            fontFamily = OutfitFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            color = textColor
        )
    }
}

@Composable
private fun LevelChip(level: Int, xpInLevel: Int) {
    val progress = (xpInLevel / 100f).coerceIn(0f, 1f)
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFFFF3B0))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text("⭐", fontSize = 11.sp)
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                "Lv.$level",
                fontFamily = OutfitFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = Color(0xFFA07800)
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.width(32.dp).height(3.dp).clip(RoundedCornerShape(50)),
                color = Color(0xFFE6A000),
                trackColor = Color(0xFFE6A000).copy(alpha = 0.25f)
            )
        }
    }
}

@Composable
private fun IconBox(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun DeckRow(
    deck: DeckWithStats,
    depth: Int,
    isExpanded: Boolean,
    todayDate: String,
    onRowClick: () -> Unit,
    onExpandClick: () -> Unit,
    onDelete: () -> Unit = {},
    onPin: () -> Unit = {},
    onLongClick: (() -> Unit)? = null
) {
    val indentDp = (14 + depth * 20).dp
    val indicatorColor = try { Color(android.graphics.Color.parseColor(deck.deck.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            value == SwipeToDismissBoxValue.EndToStart
        }
    )

    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                onPin()
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.EndToStart -> onDelete()
            else -> {}
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val isPinSwipe = direction == SwipeToDismissBoxValue.StartToEnd
            val bgColor = if (isPinSwipe) FdPrimary else FdRed
            val icon = if (isPinSwipe) "📌" else "🗑️"
            val fraction = dismissState.progress.coerceIn(0f, 1f)
            val alpha = (fraction * 3f).coerceIn(0f, 1f)

            Box(
                Modifier
                    .fillMaxSize()
                    .background(bgColor.copy(alpha = alpha)),
                contentAlignment = if (isPinSwipe) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Text(
                    icon,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .combinedClickable(onClick = onRowClick, onLongClick = onLongClick)
                .padding(start = indentDp, end = 14.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(indicatorColor)
            )
            Spacer(Modifier.width(10.dp))
            // Expand/collapse if has children
            if (deck.children.isNotEmpty()) {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                        .clickable(onClick = onExpandClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier
                            .size(14.dp)
                            .rotate(if (isExpanded) 90f else 0f),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(Modifier.size(24.dp))
            }
            Spacer(Modifier.width(10.dp))
            // Deck info
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (deck.deck.isPinned) Text("📌", fontSize = 11.sp)
                    Text(
                        deck.deck.name,
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
                Text(
                    "${deck.totalCards} карт · ${deck.newCards} новых",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            // Due badge or chevron
            if (deck.dueCards > 0) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                ) {
                    Text(
                        "${deck.dueCards}",
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            } else {
                Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
    }
    } // SwipeToDismissBox
}

@Composable
private fun EmptyHomeState(onCreateDeck: () -> Unit, onImport: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(R.drawable.ic_deck_empty), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "Начните учиться",
            fontFamily = OutfitFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Создайте первую колоду или импортируйте список слов",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp,
            modifier = Modifier.widthIn(max = 220.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        // Create deck button
        Box(
            Modifier
                .width(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary)
                .border(4.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                .clickable(onClick = onCreateDeck)
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Создать колоду",
                fontFamily = OutfitFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(Modifier.height(10.dp))
        // Import CSV button
        Box(
            Modifier
                .width(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .clickable(onClick = onImport)
                .padding(13.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Импортировать CSV",
                fontFamily = OutfitFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun EmptyHomeStatePreview() {
    FlashCardsAppTheme {
        EmptyHomeState(onCreateDeck = {}, onImport = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun BadgePreview() {
    FlashCardsAppTheme {
        Badge(streak = 5, icon = "🔥", bgColor = FdOrangeLight, textColor = FdOrange)
    }
}

@Preview(showBackground = true)
@Composable
private fun DeckRowPreview() {
    FlashCardsAppTheme {
        val testDeck = DeckWithStats(
            deck = uz.nodirbek.flashcardsapp.domain.model.Deck("1", "Тестовая колода", colorHex = "#4255FF"),
            totalCards = 42,
            newCards = 10,
            dueCards = 5
        )
        DeckRow(deck = testDeck, depth = 0, isExpanded = false, todayDate = "2026-07-23", onRowClick = {}, onExpandClick = {})
    }
}

/** Bottom sheet «Поделиться колодой»: share intent или сохранение в файл. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeckShareSheet(
    deck: Deck,
    transferRepository: DeckTransferRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportError by remember { mutableStateOf<String?>(null) }

    // «Сохранить в файл» через системный диалог создания документа
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
            } catch (e: Exception) {
                exportError = e.message
            }
        }
        pendingContent = null
        onDismiss()
    }

    suspend fun buildContent(): String {
        val file = transferRepository.exportDeck(deck.id, DeckShareHelper.appVersion(context))
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
                "Файл .fdeck можно отправить в любой мессенджер — получатель импортирует его в приложении",
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
                            onDismiss()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDeckBottomSheet(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline)
            )
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Новая колода",
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Название колоды", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                singleLine = true
            )
            Spacer(Modifier.height(24.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (name.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    .border(4.dp, if (name.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .clickable(enabled = name.isNotBlank()) { onAdd(name.trim()) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Создать колоду",
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
