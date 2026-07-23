package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import kotlinx.coroutines.delay
import uz.nodirbek.flashcardsapp.ui.theme.*
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel

private data class MatchTile(val id: String, val text: String, val isFront: Boolean)

@Composable
fun MatchScreen(
    deckId: String,
    viewModel: HomeViewModel,
    onBackClick: () -> Unit,
    onFinished: (seconds: Int, isNewRecord: Boolean, bestSeconds: Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val cards = remember(uiState.cards, deckId) {
        uiState.cards.filter { it.deckId == deckId }.shuffled().take(6)
    }

    if (cards.isEmpty()) {
        Box(Modifier.fillMaxSize().background(FdBackground), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎯", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text("Нет карточек", fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = FdText)
                Spacer(Modifier.height(20.dp))
                uz.nodirbek.flashcardsapp.ui.components.PressButton(
                    onClick = onBackClick, modifier = Modifier.width(160.dp).height(48.dp),
                    color = FdPrimary, shadowColor = FdPrimaryDark, shape = RoundedCornerShape(12.dp)
                ) { Text("Назад", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White) }
            }
        }
        return
    }

    val tiles = remember(cards) {
        val fronts = cards.map { MatchTile(it.id, it.front, isFront = true) }
        val backs = cards.map { MatchTile(it.id, it.back, isFront = false) }
        (fronts + backs).shuffled()
    }

    var selectedId by remember { mutableStateOf<String?>(null) }
    var selectedFront by remember { mutableStateOf<Boolean?>(null) }
    var matched by remember { mutableStateOf(emptySet<String>()) }
    var wrongIds by remember { mutableStateOf(emptySet<String>()) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }

    // Timer
    LaunchedEffect(done) {
        if (!done) {
            while (true) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    // Wrong flash reset
    LaunchedEffect(wrongIds) {
        if (wrongIds.isNotEmpty()) {
            delay(600)
            wrongIds = emptySet()
            selectedId = null
            selectedFront = null
        }
    }

    // Done check
    LaunchedEffect(matched) {
        if (matched.size == cards.size) {
            done = true
            onFinished(elapsedSeconds, false, elapsedSeconds)
        }
    }

    Scaffold(containerColor = FdBackground) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Top bar
            Surface(color = FdSurface, shadowElevation = 0.dp) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) { Icon(Icons.Default.Close, null, tint = FdText) }
                        Text("Совпадение", fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = FdText, modifier = Modifier.weight(1f))
                        val mins = elapsedSeconds / 60
                        val secs = elapsedSeconds % 60
                        Box(
                            Modifier.clip(RoundedCornerShape(8.dp)).background(FdPrimaryLight)
                                .border(1.5.dp, FdPrimary.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "%d:%02d".format(mins, secs),
                                fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FdPrimary
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    LinearProgressIndicator(
                        progress = if (cards.isNotEmpty()) matched.size.toFloat() / cards.size else 0f,
                        modifier = Modifier.fillMaxWidth(),
                        color = FdGreen, trackColor = FdBorder
                    )
                }
            }

            // Tile grid (2 columns) built with Row/Column to avoid LazyVerticalGrid API issues
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                tiles.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { tile ->
                            val isMatched = tile.id in matched
                            val isSelected = selectedId == tile.id && selectedFront == tile.isFront
                            val isWrong = tile.id in wrongIds && isSelected

                            val bgColor = when {
                                isMatched -> FdGreenLight
                                isWrong -> FdRedLight
                                isSelected -> FdPrimaryLight
                                else -> FdSurface
                            }
                            val borderColor = when {
                                isMatched -> FdGreen
                                isWrong -> FdRed
                                isSelected -> FdPrimary
                                else -> FdBorder
                            }

                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(bgColor)
                                    .border(2.5.dp, borderColor, RoundedCornerShape(14.dp))
                                    .then(
                                        if (!isMatched) Modifier.clickable {
                                            if (isSelected) return@clickable
                                            val prevId = selectedId
                                            val prevFront = selectedFront
                                            if (prevId == null) {
                                                selectedId = tile.id
                                                selectedFront = tile.isFront
                                            } else {
                                                if (prevId == tile.id && prevFront != tile.isFront) {
                                                    matched = matched + tile.id
                                                    selectedId = null
                                                    selectedFront = null
                                                } else {
                                                    wrongIds = setOf(prevId, tile.id)
                                                    selectedId = tile.id
                                                    selectedFront = tile.isFront
                                                }
                                            }
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isMatched) {
                                    Text("✓", fontSize = 20.sp, color = FdGreen)
                                } else {
                                    Text(
                                        tile.text,
                                        fontFamily = OutfitFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) FdPrimary else FdText,
                                        textAlign = TextAlign.Center,
                                        maxLines = 3,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                        // Fill empty slot if odd number of tiles in last row
                        if (row.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
