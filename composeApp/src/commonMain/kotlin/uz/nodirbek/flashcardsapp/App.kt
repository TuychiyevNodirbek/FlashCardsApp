package uz.nodirbek.flashcardsapp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uz.nodirbek.flashcardsapp.ui.screen.OnboardingScreen
import uz.nodirbek.flashcardsapp.ui.theme.FlashCardsAppTheme

/**
 * Временная общая точка входа для iOS. NavGraph/AppContainer/HomeViewModel — androidMain-only
 * (завязаны на android.content.Context, WorkManager и т.д.), их перенос — Фаза 6, ещё не сделана.
 * Это лишь smoke-test-экран: подтверждает, что весь CMP-стек (тема, шрифты Outfit/Inter,
 * PressButton, HorizontalPager) реально рендерится на iOS-таргете.
 */
@Composable
fun App() {
    val darkTheme = isSystemInDarkTheme()
    var onboardingFinished by remember { mutableStateOf(false) }

    FlashCardsAppTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!onboardingFinished) {
                OnboardingScreen(onFinish = { onboardingFinished = true })
            } else {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Полный экран приложения (навигация, данные) появится в Фазе 6.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
