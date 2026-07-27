package uz.nodirbek.flashcardsapp.ui.screen.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.nodirbek.flashcardsapp.domain.model.Card
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.components.ProgressAppBar
import uz.nodirbek.flashcardsapp.ui.theme.*

// Буква в пуле (с уникальным индексом, чтобы различать дубли)
private data class LetterTile(val ch: Char, val idx: Int, val isFixed: Boolean = false)

@Composable
fun ScrambleContent(
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
    var revealed by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var answeredCount by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }

    // Пул букв и строка ответа для текущей карточки
    val word = currentCard.front
    val pool = remember(currentCard.id) { buildPool(word) }
    // usedIndices: индексы из pool, выбранные в строку ответа по порядку
    val answerIndices = remember(currentCard.id) { mutableStateListOf<Int>() }

    // Строка ответа с учётом фиксированных символов
    val answerDisplay = buildAnswerDisplay(word, pool, answerIndices)
    val nonFixedLength = word.count { it != ' ' && it != '-' }
    val canCheck = answerIndices.size == nonFixedLength && !revealed

    fun advance() {
        val next = errorQueue.next()
        if (next == null) {
            onDone(correctCount, errorQueue.totalPrimary)
        } else {
            currentCard = next
            revealed = false
            isCorrect = false
            answerIndices.clear()
        }
    }

    fun check() {
        if (!canCheck) return
        val built = buildRawAnswer(word, pool, answerIndices)
        val correct = built.equals(word, ignoreCase = true)
        isCorrect = correct
        revealed = true
        if (errorQueue.isFirstAttempt(currentCard.id)) {
            answeredCount++
            if (correct) correctCount++ else errorQueue.addError(currentCard)
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
                "Собери слово из букв",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            // Перевод — подсказка
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    currentCard.back,
                    fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))

            // Строка ответа
            val answerBorder = when {
                !revealed -> FdPrimary
                isCorrect -> FdGreen
                else -> FdRed
            }
            val answerBg = when {
                !revealed -> MaterialTheme.colorScheme.surface
                isCorrect -> FdGreenLight
                else -> FdRedLight
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(answerBg)
                    .border(2.dp, answerBorder, RoundedCornerShape(12.dp))
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    answerDisplay.forEach { tile ->
                        if (tile.isFixed) {
                            // Пробел или дефис — показываем как есть
                            Text(
                                tile.ch.toString(),
                                fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        } else if (tile.idx >= 0) {
                            // Выбранная буква — тап возвращает в пул
                            Box(
                                Modifier
                                    .padding(3.dp)
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(FdPrimaryLight)
                                    .border(1.5.dp, FdPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .then(if (!revealed) Modifier.clickable { answerIndices.remove(tile.idx) } else Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    tile.ch.uppercase(),
                                    fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                                    color = FdPrimary
                                )
                            }
                        } else {
                            // Пустое место
                            Box(
                                Modifier
                                    .padding(3.dp)
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
            }

            // Фидбек при ошибке
            if (revealed && !isCorrect) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Правильно: ${currentCard.front}",
                    fontSize = 13.sp, color = FdRed,
                    fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(24.dp))

            // Пул букв (только невыбранные)
            val availablePool = pool.filterIndexed { idx, _ -> idx !in answerIndices }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 44.dp),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                userScrollEnabled = false
            ) {
                itemsIndexed(pool) { poolIdx, tile ->
                    val available = poolIdx !in answerIndices
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (available) MaterialTheme.colorScheme.surface
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                            )
                            .border(
                                1.5.dp,
                                if (available) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                RoundedCornerShape(10.dp)
                            )
                            .then(
                                if (available && !revealed)
                                    Modifier.clickable { answerIndices.add(poolIdx) }
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (available) tile.ch.uppercase() else "",
                            fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            if (!revealed) {
                PressButton(
                    onClick = { check() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    color = FdPrimary, shadowColor = FdPrimaryDark, shape = RoundedCornerShape(14.dp),
                    enabled = canCheck
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

// Строит пул: буквы слова (без пробелов/дефисов) + 3 лишние буквы, перемешать
private fun buildPool(word: String): List<LetterTile> {
    val realLetters = word.filter { it != ' ' && it != '-' }.mapIndexed { i, ch -> LetterTile(ch, i) }
    val extras = generateExtraLetters(word, 3)
    return (realLetters + extras).shuffled()
}

private fun generateExtraLetters(word: String, count: Int): List<LetterTile> {
    val alphabet = "abcdefghijklmnopqrstuvwxyz"
    val wordLower = word.lowercase()
    val result = mutableListOf<LetterTile>()
    var idx = 10000
    val shuffled = alphabet.toList().shuffled()
    for (ch in shuffled) {
        if (result.size >= count) break
        if (!wordLower.contains(ch)) {
            result.add(LetterTile(ch, idx++))
        }
    }
    // Если алфавит исчерпан (слово содержит все буквы) — добавим повторы
    while (result.size < count) {
        result.add(LetterTile(alphabet.random(), idx++))
    }
    return result
}

// Отображаемые плитки строки ответа (включая фиксированные пробелы/дефисы)
private data class DisplayTile(val ch: Char, val idx: Int, val isFixed: Boolean)

private fun buildAnswerDisplay(word: String, pool: List<LetterTile>, answerIndices: List<Int>): List<DisplayTile> {
    val result = mutableListOf<DisplayTile>()
    var answerPos = 0
    for (ch in word) {
        if (ch == ' ' || ch == '-') {
            result.add(DisplayTile(ch, -1, isFixed = true))
        } else {
            val poolIdx = answerIndices.getOrNull(answerPos)
            if (poolIdx != null) {
                result.add(DisplayTile(pool[poolIdx].ch, poolIdx, isFixed = false))
            } else {
                result.add(DisplayTile('_', -1, isFixed = false))
            }
            answerPos++
        }
    }
    return result
}

private fun buildRawAnswer(word: String, pool: List<LetterTile>, answerIndices: List<Int>): String {
    val sb = StringBuilder()
    var answerPos = 0
    for (ch in word) {
        if (ch == ' ' || ch == '-') {
            sb.append(ch)
        } else {
            val poolIdx = answerIndices.getOrNull(answerPos)
            if (poolIdx != null) sb.append(pool[poolIdx].ch) else sb.append('?')
            answerPos++
        }
    }
    return sb.toString()
}
