package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.nodirbek.flashcardsapp.domain.model.DailyStats
import uz.nodirbek.flashcardsapp.ui.theme.*
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun StatsScreen(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    val last7 = uiState.last7DaysStats.ifEmpty { buildLast7DaysStats() }
    val allStats = uiState.allStats

    val totalReviewed = last7.sumOf { it.reviewCount }
    val totalCorrect = last7.sumOf { it.correctCount }
    val avgAccuracy = if (totalReviewed > 0) totalCorrect.toFloat() / totalReviewed else 0f
    val maxStreak = uiState.streak

    Scaffold(containerColor = FdBackground) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Header
            item {
                Surface(color = FdSurface, shadowElevation = 0.dp) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Статистика", fontFamily = OutfitFamily, fontWeight = FontWeight.Black, fontSize = 22.sp, color = FdText)
                        }
                        Divider(color = FdBorder, thickness = 1.5.dp)
                    }
                }
            }

            // Stat tiles
            item {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatTile("$totalReviewed", "За 7 дней", FdPrimary, Modifier.weight(1f))
                    StatTile("${(avgAccuracy * 100).toInt()}%", "Точность", FdGreen, Modifier.weight(1f))
                    StatTile("$maxStreak", "Серия 🔥", FdOrange, Modifier.weight(1f))
                    StatTile("${uiState.xp}", "XP ⚡", FdPurple, Modifier.weight(1f))
                }
                Spacer(Modifier.height(20.dp))
            }

            // Bar chart — last 7 days
            item {
                SectionCard(title = "Последние 7 дней") {
                    if (last7.all { it.reviewCount == 0 }) {
                        Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            Text("Нет данных", color = FdTextSub, fontSize = 13.sp)
                        }
                    } else {
                        ReviewBarChart(stats = last7, modifier = Modifier.fillMaxWidth().height(140.dp).padding(top = 8.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Year heatmap
            item {
                SectionCard(title = "Активность за год") {
                    YearHeatmap(allStats = allStats, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(16.dp))
            }

            // XP / level info
            item {
                SectionCard(title = "Уровень") {
                    val level = (uiState.xp / 100).toInt() + 1
                    val xpInLevel = uiState.xp % 100
                    Column(Modifier.padding(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Уровень $level", fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = FdText)
                            Text("${uiState.xp} XP", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FdPrimary)
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = xpInLevel / 100f,
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                            color = FdPrimary,
                            trackColor = FdBorder
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("${100 - xpInLevel} XP до следующего уровня", fontSize = 11.sp, color = FdTextSub)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        Text(title, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FdTextSub)
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(FdSurface)
                .border(1.5.dp, FdBorder, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.5.dp, color.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = color)
            Text(label, fontSize = 10.sp, color = FdTextSub, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun ReviewBarChart(stats: List<DailyStats>, modifier: Modifier = Modifier) {
    val maxCount = stats.maxOf { it.reviewCount }.coerceAtLeast(1)
    val barColor = FdPrimary
    val correctColor = FdGreen
    val dayLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    Row(modifier.padding(bottom = 4.dp), verticalAlignment = Alignment.Bottom) {
        stats.forEachIndexed { idx, day ->
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Canvas(Modifier.fillMaxWidth().weight(1f)) {
                    val barWidth = size.width * 0.55f
                    val x = (size.width - barWidth) / 2f
                    val totalHeight = (day.reviewCount.toFloat() / maxCount) * size.height
                    val correctHeight = if (day.reviewCount > 0)
                        (day.correctCount.toFloat() / day.reviewCount) * totalHeight
                    else 0f

                    if (totalHeight > 0) {
                        // wrong bar (full height, red-ish)
                        drawRoundRect(
                            color = barColor.copy(alpha = 0.25f),
                            topLeft = Offset(x, size.height - totalHeight),
                            size = Size(barWidth, totalHeight),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                        // correct bar
                        if (correctHeight > 0) {
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(x, size.height - correctHeight),
                                size = Size(barWidth, correctHeight),
                                cornerRadius = CornerRadius(4.dp.toPx())
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    dayLabels.getOrElse(idx) { "" },
                    fontSize = 10.sp,
                    color = FdTextSub,
                    fontFamily = OutfitFamily
                )
            }
        }
    }
}

@Composable
private fun YearHeatmap(allStats: List<DailyStats>, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val statMap = allStats.associateBy { it.date }

    // Build 52 weeks x 7 days grid starting from 52 weeks ago
    val startDate = today.minusWeeks(51).with(java.time.DayOfWeek.MONDAY)
    val maxReviews = allStats.maxOfOrNull { it.reviewCount }?.coerceAtLeast(1) ?: 1

    val cellSize = 12.dp
    val gap = 2.dp

    Box(modifier.horizontalScroll(rememberScrollState())) {
        Canvas(
            Modifier.size(
                width = (cellSize + gap) * 52 + gap,
                height = (cellSize + gap) * 7 + gap + 16.dp
            )
        ) {
            val cellPx = cellSize.toPx()
            val gapPx = gap.toPx()

            for (week in 0 until 52) {
                for (dayOfWeek in 0 until 7) {
                    val date = startDate.plusDays((week * 7 + dayOfWeek).toLong())
                    if (date.isAfter(today)) continue
                    val dateStr = date.format(fmt)
                    val stat = statMap[dateStr]
                    val intensity = if (stat != null && maxReviews > 0)
                        (stat.reviewCount.toFloat() / maxReviews).coerceIn(0f, 1f)
                    else 0f

                    val color = when {
                        intensity == 0f -> Color(0xFFE8E8F0)
                        intensity < 0.25f -> FdPrimary.copy(alpha = 0.3f)
                        intensity < 0.5f -> FdPrimary.copy(alpha = 0.55f)
                        intensity < 0.75f -> FdPrimary.copy(alpha = 0.75f)
                        else -> FdPrimary
                    }

                    val x = gapPx + week * (cellPx + gapPx)
                    val y = gapPx + dayOfWeek * (cellPx + gapPx)

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(cellPx, cellPx),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                }
            }
        }
    }

    // Legend
    Spacer(Modifier.height(8.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Меньше", fontSize = 10.sp, color = FdTextSub)
        Spacer(Modifier.width(4.dp))
        listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { alpha ->
            Box(
                Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (alpha == 0f) Color(0xFFE8E8F0) else FdPrimary.copy(alpha = alpha))
            )
            Spacer(Modifier.width(2.dp))
        }
        Text("Больше", fontSize = 10.sp, color = FdTextSub)
    }
}

private fun buildLast7DaysStats(): List<DailyStats> {
    val today = LocalDate.now()
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    return (6 downTo 0).map { offset ->
        DailyStats(date = today.minusDays(offset.toLong()).format(fmt), reviewCount = 0, correctCount = 0)
    }
}

private fun buildAllStats(): List<DailyStats> = emptyList()
