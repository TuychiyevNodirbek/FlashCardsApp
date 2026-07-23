package uz.nodirbek.flashcardsapp.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
    BottomNavItem(Screen.Home, "Главная", R.drawable.ic_nav_home, R.drawable.ic_nav_home_filled),
    BottomNavItem(Screen.Stats, "Статистика", R.drawable.ic_nav_stats, R.drawable.ic_nav_stats_filled),
    BottomNavItem(Screen.Settings, "Настройки", R.drawable.ic_nav_settings, R.drawable.ic_nav_settings_filled)
)

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = androidx.compose.ui.unit.Dp.Hairline
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.screen.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != item.screen.route) {
                        navController.navigate(item.screen.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(
                            if (selected) item.iconSelectedRes else item.iconRes
                        ),
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF4255FF),
                    selectedTextColor = Color(0xFF4255FF),
                    unselectedIconColor = Color(0xFF6B6B80),
                    unselectedTextColor = Color(0xFF6B6B80),
                    indicatorColor = Color(0xFFEEF0FF)
                )
            )
        }
    }
}
