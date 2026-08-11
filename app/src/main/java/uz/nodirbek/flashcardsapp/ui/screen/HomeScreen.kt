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
import kotlin.math.ceil
import uz.nodirbek.flashcardsapp.ui.components.BannerAction
import uz.nodirbek.flashcardsapp.ui.components.BannerEmojiBadge
import uz.nodirbek.flashcardsapp.ui.components.BannerLeadingSize
import uz.nodirbek.flashcardsapp.ui.components.BannerStyle
import uz.nodirbek.flashcardsapp.ui.components.HomeBanner
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.components.SnackbarData
import uz.nodirbek.flashcardsapp.ui.components.TopBannerCarousel
import uz.nodirbek.flashcardsapp.ui.components.TopSnackbar
import uz.nodirbek.flashcardsapp.ui.components.rememberSnackbarState
import uz.nodirbek.flashcardsapp.ui.state.DeckWithStats
import uz.nodirbek.flashcardsapp.ui.state.HomeUiState
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
    var deckToDelete by remember { mutableStateOf<Deck?>(null) }
    var deleteDepth by remember { mutableStateOf(0) }
    var deletePhase by remember { mutableStateOf(1) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = rememberSnackbarState()
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
                TopBannerCarousel(
                    banners = homeBanners(
                        uiState = uiState,
                        reviewedToday = reviewedToday,
                        deckCount = uiState.decks.size,
                        onStartReview = onNavigateToStudy,
                        onOpenImport = onNavigateToImport
                    )
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
                                onDelete = { deckToDelete = deck.deck; deleteDepth = depth; deletePhase = 1 },
                                onPin = {
                                    viewModel.pinDeck(deck.deck)
                                    scope.launch { snackbar.show(SnackbarData("«${deck.deck.name}» закреплено", icon = "📌", color = FdPrimary)) }
                                },
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
                onDismiss = { sharingDeck = null },
                onSuccess = { msg ->
                    sharingDeck = null
                    scope.launch { snackbar.show(SnackbarData(msg, icon = "✅", color = FdGreen)) }
                }
            )
        }
    }

    deckToDelete?.let { target ->
        if (deletePhase == 1) {
            AlertDialog(
                onDismissRequest = { deckToDelete = null },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        if (deleteDepth == 0) "Удалить курс?" else "Удалить тему?",
                        fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold
                    )
                },
                text = {
                    Text("«${target.name}»" + if (deleteDepth == 0) " — все темы и слова будут удалены." else " — все слова темы будут удалены.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (deleteDepth == 0) deletePhase = 2
                        else { viewModel.deleteDeck(target); scope.launch { snackbar.show(SnackbarData("«${target.name}» удалено", icon = "🗑️", color = FdRed)) }; deckToDelete = null }
                    }) {
                        Text(if (deleteDepth == 0) "Продолжить" else "Удалить", fontWeight = FontWeight.Bold, color = FdRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deckToDelete = null }) {
                        Text("Отмена", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { deckToDelete = null },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text("Точно удалить?", fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold)
                },
                text = {
                    Text("Восстановить «${target.name}» невозможно. Все данные курса исчезнут навсегда.")
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.deleteDeck(target); scope.launch { snackbar.show(SnackbarData("«${target.name}» удалено", icon = "🗑️", color = FdRed)) }; deckToDelete = null }) {
                        Text("Да, удалить", fontWeight = FontWeight.ExtraBold, color = FdRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deckToDelete = null }) {
                        Text("Отмена", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }

    TopSnackbar(data = snackbar.data)

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

/**
 * Набор баннеров верхней карусели. Пустые по смыслу баннеры (нулевая серия,
 * пустая коллекция) не показываются, чтобы карусель не крутила заглушки.
 */
@Composable
private fun homeBanners(
    uiState: HomeUiState,
    reviewedToday: Int,
    deckCount: Int,
    onStartReview: () -> Unit,
    onOpenImport: () -> Unit
): List<HomeBanner> {
    val banners = mutableListOf<HomeBanner>()

    // ── Цель дня ──────────────────────────────────────────────────
    val dailyGoal = uiState.dailyGoal
    val goalMet = dailyGoal > 0 && reviewedToday >= dailyGoal
    val progress = if (dailyGoal > 0) (reviewedToday.toFloat() / dailyGoal).coerceIn(0f, 1f) else 0f
    val reviewCount = uiState.todayReviewCount
    val newCount = uiState.todayNewCount
    val hasWork = reviewCount > 0 || newCount > 0
    banners += HomeBanner(
        id = "daily_goal",
        title = "Сегодня",
        subtitle = when {
            !hasWork -> "Всё повторено ✓"
            reviewCount > 0 && newCount > 0 -> "$reviewCount повторов · $newCount новых"
            reviewCount > 0 -> "$reviewCount на повтор"
            else -> "$newCount новых слов"
        },
        subtitleColor = if (hasWork) null else FdGreen,
        action = if (hasWork) {
            BannerAction(
                label = "Начать",
                color = FdOrange,
                shadowColor = FdOrangeDark,
                onClick = onStartReview
            )
        } else null,
        leading = {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(BannerLeadingSize),
                    color = if (goalMet) FdGreen else FdPrimary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    strokeWidth = 6.dp
                )
                Text(
                    if (goalMet) "✓" else "${(progress * 100).toInt()}%",
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (goalMet) 22.sp else 14.sp,
                    color = if (goalMet) FdGreen else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    )

    // ── Серия ─────────────────────────────────────────────────────
    if (uiState.streak > 0) {
        val streak = uiState.streak
        banners += HomeBanner(
            id = "streak",
            title = "Серия $streak ${plural(streak, "день", "дня", "дней")}",
            subtitle = if (uiState.streakRecord > streak)
                "Рекорд: ${uiState.streakRecord} ${plural(uiState.streakRecord, "день", "дня", "дней")}"
            else
                "Это твой рекорд — не прерывай!",
            subtitleColor = if (uiState.streakRecord > streak) null else FdOrange,
            action = BannerAction(
                label = "Продолжить",
                color = FdOrange,
                shadowColor = FdOrangeDark,
                onClick = onStartReview
            ),
            leading = { BannerEmojiBadge("🔥", FdOrange.copy(alpha = 0.14f)) }
        )
    }

    // ── Промо: прокачай уровень ───────────────────────────────────
    // 100 XP на уровень, 10 XP за верный ответ (HomeViewModel.rateCard),
    // поэтому «осталось карточек» — честная оценка, а не круглое число.
    val xpToNextLevel = 100 - (uiState.xp % 100).toInt()
    val cardsToNextLevel = ceil(xpToNextLevel / 10.0).toInt()
    banners += HomeBanner(
        id = "promo_level",
        title = "Прокачай уровень ⚡",
        subtitle = "До Lv.${uiState.level + 1} осталось $xpToNextLevel XP — это примерно " +
            "$cardsToNextLevel ${plural(cardsToNextLevel, "карточка", "карточки", "карточек")}",
        style = BannerStyle.Promo(FdPrimary, FdPurple),
        action = BannerAction(
            label = "Вперёд",
            color = Color.White,
            shadowColor = Color.White.copy(alpha = 0.45f),
            textColor = FdPrimaryDark,
            onClick = onStartReview
        ),
        leading = { BannerEmojiBadge("⭐", Color.White.copy(alpha = 0.22f)) }
    )

    // ── Промо: готовые колоды ─────────────────────────────────────
    banners += HomeBanner(
        id = "promo_import",
        title = "Учись легко 📦",
        subtitle = "Импортируй колоду из Anki или CSV — тысячи слов за минуту",
        style = BannerStyle.Promo(FdOrange, FdRed),
        action = BannerAction(
            label = "Открыть",
            color = Color.White,
            shadowColor = Color.White.copy(alpha = 0.45f),
            textColor = FdOrangeDark,
            onClick = onOpenImport
        ),
        onClick = onOpenImport,
        leading = { BannerEmojiBadge("🚀", Color.White.copy(alpha = 0.22f)) }
    )

    // ── Коллекция ─────────────────────────────────────────────────
    if (uiState.cardCount > 0) {
        banners += HomeBanner(
            id = "collection",
            title = "Коллекция",
            subtitle = "$deckCount ${plural(deckCount, "колода", "колоды", "колод")} · " +
                "${uiState.cardCount} ${plural(uiState.cardCount, "карта", "карты", "карт")}",
            action = BannerAction(
                label = "Добавить",
                color = FdPrimary,
                shadowColor = FdPrimaryDark,
                onClick = onOpenImport
            ),
            leading = { BannerEmojiBadge("📚", FdPrimary.copy(alpha = 0.14f)) }
        )
    }

    return banners
}

/** Русские падежные формы: 1 день / 2 дня / 5 дней. */
private fun plural(n: Int, one: String, few: String, many: String): String {
    val mod100 = n % 100
    if (mod100 in 11..14) return many
    return when (n % 10) {
        1 -> one
        2, 3, 4 -> few
        else -> many
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
private fun CountChip(label: String, bgColor: Color, textColor: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            label,
            fontFamily = OutfitFamily,
            fontWeight = FontWeight.Bold,
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
            SwipeToDismissBoxValue.EndToStart -> {
                onDelete()
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
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
                    "${deck.totalCards} карт",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            // Status chips: new (blue) + review (orange) or checkmark
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (deck.newCards == 0 && deck.dueCards == 0) {
                    Text("✓", fontSize = 16.sp, color = FdGreen, fontWeight = FontWeight.Bold)
                } else {
                    if (deck.newCards > 0) {
                        CountChip(label = "🆕 ${deck.newCards}", bgColor = FdPrimary.copy(alpha = 0.12f), textColor = FdPrimary)
                    }
                    if (deck.dueCards > 0) {
                        CountChip(label = "🔄 ${deck.dueCards}", bgColor = FdOrangeLight, textColor = FdOrangeDark)
                    }
                }
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
    onDismiss: () -> Unit,
    onSuccess: (message: String) -> Unit = {}
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
