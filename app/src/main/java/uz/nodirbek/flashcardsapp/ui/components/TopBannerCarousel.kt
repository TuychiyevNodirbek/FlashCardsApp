package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import uz.nodirbek.flashcardsapp.ui.theme.OutfitFamily

/** Пауза между автоматическими перелистываниями баннеров. */
const val BANNER_AUTO_SCROLL_MS = 5_000L

/** Кнопка справа внутри баннера. */
data class BannerAction(
    val label: String,
    val color: Color,
    val shadowColor: Color,
    val onClick: () -> Unit
)

/**
 * Один баннер верхней карусели.
 *
 * @param id стабильный ключ — по нему pager не сбрасывается при обновлении данных
 * @param leading левая иконка/индикатор (46.dp)
 */
data class HomeBanner(
    val id: String,
    val title: String,
    val subtitle: String,
    val subtitleColor: Color? = null,
    val action: BannerAction? = null,
    val leading: @Composable () -> Unit
)

/**
 * Карусель баннеров в шапке экрана: автопрокрутка раз в [autoScrollMillis],
 * ручной свайп сбрасывает таймер. Индикатор-точки скрыт для одиночного баннера.
 */
@Composable
fun TopBannerCarousel(
    banners: List<HomeBanner>,
    modifier: Modifier = Modifier,
    autoScrollMillis: Long = BANNER_AUTO_SCROLL_MS
) {
    if (banners.isEmpty()) return

    val pagerState = rememberPagerState { banners.size }

    // Таймер перезапускается на каждой смене страницы и на время ручного свайпа,
    // поэтому после жеста баннер всегда живёт полные 5 секунд.
    if (banners.size > 1) {
        LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress, banners.size) {
            if (!pagerState.isScrollInProgress) {
                delay(autoScrollMillis)
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % banners.size)
            }
        }
    }

    Column(modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 14.dp),
            pageSpacing = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) { page ->
            BannerCard(banners[page])
        }
        if (banners.size > 1) {
            DotsIndicator(
                count = banners.size,
                current = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 6.dp)
            )
        } else {
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun BannerCard(banner: HomeBanner) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        banner.leading()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                banner.title,
                fontFamily = OutfitFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                banner.subtitle,
                fontSize = 12.sp,
                color = banner.subtitleColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        banner.action?.let { action ->
            Spacer(Modifier.width(10.dp))
            PressButton(
                onClick = action.onClick,
                modifier = Modifier.height(38.dp),
                color = action.color,
                shadowColor = action.shadowColor,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    action.label,
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun DotsIndicator(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(count) { index ->
            val active = index == current
            val width by animateDpAsState(
                targetValue = if (active) 18.dp else 6.dp,
                animationSpec = tween(220),
                label = "bannerDotWidth"
            )
            Box(
                Modifier
                    .width(width)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
            )
        }
    }
}

/** Круглая эмодзи-плашка для [HomeBanner.leading]. */
@Composable
fun BannerEmojiBadge(emoji: String, bgColor: Color, onClick: (() -> Unit)? = null) {
    Box(
        Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 22.sp)
    }
}
