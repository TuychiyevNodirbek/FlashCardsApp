package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import uz.nodirbek.flashcardsapp.ui.theme.LocalIsDarkTheme
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
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
    val isDarkTheme = LocalIsDarkTheme.current
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val ttsManager = remember { uz.nodirbek.flashcardsapp.tts.TtsManager(context) }
    DisposableEffect(Unit) { onDispose { ttsManager.shutdown() } }
    val sessionCards = remember(uiState.cards, deckId, uiState.dailyNewLimit, uiState.dailyReviewLimit) {
        val cards = viewModel.getDueCardsForDeck(deckId)
            .ifEmpty { uiState.cards.filter { it.deckId == deckId } }
        mutableStateListOf(*cards.toTypedArray())
    }

    if (sessionCards.isEmpty()) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎉", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text("Нечего повторять!", fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Text("Все карточки изучены на сегодня", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            onDone = onSessionDone,
            isDarkTheme = isDarkTheme,
            onSpeak = { text -> ttsManager.speak(text, uiState.ttsLang, uiState.ttsSpeed) }
        )
    }
}

@Composable
private fun SrsSessionContent(
    cards: MutableList<Card>,
    streak: Int,
    onRateCard: (String, Int) -> Unit,
    onBack: () -> Unit,
    onDone: (Int, Float, Int) -> Unit,
    isDarkTheme: Boolean = LocalIsDarkTheme.current,
    onSpeak: (String) -> Unit = {}
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var correctCount by remember { mutableIntStateOf(0) }
    var isDoubleTapped by remember { mutableStateOf(false) }

    LaunchedEffect(isDoubleTapped) {
        if (isDoubleTapped) {
            kotlinx.coroutines.delay(300)
            isDoubleTapped = false
        }
    }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "cardFlip"
    )

    val offsetX = remember { Animatable(0f) }
    var screenWidth by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    if (currentIndex >= cards.size) {
        val accuracy = if (cards.isNotEmpty()) correctCount.toFloat() / cards.size else 0f
        val xp = cards.size * 10
        onDone(cards.size, accuracy, xp)
        ReviewDoneInline(
            reviewed = cards.size,
            accuracy = accuracy,
            xp = xp,
            streak = streak,
            onBack = onBack,
            isDarkTheme = isDarkTheme
        )
        return
    }

    val card = cards[currentIndex]

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Top bar ──────────────────────────────────────────────────────
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 0.dp) {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .height(54.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface)
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
                                            else -> MaterialTheme.colorScheme.outline
                                        }
                                    )
                            )
                        }
                    }
                    // Streak badge
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDarkTheme) Color(0xFF3D2A1A) else FdOrangeLight)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "🔥 $streak",
                            fontFamily = OutfitFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isDarkTheme) Color(0xFFFFB347) else FdOrange
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.5.dp)
            }
        }

        // ── Card area ────────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(20.dp)
                .onSizeChanged { screenWidth = it.width.toFloat() }
        ) {
            // Swipe indicators
            val threshold = screenWidth * 0.4f
            val rightOpacity = if (offsetX.value > 0) (offsetX.value / threshold).coerceIn(0f, 1f) else 0f
            val leftOpacity = if (offsetX.value < 0) (kotlin.math.abs(offsetX.value) / threshold).coerceIn(0f, 1f) else 0f

            if (rightOpacity > 0) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(FdGreen.copy(alpha = rightOpacity * 0.3f)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column(
                        Modifier.padding(start = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "ПОМНЮ",
                            fontFamily = OutfitFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = FdGreen.copy(alpha = rightOpacity)
                        )
                        Text("✓", fontSize = 32.sp, color = FdGreen.copy(alpha = rightOpacity))
                    }
                }
            }
            if (leftOpacity > 0) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(FdRed.copy(alpha = leftOpacity * 0.3f)),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Column(
                        Modifier.padding(end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "ЗАБЫЛ",
                            fontFamily = OutfitFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = FdRed.copy(alpha = leftOpacity)
                        )
                        Text("✕", fontSize = 32.sp, color = FdRed.copy(alpha = leftOpacity))
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density
                    }
                    .pointerInput(isFlipped) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (!isFlipped) {
                                    scope.launch {
                                        offsetX.snapTo(offsetX.value + dragAmount.x)
                                    }
                                }
                            },
                            onDragEnd = {
                                if (!isFlipped) {
                                    scope.launch {
                                        when {
                                            offsetX.value > threshold -> {
                                                onRateCard(card.id, 3)
                                                offsetX.snapTo(0f)
                                                currentIndex++
                                            }
                                            offsetX.value < -threshold -> {
                                                onRateCard(card.id, 0)
                                                if (cards.size > 1) {
                                                    cards.removeAt(currentIndex)
                                                    cards.add(card)
                                                } else {
                                                    currentIndex++
                                                }
                                                offsetX.snapTo(0f)
                                            }
                                            else -> {
                                                offsetX.animateTo(0f)
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .clickable(enabled = !isFlipped) {
                        if (isDoubleTapped) {
                            isFlipped = !isFlipped
                            isDoubleTapped = false
                        } else {
                            isDoubleTapped = true
                        }
                    }
            ) {
                if (rotation <= 90f) {
                    // Front
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDarkTheme) Color(0xFF3D3A1A) else FdPrimaryLight)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "${currentIndex + 1} / ${cards.size}",
                                    fontSize = 11.sp,
                                    fontFamily = OutfitFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color(0xFFD4AF37) else FdPrimary
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                card.front,
                                fontFamily = OutfitFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 30.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(12.dp))
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(FdPrimaryLight)
                                    .clickable { onSpeak(card.front) }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("🔊", fontSize = 16.sp)
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Нажмите для переворота",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Back
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDarkTheme) Color(0xFF0F0F14) else MaterialTheme.colorScheme.onSurface)
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
                    color = MaterialTheme.colorScheme.surface,
                    shadowColor = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Показать ответ", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            } else {
                // Rating row 1
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RatingBtn(
                        label = "Забыл",
                        emoji = "✕",
                        color = MaterialTheme.colorScheme.surface,
                        shadowColor = MaterialTheme.colorScheme.outline,
                        textColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    ) {
                        onRateCard(card.id, 0)
                        isFlipped = false
                        cards.removeAt(currentIndex)
                        cards.add(card)  // Re-queue at end
                    }
                    RatingBtn(
                        label = "Трудно",
                        emoji = "😓",
                        color = if (isDarkTheme) Color(0xFF3D2A1A) else FdOrangeLight,
                        shadowColor = if (isDarkTheme) Color(0xFF663300) else FdOrangeDark.copy(alpha = 0.5f),
                        textColor = if (isDarkTheme) Color(0xFFFFB347) else FdOrange,
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
                        color = MaterialTheme.colorScheme.primary,
                        shadowColor = if (isDarkTheme) Color(0xFF2F3D7A) else FdPrimaryDark,
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
                        color = if (isDarkTheme) Color(0xFF2D5A2D) else FdGreen,
                        shadowColor = if (isDarkTheme) Color(0xFF1A3A1A) else FdGreenDark,
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
    reviewed: Int, accuracy: Float, xp: Int, streak: Int, onBack: () -> Unit,
    isDarkTheme: Boolean = LocalIsDarkTheme.current
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
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
