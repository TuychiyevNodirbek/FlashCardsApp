package uz.nodirbek.flashcardsapp.ui.screen.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.nodirbek.flashcardsapp.shared.model.Card
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.components.ProgressAppBar
import uz.nodirbek.flashcardsapp.ui.theme.*

@Composable
fun WriteContent(
    cards: List<Card>,
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

    val errorQueue = remember { ErrorQueue(cards) }
    var currentCard by remember { mutableStateOf(errorQueue.next()!!) }
    var input by remember { mutableStateOf("") }
    var revealed by remember { mutableStateOf(false) }
    var answeredCount by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }

    val checkResult: AnswerResult? = if (revealed) {
        when {
            isAnswerCorrect(input, currentCard.front) -> AnswerResult.CORRECT
            isAnswerAlmost(input, currentCard.front) -> AnswerResult.ALMOST
            else -> AnswerResult.WRONG
        }
    } else null

    LaunchedEffect(currentCard.id) {
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    fun check() {
        if (input.isBlank() || revealed) return
        revealed = true
        val result = when {
            isAnswerCorrect(input, currentCard.front) -> AnswerResult.CORRECT
            isAnswerAlmost(input, currentCard.front) -> AnswerResult.ALMOST
            else -> AnswerResult.WRONG
        }
        if (errorQueue.isFirstAttempt(currentCard.id)) {
            answeredCount++
            if (result != AnswerResult.WRONG) {
                correctCount++
            } else {
                errorQueue.addError(currentCard)
            }
        }
    }

    fun advance() {
        val next = errorQueue.next()
        if (next == null) {
            onDone(correctCount, errorQueue.totalPrimary)
        } else {
            currentCard = next
            input = ""
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
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                "Напечатайте слово",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))

            // Перевод — подсказка
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    currentCard.back,
                    fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(20.dp))

            // Поле ввода
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        2.dp,
                        when (checkResult) {
                            AnswerResult.CORRECT, AnswerResult.ALMOST -> FdGreen
                            AnswerResult.WRONG -> FdRed
                            null -> if (input.isNotEmpty()) FdPrimary else MaterialTheme.colorScheme.outline
                        },
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = input,
                    onValueChange = { if (!revealed) input = it },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    cursorBrush = SolidColor(FdPrimary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { check() }),
                    decorationBox = { inner ->
                        if (input.isEmpty()) {
                            Text("Введите слово...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                        inner()
                    }
                )
            }

            // Фидбек
            if (checkResult != null) {
                Spacer(Modifier.height(12.dp))
                val (bg, border, label) = when (checkResult) {
                    AnswerResult.CORRECT -> Triple(FdGreenLight, FdGreen, "✓ Правильно!")
                    AnswerResult.ALMOST -> Triple(FdGreenLight, FdGreen, "✓ Почти верно! Проверь написание: ${currentCard.front}")
                    AnswerResult.WRONG -> Triple(FdRedLight, FdRed, "✗ Неверно. Правильно: ${currentCard.front}")
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .border(2.dp, border, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        label,
                        fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        color = border
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            if (!revealed) {
                PressButton(
                    onClick = { check() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    color = FdPrimary, shadowColor = FdPrimaryDark, shape = RoundedCornerShape(14.dp),
                    enabled = input.isNotBlank()
                ) {
                    Text("Проверить", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }
            } else {
                PressButton(
                    onClick = { advance() },
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

private enum class AnswerResult { CORRECT, ALMOST, WRONG }

fun isAnswerCorrect(input: String, expected: String): Boolean {
    val a = input.trim().lowercase()
    val b = expected.trim().lowercase()
    return a == b
}

fun isAnswerAlmost(input: String, expected: String): Boolean {
    val a = input.trim().lowercase()
    val b = expected.trim().lowercase()
    if (a == b) return false
    // Прощаем 1 опечатку только в словах от 4 букв
    return b.length >= 4 && levenshtein(a, b) <= 1
}

fun levenshtein(a: String, b: String): Int {
    val dp = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) dp[i][0] = i
    for (j in 0..b.length) dp[0][j] = j
    for (i in 1..a.length) for (j in 1..b.length) {
        val cost = if (a[i - 1] == b[j - 1]) 0 else 1
        dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
    }
    return dp[a.length][b.length]
}
