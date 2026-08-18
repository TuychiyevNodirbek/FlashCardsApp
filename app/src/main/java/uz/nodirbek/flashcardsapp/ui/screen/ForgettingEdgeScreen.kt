package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.nodirbek.flashcardsapp.shared.scheduler.GetForgettingEdgeCardsUseCase
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.theme.*
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel
import uz.nodirbek.flashcardsapp.ui.components.UnifiedAppBar

@Composable
fun ForgettingEdgeScreen(
    deckId: String,
    viewModel: HomeViewModel,
    onBackClick: () -> Unit,
    onStartReview: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val forgettingCards = remember(uiState.cards, deckId) {
        val deckCards = uiState.cards.filter { it.deckId == deckId }
        GetForgettingEdgeCardsUseCase().invoke(deckCards)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            UnifiedAppBar(
                title = "Грань забывания",
                onBackClick = onBackClick,
                showBackButton = true,
                showDivider = true
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            if (forgettingCards.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✨", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Всё под контролем!", fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        Text("Нет карточек на грани забывания", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp)
                ) {
                    item {
                        // Summary header
                        Box(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(FdRedLight)
                                .border(1.5.dp, FdRed.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⚠️", fontSize = 24.sp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "${forgettingCards.size} карточек рискуют забыться",
                                        fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FdRed
                                    )
                                    Text(
                                        "Повторите их прямо сейчас",
                                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    items(forgettingCards) { item ->
                        ForgettingCardItem(item)
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // Bottom action
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
                    PressButton(
                        onClick = { onStartReview(deckId) },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        color = FdRed, shadowColor = FdRedDark, shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            "Повторить все (${forgettingCards.size})",
                            fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ForgettingCardItem(item: GetForgettingEdgeCardsUseCase.ForgettingEdgeCard) {
    val (urgencyColor, urgencyLabel) = when (item.urgency) {
        GetForgettingEdgeCardsUseCase.Urgency.HIGH -> FdRed to "Высокий"
        GetForgettingEdgeCardsUseCase.Urgency.MEDIUM -> FdOrange to "Средний"
        GetForgettingEdgeCardsUseCase.Urgency.LOW -> FdGreenDark to "Низкий"
    }
    val bgColor = when (item.urgency) {
        GetForgettingEdgeCardsUseCase.Urgency.HIGH -> FdRedLight
        GetForgettingEdgeCardsUseCase.Urgency.MEDIUM -> FdOrangeLight
        GetForgettingEdgeCardsUseCase.Urgency.LOW -> FdGreenLight
    }

    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.5.dp, urgencyColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.card.front, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text(item.card.back, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    Modifier.clip(CircleShape)
                        .background(urgencyColor.copy(alpha = 0.15f))
                        .border(1.dp, urgencyColor.copy(alpha = 0.4f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(urgencyLabel, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = urgencyColor)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${item.daysUntilForgotten}д",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
