package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
fun MatchDoneScreen(
    timeSeconds: Int,
    isNewRecord: Boolean,
    bestSeconds: Int,
    onPlayAgain: () -> Unit,
    onDone: () -> Unit
) {
    val mins = timeSeconds / 60
    val secs = timeSeconds % 60
    val timeStr = "%d:%02d".format(mins, secs)

    Column(
        Modifier
            .fillMaxSize()
            .background(FdPrimary)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isNewRecord) {
            Text("🏆", fontSize = 60.sp)
            Spacer(Modifier.height(8.dp))
            Text("Новый рекорд!", fontFamily = OutfitFamily, fontWeight = FontWeight.Black, fontSize = 28.sp, color = Color.White)
        } else {
            Text("🎯", fontSize = 60.sp)
            Spacer(Modifier.height(8.dp))
            Text("Завершено!", fontFamily = OutfitFamily, fontWeight = FontWeight.Black, fontSize = 28.sp, color = Color.White)
        }
        Spacer(Modifier.height(28.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MatchStatBox(timeStr, "Время", Modifier.weight(1f))
            if (bestSeconds > 0) {
                val bMins = bestSeconds / 60
                val bSecs = bestSeconds % 60
                MatchStatBox("%d:%02d".format(bMins, bSecs), "Рекорд", Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(36.dp))

        PressButton(
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            color = Color.White, shadowColor = FdPrimaryDark, shape = RoundedCornerShape(14.dp)
        ) {
            Text("Сыграть снова", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FdPrimary)
        }
        Spacer(Modifier.height(12.dp))
        PressButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            color = Color.White.copy(alpha = 0.15f),
            shadowColor = Color.White.copy(alpha = 0.25f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Готово", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
        }
    }
}

@Composable
private fun MatchStatBox(value: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
        }
    }
}
