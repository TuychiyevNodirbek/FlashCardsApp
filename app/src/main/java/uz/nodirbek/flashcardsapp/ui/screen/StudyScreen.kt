package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.nodirbek.flashcardsapp.domain.model.Card
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.theme.*
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel

@Composable
fun StudyScreen(
    viewModel: HomeViewModel,
    deckId: String = "default",
    onBackClick: () -> Unit,
    onSessionDone: (count: Int, accuracy: Float, xp: Int) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val sessionCards = remember {
        val today = uz.nodirbek.flashcardsapp.domain.usecase.RateCardUseCase.getTodayDate()
        uiState.cards.filter { it.deckId == deckId && it.dueDate <= today }
            .ifEmpty { uiState.cards.filter { it.deckId == deckId } }
    }

    if (sessionCards.isEmpty()) {
        Box(Modifier.fillMaxSize().background(FdBackground), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎉", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text("Нечего повторять!", fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = FdText)
                Spacer(Modifier.height(8.dp))
                Text("Все карточки изучены на сегодня", fontSize = 13.sp, color = FdTextSub)
                Spacer(Modifier.height(24.dp))
                PressButton(
                    onClick = onBackClick,
                    modifier = Modifier.width(180.dp).height(48.dp),
                    color = FdPrimary, shadowColor = FdPrimaryDark,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Назад", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }
            }
        }
    } else {
        SrsSessionContent(
            cards = sessionCards,
            streak = uiState.streak,
            onRateCard = { cardId, quality -> viewModel.rateCard(cardId, quality) },
            onBack = onBackClick,
            onDone = onSessionDone
        )
    }
}

@Composable
private fun SrsSessionContent(
    cards: List<Card>,
    streak: Int,
    onRateCard: (String, Int) -> Unit,
    onBack: () -> Unit,
    onDone: (Int, Float, Int) -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var correctCount by remember { mutableStateOf(0) }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "cardFlip"
    )

    if (currentIndex >= cards.size) {
        val accuracy = if (cards.isNotEmpty()) correctCount.toFloat() / cards.size else 0f
        val xp = cards.size * 10
        onDone(cards.size, accuracy, xp)
        ReviewDoneInline(
            reviewed = cards.size,
            accuracy = accuracy,
            xp = xp,
            streak = streak,
            onBack = onBack
        )
        return
    }

    val card = cards[currentIndex]

    Column(
        Modifier
            .fillMaxSize()
            .background(FdBackground)
    ) {
        // ── Top bar ──────────────────────────────────────────────────────
        Surface(color = FdSurface, shadowElevation = 0.dp) {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .height(54.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, null, tint = FdText)
                    }
                    // Segmented progress
                    Row(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        repeat(cards.size) { idx ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        when {
                                            idx < currentIndex -> FdPrimary
                                            idx == currentIndex -> FdPrimary.copy(alpha = 0.4f)
                                            else -> FdBorder
                                        }
                                    )
                            )
                        }
                    }
                    // Streak badge
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(FdOrangeLight)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "🔥 $streak",
                            fontFamily = OutfitFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = FdOrange
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Divider(color = FdBorder, thickness = 1.5.dp)
            }
        }

        // ── Card area ────────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(20.dp)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density
                    }
                    .clickable { isFlipped = !isFlipped }
            ) {
                if (rotation <= 90f) {
                    // Front
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                            .background(FdSurface)
                            .border(2.dp, FdBorder, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(FdPrimaryLight)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "${currentIndex + 1} / ${cards.size}",
                                    fontSize = 11.sp,
                                    fontFamily = OutfitFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = FdPrimary
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                card.front,
                                fontFamily = OutfitFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 30.sp,
                                textAlign = TextAlign.Center,
                                color = FdText
                            )
                            Spacer(Modifier.height(20.dp))
                            Text(
                                "Нажмите для переворота",
                                fontSize = 11.sp,
                                color = FdTextSub
                            )
                        }
                    }
                } else {
                    // Back
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                            .background(FdText)
                            .graphicsLayer { rotationY = 180f },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "Ответ",
                                    fontSize = 11.sp,
                                    fontFamily = OutfitFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                card.back,
                                fontFamily = OutfitFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 28.sp,
                                textAlign = TextAlign.Center,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // ── Bottom action area ───────────────────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp)
        ) {
            if (!isFlipped) {
                PressButton(
                    onClick = { isFlipped = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    color = FdSurface,
                    shadowColor = FdBorder,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Показать ответ", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FdText)
                }
            } else {
                // Rating row 1
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RatingBtn(
                        label = "Забыл",
                        emoji = "✕",
                        color = FdSurface,
                        shadowColor = FdBorder,
                        textColor = FdText,
                        modifier = Modifier.weight(1f)
                    ) {
                        onRateCard(card.id, 0)
                        isFlipped = false
                        currentIndex++
                    }
                    RatingBtn(
                        label = "Трудно",
                        emoji = "😓",
                        color = FdOrangeLight,
                        shadowColor = FdOrangeDark.copy(alpha = 0.5f),
                        textColor = FdOrange,
                        modifier = Modifier.weight(1f)
                    ) {
                        onRateCard(card.id, 1)
                        isFlipped = false
                        currentIndex++
                    }
                }
                Spacer(Modifier.height(10.dp))
                // Rating row 2
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RatingBtn(
                        label = "Хорошо",
                        emoji = "✓",
                        color = FdPrimary,
                        shadowColor = FdPrimaryDark,
                        textColor = Color.White,
                        modifier = Modifier.weight(1f)
                    ) {
                        correctCount++
                        onRateCard(card.id, 2)
                        isFlipped = false
                        currentIndex++
                    }
                    RatingBtn(
                        label = "Легко",
                        emoji = "⚡",
                        color = FdGreen,
                        shadowColor = FdGreenDark,
                        textColor = Color.White,
                        modifier = Modifier.weight(1f)
                    ) {
                        correctCount++
                        onRateCard(card.id, 3)
                        isFlipped = false
                        currentIndex++
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingBtn(
    label: String, emoji: String,
    color: Color, shadowColor: Color, textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    PressButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        color = color,
        shadowColor = shadowColor,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(emoji, fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(label, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
        }
    }
}

@Composable
private fun ReviewDoneInline(
    reviewed: Int, accuracy: Float, xp: Int, streak: Int, onBack: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(FdPrimary)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎉", fontSize = 52.sp)
        Spacer(Modifier.height(16.dp))
        Text("Сессия завершена!", fontFamily = OutfitFamily, fontWeight = FontWeight.Black, fontSize = 26.sp, color = Color.White)
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DoneStatBox("$reviewed", "Карточек", Modifier.weight(1f))
            DoneStatBox("${(accuracy * 100).toInt()}%", "Точность", Modifier.weight(1f))
            DoneStatBox("+$xp", "XP", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        DoneStatBox("$streak дн.", "Серия 🔥", Modifier.fillMaxWidth())
        Spacer(Modifier.height(36.dp))
        PressButton(
            onClick = onBack,
            modifier = Modifier.width(200.dp).height(52.dp),
            color = Color.White,
            shadowColor = FdPrimaryDark,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Готово", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FdPrimary)
        }
    }
}

@Composable
private fun DoneStatBox(value: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color.White)
            Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
        }
    }
}
