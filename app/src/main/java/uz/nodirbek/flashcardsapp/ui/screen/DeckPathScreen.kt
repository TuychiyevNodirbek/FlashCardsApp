package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.nodirbek.flashcardsapp.data.repository.SubRowUnits
import uz.nodirbek.flashcardsapp.data.repository.UnitRepository
import uz.nodirbek.flashcardsapp.domain.model.Deck
import uz.nodirbek.flashcardsapp.domain.model.StudyUnit
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.theme.*

/**
 * Главный экран курса: путь юнитов, сгруппированных по subRow (темам).
 * Duolingo-стиль: вертикальный зигзаг узлов, последовательная разблокировка внутри темы.
 */
@Composable
fun DeckPathContent(
    courseDeckId: String,
    unitRepository: UnitRepository,
    dueCount: Int,
    onOpenUnit: (deckId: String, unitIndex: Int) -> Unit,
    onStartReview: () -> Unit,
    onAddSubRow: () -> Unit,
    onSubRowLongPress: (Deck) -> Unit,
    onAddWords: () -> Unit,
    onDeleteUnit: (deckId: String, unitIndex: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val path by unitRepository.getPath(courseDeckId).collectAsState(initial = null)
    val sections = path ?: return

    // Юнит, для которого открыто подтверждение удаления: (deckId, unit)
    var unitPendingDelete by remember { mutableStateOf<Pair<String, StudyUnit>?>(null) }

    val totalUnits = sections.sumOf { it.units.size }
    if (totalUnits == 0 && sections.none { it.deck.id != courseDeckId }) {
        // Совсем пустой курс
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📦", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Нет слов",
                    fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Добавьте слова или создайте тему",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                PressButton(
                    onClick = onAddWords,
                    modifier = Modifier.height(46.dp),
                    color = FdPrimary, shadowColor = FdPrimaryDark,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "+ Добавить слова",
                        fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                }
            }
        }
        return
    }

    val showHeaders = sections.size > 1 || sections.any { it.deck.id != courseDeckId }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Узел повторения SRS — закреплён сверху пути
        if (dueCount > 0) {
            item(key = "review") {
                ReviewNode(dueCount = dueCount, onClick = onStartReview)
                Spacer(Modifier.height(8.dp))
            }
        }

        sections.forEach { section ->
            if (showHeaders) {
                item(key = "header_${section.deck.id}") {
                    SubRowHeader(
                        section = section,
                        isImplicit = section.deck.id == courseDeckId,
                        onLongPress = { onSubRowLongPress(section.deck) }
                    )
                }
            }

            if (section.units.isEmpty()) {
                item(key = "empty_${section.deck.id}") {
                    Text(
                        "Пока нет слов в этой теме",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                section.units.forEach { unit ->
                    item(key = "unit_${section.deck.id}_${unit.index}") {
                        UnitNode(
                            unit = unit,
                            zigzagIndex = unit.index,
                            onClick = { onOpenUnit(section.deck.id, unit.index) },
                            onLongClick = { unitPendingDelete = section.deck.id to unit }
                        )
                    }
                }
            }
        }

        // Создание новой темы
        item(key = "add_subrow") {
            Spacer(Modifier.height(12.dp))
            PressButton(
                onClick = onAddSubRow,
                modifier = Modifier.height(44.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowColor = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "+ Новая тема",
                    fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

    unitPendingDelete?.let { (sectionDeckId, unit) ->
        AlertDialog(
            onDismissRequest = { unitPendingDelete = null },
            title = {
                Text(
                    "Удалить юнит ${unit.index + 1}?",
                    fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    "${unit.cards.size} слов будут удалены вместе с юнитом. " +
                        "Их можно вернуть из «Недавно удалённых» в настройках.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteUnit(sectionDeckId, unit.index)
                    unitPendingDelete = null
                }) {
                    Text("Удалить", color = FdRed, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { unitPendingDelete = null }) {
                    Text("Отмена", fontFamily = OutfitFamily)
                }
            }
        )
    }
}

@Composable
private fun ReviewNode(dueCount: Int, onClick: () -> Unit) {
    PressButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(56.dp),
        color = FdOrange, shadowColor = FdOrangeDark,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔄", fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                "Повторение · $dueCount слов",
                fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                color = Color.White
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SubRowHeader(
    section: SubRowUnits,
    isImplicit: Boolean,
    onLongPress: () -> Unit
) {
    val color = remember(section.deck.colorHex) {
        try { Color(android.graphics.Color.parseColor(section.deck.colorHex)) }
        catch (e: Exception) { FdPrimary }
    }
    val done = section.units.count { it.completed }
    val total = section.units.size

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = {}, onLongClick = if (isImplicit) null else onLongPress)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (isImplicit) "Общее" else section.deck.name,
                fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (total > 0) {
                Text(
                    "$done/$total юнитов",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(
            Modifier.width(48.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun UnitNode(
    unit: StudyUnit,
    zigzagIndex: Int,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    // Зигзаг: 0, +36, 0, -36, 0, +36 ...
    val xOffset = when (zigzagIndex % 4) {
        1 -> 36.dp
        3 -> (-36).dp
        else -> 0.dp
    }
    val isCurrent = !unit.locked && !unit.completed

    val scale = if (isCurrent) {
        val transition = rememberInfiniteTransition(label = "pulse")
        transition.animateFloat(
            initialValue = 1f, targetValue = 1.07f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "pulse_scale"
        ).value
    } else 1f

    Column(
        Modifier
            .padding(vertical = 8.dp)
            .offset(x = xOffset),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.scale(scale)) {
            PressButton(
                // Заблокированный юнит нельзя открыть, но удалить (long-press) — можно,
                // поэтому кнопку не выключаем, а гасим только сам onClick.
                onClick = { if (!unit.locked) onClick() },
                modifier = Modifier.size(72.dp),
                color = when {
                    unit.completed -> FdGreen
                    unit.locked -> MaterialTheme.colorScheme.surfaceVariant
                    else -> FdPrimary
                },
                shadowColor = when {
                    unit.completed -> FdGreenDark
                    unit.locked -> MaterialTheme.colorScheme.outline
                    else -> FdPrimaryDark
                },
                shape = CircleShape,
                shadowDp = 6.dp,
                enabled = !unit.locked || onLongClick != null,
                onLongClick = onLongClick
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        when {
                            unit.completed -> "✓"
                            unit.locked -> "🔒"
                            else -> "★"
                        },
                        fontSize = if (unit.locked) 20.sp else 24.sp,
                        fontWeight = FontWeight.Black,
                        color = if (unit.locked) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                    )
                    if (unit.completed) {
                        Text(
                            "${(unit.bestAccuracy * 100).toInt()}%",
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White
                        )
                    }
                }
            }
            // Индикатор шага для начатого юнита
            if (isCurrent && unit.completedSteps > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-4).dp)
                        .clip(CircleShape)
                        .background(FdOrange)
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        "${unit.completedSteps}/6",
                        fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Юнит ${unit.index + 1} · ${unit.cards.size} слов",
            fontSize = 11.sp,
            fontFamily = OutfitFamily,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (unit.locked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
