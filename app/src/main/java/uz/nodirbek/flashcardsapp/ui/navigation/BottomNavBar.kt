package uz.nodirbek.flashcardsapp.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import uz.nodirbek.flashcardsapp.R

private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    @DrawableRes val iconRes: Int,
    @DrawableRes val iconSelectedRes: Int
)

private val items = listOf(
    BottomNavItem(Screen.Home, "Колоды", R.drawable.ic_nav_home, R.drawable.ic_nav_home_filled),
    BottomNavItem(Screen.Stats, "Статистика", R.drawable.ic_nav_stats, R.drawable.ic_nav_stats_filled),
    BottomNavItem(Screen.Settings, "Настройки", R.drawable.ic_nav_settings, R.drawable.ic_nav_settings_filled)
)

@Composable
fun BottomNavBar(navController: NavController, modifier: Modifier = Modifier) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isDark = isSystemInDarkTheme()

    val glassBackground = if (isDark)
        Color(0xFF2C2C2E).copy(alpha = 0.82f)
    else
        Color(0xFFFFFFFF).copy(alpha = 0.72f)

    val borderTop = if (isDark)
        Color.White.copy(alpha = 0.25f)
    else
        Color.White.copy(alpha = 0.90f)

    val borderBottom = if (isDark)
        Color.White.copy(alpha = 0.05f)
    else
        Color.White.copy(alpha = 0.30f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 36.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(CircleShape)
                .background(glassBackground)
                .border(
                    width = Dp.Hairline,
                    brush = Brush.verticalGradient(
                        colors = listOf(borderTop, borderBottom)
                    ),
                    shape = CircleShape
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.screen.route

                val alpha by animateFloatAsState(
                    targetValue = if (selected) 1f else 0.38f,
                    label = "alpha_${item.label}"
                )
                val scale by animateFloatAsState(
                    targetValue = if (selected) 1f else 0.92f,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessLow,
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    ),
                    label = "scale_${item.label}"
                )

                Column(
                    modifier = Modifier
                        .scale(scale)
                        .alpha(alpha)
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(item.screen.route) {
                            detectTapGestures {
                                if (currentRoute != item.screen.route) {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (selected) item.iconSelectedRes else item.iconRes
                        ),
                        contentDescription = item.label,
                        tint = if (selected)
                            MaterialTheme.colorScheme.primary
                        else if (isDark)
                            Color.White
                        else
                            Color(0xFF3C3C3E)
                    )
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected)
                            MaterialTheme.colorScheme.primary
                        else if (isDark)
                            Color.White
                        else
                            Color(0xFF3C3C3E)
                    )
                }
            }
        }
    }
}
