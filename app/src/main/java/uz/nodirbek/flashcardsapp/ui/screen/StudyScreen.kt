package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.nodirbek.flashcardsapp.shared.model.Achievement
import uz.nodirbek.flashcardsapp.shared.model.Card
import uz.nodirbek.flashcardsapp.ui.components.HtmlText
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.theme.*
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel
import uz.nodirbek.flashcardsapp.ui.screen.exercise.SingleCardMultiChoice
import uz.nodirbek.flashcardsapp.ui.screen.exercise.SingleCardWrite
import uz.nodirbek.flashcardsapp.ui.screen.exercise.SingleCardScramble
import uz.nodirbek.flashcardsapp.ui.screen.exercise.SingleCardListen

// ── Exercise type per card in SRS session ────────────────────────────────────

enum class ExerciseType { FLIP_CARD, MULTI_CHOICE, WRITE, SCRAMBLE, LISTEN }

private data class StudyItem(val card: Card, val exerciseType: ExerciseType)

private fun determineExerciseType(
    card: Card,
    allCards: List<Card>,
    history: List<ExerciseType>,
    forceExercise: Boolean = false
): ExerciseType {
    if (!forceExercise && card.reps <= 1) return ExerciseType.FLIP_CARD
    val candidates = buildList {
        if (allCards.size >= 4) add(ExerciseType.MULTI_CHOICE)
        add(ExerciseType.WRITE)
        val wordLen = card.front.filter { it != ' ' && it != '-' }.length
        if (wordLen >= 3) add(ExerciseType.SCRAMBLE)
        if (!forceExercise) add(ExerciseType.LISTEN)
    }
    if (candidates.isEmpty()) return if (forceExercise) ExerciseType.WRITE else ExerciseType.FLIP_CARD
    val last3 = history.takeLast(3)
    val filtered = if (last3.size >= 3 && last3.all { it == last3.first() }) {
        candidates.filter { it != last3.first() }.ifEmpty { candidates }
    } else candidates
    return filtered.random()
}

private fun buildStudyQueue(cards: List<Card>): List<StudyItem> {
    val history = mutableListOf<ExerciseType>()
    return cards.map { card ->
        val type = determineExerciseType(card, cards, history)
        history.add(type)
        StudyItem(card, type)
    }
}

private fun splitIntoUnits(total: Int): List<Int> {
    val target = when {
        total <= 20 -> 5
        total <= 50 -> 7
        else -> 10
    }
    val unitCount = maxOf(1, kotlin.math.ceil(total.toDouble() / target).toInt())
    val base = total / unitCount
    val remainder = total % unitCount
    return List(unitCount) { i -> if (i < remainder) base + 1 else base }
}

// ── Screen entry point ────────────────────────────────────────────────────────

