package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // no-op на iOS пока нет свайп-назад интеграции
}
