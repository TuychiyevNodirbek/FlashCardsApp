package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.theme.*

@Composable
fun TestResultsScreen(
    results: List<TestResult> = emptyList(),
    onDone: () -> Unit,
    onRepeatMistakes: () -> Unit
) {
    val correct = results.count { it.isCorrect }
    val total = results.size
    val accuracy = if (total > 0) correct.toFloat() / total else 0f
    val mistakes = results.filter { !it.isCorrect }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Score header
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(100.dp).clip(CircleShape)
                            .background(
                                when {
                                    accuracy >= 0.8f -> FdGreen
                                    accuracy >= 0.5f -> FdOrange
                                    else -> FdRed
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${(accuracy * 100).toInt()}%",
                            fontFamily = OutfitFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        when {
                            accuracy >= 0.8f -> "Отлично!"
                            accuracy >= 0.5f -> "Неплохо!"
                            else -> "Нужно практиковаться"
                        },
                        fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("$correct из $total правильных", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))

                    // Stat row
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ResultStatBox("$correct", "Правильно", FdGreen, Modifier.weight(1f))
                        ResultStatBox("${total - correct}", "Ошибок", FdRed, Modifier.weight(1f))
                        ResultStatBox("$total", "Всего", FdPrimary, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }

            // Mistakes
            if (mistakes.isNotEmpty()) {
                item {
                    Text(
                        "Ошибки",
                        fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                }
                items(mistakes) { result ->
                    Column {
                        Box(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(FdRedLight)
                                .border(1.5.dp, FdRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(result.card.front, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.height(4.dp))
                                Text("Ваш ответ: ${result.userAnswer}", fontSize = 12.sp, color = FdRed)
                                Text("Правильно: ${result.card.back}", fontSize = 12.sp, color = FdGreen)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }

            // Buttons
            item {
                if (mistakes.isNotEmpty()) {
                    PressButton(
                        onClick = onRepeatMistakes,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        color = FdOrange, shadowColor = FdOrangeDark, shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            "Повторить ошибки (${mistakes.size})",
                            fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
                PressButton(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    color = FdPrimary, shadowColor = FdPrimaryDark, shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Готово", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ResultStatBox(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = color)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TestResultsScreenPreview() {
    FlashCardsAppTheme {
        fun previewCard(n: Int) = uz.nodirbek.flashcardsapp.domain.model.Card(
            id = "$n", front = "Question $n", back = "Answer $n", dueDate = "", createdAt = 0L
        )
        val results = listOf(
            TestResult(previewCard(1), "Answer 1", true),
            TestResult(previewCard(2), "Wrong 2", false),
            TestResult(previewCard(3), "Answer 3", true)
        )
        TestResultsScreen(results = results, onDone = {}, onRepeatMistakes = {})
    }
}
