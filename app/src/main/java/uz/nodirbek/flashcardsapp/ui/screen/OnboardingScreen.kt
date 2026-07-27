package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.theme.OutfitFamily

private data class OnboardingPage(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val bgColor: Color
)

private val PAGES = listOf(
    OnboardingPage(
        emoji = "🃏",
        title = "Учи слова по юнитам",
        subtitle = "Каждый юнит — 6 упражнений подряд: карточки, матч, тест, аудио, анаграмма и письмо. Как в Duolingo, только для твоих слов.",
        bgColor = Color(0xFF4255FF)
    ),
    OnboardingPage(
        emoji = "🏆",
        title = "Прогресс виден сразу",
        subtitle = "Zigzag-путь по юнитам, ежедневная цель, серия дней и XP за каждый пройденный юнит. Учиться становится интересно.",
        bgColor = Color(0xFF1AA34A)
    ),
    OnboardingPage(
        emoji = "📦",
        title = "Делись колодами",
        subtitle = "Экспортируй колоду в файл .fdeck одним нажатием и отправь другу. Они откроют его прямо в приложении.",
        bgColor = Color(0xFFFF6B00)
    )
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == PAGES.size - 1

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
            val page = PAGES[index]
            Column(
                Modifier
                    .fillMaxSize()
                    .background(page.bgColor)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(page.emoji, fontSize = 80.sp)
                Spacer(Modifier.height(32.dp))
                Text(
                    page.title,
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    page.subtitle,
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        }

        // Dots + action buttons
        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(PAGES.size) { i ->
                    val active = pagerState.currentPage == i
                    val color by animateColorAsState(
                        if (active) Color.White else Color.White.copy(alpha = 0.4f),
                        animationSpec = tween(200), label = "dot$i"
                    )
                    Box(
                        Modifier
                            .height(8.dp)
                            .width(if (active) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            PressButton(
                onClick = {
                    if (isLastPage) onFinish()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                color = Color.White,
                shadowColor = Color.White.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (isLastPage) "Начать" else "Далее",
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = PAGES[pagerState.currentPage].bgColor
                )
            }

            if (!isLastPage) {
                Text(
                    "Пропустить",
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onFinish
                    )
                )
            }
        }
    }
}
