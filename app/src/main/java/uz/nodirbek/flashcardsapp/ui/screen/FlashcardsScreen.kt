package uz.nodirbek.flashcardsapp.ui.screen

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.theme.*
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel
import uz.nodirbek.flashcardsapp.ui.theme.LocalIsDarkTheme

private fun PagerState.offsetForPage(page: Int) =
    (currentPage - page) + currentPageOffsetFraction

private fun PagerState.startOffsetForPage(page: Int) =
    offsetForPage(page).coerceAtLeast(0f)

@Composable
fun FlashcardsScreen(
    deckId: String,
    viewModel: HomeViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val cards = remember(uiState.cards, deckId) { uiState.cards.filter { it.deckId == deckId } }
    FlashcardsContent(
        cards = cards,
        ttsLang = uiState.ttsLang,
        ttsSpeed = uiState.ttsSpeed,
        onDone = onBackClick,
        onBackClick = onBackClick
    )
}

@Composable
fun FlashcardsContent(
    cards: List<uz.nodirbek.flashcardsapp.domain.model.Card>,
    ttsLang: String = "en",
    ttsSpeed: Float = 1f,
    onDone: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = LocalIsDarkTheme.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val ttsManager = remember { uz.nodirbek.flashcardsapp.tts.TtsManager(context) }
    DisposableEffect(Unit) { onDispose { ttsManager.shutdown() } }

    var displayCards by remember { mutableStateOf(cards.shuffled()) }
    val pagerState = rememberPagerState { displayCards.size }
    val coroutineScope = rememberCoroutineScope()
    val flippedPages = remember { mutableStateMapOf<Int, Boolean>() }

    val currentPage = pagerState.currentPage

    if (cards.isEmpty()) {
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📋", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text("Нет карточек", fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Text("Добавьте карточки в колоду", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                PressButton(onClick = onBackClick, modifier = Modifier.width(160.dp).height(48.dp),
                    color = FdPrimary, shadowColor = FdPrimaryDark, shape = RoundedCornerShape(12.dp)) {
                    Text("Назад", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }
            }
        }
        return
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 0.dp) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            "Карточки ${currentPage + 1}/${displayCards.size}",
                            fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .clickable {
                                    displayCards = cards.shuffled()
                                    coroutineScope.launch { pagerState.scrollToPage(0) }
                                    flippedPages.clear()
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("🔀", fontSize = 14.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    LinearProgressIndicator(
                        progress = if (displayCards.isNotEmpty()) (currentPage + 1f) / displayCards.size else 0f,
                        modifier = Modifier.fillMaxWidth(),
                        color = FdPrimary,
                        trackColor = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── Pager ────────────────────────────────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 28.dp),
                pageSpacing = 14.dp,
            ) { page ->
                val card = displayCards[page]
                val isFlipped = flippedPages[page] ?: false
                val rotation by animateFloatAsState(
                    targetValue = if (isFlipped) 180f else 0f,
                    animationSpec = tween(420),
                    label = "flip_$page"
                )

                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(vertical = 20.dp)
                        // Movie carousel effect
                        .graphicsLayer {
                            val startOffset = pagerState.startOffsetForPage(page)
                            translationX = size.width * (startOffset * .99f)
                            alpha = (2f - startOffset) / 2f
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val blur = (startOffset * 20f).coerceAtLeast(0.1f)
                                renderEffect = RenderEffect
                                    .createBlurEffect(blur, blur, Shader.TileMode.DECAL)
                                    .asComposeRenderEffect()
                            }
                            val scale = 1f - (startOffset * .1f)
                            scaleX = scale
                            scaleY = scale
                        }
                        // Flip animation (separate layer)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
                        .clickable { flippedPages[page] = !isFlipped }
                ) {
                    if (rotation <= 90f) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                Box(
                                    Modifier.clip(CircleShape)
                                        .background(if (isDarkTheme) Color(0xFF3D3A1A) else FdPrimaryLight)
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("ЛИЦО", fontSize = 10.sp, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color(0xFFD4AF37) else FdPrimary)
                                }
                                Spacer(Modifier.height(20.dp))
                                Text(card.front, fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp,
                                    textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.height(20.dp))
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(FdPrimaryLight)
                                        .clickable { ttsManager.speak(card.front, ttsLang, ttsSpeed) }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("🔊", fontSize = 16.sp)
                                }
                                Spacer(Modifier.height(12.dp))
                                Text("Нажмите для переворота", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            }
                        }
                    } else {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isDarkTheme) Color(0xFF0F0F14) else MaterialTheme.colorScheme.onSurface)
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

            // ── Navigation ───────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PressButton(
                    onClick = {
                        if (currentPage > 0) {
                            coroutineScope.launch { pagerState.animateScrollToPage(currentPage - 1) }
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowColor = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(14.dp),
                    enabled = currentPage > 0
                ) {
                    Text("← Назад", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                PressButton(
                    onClick = {
                        if (currentPage < displayCards.size - 1) {
                            coroutineScope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                        } else {
                            onDone()
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowColor = if (isDarkTheme) Color(0xFF2F3D7A) else FdPrimaryDark,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        if (currentPage < displayCards.size - 1) "Вперёд →" else "Готово",
                        fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White
                    )
                }
            }
        }
    }

}
