package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.nodirbek.flashcardsapp.domain.model.Card
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.theme.*
import uz.nodirbek.flashcardsapp.ui.components.ProgressAppBar
import uz.nodirbek.flashcardsapp.ui.screen.exercise.ErrorQueue
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel

data class TestResult(val card: Card, val userAnswer: String, val isCorrect: Boolean)

@Composable
fun TestScreen(
    deckId: String,
    viewModel: HomeViewModel,
    count: Int = 10,
    isWritten: Boolean = false,
    onBackClick: () -> Unit,
    onFinished: (results: List<TestResult>) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val allCards = remember(uiState.cards, deckId) { uiState.cards.filter { it.deckId == deckId } }
    val questions = remember(uiState.cards, deckId, count) { allCards.shuffled().take(count) }

    val results = remember { mutableStateListOf<TestResult>() }
    TestContent(
        cards = questions,
        allCardsForDistractors = allCards,
        isWritten = isWritten,
        onBackClick = onBackClick,
        onDone = { _, _ -> onFinished(results.toList()) },
        onAnswerRecorded = { card, answer, correct ->
            results.add(TestResult(card, answer, correct))
        }
    )
}

@Composable
fun TestContent(
    cards: List<uz.nodirbek.flashcardsapp.domain.model.Card>,
    allCardsForDistractors: List<uz.nodirbek.flashcardsapp.domain.model.Card> = cards,
    isWritten: Boolean = false,
    onBackClick: () -> Unit,
    onDone: (correct: Int, total: Int) -> Unit,
    onAnswerRecorded: ((card: uz.nodirbek.flashcardsapp.domain.model.Card, answer: String, correct: Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (cards.isEmpty()) {
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Text("Нет карточек для теста", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
        return
    }

    val errorQueue = remember { ErrorQueue(cards) }
    var currentCard by remember { mutableStateOf(errorQueue.next()!!) }
    var answeredCount by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }

    fun advance(card: Card, answer: String, isCorrect: Boolean) {
        if (errorQueue.isFirstAttempt(card.id)) {
            answeredCount++
            if (isCorrect) correctCount++ else errorQueue.addError(card)
        }
        onAnswerRecorded?.invoke(card, answer, isCorrect)
        val next = errorQueue.next()
        if (next == null) {
            onDone(correctCount, errorQueue.totalPrimary)
        } else {
            currentCard = next
        }
    }

    if (isWritten) {
        WrittenQuestion(
            card = currentCard,
            index = answeredCount,
            total = errorQueue.totalPrimary,
            onAnswer = { answer ->
                val correct = answer.trim().equals(currentCard.back.trim(), ignoreCase = true)
                advance(currentCard, answer, correct)
            },
            onBack = onBackClick
        )
    } else {
        val options = remember(currentCard, allCardsForDistractors) {
            val distractors = allCardsForDistractors.filter { it.id != currentCard.id }.map { it.back }.shuffled().take(3)
            (distractors + currentCard.back).shuffled()
        }
        MultiChoiceQuestion(
            card = currentCard,
            options = options,
            index = answeredCount,
            total = errorQueue.totalPrimary,
            onAnswer = { answer ->
                val correct = answer == currentCard.back
                advance(currentCard, answer, correct)
            },
            onBack = onBackClick
        )
    }
}

@Composable
private fun QuestionHeader(index: Int, total: Int, onBack: () -> Unit) {
    ProgressAppBar(
        title = "",
        progress = if (total > 0) index.toFloat() / total else 0f,
        onBackClick = onBack,
        currentIndex = index,
        total = total
    )
}

@Composable
private fun MultiChoiceQuestion(
    card: Card, options: List<String>, index: Int, total: Int,
    onAnswer: (String) -> Unit, onBack: () -> Unit
) {
    var selected by remember { mutableStateOf<String?>(null) }
    var revealed by remember { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { QuestionHeader(index, total, onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(24.dp))
            Text("Вопрос", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(card.front, fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(20.dp))
            Text("Выберите ответ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            options.forEach { option ->
                val isSelected = selected == option
                val isCorrect = option == card.back
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
                        }
                        .padding(16.dp)
                ) {
                    Text(option, fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(Modifier.weight(1f))
            if (revealed && selected != null) {
                PressButton(
                    onClick = { onAnswer(selected!!) },
                    modifier = Modifier.fillMaxWidth().height(52.dp).padding(bottom = 0.dp),
                    color = FdPrimary, shadowColor = FdPrimaryDark, shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Далее →", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun WrittenQuestion(
    card: Card, index: Int, total: Int,
    onAnswer: (String) -> Unit, onBack: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var revealed by remember { mutableStateOf(false) }
    val isCorrect = input.trim().equals(card.back.trim(), ignoreCase = true)

    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { QuestionHeader(index, total, onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(card.front, fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(24.dp))
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(2.dp, if (input.isNotEmpty()) FdPrimary else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = input,
                    onValueChange = { if (!revealed) input = it },
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold),
                    cursorBrush = SolidColor(FdPrimary),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (input.isEmpty()) Text("Введите ответ...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        inner()
                    }
                )
            }
            if (revealed) {
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isCorrect) FdGreenLight else FdRedLight)
                        .border(2.dp, if (isCorrect) FdGreen else FdRed, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            if (isCorrect) "✓ Правильно!" else "✗ Неверно",
                            fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            color = if (isCorrect) FdGreen else FdRed
                        )
                        if (!isCorrect) {
                            Text("Правильный ответ: ${card.back}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (!revealed) {
                PressButton(
                    onClick = { revealed = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    color = FdPrimary, shadowColor = FdPrimaryDark, shape = RoundedCornerShape(14.dp),
                    enabled = input.isNotBlank()
                ) {
                    Text("Проверить", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }
            } else {
                PressButton(
                    onClick = { onAnswer(input) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    color = FdPrimary, shadowColor = FdPrimaryDark, shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Далее →", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
