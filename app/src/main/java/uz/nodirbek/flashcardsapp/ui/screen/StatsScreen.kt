package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

private enum class StatsRange(val label: String) { WEEK("7 дн"), MONTH("30 дн"), ALL("Всё") }

@Composable
fun StatsScreen(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var range by remember { mutableStateOf(StatsRange.WEEK) }

    val allStats = uiState.allStats
    val rangeStats = remember(range, allStats) {
        when (range) {
            StatsRange.WEEK -> fillDays(allStats, 7)
            StatsRange.MONTH -> fillDays(allStats, 30)
            StatsRange.ALL -> if (allStats.size > 30) allStats else fillDays(allStats, 30)
        }
    }

    val totalReviewed = rangeStats.sumOf { it.reviewCount }
    val totalCorrect = rangeStats.sumOf { it.correctCount }
    val accuracy = if (totalReviewed > 0) (totalCorrect * 100 / totalReviewed) else 0
    val learnedCount = uiState.cards.count { it.reps > 0 && it.interval >= 7 }
    val inProgressCount = uiState.cards.count { it.reps > 0 && it.interval < 7 }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Статистика", fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.5.dp)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Hero tiles: streak + record
            item {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Серия — filled primary
                    Column(
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(FdPrimary)
                            .padding(16.dp)
                    ) {
                        Text("СЕРИЯ", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                        Spacer(Modifier.height(4.dp))
                        Text("${uiState.streak} 🔥", fontFamily = OutfitFamily, fontWeight = FontWeight.Black, fontSize = 34.sp, color = Color.White)
                        Spacer(Modifier.height(2.dp))
                        Text("текущая серия дней", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                    // Рекорд — outlined
                    Column(
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Text("РЕКОРД", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("${uiState.streakRecord}", fontFamily = OutfitFamily, fontWeight = FontWeight.Black, fontSize = 34.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(2.dp))
                        Text("дней подряд", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // Reviews chart with range selector
            item {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Повторения", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Row(
                            Modifier.clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        ) {
                            StatsRange.values().forEach { r ->
                                val sel = range == r
                                Box(
                                    Modifier
                                        .background(if (sel) FdPrimary else Color.Transparent)
                                        .clickable { range = r }
                                        .padding(horizontal = 9.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        r.label,
                                        fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                                        color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    RangeBarChart(
                        stats = rangeStats,
                        showWeekdayLabels = range == StatsRange.WEEK,
                        modifier = Modifier.fillMaxWidth().height(96.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
            }

            // 3-tile grid: learned / in progress / accuracy
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniStatTile("$learnedCount", "выучено", FdPrimary, Modifier.weight(1f))
                    MiniStatTile("$inProgressCount", "в работе", MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                    MiniStatTile("$accuracy%", "точность", FdGreen, Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
            }

            // Year heatmap
            item {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Text("АКТИВНОСТЬ ЗА ГОД", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    YearHeatmap(allStats = allStats, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun MiniStatTile(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontFamily = OutfitFamily, fontWeight = FontWeight.Black, fontSize = 22.sp, color = color)
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun RangeBarChart(stats: List<DailyStats>, showWeekdayLabels: Boolean, modifier: Modifier = Modifier) {
    val maxCount = (stats.maxOfOrNull { it.reviewCount } ?: 0).coerceAtLeast(1)
    val emptyColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val weekdays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    Column(modifier) {
        Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            stats.forEach { day ->
                Canvas(Modifier.weight(1f).fillMaxHeight()) {
                    val h = if (day.reviewCount == 0) 2.dp.toPx()
                    else (day.reviewCount.toFloat() / maxCount * size.height).coerceAtLeast(2.dp.toPx())
                    drawRoundRect(
                        color = if (day.reviewCount == 0) emptyColor else FdPrimary,
                        topLeft = Offset(0f, size.height - h),
                        size = Size(size.width, h),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                }
            }
        }
        Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            stats.forEachIndexed { idx, day ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    val label = when {
                        showWeekdayLabels -> {
                            val dow = runCatching { LocalDate.parse(day.date).dayOfWeek.value }.getOrDefault(idx + 1)
                            weekdays.getOrElse(dow - 1) { "" }
                        }
                        stats.size <= 31 -> if (idx % 5 == 0) day.date.takeLast(2) else ""
                        else -> ""
                    }
                    Text(label, fontSize = 9.sp, color = labelColor, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun YearHeatmap(allStats: List<DailyStats>, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val statMap = allStats.associateBy { it.date }

    val startDate = today.minusWeeks(51).with(java.time.DayOfWeek.MONDAY)
    val maxReviews = allStats.maxOfOrNull { it.reviewCount }?.coerceAtLeast(1) ?: 1
    val emptyCellColor = MaterialTheme.colorScheme.surfaceVariant

    val cellSize = 10.dp
    val gap = 2.dp

    Box(modifier.horizontalScroll(rememberScrollState())) {
        Canvas(
            Modifier.size(
                width = (cellSize + gap) * 52 + gap,
                height = (cellSize + gap) * 7 + gap
            )
        ) {
            val cellPx = cellSize.toPx()
            val gapPx = gap.toPx()

            for (week in 0 until 52) {
                for (dayOfWeek in 0 until 7) {
                    val date = startDate.plusDays((week * 7 + dayOfWeek).toLong())
                    if (date.isAfter(today)) continue
                    val stat = statMap[date.format(fmt)]
                    val intensity = if (stat != null) (stat.reviewCount.toFloat() / maxReviews).coerceIn(0f, 1f) else 0f

                    val color = when {
                        intensity == 0f -> emptyCellColor
                        intensity < 0.25f -> FdPrimary.copy(alpha = 0.2f)
                        intensity < 0.5f -> FdPrimary.copy(alpha = 0.4f)
                        intensity < 0.75f -> FdPrimary.copy(alpha = 0.65f)
                        else -> FdPrimary
                    }

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(gapPx + week * (cellPx + gapPx), gapPx + dayOfWeek * (cellPx + gapPx)),
                        size = Size(cellPx, cellPx),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Меньше", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        listOf(0f, 0.2f, 0.45f, 0.7f, 1f).forEach { alpha ->
            Box(
                Modifier.size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (alpha == 0f) emptyCellColor else FdPrimary.copy(alpha = alpha))
            )
            Spacer(Modifier.width(2.dp))
        }
        Text("Больше", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Continuous series of the last [days] days, zero-filled where there is no data. */
private fun fillDays(allStats: List<DailyStats>, days: Int): List<DailyStats> {
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val map = allStats.associateBy { it.date }
    val today = LocalDate.now()
    return (days - 1 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong()).format(fmt)
        map[date] ?: DailyStats(date = date, reviewCount = 0, correctCount = 0)
    }
}
