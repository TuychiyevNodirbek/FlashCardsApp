package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.nodirbek.flashcardsapp.domain.model.DailyStats
import uz.nodirbek.flashcardsapp.ui.theme.*
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

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

            // Level + XP tile
            item {
                val xpInLevel = (uiState.xp % 100).toInt()
                val xpProgress = (xpInLevel / 100f).coerceIn(0f, 1f)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFFF3B0))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⭐", fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Уровень ${uiState.level}",
                                fontFamily = OutfitFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color(0xFFA07800)
                            )
                            Text(
                                "${uiState.xp} XP всего",
                                fontSize = 12.sp,
                                color = Color(0xFFA07800).copy(alpha = 0.75f)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { xpProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                            color = Color(0xFFE6A000),
                            trackColor = Color(0xFFE6A000).copy(alpha = 0.25f)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "$xpInLevel / 100 XP до следующего уровня",
                            fontSize = 11.sp,
                            color = Color(0xFFA07800).copy(alpha = 0.7f)
                        )
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
                    Text("АКТИВНОСТЬ", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    MonthActivityCalendar(allStats = allStats, modifier = Modifier.fillMaxWidth())
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
private fun MonthActivityCalendar(allStats: List<DailyStats>, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val statMap = remember(allStats) { allStats.associateBy { it.date } }
    val maxReviews = remember(allStats) { allStats.maxOfOrNull { it.reviewCount }?.coerceAtLeast(1) ?: 1 }
    val emptyCellColor = MaterialTheme.colorScheme.surfaceVariant

    var yearMonth by remember { mutableStateOf(YearMonth.now()) }
    val currentYearMonth = YearMonth.now()
    val isCurrentMonth = yearMonth == currentYearMonth

    val monthKey = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    val monthStats = remember(allStats, yearMonth) {
        allStats.filter { it.date.startsWith(monthKey) }
    }
    val totalReviews = monthStats.sumOf { it.reviewCount }
    val totalCorrect = monthStats.sumOf { it.correctCount }
    val accuracy = if (totalReviews > 0) totalCorrect * 100 / totalReviews else 0

    val firstDay = yearMonth.atDay(1)
    val offset = firstDay.dayOfWeek.value - 1 // Пн=0 … Вс=6
    val daysInMonth = yearMonth.lengthOfMonth()
    val totalCells = offset + daysInMonth
    val rows = (totalCells + 6) / 7

    val monthLabel = yearMonth.month
        .getDisplayName(TextStyle.FULL_STANDALONE, Locale("ru"))
        .replaceFirstChar { it.uppercaseChar() }
    val weekdays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    Column(modifier) {
        // ── Навигация ────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { yearMonth = yearMonth.minusMonths(1) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Предыдущий месяц",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                "$monthLabel ${yearMonth.year}",
                fontFamily = OutfitFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = { yearMonth = yearMonth.plusMonths(1) },
                enabled = !isCurrentMonth
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Следующий месяц",
                    tint = if (!isCurrentMonth) MaterialTheme.colorScheme.onSurface
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── Хедер дней недели ────────────────────────────────────────
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { day ->
                Text(
                    day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // ── Сетка дней ───────────────────────────────────────────────
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    val day = cellIndex - offset + 1
                    val isValidDay = day in 1..daysInMonth
                    val date = if (isValidDay) yearMonth.atDay(day) else null
                    val isFuture = date != null && date.isAfter(today)
                    val stat = if (isValidDay && !isFuture) statMap[date!!.format(fmt)] else null

                    val intensity = when {
                        !isValidDay || isFuture -> -1f
                        stat != null -> (stat.reviewCount.toFloat() / maxReviews).coerceIn(0f, 1f)
                        else -> 0f
                    }

                    val bgColor = when {
                        intensity < 0f -> Color.Transparent
                        intensity == 0f -> emptyCellColor
                        intensity < 0.25f -> FdPrimary.copy(alpha = 0.2f)
                        intensity < 0.5f -> FdPrimary.copy(alpha = 0.4f)
                        intensity < 0.75f -> FdPrimary.copy(alpha = 0.65f)
                        else -> FdPrimary
                    }

                    val isToday = date == today

                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(bgColor)
                            .then(
                                if (isToday) Modifier.border(1.5.dp, FdPrimary, CircleShape)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isValidDay) {
                            Text(
                                "$day",
                                fontSize = 11.sp,
                                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Normal,
                                color = when {
                                    isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                    intensity > 0.5f -> Color.White
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
            if (row < rows - 1) Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(12.dp))

        // ── Итоги месяца ─────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (totalReviews > 0) {
                Text(
                    "$totalReviews повторений • $accuracy% точность",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "Нет активности в этом месяце",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Легенда ───────────────────────────────────────────────────
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
