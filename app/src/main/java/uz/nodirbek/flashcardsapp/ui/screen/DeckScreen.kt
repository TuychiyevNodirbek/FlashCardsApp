package uz.nodirbek.flashcardsapp.ui.screen

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.nodirbek.flashcardsapp.domain.model.Card
import uz.nodirbek.flashcardsapp.domain.usecase.RateCardUseCase
import uz.nodirbek.flashcardsapp.ui.state.DeckWithStats
import uz.nodirbek.flashcardsapp.ui.theme.*
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel

private enum class CardFilter { ALL, NEW, LEARNING, DUE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckScreen(
    deckId: String,
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    onNavigateToSrs: (String) -> Unit,
    onNavigateToFlashcards: (String) -> Unit,
    onNavigateToTestSetup: (String) -> Unit,
    onNavigateToMatch: (String) -> Unit,
    onNavigateToForgetting: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val deckWithStats = remember(uiState.decks, deckId) {
        fun findDeck(list: List<DeckWithStats>): DeckWithStats? =
            list.firstOrNull { it.deck.id == deckId } ?: list.flatMap { it.children }.let { findDeck(it) }
        findDeck(uiState.decks)
    }
    val deckCards = remember(uiState.cards, deckId) {
        uiState.cards.filter { it.deckId == deckId }
    }

    var activeFilter by remember { mutableStateOf(CardFilter.ALL) }
    var showAddCardSheet by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<Card?>(null) }

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
        try { Color(android.graphics.Color.parseColor(deckWithStats?.deck?.colorHex ?: "#4255FF")) }
        catch (e: Exception) { FdPrimary }
    }

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
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.5.dp)
                }
            }
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudyModeBtn(
                        label = "Карточки", emoji = "🃏",
                        color = FdGreen, shadowColor = FdGreenDark,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToFlashcards(deckId) }
                    )
                    Box(Modifier.weight(1f)) {
                        StudyModeBtn(
                            label = "Учить SRS", emoji = "🔄",
                            color = FdPrimary, shadowColor = FdPrimaryDark,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onNavigateToSrs(deckId) }
                        )
                        if ((deckWithStats?.dueCards ?: 0) > 0) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(FdRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${deckWithStats?.dueCards}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    StudyModeBtn(
                        label = "Тест", emoji = "✏️",
                        color = FdOrange, shadowColor = FdOrangeDark,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTestSetup(deckId) }
                    )
                    StudyModeBtn(
                        label = "Match", emoji = "🎯",
                        color = FdPurple, shadowColor = FdPurpleDark,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToMatch(deckId) }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
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
                    StatPill("${deckWithStats?.dueCards ?: 0}", "Учится", FdOrange, Modifier.weight(1f))
                    StatPill(
                        "${((deckWithStats?.totalCards ?: 0) - (deckWithStats?.newCards ?: 0) - (deckWithStats?.dueCards ?: 0)).coerceAtLeast(0)}",
                        "Сегодня",
                        FdRed,
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
                            .clickable { onNavigateToForgetting(deckId) }
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
                    items(CardFilter.values()) { filter ->
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
                                .clickable { activeFilter = filter }
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
                        onLongClick = { editingCard = card }
                    )
                }
            }
        }
    }

    if (showAddCardSheet) {
        AddWordBottomSheet(
            deckId = deckId,
            onDismiss = { showAddCardSheet = false },
            onSave = { card ->
                viewModel.addCard(card)
                showAddCardSheet = false
            },
            onSaveAndNext = { card -> viewModel.addCard(card) }
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
            }
        )
    }
}

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
    uz.nodirbek.flashcardsapp.ui.components.PressButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        color = color,
        shadowColor = shadowColor,
        shape = RoundedCornerShape(12.dp),
        shadowDp = 4.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(emoji, fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(label, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
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
                Text(card.front, fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text(card.back, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
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

@Preview(showBackground = true)
@Composable
private fun CardRowPreview() {
    FlashCardsAppTheme {
        val testCard = Card(
            id = "1",
            deckId = "deck1",
            front = "Hello",
            back = "Привет",
            dueDate = "2026-07-20",
            createdAt = System.currentTimeMillis(),
            reps = 0
        )
//        CardRow(card = testCard, todayDate = "2026-07-23", onLongClick = {})
    }
}
