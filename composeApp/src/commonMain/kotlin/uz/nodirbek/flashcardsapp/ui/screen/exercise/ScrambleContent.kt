@file:OptIn(ExperimentalLayoutApi::class)

package uz.nodirbek.flashcardsapp.ui.screen.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import uz.nodirbek.flashcardsapp.shared.model.Card
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.components.ProgressAppBar
import uz.nodirbek.flashcardsapp.ui.theme.*

// Буква/слог в пуле (с уникальным индексом, чтобы различать дубли)
private data class LetterTile(val text: String, val idx: Int)

private data class QueueScrambleSlot(
    val isFixed: Boolean,
    val fixedChar: Char = ' ',
    val expectedText: String = ""
)

// Длинные слова разбиваются не на отдельные буквы, а на пары/тройки букв,
// чтобы ячейки помещались на экране и переносились на 2-3 строки, а не сжимались в одну.
private fun scrambleChunkSize(nonFixedCount: Int) = when {
    nonFixedCount > 16 -> 3
    nonFixedCount > 10 -> 2
    else -> 1
}

private fun buildQueueScrambleSlots(word: String, chunkSize: Int): List<QueueScrambleSlot> {
    val result = mutableListOf<QueueScrambleSlot>()
    var i = 0
    while (i < word.length) {
        val ch = word[i]
        if (ch == ' ' || ch == '-') {
            result.add(QueueScrambleSlot(isFixed = true, fixedChar = ch))
            i++
        } else {
            val seg = buildString {
                while (i < word.length && word[i] != ' ' && word[i] != '-') append(word[i++])
            }
            seg.chunked(chunkSize).forEach { chunk ->
                result.add(QueueScrambleSlot(isFixed = false, expectedText = chunk))
            }
        }
    }
    return result
}

// Строит пул: слово, разбитое на буквы/слоги, + 3 лишних плитки, перемешать
private fun buildPool(word: String, chunkSize: Int): List<LetterTile> {
    val letters = word.filter { it != ' ' && it != '-' }
    val realTiles = letters.chunked(chunkSize).mapIndexed { i, chunk -> LetterTile(chunk, i) }
    val extras = generateExtraTiles(word, chunkSize, 3)
    return (realTiles + extras).shuffled()
}

private fun generateExtraTiles(word: String, chunkSize: Int, count: Int): List<LetterTile> {
    val alphabet = "abcdefghijklmnopqrstuvwxyz"
    val wordLower = word.lowercase()
    val result = mutableListOf<LetterTile>()
    var idx = 10000
    var attempts = 0
    while (result.size < count && attempts++ < 120) {
        val chunk = (1..chunkSize).map { alphabet.random() }.joinToString("")
        if (!wordLower.contains(chunk) && result.none { it.text == chunk }) {
            result.add(LetterTile(chunk, idx++))
        }
    }
    while (result.size < count) {
        result.add(LetterTile(alphabet.random().toString(), idx++))
    }
    return result
}

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

    val word = currentCard.front
    val nonFixedCount = remember(currentCard.id) { word.count { it != ' ' && it != '-' } }
    val chunkSize = remember(currentCard.id) { scrambleChunkSize(nonFixedCount) }
    val slots = remember(currentCard.id) { buildQueueScrambleSlots(word, chunkSize) }
    val pool = remember(currentCard.id) { buildPool(word, chunkSize) }
    val nonFixedSlotCount = remember(currentCard.id) { slots.count { !it.isFixed } }

    // answerSlots[slotIndex] = индекс в pool, или -1 если пусто
    val answerSlots = remember(currentCard.id) { mutableStateListOf(*Array(nonFixedSlotCount) { -1 }) }
    val usedPoolIndices = answerSlots.toSet()
    val canCheck = answerSlots.all { it >= 0 } && !revealed

    fun addTile(poolListIdx: Int) {
        if (poolListIdx in usedPoolIndices) return
        val firstEmpty = answerSlots.indexOfFirst { it < 0 }
        if (firstEmpty >= 0) answerSlots[firstEmpty] = poolListIdx
    }

    fun removeTile(slotIdx: Int) {
        if (revealed) return
        answerSlots[slotIdx] = -1
    }

    fun advance() {
        val next = errorQueue.next()
        if (next == null) {
            onDone(correctCount, errorQueue.totalPrimary)
        } else {
            currentCard = next
            revealed = false
            isCorrect = false
        }
    }

    fun check() {
        if (!canCheck) return
        var slotPos = 0
        val built = buildString {
            for (slot in slots) {
                if (slot.isFixed) append(slot.fixedChar)
                else {
                    val poolIdx = answerSlots[slotPos++]
                    append(if (poolIdx >= 0) pool[poolIdx].text else "?")
                }
            }
        }
        val correct = built.equals(word, ignoreCase = true)
        isCorrect = correct
        revealed = true
        if (errorQueue.isFirstAttempt(currentCard.id)) {
            answeredCount++
            if (correct) correctCount++ else errorQueue.addError(currentCard)
        }
    }

    // Размеры плиток уменьшаются для крупных слогов, чтобы всё смотрелось сбалансированно
    val tileFontSp = if (chunkSize == 1) 16 else 14
    val poolFontSp = if (chunkSize == 1) 18 else 15
    val tileMinWidthDp = if (chunkSize == 1) 32 else chunkSize * 14
    val emptySlotWidthDp = if (chunkSize == 1) 32 else chunkSize * 14

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
                if (chunkSize == 1) "Собери слово из букв" else "Собери слово из слогов",
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
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    var slotIdx = 0
                    slots.forEach { slot ->
                        if (slot.isFixed) {
                            Box(
                                Modifier.height(36.dp).padding(horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    slot.fixedChar.toString(),
                                    fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                )
                            }
                        } else {
                            val currentSlot = slotIdx
                            val poolIdx = answerSlots[slotIdx]
                            slotIdx++
                            if (poolIdx >= 0) {
                                Box(
                                    Modifier
                                        .padding(3.dp)
                                        .height(36.dp)
                                        .widthIn(min = tileMinWidthDp.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(FdPrimaryLight)
                                        .border(1.5.dp, FdPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .then(if (!revealed) Modifier.clickable { removeTile(currentSlot) } else Modifier)
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        pool[poolIdx].text.uppercase(),
                                        fontFamily = OutfitFamily, fontWeight = FontWeight.Bold,
                                        fontSize = tileFontSp.sp, color = FdPrimary
                                    )
                                }
                            } else {
                                Box(
                                    Modifier
                                        .padding(3.dp)
                                        .height(36.dp)
                                        .width(emptySlotWidthDp.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                )
                            }
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

            // Пул букв/слогов
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                pool.forEachIndexed { poolListIdx, tile ->
                    val inUse = poolListIdx in usedPoolIndices
                    Box(
                        Modifier
                            .height(44.dp)
                            .widthIn(min = 44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (inUse) MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                1.5.dp,
                                if (inUse) MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(10.dp)
                            )
                            .then(
                                if (!inUse && !revealed) Modifier.clickable { addTile(poolListIdx) }
                                else Modifier
                            )
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!inUse) {
                            Text(
                                tile.text.uppercase(),
                                fontFamily = OutfitFamily, fontWeight = FontWeight.Bold,
                                fontSize = poolFontSp.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
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
