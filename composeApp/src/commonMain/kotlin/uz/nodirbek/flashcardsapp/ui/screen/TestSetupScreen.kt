package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import uz.nodirbek.flashcardsapp.ui.theme.*
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel
import uz.nodirbek.flashcardsapp.ui.components.UnifiedAppBar

@Composable
fun TestSetupScreen(
    deckId: String,
    viewModel: HomeViewModel,
    onBackClick: () -> Unit,
    onStartTest: (deckId: String, count: Int, isWritten: Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val totalCards = remember(uiState.cards, deckId) { uiState.cards.count { it.deckId == deckId } }

    var selectedCount by remember { mutableStateOf(10.coerceAtMost(totalCards.coerceAtLeast(1))) }
    var isWritten by remember { mutableStateOf(false) }

    val countOptions = buildList {
        addAll(listOf(5, 10, 20, 50).filter { it < totalCards })
        add(totalCards)
    }.distinct().sorted()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            UnifiedAppBar(
                title = "Настройки теста",
                onBackClick = onBackClick,
                showBackButton = true,
                showDivider = true
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Spacer(Modifier.height(8.dp))

                // ── Question count ─────────────────────────────────────
                SectionLabel("Количество вопросов")
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    countOptions.forEach { count ->
                        val selected = selectedCount == count
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) FdPrimary else MaterialTheme.colorScheme.surface)
                                .border(2.dp, if (selected) FdPrimaryDark else MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                .clickable { selectedCount = count }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$count",
                                fontFamily = OutfitFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                // Custom count slider
                Spacer(Modifier.height(16.dp))
                Text(
                    "Или выберите: $selectedCount",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Slider(
                    value = selectedCount.toFloat(),
                    onValueChange = { selectedCount = it.toInt() },
                    valueRange = 1f..totalCards.coerceAtLeast(1).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = FdPrimary,
                        activeTrackColor = FdPrimary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(Modifier.height(28.dp))

                // ── Test type ──────────────────────────────────────────
                SectionLabel("Тип теста")
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TypeOption(
                        label = "Выбор ответа",
                        emoji = "🔤",
                        selected = !isWritten,
                        modifier = Modifier.weight(1f),
                        onClick = { isWritten = false }
                    )
                    TypeOption(
                        label = "Письменный",
                        emoji = "✏️",
                        selected = isWritten,
                        modifier = Modifier.weight(1f),
                        onClick = { isWritten = true }
                    )
                }

                Spacer(Modifier.height(40.dp))

                // ── Summary card ───────────────────────────────────────
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(FdPrimaryLight)
                        .border(1.5.dp, FdPrimary.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📝", fontSize = 22.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "$selectedCount вопросов · ${if (isWritten) "Письменный" else "Выбор ответа"}",
                                fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "из $totalCards карточек в колоде",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                PressButton(
                    onClick = { onStartTest(deckId, selectedCount, isWritten) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    color = FdPrimary, shadowColor = FdPrimaryDark, shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Начать тест", fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun TypeOption(label: String, emoji: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) FdPrimary else MaterialTheme.colorScheme.surface)
            .border(2.dp, if (selected) FdPrimaryDark else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                fontFamily = OutfitFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
