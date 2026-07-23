package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.nodirbek.flashcardsapp.domain.model.Card
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.theme.*
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel

@Composable
fun FlashcardsScreen(
    deckId: String,
    viewModel: HomeViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val cards = remember(uiState.cards, deckId) { uiState.cards.filter { it.deckId == deckId } }
    val shuffledCards = remember(cards) { cards.shuffled() }
    var displayCards by remember(shuffledCards) { mutableStateOf(shuffledCards) }

    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(420),
        label = "flip"
    )

    if (cards.isEmpty()) {
        Box(Modifier.fillMaxSize().background(FdBackground), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📋", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text("Нет карточек", fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = FdText)
                Spacer(Modifier.height(8.dp))
                Text("Добавьте карточки в колоду", fontSize = 13.sp, color = FdTextSub)
                Spacer(Modifier.height(24.dp))
                PressButton(onClick = onBackClick, modifier = Modifier.width(160.dp).height(48.dp),
                    color = FdPrimary, shadowColor = FdPrimaryDark, shape = RoundedCornerShape(12.dp)) {
                    Text("Назад", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }
            }
        }
        return
    }

    val card = displayCards.getOrNull(currentIndex)

    Scaffold(containerColor = FdBackground) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // ── Top bar ───────────────────────────────────────
            Surface(color = FdSurface, shadowElevation = 0.dp) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, null, tint = FdText)
                        }
                        Text(
                            "Карточки ${if (displayCards.isNotEmpty()) currentIndex + 1 else 0}/${displayCards.size}",
                            fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FdText,
                            modifier = Modifier.weight(1f)
                        )
                        // Shuffle button
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, FdBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    displayCards = cards.shuffled()
                                    currentIndex = 0
                                    isFlipped = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("🔀", fontSize = 14.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    // Progress bar
                    LinearProgressIndicator(
                        progress = if (displayCards.isNotEmpty()) (currentIndex + 1f) / displayCards.size else 0f,
                        modifier = Modifier.fillMaxWidth(),
                        color = FdPrimary,
                        trackColor = FdBorder
                    )
                }
            }

            // ── Card ──────────────────────────────────────────
            if (card != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(20.dp)
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationY = rotation
                                cameraDistance = 12f * density
                            }
                            .clickable { isFlipped = !isFlipped }
                    ) {
                        if (rotation <= 90f) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(FdSurface)
                                    .border(2.dp, FdBorder, RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                    Box(Modifier.clip(CircleShape).background(FdPrimaryLight).padding(horizontal = 12.dp, vertical = 4.dp)) {
                                        Text("ЛИЦО", fontSize = 10.sp, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, color = FdPrimary)
                                    }
                                    Spacer(Modifier.height(20.dp))
                                    Text(card.front, fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp,
                                        textAlign = TextAlign.Center, color = FdText)
                                    Spacer(Modifier.height(20.dp))
                                    Text("Нажмите для переворота", fontSize = 11.sp, color = FdTextSub)
                                }
                            }
                        } else {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(FdText)
                                    .graphicsLayer { rotationY = 180f },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                    Box(Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                                        Text("ОБОРОТ", fontSize = 10.sp, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                                    }
                                    Spacer(Modifier.height(20.dp))
                                    Text(card.back, fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp,
                                        textAlign = TextAlign.Center, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // ── Navigation ────────────────────────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PressButton(
                        onClick = {
                            if (currentIndex > 0) {
                                currentIndex--
                                isFlipped = false
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        color = FdSurface,
                        shadowColor = FdBorder,
                        shape = RoundedCornerShape(14.dp),
                        enabled = currentIndex > 0
                    ) {
                        Text("← Назад", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FdText)
                    }
                    PressButton(
                        onClick = {
                            if (currentIndex < displayCards.size - 1) {
                                currentIndex++
                                isFlipped = false
                            } else {
                                onBackClick()
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        color = FdPrimary,
                        shadowColor = FdPrimaryDark,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            if (currentIndex < displayCards.size - 1) "Вперёд →" else "Готово",
                            fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White
                        )
                    }
                }
            }
        }
    }
}
