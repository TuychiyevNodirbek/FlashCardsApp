package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.components.UnifiedAppBar
import uz.nodirbek.flashcardsapp.ui.state.DeletedCardBatch
import uz.nodirbek.flashcardsapp.ui.state.DeletedDeckItem
import uz.nodirbek.flashcardsapp.ui.theme.*
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentlyDeletedScreen(
    viewModel: HomeViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val deletedDecks = uiState.deletedDecks
    val deletedBatches = uiState.deletedCardBatches

    // Only show root deleted decks (parentId == null or parent is not deleted)
    val deletedDeckIds = deletedDecks.map { it.deck.id }.toSet()
    val rootDeletedDecks = deletedDecks.filter { item ->
        item.deck.parentId == null || item.deck.parentId !in deletedDeckIds
    }

    val isEmpty = rootDeletedDecks.isEmpty() && deletedBatches.isEmpty()
    var confirmDeleteAll by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            UnifiedAppBar(
                title = "Недавно удалённые",
                onBackClick = onBackClick,
                showBackButton = true,
                showDivider = true
            )
        }
    ) { padding ->
        if (isEmpty) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🗑️", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Здесь пусто",
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Удалённые колоды и юниты появятся здесь",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (rootDeletedDecks.isNotEmpty()) {
                    item(key = "hdr_decks") { SectionHeader("Колоды") }
                    items(rootDeletedDecks, key = { "deck_${it.deck.id}" }) { item ->
                        DeletedDeckCard(
                            item = item,
                            onRestore = { viewModel.restoreDeck(item.deck) },
                            onDelete = { viewModel.permanentlyDeleteDeck(item.deck) }
                        )
                    }
                }

                if (deletedBatches.isNotEmpty()) {
                    item(key = "hdr_units") { SectionHeader("Юниты") }
                    items(deletedBatches, key = { "batch_${it.id}" }) { batch ->
                        DeletedBatchCard(
                            batch = batch,
                            onRestore = { viewModel.restoreCardBatch(batch) },
                            onDelete = { viewModel.permanentlyDeleteCardBatch(batch) }
                        )
                    }
                }
            }

            // "Delete all" button at bottom
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                PressButton(
                    onClick = { confirmDeleteAll = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    color = if (LocalIsDarkTheme.current) Color(0xFF3D1A1A) else FdRedLight,
                    shadowColor = FdRed.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Удалить всё навсегда",
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = FdRed
                    )
                }
            }
        }

        if (confirmDeleteAll) {
            AlertDialog(
                onDismissRequest = { confirmDeleteAll = false },
                title = {
                    Text(
                        "Удалить всё навсегда?",
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                text = {
                    Text(
                        "Это действие нельзя отменить. Все удалённые колоды, юниты и карточки будут стёрты.",
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        rootDeletedDecks.forEach { viewModel.permanentlyDeleteDeck(it.deck) }
                        deletedBatches.forEach { viewModel.permanentlyDeleteCardBatch(it) }
                        confirmDeleteAll = false
                    }) {
                        Text("Удалить", color = FdRed, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDeleteAll = false }) {
                        Text("Отмена", fontFamily = OutfitFamily)
                    }
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        fontFamily = OutfitFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun DeletedBatchCard(
    batch: DeletedCardBatch,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    var confirmDelete by remember { mutableStateOf(false) }
    val preview = batch.cards.take(4).joinToString(" · ") { it.front }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📚", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Юнит из «${batch.deckName}»",
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${batch.cards.size} слов · удалено ${formatDeletedDate(batch.deletedAt)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (preview.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    if (batch.cards.size > 4) "$preview …" else preview,
                    fontSize = 12.sp,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PressButton(
                    onClick = onRestore,
                    modifier = Modifier.weight(1f).height(40.dp),
                    color = if (isDark) Color(0xFF1A3D1A) else FdGreenLight,
                    shadowColor = FdGreen.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "↩ Восстановить",
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = FdGreen
                    )
                }
                PressButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.weight(1f).height(40.dp),
                    color = if (isDark) Color(0xFF3D1A1A) else FdRedLight,
                    shadowColor = FdRed.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "🗑 Удалить",
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = FdRed
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = {
                Text(
                    "Удалить навсегда?",
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    "${batch.cards.size} слов будут стёрты безвозвратно.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    confirmDelete = false
                }) {
                    Text("Удалить", color = FdRed, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Отмена", fontFamily = OutfitFamily)
                }
            }
        )
    }
}

@Composable
private fun DeletedDeckCard(
    item: DeletedDeckItem,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    var confirmDelete by remember { mutableStateOf(false) }
    val deckColor = runCatching { android.graphics.Color.parseColor(item.deck.colorHex) }
        .getOrElse { android.graphics.Color.parseColor("#4255FF") }
        .let { Color(it) }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(deckColor)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.deck.name,
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${item.cardCount} карточек · удалено ${formatDeletedDate(item.deletedAt)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PressButton(
                    onClick = onRestore,
                    modifier = Modifier.weight(1f).height(40.dp),
                    color = if (isDark) Color(0xFF1A3D1A) else FdGreenLight,
                    shadowColor = FdGreen.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "↩ Восстановить",
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = FdGreen
                    )
                }
                PressButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.weight(1f).height(40.dp),
                    color = if (isDark) Color(0xFF3D1A1A) else FdRedLight,
                    shadowColor = FdRed.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "🗑 Удалить",
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = FdRed
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = {
                Text(
                    "Удалить навсегда?",
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    "«${item.deck.name}» и ${item.cardCount} карточек будут стёрты безвозвратно.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    confirmDelete = false
                }) {
                    Text("Удалить", color = FdRed, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Отмена", fontFamily = OutfitFamily)
                }
            }
        )
    }
}

private fun formatDeletedDate(millis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - millis
    val days = diff / (1000 * 60 * 60 * 24)
    return when {
        days == 0L -> "сегодня"
        days == 1L -> "вчера"
        days < 7 -> "$days дн. назад"
        else -> SimpleDateFormat("d MMM", Locale("ru")).format(Date(millis))
    }
}