private const val SESSION_SIZE = 20  // max cards per session (neuroscience optimum: 15-25)

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

    // Overlay state
    var showDoubleXp by remember { mutableStateOf(false) }
    var currentAchievement by remember { mutableStateOf<Achievement?>(null) }

    LaunchedEffect(Unit) {
        launch {
            viewModel.doubleXpEvent.collect {
                showDoubleXp = true
                delay(1600)
                showDoubleXp = false
            }
        }
        launch {
            viewModel.achievementEvent.collect { achievement ->
                currentAchievement = achievement
                delay(3000)
                currentAchievement = null
            }
        }
    }

    // sessionStart drives session restarts when user taps "Continue"
    var sessionStart by remember { mutableIntStateOf(0) }
    // null = main session, non-null = forgotten words practice phase
    var forgottenForPractice by remember { mutableStateOf<List<Card>?>(null) }

    val allDueCards = remember(uiState.cards, deckId, uiState.dailyNewLimit, uiState.dailyReviewLimit, sessionStart) {
        viewModel.getDueCardsForDeck(deckId)
    }
    val sessionCards = remember(allDueCards, sessionStart) {
        mutableStateListOf(*allDueCards.take(SESSION_SIZE).toTypedArray())
    }
    val remainingAfterSession = (allDueCards.size - SESSION_SIZE).coerceAtLeast(0)

    Box(Modifier.fillMaxSize()) {
        if (forgottenForPractice != null) {
            ForgottenPracticeSession(
                forgottenCards = forgottenForPractice!!,
                allDeckCards = uiState.cards.filter { it.deckId == deckId },
                streak = uiState.streak,
                isDarkTheme = isDarkTheme,
                onRateCard = { cardId, q -> viewModel.rateCard(cardId, q) },
                onDone = onBackClick
            )
        } else if (sessionCards.isEmpty()) {
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
            val studyQueue = remember(sessionCards.toList()) {
                mutableStateListOf(*buildStudyQueue(sessionCards).toTypedArray())
            }
            SrsSessionContent(
                studyQueue = studyQueue,
                streak = uiState.streak,
                remainingDue = remainingAfterSession,
                onRateCard = { cardId, quality -> viewModel.rateCard(cardId, quality) },
                onBack = onBackClick,
                onDone = { count, accuracy, xp ->
                    viewModel.onSessionCompleted(count, accuracy)
                    onSessionDone(count, accuracy, xp)
                },
                onContinue = { sessionStart++ },
                onPracticeForgotten = { forgotten -> forgottenForPractice = forgotten },
                isDarkTheme = isDarkTheme,
                onSpeak = { text -> ttsManager.speak(text, uiState.ttsLang, uiState.ttsSpeed) }
            )
        }

        // ── Double XP overlay ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = showDoubleXp,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 },
            exit = fadeOut(tween(400)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFD700).copy(alpha = 0.95f))
                    .padding(horizontal = 28.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚡", fontSize = 36.sp)
                    Text(
                        "×2 XP!",
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = Color(0xFF7A5500)
                    )
                    Text(
                        "Удача! Двойные очки",
                        fontFamily = OutfitFamily,
                        fontSize = 12.sp,
                        color = Color(0xFF7A5500).copy(alpha = 0.8f)
                    )
                }
            }
        }

        // ── Achievement unlock banner ─────────────────────────────────────
        AnimatedVisibility(
            visible = currentAchievement != null,
            enter = slideInVertically(tween(350)) { it } + fadeIn(tween(350)),
            exit = slideOutVertically(tween(350)) { it } + fadeOut(tween(350)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
        ) {
            currentAchievement?.let { ach ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDarkTheme) Color(0xFF2A2A3D) else Color.White)
                        .border(1.5.dp, FdPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(ach.emoji, fontSize = 32.sp)
                        Column {
                            Text(
                                "Достижение разблокировано!",
                                fontFamily = OutfitFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = FdPrimary
                            )
                            Text(
                                ach.title,
                                fontFamily = OutfitFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                ach.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Session content ───────────────────────────────────────────────────────────

@Composable
private fun SrsSessionContent(
    studyQueue: androidx.compose.runtime.snapshots.SnapshotStateList<StudyItem>,
    streak: Int,
    remainingDue: Int = 0,
    onRateCard: (String, Int) -> Unit,
    onBack: () -> Unit,
    onDone: (Int, Float, Int) -> Unit,
    onContinue: () -> Unit = {},
    onPracticeForgotten: ((List<Card>) -> Unit)? = null,
    isDarkTheme: Boolean = LocalIsDarkTheme.current,
    onSpeak: (String) -> Unit = {}
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var correctCount by remember { mutableIntStateOf(0) }
    var isDoubleTapped by remember { mutableStateOf(false) }
    val forgottenCards = remember { mutableStateListOf<Card>() }
    val allCards = remember(studyQueue.size) { studyQueue.map { it.card } }

    // ── Unit splitting ────────────────────────────────────────────────────────
    val unitSizes = remember(studyQueue.size) { splitIntoUnits(studyQueue.size) }
    val unitStartIndices = remember(unitSizes) {
        var acc = 0; unitSizes.map { size -> acc.also { acc += size } }
    }
    val totalUnits = unitSizes.size
    val currentUnit = remember(currentIndex) {
        unitStartIndices.indexOfLast { it <= currentIndex }.coerceAtLeast(0)
    }
    val unitStart = unitStartIndices.getOrElse(currentUnit) { 0 }
    val unitSize = unitSizes.getOrElse(currentUnit) { studyQueue.size }
    val indexInUnit = currentIndex - unitStart

    var showUnitDone by remember { mutableStateOf(false) }
    var completedUnit by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentUnit) {
        if (currentUnit > 0 && currentIndex < studyQueue.size) {
            completedUnit = currentUnit - 1
            showUnitDone = true
            delay(1800)
            showUnitDone = false
        }
    }

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

    if (currentIndex >= studyQueue.size) {
        val total = studyQueue.size
        val accuracy = if (total > 0) correctCount.toFloat() / total else 0f
        val xp = total * 10
        onDone(total, accuracy, xp)
        ReviewDoneInline(
            reviewed = total,
            accuracy = accuracy,
            xp = xp,
            streak = streak,
            remainingDue = remainingDue,
            forgottenCards = forgottenCards.toList(),
            onBack = onBack,
            onContinue = onContinue,
            onPracticeForgotten = if (forgottenCards.isNotEmpty()) onPracticeForgotten else null,
            isDarkTheme = isDarkTheme
        )
        return
    }

    val studyItem = studyQueue[currentIndex]
    val card = studyItem.card
    val isFlipCard = studyItem.exerciseType == ExerciseType.FLIP_CARD

    // Callback when an exercise (non-flip) reports result
    val onExerciseResult: (Boolean) -> Unit = { isCorrect ->
        onRateCard(card.id, if (isCorrect) 2 else 0)
        if (isCorrect) correctCount++ else forgottenCards.add(card)
        currentIndex++
    }

    // Replace current LISTEN task with a non-audio fallback (Duolingo "can't listen" feature)
    val onCantListen = {
        val fallback = if (allCards.size >= 4) ExerciseType.MULTI_CHOICE else ExerciseType.WRITE
        studyQueue[currentIndex] = studyQueue[currentIndex].copy(exerciseType = fallback)
    }

    Box(Modifier.fillMaxSize()) {
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
                    Row(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        repeat(unitSize) { idx ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        when {
                                            idx < indexInUnit -> FdPrimary
                                            idx == indexInUnit -> FdPrimary.copy(alpha = 0.4f)
                                            else -> MaterialTheme.colorScheme.outline
                                        }
                                    )
                            )
                        }
                    }
                    if (totalUnits > 1) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(FdPrimaryLight)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "${currentUnit + 1}/$totalUnits",
                                fontFamily = OutfitFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = FdPrimary
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }
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

        // ── Content area ─────────────────────────────────────────────────
        if (isFlipCard) {
            // ── Flip card (basic level) ───────────────────────────────────
            val threshold = if (screenWidth > 0f) screenWidth * 0.35f else Float.MAX_VALUE
            val rawRight = if (offsetX.value > 0f) offsetX.value / threshold else 0f
            val rawLeft  = if (offsetX.value < 0f) kotlin.math.abs(offsetX.value) / threshold else 0f
            val rightOpacity = rawRight.coerceIn(0f, 1f)
            val leftOpacity  = rawLeft.coerceIn(0f, 1f)

            // ── Full-screen card area (Quizlet-style background) ──────────
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Background color fills whole area based on swipe direction
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            when {
                                rightOpacity > 0.01f -> FdGreen.copy(alpha = (rightOpacity * 0.25f).coerceIn(0f, 1f))
                                leftOpacity  > 0.01f -> FdRed.copy(alpha  = (leftOpacity  * 0.25f).coerceIn(0f, 1f))
                                else -> Color.Transparent
                            }
                        )
                )

                // Card stack (padded, measures card width for threshold)
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .onSizeChanged { screenWidth = it.width.toFloat() }
                ) {
                    // Peek cards behind (stack effect)
                    for (peek in 2 downTo 1) {
                        val peekIndex = currentIndex + peek
                        if (peekIndex < studyQueue.size) {
                            val peekScale = 1f - peek * 0.05f
                            val peekOffsetY = (peek * 14).dp
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .offset(y = peekOffsetY)
                                    .graphicsLayer { scaleX = peekScale; scaleY = peekScale }
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    studyQueue[peekIndex].card.front,
                                    fontFamily = OutfitFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                                    modifier = Modifier.padding(24.dp)
                                )
                            }
                        }
                    }

                    // Draggable top card (no clip on container — card has its own clip)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                            .graphicsLayer {
                                rotationY = rotation
                                rotationZ = if (rotation <= 90f) offsetX.value * 0.04f else 0f
                                cameraDistance = 12f * density
                            }
                            .pointerInput(isFlipped) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (!isFlipped) {
                                            scope.launch { offsetX.snapTo(offsetX.value + dragAmount.x) }
                                        }
                                    },
                                    onDragEnd = {
                                        if (!isFlipped) {
                                            val capturedCard = card
                                            scope.launch {
                                                when {
                                                    offsetX.value > threshold -> {
                                                        offsetX.animateTo(screenWidth * 1.8f, tween(260))
                                                        correctCount++
                                                        onRateCard(capturedCard.id, 2)
                                                        currentIndex++
                                                        isFlipped = false
                                                        offsetX.snapTo(0f)
                                                    }
                                                    offsetX.value < -threshold -> {
                                                        offsetX.animateTo(-screenWidth * 1.8f, tween(260))
                                                        onRateCard(capturedCard.id, 0)
                                                        forgottenCards.add(capturedCard)
                                                        currentIndex++
                                                        isFlipped = false
                                                        offsetX.snapTo(0f)
                                                    }
                                                    else -> offsetX.animateTo(0f, spring(stiffness = 300f))
                                                }
                                            }
                                        }
                                    }
                                )
                            }
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
                            // Front face
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
                                            "${currentIndex + 1} / ${studyQueue.size}",
                                            fontSize = 11.sp,
                                            fontFamily = OutfitFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkTheme) Color(0xFFD4AF37) else FdPrimary
                                        )
                                    }
                                    Spacer(Modifier.height(20.dp))
                                    HtmlText(
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
                                    if (!isFlipped) {
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            "← свайп или нажмите →",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            // Back face
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
                                        Text("Ответ", fontSize = 11.sp, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                                    }
                                    Spacer(Modifier.height(20.dp))
                                    HtmlText(
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

                // Swipe labels — edge-positioned over everything (Quizlet style)
                if (rightOpacity > 0.04f) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "✓",
                            fontSize = 38.sp,
                            color = FdGreen.copy(alpha = rightOpacity),
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "ПОМНЮ",
                            fontFamily = OutfitFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = FdGreen.copy(alpha = rightOpacity)
                        )
                    }
                }
                if (leftOpacity > 0.04f) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "✕",
                            fontSize = 38.sp,
                            color = FdRed.copy(alpha = leftOpacity),
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "ЗАБЫЛ",
                            fontFamily = OutfitFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = FdRed.copy(alpha = leftOpacity)
                        )
                    }
                }
            }

            // ── Bottom: flip hint or 2 rate buttons ──────────────────────
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PressButton(
                            onClick = {
                                onRateCard(card.id, 0)
                                forgottenCards.add(card)
                                isFlipped = false
                                currentIndex++
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            color = if (isDarkTheme) Color(0xFF3D1A1A) else FdRedLight,
                            shadowColor = if (isDarkTheme) Color(0xFF5A1A1A) else FdRed.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("✕", fontSize = 18.sp, color = FdRed)
                                Text("Не знал", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FdRed)
                            }
                        }
                        PressButton(
                            onClick = {
                                correctCount++
                                onRateCard(card.id, 2)
                                isFlipped = false
                                currentIndex++
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            color = if (isDarkTheme) Color(0xFF1A3D1A) else FdGreenLight,
                            shadowColor = if (isDarkTheme) Color(0xFF1A5A1A) else FdGreen.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("✓", fontSize = 18.sp, color = FdGreen)
                                Text("Знал", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FdGreen)
                            }
                        }
                    }
                }
            }
        } else {
            // ── Exercise (advanced level) ─────────────────────────────────
            key(card.id, studyItem.exerciseType) {
                when (studyItem.exerciseType) {
                    ExerciseType.MULTI_CHOICE -> SingleCardMultiChoice(
                        card = card,
                        allCards = allCards,
                        modifier = Modifier.weight(1f),
                        onResult = onExerciseResult
                    )
                    ExerciseType.WRITE -> SingleCardWrite(
                        card = card,
                        modifier = Modifier.weight(1f),
                        onResult = onExerciseResult
                    )
                    ExerciseType.SCRAMBLE -> SingleCardScramble(
                        card = card,
                        modifier = Modifier.weight(1f),
                        onResult = onExerciseResult
                    )
                    ExerciseType.LISTEN -> SingleCardListen(
                        card = card,
                        allCards = allCards,
                        modifier = Modifier.weight(1f),
                        onSpeak = onSpeak,
                        onResult = onExerciseResult,
                        onCantListen = onCantListen
                    )
                    ExerciseType.FLIP_CARD -> {}
                }
            }
        }
    }

    // ── Unit completion overlay ───────────────────────────────────────────
    AnimatedVisibility(
        visible = showUnitDone,
        enter = fadeIn(tween(250)) + slideInVertically(tween(300)) { it / 3 },
        exit = fadeOut(tween(350)),
        modifier = Modifier.align(Alignment.Center)
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(FdPrimary)
                .padding(horizontal = 40.dp, vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎯", fontSize = 44.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Юнит ${completedUnit + 1} завершён!",
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${unitSizes.getOrElse(completedUnit) { 0 }} карточек",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontFamily = OutfitFamily
                )
            }
        }
    }

    } // end outer Box
}

