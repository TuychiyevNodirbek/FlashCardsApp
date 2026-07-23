package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.material3.FloatingActionButtonDefaults
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
import uz.nodirbek.flashcardsapp.R
import uz.nodirbek.flashcardsapp.domain.usecase.RateCardUseCase
import uz.nodirbek.flashcardsapp.ui.state.DeckWithStats
import uz.nodirbek.flashcardsapp.ui.theme.*
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel
import uz.nodirbek.flashcardsapp.ui.components.UnifiedAppBar

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToStudy: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDeck: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDeckSheet by remember { mutableStateOf(false) }
    val expandedDecks = remember { mutableStateOf(setOf<String>()) }

    val today = RateCardUseCase.getTodayDate()

    val filteredDecks = remember(uiState.decks, searchQuery) {
        if (searchQuery.isBlank()) uiState.decks
        else viewModel.searchDecks(searchQuery, uiState.decks)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDeckSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(6.dp, 8.dp)
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
                        // XP badge
                        Badge(
                            streak = uiState.xp.toInt(),
                            icon = "⚡",
                            bgColor = Color(0xFFFFFBE6),
                            textColor = Color(0xFFA07800)
                        )
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
                                }
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

@Composable
private fun DeckRow(
    deck: DeckWithStats,
    depth: Int,
    isExpanded: Boolean,
    todayDate: String,
    onRowClick: () -> Unit,
    onExpandClick: () -> Unit
) {
    val indentDp = (14 + depth * 20).dp
    val indicatorColor = try { Color(android.graphics.Color.parseColor(deck.deck.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onRowClick)
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
                Text(
                    deck.deck.name,
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
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
