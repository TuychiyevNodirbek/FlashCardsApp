package uz.nodirbek.flashcardsapp.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import uz.nodirbek.flashcardsapp.ui.theme.LocalIsDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.hazeEffect
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
fun BottomNavBar(
    navController: NavController,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isDark = LocalIsDarkTheme.current

    // Liquid glass colors
    val tintColor = if (isDark)
        Color(0xFF1C1C1E).copy(alpha = 0.55f)
    else
        Color(0xFFFFFFFF).copy(alpha = 0.45f)

    val hazeStyle = HazeStyle(
        blurRadius = 24.dp,
        backgroundColor = Color.Transparent,
        tints = listOf(HazeTint(tintColor))
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 28.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(CircleShape)
                // Real backdrop blur via haze
                .hazeEffect(state = hazeState, style = hazeStyle)
                // Specular top highlight — light mode only
                .drawWithContent {
                    drawContent()
                    if (!isDark) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.60f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = size.height * 0.32f
                            )
                        )
                    }
                },
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
                    targetValue = if (selected) 1f else 0.90f,
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
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                            inclusive = false
                                        }
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
