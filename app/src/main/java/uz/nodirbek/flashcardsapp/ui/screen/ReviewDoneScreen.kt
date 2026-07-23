package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.theme.*

@Composable
fun ReviewDoneScreen(
    sessionCount: Int,
    accuracy: Float,
    xpEarned: Int,
    streak: Int,
    onContinue: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(FdPrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 28.dp)
        ) {
            val emoji = when {
                accuracy >= 0.9f -> "🏆"
                accuracy >= 0.7f -> "🎉"
                accuracy >= 0.5f -> "👍"
                else -> "💪"
            }
            Text(emoji, fontSize = 72.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Сессия завершена!",
                fontFamily = OutfitFamily,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Отличная работа, продолжайте!",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(32.dp))

            // Stats row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DoneStatBox("$sessionCount", "Карточек", Modifier.weight(1f))
                DoneStatBox("${(accuracy * 100).toInt()}%", "Точность", Modifier.weight(1f))
                DoneStatBox("+$xpEarned", "XP ⚡", Modifier.weight(1f))
                DoneStatBox("🔥 $streak", "Серия", Modifier.weight(1f))
            }

            Spacer(Modifier.height(36.dp))

            PressButton(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                color = Color.White,
                shadowColor = Color(0xFFCCCCCC),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "Готово",
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = FdPrimary
                )
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
            .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(vertical = 14.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                value,
                fontFamily = OutfitFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = Color.White
            )
            Text(
                label,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}
