package uz.nodirbek.flashcardsapp.ui.screen.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.nodirbek.flashcardsapp.domain.model.Card
import uz.nodirbek.flashcardsapp.tts.TtsManager
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.components.ProgressAppBar
import uz.nodirbek.flashcardsapp.ui.theme.*

@Composable
fun AudioContent(
    cards: List<Card>,
    ttsLang: String = "en",
    ttsSpeed: Float = 1f,
    onBackClick: () -> Unit,
    onDone: (correct: Int, total: Int) -> Unit,
    showTopBar: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (cards.isEmpty()) {
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Text("Нет карточек", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
        return
    }

    val context = LocalContext.current
    val tts = remember { TtsManager(context) }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    val errorQueue = remember { ErrorQueue(cards) }
    var currentCard by remember { mutableStateOf(errorQueue.next()!!) }
    var selected by remember { mutableStateOf<String?>(null) }
    var revealed by remember { mutableStateOf(false) }
    var answeredCount by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }

    // Генерация 4 вариантов ответа для текущей карточки
    val options = remember(currentCard) {
        val distractors = cards.filter { it.id != currentCard.id }.map { it.back }.shuffled().take(3)
        (distractors + currentCard.back).shuffled()
    }

    // Автовоспроизведение при смене карточки
    LaunchedEffect(currentCard.id) {
        tts.speak(currentCard.front, ttsLang, ttsSpeed)
    }

    fun advance() {
        val next = errorQueue.next()
        if (next == null) {
            onDone(correctCount, errorQueue.totalPrimary)
        } else {
            currentCard = next
            selected = null
            revealed = false
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (showTopBar) ProgressAppBar(
                title = "",
                progress = if (errorQueue.totalPrimary > 0) answeredCount.toFloat() / errorQueue.totalPrimary else 0f,
                onBackClick = onBackClick,
                currentIndex = answeredCount,
                total = errorQueue.totalPrimary
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                "Прослушайте и выберите перевод",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(20.dp))

            // Кнопка воспроизведения
            Box(
                Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(FdPrimaryLight)
                    .border(2.dp, FdPrimary.copy(alpha = 0.3f), CircleShape)
                    .clickable { tts.speak(currentCard.front, ttsLang, ttsSpeed) },
                contentAlignment = Alignment.Center
            ) {
                Text("🔊", fontSize = 40.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text("Нажмите для повтора", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(28.dp))
            Text(
                "Выберите перевод",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))

            options.forEach { option ->
                val isSelected = selected == option
                val isCorrect = option == currentCard.back
                val bgColor = when {
                    !revealed -> if (isSelected) FdPrimaryLight else MaterialTheme.colorScheme.surface
                    isCorrect -> FdGreenLight
                    isSelected && !isCorrect -> FdRedLight
                    else -> MaterialTheme.colorScheme.surface
                }
                val borderColor = when {
                    !revealed -> if (isSelected) FdPrimary else MaterialTheme.colorScheme.outline
                    isCorrect -> FdGreen
                    isSelected && !isCorrect -> FdRed
                    else -> MaterialTheme.colorScheme.outline
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                        .clickable(enabled = !revealed) {
                            selected = option
                            revealed = true
                            val isRight = option == currentCard.back
                            if (errorQueue.isFirstAttempt(currentCard.id)) {
                                answeredCount++
                                if (isRight) correctCount++
                                if (!isRight) errorQueue.addError(currentCard)
                            }
                        }
                        .padding(16.dp)
                ) {
                    Text(
                        option,
                        fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            if (revealed) {
                PressButton(
                    onClick = { advance() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    color = FdPrimary, shadowColor = FdPrimaryDark, shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Далее →", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
