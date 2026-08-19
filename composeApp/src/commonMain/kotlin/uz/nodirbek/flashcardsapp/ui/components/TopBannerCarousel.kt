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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import uz.nodirbek.flashcardsapp.ui.theme.OutfitFamily
import kotlin.math.absoluteValue

/** Пауза между автоматическими перелистываниями баннеров. */
const val BANNER_AUTO_SCROLL_MS = 5_000L

/** Высота карточки баннера — примерно 17% высоты экрана телефона. */
val BannerHeight = 150.dp

/** Размер левой иконки/индикатора внутри баннера. */
val BannerLeadingSize = 56.dp

/** Кнопка справа внутри баннера. */
data class BannerAction(
    val label: String,
    val color: Color,
    val shadowColor: Color,
    val textColor: Color = Color.White,
    val onClick: () -> Unit
)

/**
 * Оформление баннера.
 *
 * [Surface] — обычная карточка со статистикой: фон поверхности и рамка.
 * [Promo] — рекламный баннер: градиент во всю карточку, белый текст, без рамки.
 */
sealed interface BannerStyle {
    data object Surface : BannerStyle
    data class Promo(val from: Color, val to: Color) : BannerStyle
}

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
    val style: BannerStyle = BannerStyle.Surface,
    val action: BannerAction? = null,
    val onClick: (() -> Unit)? = null,
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

    // Отсчёт начинается заново каждый раз, когда пейджер останавливается на
    // странице — и после автопрокрутки, и после ручного свайпа, так что баннер
    // всегда живёт полные 5 секунд.
    //
    // Важно: settledPage меняется только когда скролл завершился. Ключом
    // LaunchedEffect это быть не может — currentPage/isScrollInProgress
    // обновляются в начале анимации и отменяют её же вместе с эффектом.
    if (banners.size > 1) {
        LaunchedEffect(pagerState, banners.size) {
            snapshotFlow { pagerState.settledPage }.collectLatest {
                delay(autoScrollMillis)
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % banners.size)
            }
        }
    }

    Column(modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 10.dp),
            pageSpacing = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) { page ->
            // Соседние баннеры чуть уменьшены и приглушены — активный читается первым.
            val distance = (
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                ).absoluteValue.coerceIn(0f, 1f)
            BannerCard(
                banner = banners[page],
                modifier = Modifier.graphicsLayer {
                    val scale = lerp(0.92f, 1f, 1f - distance)
                    scaleX = scale
                    scaleY = scale
                    alpha = lerp(0.55f, 1f, 1f - distance)
                }
            )
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
private fun BannerCard(banner: HomeBanner, modifier: Modifier = Modifier) {
    val style = banner.style
    val shape = RoundedCornerShape(20.dp)
    val titleColor = when (style) {
        is BannerStyle.Promo -> Color.White
        BannerStyle.Surface -> MaterialTheme.colorScheme.onSurface
    }
    val subtitleColor = banner.subtitleColor ?: when (style) {
        is BannerStyle.Promo -> Color.White.copy(alpha = 0.85f)
        BannerStyle.Surface -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val background = when (style) {
        is BannerStyle.Promo -> Modifier.background(
            Brush.horizontalGradient(listOf(style.from, style.to))
        )
        BannerStyle.Surface -> Modifier
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, shape)
    }

    Row(
        modifier
            .fillMaxWidth()
            .height(BannerHeight)
            .clip(shape)
            .then(background)
            .then(
                if (banner.onClick != null) Modifier.clickable(onClick = banner.onClick)
                else Modifier
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        banner.leading()
        Spacer(Modifier.width(14.dp))
        // Текст и кнопка идут колонкой: на высокой карточке строка выглядела
        // бы полупустой, а кнопка справа зажимала подпись до пары слов.
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                banner.title,
                fontFamily = OutfitFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = titleColor,
                maxLines = 1
            )
            Text(
                banner.subtitle,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = subtitleColor,
                // С кнопкой на подпись остаётся две строки, без неё — три:
                // иначе содержимое не помещается в BannerHeight.
                maxLines = if (banner.action != null) 2 else 3,
                modifier = Modifier.padding(top = 4.dp)
            )
            banner.action?.let { action ->
                Spacer(Modifier.height(12.dp))
                PressButton(
                    onClick = action.onClick,
                    modifier = Modifier.height(40.dp),
                    color = action.color,
                    shadowColor = action.shadowColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        action.label,
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = action.textColor,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
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

/** Квадратная эмодзи-плашка для [HomeBanner.leading]. */
@Composable
fun BannerEmojiBadge(emoji: String, bgColor: Color, onClick: (() -> Unit)? = null) {
    Box(
        Modifier
            .size(BannerLeadingSize)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 27.sp)
    }
}