@Composable
private fun ReviewDoneInline(
    reviewed: Int, accuracy: Float, xp: Int, streak: Int,
    remainingDue: Int = 0,
    forgottenCards: List<Card> = emptyList(),
    onBack: () -> Unit,
    onContinue: () -> Unit = {},
    onPracticeForgotten: ((List<Card>) -> Unit)? = null,
    isDarkTheme: Boolean = LocalIsDarkTheme.current
) {
    androidx.compose.foundation.lazy.LazyColumn(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        contentPadding = PaddingValues(vertical = 28.dp)
    ) {
        item {
            Text(if (remainingDue > 0) "✅" else "🎉", fontSize = 52.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                if (remainingDue > 0) "Раунд завершён!" else "Сессия завершена!",
                fontFamily = OutfitFamily, fontWeight = FontWeight.Black, fontSize = 26.sp, color = Color.White
            )
            if (remainingDue > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Осталось ещё $remainingDue карточек",
                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f), fontFamily = OutfitFamily
                )
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DoneStatBox("$reviewed", "Карточек", Modifier.weight(1f))
                DoneStatBox("${(accuracy * 100).toInt()}%", "Точность", Modifier.weight(1f))
                DoneStatBox("+$xp", "XP", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            DoneStatBox("$streak дн.", "Серия 🔥", Modifier.fillMaxWidth())
        }

        // ── Forgotten words section ───────────────────────────────────────
        if (forgottenCards.isNotEmpty()) {
            item {
                Spacer(Modifier.height(20.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFCC2200).copy(alpha = 0.25f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("😓", fontSize = 22.sp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Забытых слов: ${forgottenCards.size}",
                                    fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp, color = Color.White
                                )
                                Text(
                                    "Отработай их с упражнениями — так запомнишь",
                                    fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        // Show up to 5 forgotten words as chips
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            forgottenCards.take(5).forEach { card ->
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(card.front, fontSize = 11.sp, color = Color.White, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (forgottenCards.size > 5) {
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("+${forgottenCards.size - 5}", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                PressButton(
                    onClick = { onPracticeForgotten?.invoke(forgottenCards) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    color = Color.White,
                    shadowColor = FdPrimaryDark,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        "💪 Отработать ${forgottenCards.size} слов",
                        fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FdPrimary
                    )
                }
            }
        }

        // ── Continue / Done buttons ───────────────────────────────────────
        item {
            Spacer(Modifier.height(16.dp))
            if (remainingDue > 0) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "🧠 Мозгу нужен отдых — лучше вернись через 15 мин",
                        fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f),
                        fontFamily = OutfitFamily, textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(12.dp))
                PressButton(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    color = Color.White,
                    shadowColor = FdPrimaryDark,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        "Продолжить ($remainingDue)",
                        fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FdPrimary
                    )
                }
                Spacer(Modifier.height(10.dp))
                PressButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    shadowColor = Color.Transparent,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Закончить", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }
            } else {
                PressButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    color = Color.White,
                    shadowColor = FdPrimaryDark,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Готово", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FdPrimary)
                }
            }
        }
    }
}

// ── Forgotten Words Practice Session ─────────────────────────────────────────

@Composable
private fun ForgottenPracticeSession(
    forgottenCards: List<Card>,
    allDeckCards: List<Card>,
    streak: Int,
    isDarkTheme: Boolean,
    onRateCard: (String, Int) -> Unit,
    onDone: () -> Unit
) {
    val exerciseHistory = remember { mutableStateListOf<ExerciseType>() }
    val practiceQueue = remember {
        val queue = mutableStateListOf<StudyItem>()
        forgottenCards.forEach { card ->
            val type = determineExerciseType(card, allDeckCards, exerciseHistory, forceExercise = true)
            exerciseHistory.add(type)
            queue.add(StudyItem(card, type))
        }
        queue
    }
    var currentIndex by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }

    if (currentIndex >= practiceQueue.size) {
        // Practice done
        Column(
            Modifier.fillMaxSize().background(FdPrimary).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("💪", fontSize = 52.sp)
            Spacer(Modifier.height(16.dp))
            Text("Отработка завершена!", fontFamily = OutfitFamily, fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(
                "$correctCount из ${practiceQueue.size} верно",
                fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(32.dp))
            PressButton(
                onClick = onDone,
                modifier = Modifier.width(200.dp).height(52.dp),
                color = Color.White, shadowColor = FdPrimaryDark,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Готово", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FdPrimary)
            }
        }
        return
    }

    val studyItem = practiceQueue[currentIndex]
    val card = studyItem.card

    val onResult: (Boolean) -> Unit = { isCorrect ->
        onRateCard(card.id, if (isCorrect) 2 else 0)
        if (isCorrect) {
            correctCount++
            currentIndex++
        } else {
            // Re-queue within practice — must get it right
            val hist = practiceQueue.map { it.exerciseType }
            val newType = determineExerciseType(card, allDeckCards, hist, forceExercise = true)
            practiceQueue.removeAt(currentIndex)
            practiceQueue.add(StudyItem(card, newType))
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 0.dp) {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(54.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        "💪 Отработка ошибок",
                        fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDarkTheme) Color(0xFF3D2A1A) else FdOrangeLight)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("🔥 $streak", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            color = if (isDarkTheme) Color(0xFFFFB347) else FdOrange)
                    }
                    Spacer(Modifier.width(8.dp))
                }
                // Red progress bar
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val originalSize = forgottenCards.size
                    repeat(originalSize) { idx ->
                        Box(
                            Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp))
                                .background(
                                    when {
                                        idx < correctCount -> FdRed
                                        idx == correctCount -> FdRed.copy(alpha = 0.4f)
                                        else -> MaterialTheme.colorScheme.outline
                                    }
                                )
                        )
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.5.dp)
            }
        }

        // Exercise
        key(card.id, studyItem.exerciseType) {
            when (studyItem.exerciseType) {
                ExerciseType.MULTI_CHOICE -> SingleCardMultiChoice(
                    card = card, allCards = allDeckCards, modifier = Modifier.weight(1f), onResult = onResult
                )
                ExerciseType.WRITE -> SingleCardWrite(
                    card = card, modifier = Modifier.weight(1f), onResult = onResult
                )
                ExerciseType.SCRAMBLE -> SingleCardScramble(
                    card = card, modifier = Modifier.weight(1f), onResult = onResult
                )
                ExerciseType.FLIP_CARD -> {}
                ExerciseType.LISTEN -> {}  // never generated in forceExercise mode
            }
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
