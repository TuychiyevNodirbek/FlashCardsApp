package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.nodirbek.flashcardsapp.data.repository.UnitRepository
import uz.nodirbek.flashcardsapp.domain.model.StudyUnit
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.theme.*

@Composable
fun UnitListScreen(
    deckId: String,
    unitRepository: UnitRepository,
    onBack: () -> Unit,
    onOpenUnit: (Int) -> Unit
) {
    val units by unitRepository.getUnits(deckId).collectAsState(initial = emptyList())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 0.dp) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            "Юниты",
                            fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                }
            }
        }
    ) { padding ->
        if (units.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📦", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Нет карточек",
                        fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Добавьте карточки в колоду",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(units) { _, unit ->
                    UnitCard(unit = unit, onStart = { onOpenUnit(unit.index) })
                }
            }
        }
    }
}

@Composable
private fun UnitCard(unit: StudyUnit, onStart: () -> Unit) {
    val isDone = unit.completed
    val isLocked = unit.locked
    val bgColor = when {
        isDone -> MaterialTheme.colorScheme.surface
        isLocked -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        isDone -> FdGreen
        isLocked -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        else -> FdPrimary
    }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val emoji = when {
                    isDone -> "✅"
                    isLocked -> "🔒"
                    else -> "▶"
                }
                Text(emoji, fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Юнит ${unit.index + 1}",
                        fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${unit.cards.size} слов",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isDone) {
                    val pct = (unit.bestAccuracy * 100).toInt()
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(FdGreenLight)
                            .border(1.dp, FdGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "$pct%",
                            fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FdGreenDark
                        )
                    }
                }
            }

            if (!isLocked && !isDone) {
                Spacer(Modifier.height(12.dp))
                PressButton(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    color = FdPrimary, shadowColor = FdPrimaryDark, shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Начать", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                }
            }

            if (isDone) {
                Spacer(Modifier.height(12.dp))
                PressButton(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowColor = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "Повторить",
                        fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
