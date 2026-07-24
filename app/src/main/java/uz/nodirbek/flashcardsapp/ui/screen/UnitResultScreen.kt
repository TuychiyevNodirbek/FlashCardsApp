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
fun UnitResultScreen(
    unitIndex: Int,
    correctAnswers: Int,
    totalAnswers: Int,
    hasNextUnit: Boolean,
    onBackToUnits: () -> Unit,
    onNextUnit: () -> Unit
) {
    val accuracy = if (totalAnswers == 0) 1f else correctAnswers.toFloat() / totalAnswers
    val pct = (accuracy * 100).toInt()
    val isPerfect = accuracy >= 0.9f

    Column(
        Modifier
            .fillMaxSize()
            .background(if (isPerfect) FdPrimary else MaterialTheme.colorScheme.background)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(if (isPerfect) "🏆" else "✅", fontSize = 64.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            if (isPerfect) "Идеально!" else "Юнит пройден!",
            fontFamily = OutfitFamily, fontWeight = FontWeight.Black, fontSize = 28.sp,
            color = if (isPerfect) Color.White else MaterialTheme.colorScheme.onSurface
        )
        Text(
            "Юнит ${unitIndex + 1}",
            fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            color = if (isPerfect) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        // Stats row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ResultStatBox(
                value = "$pct%",
                label = "Точность",
                color = if (isPerfect) Color.White else FdGreen,
                isPerfect = isPerfect,
                modifier = Modifier.weight(1f)
            )
            ResultStatBox(
                value = "$correctAnswers/$totalAnswers",
                label = "Верно",
                color = if (isPerfect) Color.White else FdPrimary,
                isPerfect = isPerfect,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(36.dp))

        if (hasNextUnit) {
            PressButton(
                onClick = onNextUnit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                color = if (isPerfect) Color.White else FdPrimary,
                shadowColor = if (isPerfect) FdPrimaryDark else FdPrimaryDark,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "Следующий юнит →",
                    fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = if (isPerfect) FdPrimary else Color.White
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        PressButton(
            onClick = onBackToUnits,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            color = if (isPerfect) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
            shadowColor = if (isPerfect) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outline,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                "К юнитам",
                fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                color = if (isPerfect) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ResultStatBox(
    value: String,
    label: String,
    color: Color,
    isPerfect: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (isPerfect) Color.White.copy(alpha = 0.15f) else color.copy(alpha = 0.1f)
    val border = if (isPerfect) Color.White.copy(alpha = 0.3f) else color.copy(alpha = 0.3f)
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(14.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = color)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, color = color.copy(alpha = 0.75f))
        }
    }
}
