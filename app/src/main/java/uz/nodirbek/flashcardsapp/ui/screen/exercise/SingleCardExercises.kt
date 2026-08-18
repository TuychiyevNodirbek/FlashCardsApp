@file:OptIn(ExperimentalLayoutApi::class)

package uz.nodirbek.flashcardsapp.ui.screen.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import uz.nodirbek.flashcardsapp.ui.theme.*

// ── Single-card Listen ───────────────────────────────────────────────────────

@Composable
fun SingleCardListen(
    card: Card,
    allCards: List<Card>,
    modifier: Modifier = Modifier,
    onSpeak: (String) -> Unit,
    onResult: (Boolean) -> Unit,
    onCantListen: () -> Unit
) {
    val options = remember(card.id) {
        val distractors = allCards
            .filter { it.id != card.id }
            .map { it.back }
            .shuffled()
            .take(3)
        (distractors + card.back).shuffled()
    }
    var selected by remember { mutableStateOf<String?>(null) }
    var revealed by remember { mutableStateOf(false) }

    // Auto-play on first appear
    LaunchedEffect(card.id) { onSpeak(card.front) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            "🎧 Прослушайте и выберите перевод",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(20.dp))

        // Big replay button
        Box(
            Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(FdPrimaryLight)
                .border(2.dp, FdPrimary.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .clickable { onSpeak(card.front) },
            contentAlignment = Alignment.Center
        ) {
            Text("🔊", fontSize = 44.sp)
        }

        Spacer(Modifier.height(24.dp))

        options.forEach { option ->
            val isSelected = selected == option
            val isCorrect = option == card.back
            val bgColor = when {
                !revealed -> if (isSelected) FdPrimaryLight else MaterialTheme.colorScheme.surface
                isCorrect -> FdGreenLight
                isSelected -> FdRedLight
                else -> MaterialTheme.colorScheme.surface
            }
            val borderColor = when {
                !revealed -> if (isSelected) FdPrimary else MaterialTheme.colorScheme.outline
                isCorrect -> FdGreen
                isSelected -> FdRed
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
                Text(
                    option,
                    fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.weight(1f))

        if (revealed) {
            PressButton(
                onClick = { onResult(selected == card.back) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                color = FdPrimary, shadowColor = FdPrimaryDark, shape = RoundedCornerShape(14.dp)
            ) {
                Text("Далее →", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            }
        } else {
            Text(
                "Не могу слушать сейчас",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = OutfitFamily,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable { onCantListen() }
                    .padding(vertical = 8.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ── Single-card Multi-Choice ─────────────────────────────────────────────────

@Composable
fun SingleCardMultiChoice(
    card: Card,
    allCards: List<Card>,
    modifier: Modifier = Modifier,
    onResult: (Boolean) -> Unit
) {
    val options = remember(card.id) {
        val distractors = allCards
            .filter { it.id != card.id }
            .map { it.back }
            .shuffled()
            .take(3)
        (distractors + card.back).shuffled()
    }
    var selected by remember { mutableStateOf<String?>(null) }
    var revealed by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            "Выберите перевод",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(14.dp))

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
                card.front,
                fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(20.dp))

        options.forEach { option ->
            val isSelected = selected == option
            val isCorrect = option == card.back
            val bgColor = when {
                !revealed -> if (isSelected) FdPrimaryLight else MaterialTheme.colorScheme.surface
                isCorrect -> FdGreenLight
                isSelected -> FdRedLight
                else -> MaterialTheme.colorScheme.surface
            }
            val borderColor = when {
                !revealed -> if (isSelected) FdPrimary else MaterialTheme.colorScheme.outline
                isCorrect -> FdGreen
                isSelected -> FdRed
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
                Text(
                    option,
                    fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.weight(1f))

        if (revealed) {
            PressButton(
                onClick = { onResult(selected == card.back) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                color = FdPrimary, shadowColor = FdPrimaryDark, shape = RoundedCornerShape(14.dp)
            ) {
                Text("Далее →", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ── Single-card Write ────────────────────────────────────────────────────────

@Composable
fun SingleCardWrite(
    card: Card,
    modifier: Modifier = Modifier,
    onResult: (Boolean) -> Unit
) {
    var input by remember { mutableStateOf("") }
    var revealed by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(card.id) {
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    val checkResult: Boolean? = if (revealed) {
        isAnswerCorrect(input, card.front) || isAnswerAlmost(input, card.front)
    } else null

    fun check() {
        if (input.isBlank() || revealed) return
        revealed = true
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            "Напечатайте слово",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))

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
                card.back,
                fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    2.dp,
                    when (checkResult) {
                        true -> FdGreen
                        false -> FdRed
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
                keyboardActions = KeyboardActions(onDone = {
                    if (!revealed) check() else onResult(checkResult == true)
                }),
                decorationBox = { inner ->
                    if (input.isEmpty()) Text("Введите слово...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    inner()
                }
            )
        }

        if (revealed) {
            Spacer(Modifier.height(12.dp))
            val isCorrect = checkResult == true
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isCorrect) FdGreenLight else FdRedLight)
                    .border(2.dp, if (isCorrect) FdGreen else FdRed, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    if (isCorrect) "✓ Правильно!" else "✗ Правильно: ${card.front}",
                    fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    color = if (isCorrect) FdGreen else FdRed
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
                onClick = { onResult(checkResult == true) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                color = FdPrimary, shadowColor = FdPrimaryDark, shape = RoundedCornerShape(14.dp)
            ) {
                Text("Далее →", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ── Single-card Scramble ─────────────────────────────────────────────────────

private data class ScrambleSlot(
    val isFixed: Boolean,
    val fixedChar: Char = ' ',
    val expectedText: String = ""
)

private data class ScramblePoolItem(val text: String)

private fun scrambleChunkSize(nonFixedCount: Int) = when {
    nonFixedCount > 16 -> 3
    nonFixedCount > 10 -> 2
    else -> 1
}

private fun buildScrambleSlots(word: String, chunkSize: Int): List<ScrambleSlot> {
    val result = mutableListOf<ScrambleSlot>()
    var i = 0
    while (i < word.length) {
        val ch = word[i]
        if (ch == ' ' || ch == '-') {
            result.add(ScrambleSlot(isFixed = true, fixedChar = ch))
            i++
        } else {
            val seg = buildString {
                while (i < word.length && word[i] != ' ' && word[i] != '-') append(word[i++])
            }
            seg.chunked(chunkSize).forEach { chunk ->
                result.add(ScrambleSlot(isFixed = false, expectedText = chunk))
            }
        }
    }
    return result
}

private fun buildScramblePool(word: String, chunkSize: Int): List<ScramblePoolItem> {
    val letters = word.filter { it != ' ' && it != '-' }
    val realItems = letters.chunked(chunkSize).map { ScramblePoolItem(it) }.toMutableList()
    val wordLettersLower = letters.lowercase()
    val alphabet = "abcdefghijklmnopqrstuvwxyz"
    val distractors = mutableListOf<ScramblePoolItem>()
    var attempts = 0
    while (distractors.size < 3 && attempts++ < 120) {
        val chunk = (1..chunkSize).map { alphabet.random() }.joinToString("")
        if (!wordLettersLower.contains(chunk) && distractors.none { it.text == chunk }) {
            distractors.add(ScramblePoolItem(chunk))
        }
    }
    while (distractors.size < 3) distractors.add(ScramblePoolItem("x"))
    return (realItems + distractors).shuffled()
}

@Composable
fun SingleCardScramble(
    card: Card,
    modifier: Modifier = Modifier,
    onResult: (Boolean) -> Unit
) {
    val word = card.front
    val nonFixedCount = remember(card.id) { word.count { it != ' ' && it != '-' } }
    val chunkSize = remember(card.id) { scrambleChunkSize(nonFixedCount) }

    val slots = remember(card.id) { buildScrambleSlots(word, chunkSize) }
    val pool = remember(card.id) { buildScramblePool(word, chunkSize) }
    val nonFixedSlotCount = remember(card.id) { slots.count { !it.isFixed } }

    // answerSlots[slotIndex] = pool list index, or -1 if empty
    val answerSlots = remember(card.id) { mutableStateListOf(*Array(nonFixedSlotCount) { -1 }) }
    var revealed by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }

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
        isCorrect = built.equals(word, ignoreCase = true)
        revealed = true
    }

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

    // Tile dimensions scale down for larger chunks so they still look balanced
    val tileFontSp = if (chunkSize == 1) 16 else 14
    val poolFontSp = if (chunkSize == 1) 18 else 15
    val tileMinWidthDp = if (chunkSize == 1) 32 else chunkSize * 14
    val emptySlotWidthDp = if (chunkSize == 1) 32 else chunkSize * 14

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            if (chunkSize == 1) "Собери слово из букв" else "Собери слово из слогов",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))

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
                card.back,
                fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Answer field ──────────────────────────────────────────────────────
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

        if (revealed && !isCorrect) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Правильно: ${card.front}",
                fontSize = 13.sp, color = FdRed,
                fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Pool ─────────────────────────────────────────────────────────────
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            pool.forEachIndexed { poolListIdx, item ->
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
                            item.text.uppercase(),
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
                onClick = { onResult(isCorrect) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                color = FdPrimary, shadowColor = FdPrimaryDark, shape = RoundedCornerShape(14.dp)
            ) {
                Text("Далее →", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
